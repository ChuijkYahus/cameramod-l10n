package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.moonlight.api.resources.textures.SpriteUtils;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
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
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
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

                int[] wh = new int[2];
                List<BufferedImage> frames = readFramesComposited(reader, frameCount, wh);
                List<Integer> frameTicks = readFrameTicks(reader, frameCount);

                int w = wh[0], h = wh[1];

                NativeImage strip = buildVerticalStrip(frames, w, h);
                // strip.writeToFile(new File("temp_image_dump.png")); // debug output

                AnimationMetadataSection anim = buildAnimationMeta(frameTicks, w, h, frames.size());

                FrameSize size = new FrameSize(w, h);
                ResourceMetadata meta = new ResourceMetadata.Builder()
                        .put(AnimationMetadataSection.SERIALIZER, anim)
                        .build();

                return new SpriteContents(id, size, strip, meta);
            } catch (IllegalArgumentException | IOException e) {
                VistaMod.LOGGER.error("unable to build animated strip for {}", this.id, e);
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

        // === Fixed GIF frame reader ===

        private static final String GIF_IMAGE_META = "javax_imageio_gif_image_1.0";
        private static final String GIF_STREAM_META = "javax_imageio_gif_stream_1.0";

        private static List<BufferedImage> readFramesComposited(ImageReader reader, int count, int[] outWH) throws IOException {
            // --- 1) logical screen size ---
            IIOMetadata streamMeta = reader.getStreamMetadata();
            int screenW = -1, screenH = -1, bgIndex = -1;
            Color bgColor = new Color(0, true); // fully transparent

            if (streamMeta != null) {
                IIOMetadataNode root = (IIOMetadataNode) streamMeta.getAsTree(GIF_STREAM_META);
                IIOMetadataNode lsd = (IIOMetadataNode) root.getElementsByTagName("LogicalScreenDescriptor").item(0);
                if (lsd != null) {
                    screenW = Integer.parseInt(lsd.getAttribute("logicalScreenWidth"));
                    screenH = Integer.parseInt(lsd.getAttribute("logicalScreenHeight"));
                }
                IIOMetadataNode bg = (IIOMetadataNode) root.getElementsByTagName("GlobalColorTable").item(0);
                if (bg != null) {
                    String s = bg.getAttribute("backgroundColorIndex");
                    if (!s.isEmpty()) bgIndex = Integer.parseInt(s);
                    try {
                        int size = Integer.parseInt(bg.getAttribute("sizeOfGlobalColorTable"));
                        int[] r = new int[size], g = new int[size], b = new int[size];
                        for (int i = 0; i < size; i++) {
                            IIOMetadataNode ce = (IIOMetadataNode) bg.getElementsByTagName("ColorTableEntry").item(i);
                            if (ce != null) {
                                r[i] = Integer.parseInt(ce.getAttribute("red"));
                                g[i] = Integer.parseInt(ce.getAttribute("green"));
                                b[i] = Integer.parseInt(ce.getAttribute("blue"));
                            }
                        }
                        if (bgIndex >= 0 && bgIndex < r.length) {
                            bgColor = new Color(r[bgIndex], g[bgIndex], b[bgIndex], 0);
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (screenW <= 0 || screenH <= 0) {
                BufferedImage first = reader.read(0);
                screenW = first.getWidth();
                screenH = first.getHeight();
            }
            outWH[0] = screenW; outWH[1] = screenH;

            // --- 2) compositing ---
            BufferedImage canvas = new BufferedImage(screenW, screenH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gCanvas = canvas.createGraphics();
            gCanvas.setComposite(AlphaComposite.SrcOver);
            gCanvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            BufferedImage prevSnapshot = null;
            List<BufferedImage> out = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                IIOMetadata meta = reader.getImageMetadata(i);
                FrameMeta fm = parseFrameMeta(meta);

                // snapshot before drawing for disposal=3
                if (fm.disposal == 3) {
                    if (prevSnapshot == null || prevSnapshot.getWidth() != screenW || prevSnapshot.getHeight() != screenH) {
                        prevSnapshot = new BufferedImage(screenW, screenH, BufferedImage.TYPE_INT_ARGB);
                    }
                    prevSnapshot.getRaster().setDataElements(0, 0, screenW, screenH,
                            canvas.getRaster().getDataElements(0, 0, screenW, screenH, null));
                }

                BufferedImage frameImg = reader.read(i);
                gCanvas.drawImage(frameImg, fm.left, fm.top, null);

                BufferedImage copy = new BufferedImage(screenW, screenH, BufferedImage.TYPE_INT_ARGB);
                copy.getRaster().setDataElements(0, 0, screenW, screenH,
                        canvas.getRaster().getDataElements(0, 0, screenW, screenH, null));
                out.add(copy);

                // disposal for next
                switch (fm.disposal) {
                    case 2 -> { // restore to background
                        gCanvas.setComposite(AlphaComposite.Src);
                        gCanvas.setColor(bgColor);
                        gCanvas.fillRect(fm.left, fm.top, fm.width, fm.height);
                        gCanvas.setComposite(AlphaComposite.SrcOver);
                    }
                    case 3 -> { // restore to previous
                        if (prevSnapshot != null) {
                            canvas.getRaster().setDataElements(0, 0, screenW, screenH,
                                    prevSnapshot.getRaster().getDataElements(0, 0, screenW, screenH, null));
                        }
                    }
                }
            }

            gCanvas.dispose();
            if (out.isEmpty()) throw new IOException("GIF contained no frames");
            return out;
        }

        private static class FrameMeta {
            int left, top, width, height;
            int disposal; // 0/1 none, 2 background, 3 previous
        }

        private static FrameMeta parseFrameMeta(IIOMetadata meta) {
            FrameMeta fm = new FrameMeta();
            try {
                IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(GIF_IMAGE_META);
                IIOMetadataNode id = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
                fm.left   = Integer.parseInt(id.getAttribute("imageLeftPosition"));
                fm.top    = Integer.parseInt(id.getAttribute("imageTopPosition"));
                fm.width  = Integer.parseInt(id.getAttribute("imageWidth"));
                fm.height = Integer.parseInt(id.getAttribute("imageHeight"));

                IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
                if (gce != null) {
                    String disp = gce.getAttribute("disposalMethod");
                    if ("restoreToBackgroundColor".equals(disp)) fm.disposal = 2;
                    else if ("restoreToPrevious".equals(disp)) fm.disposal = 3;
                    else fm.disposal = 1;
                } else {
                    fm.disposal = 1;
                }
            } catch (Exception ignored) {
                fm.left = fm.top = 0;
                fm.width = fm.height = 0;
                fm.disposal = 1;
            }
            return fm;
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
        return new AnimationMetadataSection(frames.build(), w, h, -1, false);
        */
            return new AnimationMetadataSection(
                    List.of(),
                    w, h,
                    5,
                    false
            );
        }
    }

}
