package net.mehvahdjukaar.vista.common;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;


public class EchoCassetteItem extends Item {


    public EchoCassetteItem(Properties properties) {
        super(properties);
    }


    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            ItemStack stack = context.getItemInHand();
            stack.set(VistaMod.LINKED_FEED_COMPONENT.get(),
                    UUID.randomUUID());
        }
        return super.useOn(context);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) ||
                stack.get(VistaMod.LINKED_FEED_COMPONENT.get()) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        UUID feedId = stack.get(VistaMod.LINKED_FEED_COMPONENT.get());
        if (feedId != null) {
            tooltipComponents.add(Component.translatable("vista.tooltip.echo_cassette.linked"));
        }
    }
}
