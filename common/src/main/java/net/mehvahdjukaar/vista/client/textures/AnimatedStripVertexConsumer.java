package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class AnimatedStripVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final AnimationStripData stripData;
    private final int frameIndex;

    public AnimatedStripVertexConsumer(int frameIndex, AnimationStripData stripData, VertexConsumer delegate) {
        this.frameIndex = frameIndex;
        this.stripData = stripData;
        this.delegate = delegate;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.delegate.setUv(this.stripData.getU(u, frameIndex), this.stripData.getV(v, frameIndex));
        return this;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        this.delegate.setColor(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        this.delegate.setNormal(normalX, normalY, normalZ);
        return this;
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v, int packedOverlay, int packedLight, float normalX, float normalY, float normalZ) {
        this.delegate.addVertex(x, y, z, color, this.stripData.getU(u, frameIndex), this.stripData.getV(v, frameIndex), packedOverlay, packedLight, normalX, normalY, normalZ);
    }
}
