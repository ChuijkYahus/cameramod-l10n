package net.mehvahdjukaar.vista.client.textures;

import net.minecraft.client.renderer.texture.SpriteContents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record AnimationStripData(
        int frameWidth,
        int frameHeight,
        int frameCount,
        int fullWidth,
        int fullHeight,
        float frameRelativeH,
        float frameRelativeW,
        SpriteContents.FrameInfo[] frameInfos,
        int totalDuration
) {

    public static AnimationStripData create(SpriteContents spriteContents) {
        float invFullH = 1f / spriteContents.originalImage.getHeight();
        float invFullW = 1f / spriteContents.originalImage.getWidth();

        //calculate average time per frame
        SpriteContents.AnimatedTexture animatedTexture = spriteContents.animatedTexture;
        SpriteContents.FrameInfo[] times = new SpriteContents.FrameInfo[]{};
        if (animatedTexture != null) {
            List<SpriteContents.FrameInfo> allTimes = new ArrayList<>(animatedTexture.frames);
            //if all frames are equal we just add 1
            if (allTimes.stream().allMatch(f -> f.equals(allTimes.getFirst()))) {
                times = new SpriteContents.FrameInfo[]{allTimes.getFirst()};
            } else times = allTimes.toArray(new SpriteContents.FrameInfo[0]);
        }

        int totalDuration = Arrays.stream(times)
                .mapToInt(f -> f.time)
                .sum();

        return new AnimationStripData(
                spriteContents.width(),
                spriteContents.height(),
                spriteContents.getFrameCount(),
                spriteContents.originalImage.getWidth(),
                spriteContents.originalImage.getHeight(),
                spriteContents.height() * invFullH,
                spriteContents.width() * invFullW,
                times,
                totalDuration
        );
    }

    public static final AnimationStripData EMPTY =
            new AnimationStripData(16, 16, 1, 16, 16,
                    1f, 1f, new SpriteContents.FrameInfo[]{}, 1);

    public float getU(float u, int time) {
        return u;
    }

    /**
     * Frame index → V coordinate
     */
    public float getV(float v, int time) {
        return v * frameRelativeH + getFrameIndexFromTime(time) * frameRelativeH;
    }

    /**
     * Time (ticks) → actual texture frame index
     */
    public int getFrameIndexFromTime(int time) {
        if (frameInfos.length == 0) return 0;

        // Single repeated frame
        if (frameInfos.length == 1) {
            SpriteContents.FrameInfo f = frameInfos[0];
            return f.index;
        }

        int localTime = time % totalDuration;
        int acc = 0;

        for (var f : frameInfos) {
            acc += f.time;
            if (localTime < acc) {
                return f.index;
            }
        }

        return frameInfos[0].index; // fallback
    }
}
