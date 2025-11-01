package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.common.ViewFinderBlock;
import net.mehvahdjukaar.vista.common.ViewFinderBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
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
    }

    public void renderModel(ViewFinderBlockEntity tile, float partialTick, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        boolean isControlledByLocalInstance = ViewFinderController.isActive() &&
                ViewFinderController.access.getInternalTile() == tile;

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
        poseStack.scale(0.75f,0.75f,0.75f);
        poseStack.translate(0.25,0.5,0.25);

        ItemStack stack = Items.CREEPER_HEAD.getDefaultInstance();
      //  Minecraft.getInstance().getItemRenderer()
        //        .renderStatic(stack, ItemDisplayContext.NONE,  packedLight, packedOverlay,poseStack,
          //              bufferSource, tile.getLevel(), 0);

        poseStack.popPose();



    }


    public static LayerDefinition createMesh() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition legs = partdefinition.addOrReplaceChild("legs", CubeListBuilder.create()
                        .texOffs(0, 36).addBox(5.0F, -4.0F, -2.0F, 2.0F, 10.0F, 4.0F)
                        .texOffs(12, 36).addBox(-7.0F, -4.0F, -2.0F, 2.0F, 10.0F, 4.0F),
                PartPose.ZERO);

        PartDefinition head = legs.addOrReplaceChild("head_pivot", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, -0.1745F, 0.0F, 0.0F));

        PartDefinition bone = head.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-5.0F, -6.0F, -4F, 10.0F, 12.0F, 8.0F),
                PartPose.ZERO);

        PartDefinition base = partdefinition.addOrReplaceChild("base", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, 6.0F, -7.0F, 14.0F, 2.0F, 14.0F),
                PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}
