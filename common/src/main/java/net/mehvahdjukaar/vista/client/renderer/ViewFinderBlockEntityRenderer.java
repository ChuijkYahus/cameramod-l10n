package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlock;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ViewFinderBlockEntityRenderer implements BlockEntityRenderer<ViewFinderBlockEntity> {

    private final ModelPart head;
    private final ModelPart legs;
    private final ModelPart pivot;
    private final ModelPart model;

    public ViewFinderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart model = context.bakeLayer(VistaModClient.VIEWFINDER_MODEL);
        this.legs = model.getChild("legs");
        this.pivot = legs.getChild("head_pivot");
        this.head = pivot.getChild("head");
        this.model = model;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public void render(ViewFinderBlockEntity tile, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        this.renderModel(tile, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        if (ClientConfigs.rendersDebug()) {
            renderDebug(tile, poseStack, bufferSource);
        }
    }


    public void renderModel(ViewFinderBlockEntity tile, float partialTick, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        boolean isControlledByLocalInstance = ViewFinderController.isActiveFor(tile);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        Quaternionf rotation = tile.getBlockState().getValue(ViewFinderBlock.FACING).getOpposite().getRotation();

        poseStack.mulPose(rotation);

        VertexConsumer builder = VistaModClient.VIEW_FINDER_MATERIAL.buffer(bufferSource, RenderType::entityCutout);

        float pitchRad = tile.getPitch(partialTick) * Mth.DEG_TO_RAD;
        float yawRad = tile.getYaw(partialTick) * Mth.DEG_TO_RAD;

        Vector3f forward = new Vector3f(0f, 0, 1);

        forward.rotateX(Mth.PI - pitchRad);

        forward.rotateY(Mth.PI - yawRad);
        forward.rotate(rotation.invert());

        yawRad = (float) Mth.atan2(forward.x, forward.z);

        pitchRad = (float) Mth.atan2(-forward.y, Mth.sqrt(forward.x * forward.x + forward.z * forward.z));
        //float rollRad = (float) Math.atan2(forward.y, forward.z);

        this.legs.yRot = yawRad;
        this.pivot.xRot = pitchRad;
        this.pivot.zRot = 0;

        this.legs.visible = !isControlledByLocalInstance;
        this.head.visible = !isControlledByLocalInstance;

        this.model.render(poseStack, builder, packedLight, packedOverlay);

        poseStack.mulPose(Axis.YP.rotation(yawRad));
        poseStack.mulPose(Axis.XP.rotation(pitchRad));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.scale(0.75f, 0.75f, 0.75f);
        poseStack.translate(0.25, 0.5, 0.25);

        //ItemStack stack = Items.CREEPER_HEAD.getDefaultInstance();
        //  Minecraft.getInstance().getItemRenderer()
        //        .renderStatic(stack, ItemDisplayContext.NONE,  packedLight, packedOverlay,poseStack,
        //              bufferSource, tile.getLevel(), 0);

        poseStack.popPose();


    }



    private static void renderDebug(ViewFinderBlockEntity tile, PoseStack poseStack, MultiBufferSource bufferSource) {
      /*
        var player = tile.fakePlayer;
        if (player != null) {
            poseStack.pushPose();
            Vec3 pos = player.getEyePosition().subtract(Vec3.atLowerCornerOf(tile.getBlockPos()));
            poseStack.translate(pos.x, pos.y, pos.z);
            PoseStack.Pose pose = poseStack.last();
            VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());
            vc.addVertex(pose, 0, 0, 0)
                    .setColor(255, 0, 255, 255)
                    .setNormal(pose, 0, 1, 0);
            vc.addVertex(pose, 0, 1, 0)
                    .setColor(255, 0, 255, 255)
                    .setNormal(pose, 0, 1, 0);

            float tileYaw = tile.getYaw();
            float tilePitch = tile.getPitch();
            var tileView = Vec3.directionFromRotation(tilePitch, tileYaw).normalize();
            vc.addVertex(pose, 0, 0, 0)
                    .setColor(30, 30, 255, 255)
                    .setNormal(pose, 0, 1, 0);
            vc.addVertex(pose, (float) tileView.x, (float) tileView.y, (float) tileView.z)
                    .setColor(30, 30, 255, 255)
                    .setNormal(pose, 0, 1, 0);

            float headY = player.yHeadRot;
            float headX = player.xRotO;
            var view = Vec3.directionFromRotation(headX, headY).normalize();
            vc.addVertex(pose, 0, 0, 0)
                    .setColor(30, 255, 30, 255)
                    .setNormal(pose, 0, 1, 0);
            vc.addVertex(pose, (float) view.x, (float) view.y, (float) view.z)
                    .setColor(30, 255, 30, 255)
                    .setNormal(pose, 0, 1, 0);

            poseStack.popPose();
        }*/
    }
}
