package net.mehvahdjukaar.vista.client.textures;

import net.minecraft.client.renderer.texture.SpriteContents;
public record AnimationStripData(
        int frameWidth,
        int frameHeight,
        int frameCount,
        int fullWidth,
        int fullHeight,
        float frameRelativeH,
        float frameRelativeW
) {

    public static AnimationStripData create(SpriteContents spriteContents) {
        float invFullH = 1f / spriteContents.originalImage.getHeight();
        float invFullW = 1f / spriteContents.originalImage.getWidth();

        return new AnimationStripData(
                spriteContents.width(),
                spriteContents.height(),
                spriteContents.getFrameCount(),
                spriteContents.originalImage.getWidth(),
                spriteContents.originalImage.getHeight(),
                spriteContents.height() * invFullH,
                spriteContents.width() * invFullW
        );
    }

    public static final AnimationStripData EMPTY =
            new AnimationStripData(16, 16, 1, 16, 16, 1f, 1f);

    public float getU(float u, int frameIndex) {
        return u;
    }

    public float getV(float v, int frameIndex) {
        frameIndex %= frameCount;
        return v * frameRelativeH + frameIndex * frameRelativeH;
    }
}