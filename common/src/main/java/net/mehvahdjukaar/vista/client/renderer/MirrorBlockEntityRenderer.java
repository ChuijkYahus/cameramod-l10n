package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.LOD;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.client.MirrorReflection;
import net.mehvahdjukaar.vista.client.VistaRenderTypes;
import net.mehvahdjukaar.vista.client.textures.MirrorReflectionTexture;
import net.mehvahdjukaar.vista.client.textures.MirrorTextureManager;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlock;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class MirrorBlockEntityRenderer implements BlockEntityRenderer<MirrorBlockEntity> {

    // Forward nudge to keep the quad clear of the coplanar block-model face. Normally unused —
    // POLYGON_OFFSET_LAYERING on the render type handles z-fighting. But inside a nested level
    // render (another mirror's reflection / a TV feed) the polygon-offset state doesn't take, so
    // we fall back to this manual offset there.
    private static final float SURFACE_OFFSET = 0.01f;

    public MirrorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return ClientConfigs.MIRROR_RENDER_DISTANCE.get();
    }

    @ForgeOverride
    public AABB getRenderBoundingBox(MirrorBlockEntity tile) {
        AABB aabb = new AABB(tile.getBlockPos());
        Direction dir = tile.getBlockState().getValue(MirrorBlock.FACING);
        Vec2i connection = tile.getConnectedCount();
        float width = connection.x();
        float height = connection.y();
        if (dir == Direction.EAST) {
            return aabb.expandTowards(0, height - 1, -width + 1);
        } else if (dir == Direction.WEST) {
            return aabb.expandTowards(0, height - 1, width - 1);
        } else if (dir == Direction.NORTH) {
            return aabb.expandTowards(-width + 1, height - 1, 0);
        } else if (dir == Direction.SOUTH) {
            return aabb.expandTowards(width - 1, height - 1, 0);
        }
        return aabb;
    }

    @Override
    public void render(MirrorBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        Direction dir = blockEntity.getBlockState().getValue(MirrorBlock.FACING);
        double recession = MirrorBlock.surfaceRecession(blockEntity.getBlockState());

        // Cull against the actual mirror surface plane, recessed into the cell for the FAR model.
        // Using a fixed 0.5 (front face) here would cull the recessed quad early once the viewer
        // gets close enough to cross the front face but not the recessed surface.
        LOD lod = LOD.at(blockEntity);
        if (lod.isPlaneCulled(dir, (float) (0.5 - recession), 1.5f, 0f)) return;

        Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());
        Vec3 planePoint = Vec3.atCenterOf(blockEntity.getBlockPos()).add(normal.scale(0.5 - recession));

        Minecraft mc = Minecraft.getInstance();
        Camera mainCamera = mc.gameRenderer.mainCamera;
        MirrorReflection reflection = MirrorReflection.compute(
                planePoint, normal, mainCamera.getPosition());
        if (!reflection.viewerInFront()) return;

        int depth = VistaLevelRenderer.getCurrentDepth();
        // depth > 0 means: we're inside another mirror's reflection render. The texture lookup
        // path depends on MIRROR_RECURSION_MODE.
        MirrorReflectionTexture text;
        if (depth == 0) {
            Vec2i screenSize = blockEntity.getScreenPixelSize();
            // Capture the eye NOW, while we still have the camera that was used to render the BE.
            // The actual reflection render fires later in the frame; if we resampled the camera at
            // refresh time it could drift from the source view. Offset the raw camera position by
            // this frame's view-bob displacement (getMainBobEyeOffset) so the reflection's parallax
            // tracks the *bobbed* POV the displayed quad is drawn with — without it the far reflected
            // scene wobbles against the surface as you walk. The offset is read off the bob matrix
            // the game built, so no bob math is duplicated and mod-altered bob still works.
            Vec3 eye = mainCamera.getPosition().add(VistaLevelRenderer.getMainBobEyeOffset());
            text = MirrorTextureManager.getMirrorTexture(blockEntity, screenSize, eye);
        } else {
            text = resolveNestedTexture(blockEntity, mainCamera.getPosition(), depth);
        }

        if (text == null) return;

        drawMirrorFace(blockEntity, dir, poseStack, buffer, text, recession);
    }

    /**
     * Texture lookup when this mirror is being drawn inside another mirror's reflection render
     * (depth > 0). Dispatches by recursion mode:
     * <ul>
     *   <li>{@code OFF}: skip drawing entirely.</li>
     *   <li>{@code SHARED}: cheap path — reuse the mirror's own direct-view texture. Parallax
     *       is wrong at depth >= 1 but cost stays flat (one texture per mirror).</li>
     *   <li>{@code RECURSIVE}: allocate a chain-keyed texture so each (mirror, parent-chain) gets
     *       its own off-axis render with correct parallax. Beyond {@code MIRROR_MAX_RECURSION_DEPTH}
     *       we render nothing — better to omit the surface than to stamp a wrong-parallax fallback
     *       that gives the lie away.</li>
     * </ul>
     */
    @Nullable
    private MirrorReflectionTexture resolveNestedTexture(MirrorBlockEntity blockEntity,
                                                          Vec3 eye, int depth) {
        ClientConfigs.MirrorRecursionMode mode = ClientConfigs.MIRROR_RECURSION_MODE.get();
        Vec2i screenSize = blockEntity.getScreenPixelSize();
        switch (mode) {
            case OFF:
                return null;
            case SHARED: {
                // Reuse this mirror's own direct-view texture, read-only: re-queueing it here with
                // the nested (reflected) eye would clobber its depth-0 PENDING entry (same uuid key)
                // and corrupt/flicker the mirror's real direct reflection. Only fall back to
                // scheduling when no rendered texture exists yet (e.g. the mirror is visible *only*
                // through this one and never directly) — in that case there's no direct-view entry
                // to clobber. Parallax is still wrong at depth >= 1; that's the documented SHARED
                // trade-off (use RECURSIVE for correct nested parallax).
                MirrorReflectionTexture shared =
                        MirrorTextureManager.getMirrorTexture(blockEntity.getId(), screenSize);
                if (shared != null && shared.hasRendered()) return shared;
                return MirrorTextureManager.getMirrorTexture(blockEntity, screenSize, eye);
            }
            case RECURSIVE: {
                int maxDepth = ClientConfigs.MIRROR_MAX_RECURSION_DEPTH.get();
                if (depth > maxDepth) return null;
                List<UUID> chain = VistaLevelRenderer.getCurrentMirrorChain();
                return MirrorTextureManager.getMirrorTextureForChain(
                        blockEntity, screenSize, eye, depth, chain);
            }
            default:
                return null;
        }
    }

    private void drawMirrorFace(MirrorBlockEntity blockEntity, Direction dir, PoseStack poseStack,
                                MultiBufferSource buffer, MirrorReflectionTexture text,
                                double recession) {
        Vec2i connection = blockEntity.getConnectedCount();
        float w = connection.x();
        float h = connection.y();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - dir.toYRot()));
        // recession pushes the surface back into the cell for the FAR model. The surface sits
        // coplanar with the block-model face; POLYGON_OFFSET_LAYERING on the render type biases
        // its depth toward the camera so it wins over the face without a manual forward nudge.
        // Exception: inside a nested level render the polygon offset doesn't apply, so nudge the
        // quad forward manually there to avoid z-fighting with the block face.
        float zFightOffset = VistaLevelRenderer.isRenderingLiveFeed() ? SURFACE_OFFSET : 0f;
        poseStack.translate(0, 0, -0.5 + (float) recession - zFightOffset);

        Level level = blockEntity.getLevel();
        int skyBrightness = level.getBrightness(LightLayer.SKY, blockEntity.getBlockPos().relative(dir));
        int light = LightTexture.pack(15, skyBrightness);

        VertexConsumer vc = buffer.getBuffer(VistaRenderTypes.mirrorMaterial(
                text.getTextureLocation(), (int) w, (int) h));
        // Inset the reflection quad by a fixed 1px on the outer edge so the frame_front border shows
        // around it. The inset does NOT scale with the grid: the frame stays 1px wide no matter how
        // many blocks the mirror spans, leaving a visible (16w-2) x (16h-2) px surface — a 1:1 match
        // with the framebuffer (see MirrorBlockEntity.FRAME_PIXELS), i.e. pixel perfect at any size.
        float inset = 1f / 16f;
        // Master is at bottom-right in local-rotated space (grid extends along facing.CCW
        // = local -X), so the quad spans from local x=0.5-w to x=0.5.
        // UVs rotated 180° (u0,v0=1,1; u1,v1=0,0) — framebuffer texture is upside-down
        // and mirrored relative to the local quad orientation.
        VertexUtil.addQuad(vc, poseStack,
                0.5f - w + inset, -0.5f + inset, 0.5f - inset, h - 0.5f - inset,
                1f, 1f, 0f, 0f,
                255, 255, 255, 255,
                VertexUtil.lightU(light), VertexUtil.lightV(light));

        poseStack.popPose();
    }
}
