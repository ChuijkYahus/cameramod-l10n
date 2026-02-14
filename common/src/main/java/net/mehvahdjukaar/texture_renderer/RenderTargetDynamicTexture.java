package net.mehvahdjukaar.texture_renderer;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.monster.Creeper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.minecraft.client.Minecraft.ON_OSX;

public class RenderTargetDynamicTexture extends DynamicTexture implements Tickable {

    //runs when texture is initialized and populates it. Runs each tick if its tickable
    @NotNull
    protected final Consumer<? super RenderTargetDynamicTexture> drawingFunction;

    //thing that is drawn later
    private RenderTarget readTarget;
    //thing where it renders stuff on
    private RenderTarget writeTarget;

    private final int width;
    private final int height;
    private final ResourceLocation textureLocation;

    private volatile boolean shouldTick = true;

    public RenderTargetDynamicTexture(ResourceLocation resourceLocation, int width, int height,
                                      @NotNull Consumer<? extends RenderTargetDynamicTexture> textureDrawingFunction) {
        super(width, height, false);
        RenderSystem.assertOnRenderThread();
        this.width = width;
        this.height = height;
        this.textureLocation = resourceLocation;
        this.drawingFunction = (Consumer<? super RenderTargetDynamicTexture>) textureDrawingFunction;
        this.setUpdateNextTick(true);

        //register this texture. Call at the right time or stuff will get messed up. this internally calls texture.load
        Minecraft.getInstance().getTextureManager().register(resourceLocation, this);
    }

    public RenderTargetDynamicTexture(ResourceLocation resourceLocation, int size,
                                      @NotNull Consumer<? extends RenderTargetDynamicTexture> textureDrawingFunction) {
        this(resourceLocation, size, size, textureDrawingFunction);
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    private static void renderCall(RenderCall call) {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(call);
        } else {
            call.execute();
        }
    }

    /**
     * Force redraw using provided render function. You can also redraw manually
     */
    public void redraw() {
        renderCall(() -> {
            bind();
            writeTarget.bindWrite(true);
            drawingFunction.accept(this);
            swapBackToFront();
            writeTarget.unbindWrite();
        });
    }

    @Override
    public void load(ResourceManager manager) {
    }

    // Call after finish drawing
    public void swapBackToFront() {
        readTarget.bindWrite(true);
        writeTarget.bindRead();
        writeTarget.blitToScreen(readTarget.width, readTarget.height);
        readTarget.unbindWrite();
        writeTarget.unbindRead();
    }

    public RenderTarget getRenderTarget() {
        return writeTarget;
    }

    //bind read on the current texture
    @Override
    public void bind() {
        super.bind();
    }

    //gpu texture ID
    @Override
    public int getId() {
        RenderSystem.assertOnRenderThreadOrInit();
        //needs to be here since the super constructor calls this early
        if (this.readTarget == null || this.writeTarget == null) {
            int w = getPixels().getWidth();
            int h = getPixels().getHeight();
            this.readTarget = new TextureTarget(w, h, false, ON_OSX);
            this.writeTarget = new TextureTarget(w, h, true, ON_OSX);
        }
        //must never change since its just queried when texture is registered
        //this is what binds texture and frame buffers together
        return this.readTarget.getColorTextureId();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void releaseId() {
        super.releaseId();
        renderCall(() -> {
            if (this.writeTarget != null) {
                this.writeTarget.destroyBuffers();
                this.writeTarget = null;
            }
            if (this.readTarget != null) {
                this.readTarget.destroyBuffers();
                this.readTarget = null;
            }
        });
    }

    @Override
    public void close() {
        super.close();
        renderCall(() -> {
            Minecraft.getInstance().getTextureManager().release(textureLocation);
        });
    }

    /**
     * Downloads the GPU texture to CPU for edit
     */
    public void download() {
        this.bind();
        getPixels().downloadTexture(0, false);
    }

    public void setUpdateNextTick(boolean shouldTick) {
        this.shouldTick = shouldTick;
    }

    @ApiStatus.Internal
    @Override
    public void tick() {
        if (!shouldTick) return;
        shouldTick = false;
        redraw();
    }

    public List<Path> saveToFile(Path texturesDir) throws IOException {
        return saveToFile(texturesDir, this.textureLocation.getPath().replace("/", "_"));
    }

    public List<Path> saveToFile(Path texturesDir, String name) throws IOException {
        RenderSystem.assertOnRenderThreadOrInit();
        this.bind();

        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        List<Path> textureFiles = new ArrayList<>();

        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        int size = width * height;
        if (size == 0) {
            return List.of();
        }

        BufferedImage bufferedimage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Path output = texturesDir.resolve(name + ".png");
        IntBuffer buffer = BufferUtils.createIntBuffer(size);
        int[] data = new int[size];

        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
        buffer.get(data);
        bufferedimage.setRGB(0, 0, width, height, data, 0, width);

        ImageIO.write(bufferedimage, "png", output.toFile());
        //   WoodGood.LOGGER.info("Exported png to: {}", output.toString());
        textureFiles.add(output);

        return textureFiles;
    }


}