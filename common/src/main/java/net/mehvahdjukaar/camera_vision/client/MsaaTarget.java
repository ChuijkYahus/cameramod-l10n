package net.mehvahdjukaar.camera_vision.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

/**
 * TextureTarget with automatic MSAA: draws into an MSAA FBO and resolves into the
 * inherited single-sample color texture on unbindWrite().
 */
public class MsaaTarget extends TextureTarget {

    // MSAA resources
    private int msFbo     = -1;
    private int msColorRb = -1;
    private int msDepthRb = -1;
    private final int samples;

    public MsaaTarget(int width, int height, boolean useDepth, boolean clearError) {
        super(width, height, useDepth, clearError); // builds single-sample FBO + textures
        this.samples = clampSamples(4);             // choose your default (e.g., 4, 8, 16)
        setupMsaa();                                // create MSAA FBO with renderbuffers
    }

    private int clampSamples(int requested) {
        int max = GL11.glGetInteger(GL30.GL_MAX_SAMPLES);
        int s = Math.max(0, Math.min(requested, max));
        return s <= 1 ? 0 : s; // 0 = disabled
    }

    private void setupMsaa() {
        if (samples == 0) return;

        // COLOR renderbuffer
        msColorRb = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, msColorRb);
        GL30.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, samples, GL11.GL_RGBA8, this.width, this.height);

        // DEPTH-STENCIL renderbuffer (if needed)
        if (this.useDepth) {
            msDepthRb = GL30.glGenRenderbuffers();
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, msDepthRb);
            GL30.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, samples, GL30.GL_DEPTH24_STENCIL8, this.width, this.height);
        }

        // MSAA FBO
        msFbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, msFbo);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_RENDERBUFFER, msColorRb);
        if (this.useDepth) {
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, msDepthRb);
        }

        // Ensure we read/draw the intended attachment on all drivers
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("MSAA FBO incomplete: 0x" + Integer.toHexString(status));
        }

        // Cleanup binds; enable MS rasterization (usually already enabled)
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glEnable(GL13.GL_MULTISAMPLE);
    }

    /** Bind for writing: route draws to the MSAA FBO; fall back to base if MSAA is disabled. */
    @Override
    public void bindWrite(boolean setViewport) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> this.bindWrite(setViewport));
            return;
        }

        if (samples > 0 && msFbo > 0) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, msFbo);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        } else {
            GlStateManager._glBindFramebuffer(36160 /*GL_FRAMEBUFFER*/, this.frameBufferId);
        }

        if (setViewport) {
            GlStateManager._viewport(0, 0, this.viewWidth, this.viewHeight);
        }
    }

    /**
     * Unbind for writing: resolve MSAA -> single-sample color texture, then unbind.
     * If you also need resolved depth/stencil into textures, add the respective bits in the blit mask.
     */
    @Override
    public void unbindWrite() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(this::unbindWrite);
            return;
        }

        if (samples > 0 && msFbo > 0) {
            // Resolve color: READ = MSAA FBO, DRAW = single-sample FBO that owns colorTextureId
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, msFbo);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.frameBufferId);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);

            GL30.glBlitFramebuffer(
                    0, 0, this.width, this.height,
                    0, 0, this.width, this.height,
                    GL11.GL_COLOR_BUFFER_BIT,
                    GL11.GL_NEAREST // ignored for MSAA resolves
            );

            // If you later need depth/stencil textures updated as well, uncomment:
            // GL30.glBlitFramebuffer(0, 0, this.width, this.height,
            //                        0, 0, this.width, this.height,
            //                        GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT,
            //                        GL11.GL_NEAREST);

            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
        }

        // Finally unbind any FBO
        GlStateManager._glBindFramebuffer(36160 /*GL_FRAMEBUFFER*/, 0);
    }

    /** Ensure MSAA resources are also deleted when base buffers are destroyed. */
    @Override
    public void destroyBuffers() {
        super.destroyBuffers(); // deletes colorTextureId/depthTextureId/frameBufferId
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(this::deleteMsaaObjects);
        } else {
            deleteMsaaObjects();
        }
    }

    private void deleteMsaaObjects() {
        if (msDepthRb > 0) { GL30.glDeleteRenderbuffers(msDepthRb); msDepthRb = -1; }
        if (msColorRb > 0) { GL30.glDeleteRenderbuffers(msColorRb); msColorRb = -1; }
        if (msFbo     > 0) { GL30.glDeleteFramebuffers(msFbo);     msFbo     = -1; }
    }
}
