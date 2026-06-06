package net.mehvahdjukaar.vista.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Invoker("bobView")
    void vista$bobView(PoseStack poseStack, float partialTick);

    @Invoker("bobHurt")
    void vista$bobHurt(PoseStack poseStack, float partialTick);
}
