package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class GifPathSpriteSource implements SpriteSource {

    public static final MapCodec<GifPathSpriteSource> CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(
            Codec.STRING.fieldOf("source").forGetter((l) -> l.sourcePath),
            Codec.STRING.fieldOf("prefix").forGetter((l) -> l.idPrefix)
    ).apply(i, GifPathSpriteSource::new));

    public static final SpriteSourceType TYPE = new SpriteSourceType(CODEC);

    private final String sourcePath;
    private final String idPrefix;

    public GifPathSpriteSource(String sourcePath, String idPrefix) {
        this.sourcePath = sourcePath;
        this.idPrefix = idPrefix;
    }

    @Override
    public void run(ResourceManager resourceManager, SpriteSource.Output output) {
        FileToIdConverter fileToIdConverter = new FileToIdConverter("textures/" + this.sourcePath, ".gif");
        fileToIdConverter.listMatchingResources(resourceManager).forEach((resourceLocation, resource) -> {
            ResourceLocation id = fileToIdConverter.fileToId(resourceLocation).withPrefix(this.idPrefix);
            Optional<Resource> optional = resourceManager.getResource(resourceLocation);
            if (optional.isEmpty()) {
                VistaMod.LOGGER.warn("Unable to find texture {}", id);
            } else {
                output.add(id, new GifSpriteSupplier(id, optional.get()));
            }
        });
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }


    private record GifSpriteSupplier(ResourceLocation id, Resource resource) implements SpriteSupplier {

        @Override
        public SpriteContents apply(SpriteResourceLoader spriteResourceLoader) {
            try (InputStream inputStream = this.resource.open();
                 ImageInputStream imageStream = ImageIO.createImageInputStream(inputStream)) {

                ImageReader reader = gifReader(imageStream);
                int frameCount = reader.getNumImages(true);

                List<BufferedImage> frames = readFrames(reader, frameCount);
                List<Integer> frameTicks = readFrameTicks(reader, frameCount);

                int w = frames.get(0).getWidth();
                int h = frames.get(0).getHeight();
                normalizeFrames(frames, w, h);

                NativeImage strip = buildVerticalStrip(frames, w, h);
                AnimationMetadataSection anim = buildAnimationMeta(frameTicks, w, h, frames.size());

                FrameSize size = new FrameSize(w, h);
                ResourceMetadata meta = new ResourceMetadata.Builder()
                        .put(AnimationMetadataSection.SERIALIZER, anim)
                        .build();

                return new SpriteContents(id, size, strip, meta);
            } catch (IllegalArgumentException | IOException e) {
                VistaMod.LOGGER.error("unable to apply palette to {}", this.id, e);
            }

            return null;
        }

        // --- helpers ---

        private static ImageReader gifReader(ImageInputStream iis) throws IOException {
            Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("gif");
            if (!it.hasNext()) throw new IOException("No GIF ImageReader available");
            ImageReader r = it.next();
            r.setInput(iis, false, false);
            return r;
        }

        private static List<BufferedImage> readFrames(ImageReader reader, int count) throws IOException {
            List<BufferedImage> out = new ArrayList<>(count);
            for (int i = 0; i < count; i++) out.add(reader.read(i));
            if (out.isEmpty()) throw new IOException("GIF contained no frames");
            return out;
        }

        private static List<Integer> readFrameTicks(ImageReader reader, int count) {
            List<Integer> ticks = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int cs = 5; // fallback 50 ms
                try {
                    var meta = reader.getImageMetadata(i);
                    var root = (IIOMetadataNode) meta.getAsTree("javax_imageio_gif_image_1.0");
                    var gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
                    if (gce != null) cs = Math.max(1, Integer.parseInt(gce.getAttribute("delayTime")));
                } catch (Exception ignored) {}
                ticks.add(Math.max(1, Math.round(cs / 5.0f))); // 1 tick = 50 ms = 5 cs
            }
            return ticks;
        }

        private static void normalizeFrames(List<BufferedImage> frames, int w, int h) {
            for (int i = 0; i < frames.size(); i++) {
                BufferedImage bi = frames.get(i);
                if (bi.getWidth() == w && bi.getHeight() == h) continue;
                BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                canvas.getGraphics().drawImage(bi, 0, 0, null);
                frames.set(i, canvas);
            }
        }

        private static NativeImage buildVerticalStrip(List<BufferedImage> frames, int w, int h) {
            NativeImage out = new NativeImage(NativeImage.Format.RGBA, w, h * frames.size(), true);
            for (int i = 0; i < frames.size(); i++) {
                int yOff = i * h;
                copyArgbToAbgr(frames.get(i), out, 0, yOff, w, h);
            }
            return out;
        }

        private static void copyArgbToAbgr(BufferedImage src, NativeImage dst, int dx, int dy, int w, int h) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = src.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    int r = (argb >>> 16) & 0xFF;
                    int g = (argb >>> 8) & 0xFF;
                    int b = (argb) & 0xFF;
                    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                    dst.setPixelRGBA(dx + x, dy + y, abgr);
                }
            }
        }

        private static AnimationMetadataSection buildAnimationMeta(List<Integer> ticks, int w, int h, int frameCount) {
            /*
            boolean uniform = ticks.stream().allMatch(t -> t.equals(ticks.get(0)));
            if (uniform) {
                return new AnimationMetadataSection(
                        ImmutableList.of(),
                        w, h,
                        ticks.get(0),
                        false
                );
            }
            ImmutableList.Builder<AnimationFrame> frames = ImmutableList.builder();
            for (int i = 0; i < frameCount; i++) frames.add(new AnimationFrame(i, ticks.get(i)));
            */
            return new AnimationMetadataSection(
                    List.of(),
                    w, h,
                    1,
                    false
            );
        }
    }
}
