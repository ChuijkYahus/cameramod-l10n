package net.mehvahdjukaar.vista.client.textures;

import net.minecraft.client.renderer.texture.SpriteContents;

public record AnimationStripData(int frameWidth, int frameHeight, int frameCount, int fullWidth, int fullHeight) {

    public static AnimationStripData create(SpriteContents spriteContents) {
        return new AnimationStripData(
                spriteContents.width(),
                spriteContents.height(),
                spriteContents.getFrameCount(),
                spriteContents.originalImage.getWidth(),
                spriteContents.originalImage.getHeight()
        );
    }

    public static final AnimationStripData EMPTY =
            new AnimationStripData(16, 16, 1, 16, 16);

    public float getU(float u, int frameIndex) {
        return u;
    }

    public float getV(float v, int frameIndex) {
        float vPerFrame = (float) frameHeight / (float) fullHeight;
        return vPerFrame * frameIndex + (vPerFrame * v);
    }

}
