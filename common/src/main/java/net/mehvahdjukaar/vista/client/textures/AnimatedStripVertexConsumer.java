package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class AnimatedStripVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final AnimationStripData stripData;
    private final int frameIndex;
    private final boolean directFrameIndex;

    public AnimatedStripVertexConsumer(int frameIndex, AnimationStripData stripData, VertexConsumer delegate) {
        this(frameIndex, stripData, delegate, false);
    }

    public AnimatedStripVertexConsumer(int frameIndex, AnimationStripData stripData, VertexConsumer delegate, boolean directFrameIndex) {
        this.frameIndex = frameIndex;
        this.stripData = stripData;
        this.delegate = delegate;
        this.directFrameIndex = directFrameIndex;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        if (directFrameIndex) {
            this.delegate.setUv(this.stripData.getUForFrame(u, frameIndex), this.stripData.getVForFrame(v, frameIndex));
        } else {
            this.delegate.setUv(this.stripData.getU(u, frameIndex), this.stripData.getV(v, frameIndex));
        }
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
        if (directFrameIndex) {
            this.delegate.addVertex(x, y, z, color, this.stripData.getUForFrame(u, frameIndex), this.stripData.getVForFrame(v, frameIndex), packedOverlay, packedLight, normalX, normalY, normalZ);
        } else {
            this.delegate.addVertex(x, y, z, color, this.stripData.getU(u, frameIndex), this.stripData.getV(v, frameIndex), packedOverlay, packedLight, normalX, normalY, normalZ);
        }
    }
}
