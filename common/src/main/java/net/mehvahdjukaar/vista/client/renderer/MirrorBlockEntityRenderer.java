package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.LOD;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.LightLayer;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.client.MirrorReflection;
import net.mehvahdjukaar.vista.client.textures.MirrorReflectionTexture;
import net.mehvahdjukaar.vista.client.textures.MirrorTextureManager;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlock;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MirrorBlockEntityRenderer implements BlockEntityRenderer<MirrorBlockEntity> {

    // Push the visual quad well clear of the block-model front face so geometry doesn't overdraw it.
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

        LOD lod = LOD.at(blockEntity);
        if (lod.isPlaneCulled(dir, 0.5f, 1.5f, 0f)) return;

        // Recursion guard: a mirror inside another mirror's reflection draws only its block
        // model (frame). The cached texture is the previous frame's reflection from THIS
        // mirror's POV — meaningless content for a different reflection.
        if (VistaLevelRenderer.isRenderingLiveFeed()) return;

        Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());
        Vec3 planePoint = Vec3.atCenterOf(blockEntity.getBlockPos()).add(normal.scale(0.5));

        Minecraft mc = Minecraft.getInstance();
        Camera mainCamera = mc.gameRenderer.mainCamera;
        MirrorReflection reflection = MirrorReflection.compute(
                planePoint, normal, mainCamera.getPosition());
        if (!reflection.viewerInFront()) return;

        Vec2i screenSize = blockEntity.getScreenPixelSize();
        // Capture the eye NOW, while we still have the camera that was used to render the BE.
        // The actual reflection render fires later in the frame; if we resampled the camera at
        // refresh time it could drift from the source view. Use the raw camera position (the
        // pre-bob state vanilla exposes) — duplicating GameRenderer.bobView math here would
        // silently break under any mod that alters bob behavior, and the visual mismatch between
        // a bobbed BE quad and an un-bobbed reflection is sub-pixel for typical walk speeds.
        Vec3 eye = mainCamera.getPosition();

        MirrorReflectionTexture text = MirrorTextureManager.getMirrorTexture(blockEntity, screenSize, eye);


        if (text == null) return;

        drawMirrorFace(blockEntity, dir, poseStack, buffer, text);
    }

    private void drawMirrorFace(MirrorBlockEntity blockEntity, Direction dir, PoseStack poseStack,
                                       MultiBufferSource buffer, MirrorReflectionTexture text) {
        Vec2i connection = blockEntity.getConnectedCount();
        float w = connection.x();
        float h = connection.y();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - dir.toYRot()));
        poseStack.translate(0, 0, -0.5 - SURFACE_OFFSET);

        Level level = blockEntity.getLevel();
        int skyBrightness = level.getBrightness(LightLayer.SKY, blockEntity.getBlockPos().relative(dir));
        int light = LightTexture.pack(15, skyBrightness);

        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(text.getTextureLocation()));
//TODO: use diff render type so no normal
        // Master is at bottom-right in local-rotated space (grid extends along facing.CCW
        // = local -X), so the quad spans from local x=0.5-w to x=0.5.
        // UVs rotated 180° (u0,v0=1,1; u1,v1=0,0) — framebuffer texture is upside-down
        // and mirrored relative to the local quad orientation.
        VertexUtil.addQuad(vc, poseStack,
                0.5f - w, -0.5f, 0.5f, h - 0.5f,
                1f, 1f, 0f, 0f,
                255, 255, 255, 255,
                VertexUtil.lightU(light), VertexUtil.lightV(light));

        poseStack.popPose();
    }

    private static void vert(VertexConsumer builder, PoseStack poseStack,
                             float x, float y, float u, float v, int lu, int lv, Vector3f normal) {
        builder.addVertex(poseStack.last().pose(), x, y, 0);
        builder.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        builder.setUv(u, v);
        builder.setOverlay(OverlayTexture.NO_OVERLAY);
        builder.setUv2(lu, lv);
        builder.setNormal(normal.x, normal.y, normal.z);
    }
}
