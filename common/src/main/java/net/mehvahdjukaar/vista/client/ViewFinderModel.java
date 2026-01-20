package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class ViewFinderModel extends Model {


    public ViewFinderModel(EntityRendererProvider.Context context) {
        super(null);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {

    }


    public static LayerDefinition createMesh() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition legs = partdefinition.addOrReplaceChild("legs", CubeListBuilder.create()
                        .texOffs(0, 36).addBox(5.0F, -4.0F, -2.0F, 2.0F, 10.0F, 4.0F)
                        .texOffs(12, 36).addBox(-7.0F, -4.0F, -2.0F, 2.0F, 10.0F, 4.0F),
                PartPose.ZERO);

        PartDefinition pivot = legs.addOrReplaceChild("head_pivot", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, -0.1745F, 0.0F, 0.0F));

        pivot.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-5.0F, -6.0F, -4F, 10.0F, 12.0F, 8.0F),
                PartPose.ZERO);

        PartDefinition base = partdefinition.addOrReplaceChild("base", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, 6.0F, -7.0F, 14.0F, 2.0F, 14.0F),
                PartPose.ZERO);

        //camera lens

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}
