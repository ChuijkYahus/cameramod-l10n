package net.mehvahdjukaar.vista.common;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.UUID;


public class EchoCassetteItem extends Item {

    public EchoCassetteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (be instanceof ViewFinderBlockEntity feed) {
            if (!level.isClientSide) {

                ItemStack stack = context.getItemInHand();
                stack.set(VistaMod.LINKED_FEED_COMPONENT.get(), feed.getUUID());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
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
            if (PlatHelper.getPhysicalSide().isClient()) {
                Level level = VistaModClient.getLevel();
                var connection = ViewFinderConnection.get(level);
                if (connection == null) return;
                GlobalPos gp = connection.getLinkedFeedLocation(feedId);
                if (gp == null) {
                    tooltipComponents.add(Component.translatable("tooltip.vista.hollow_cassette.linked_unknown"));
                } else {
                    if (gp.dimension() == level.dimension()) {
                        tooltipComponents.add(Component.translatable("tooltip.vista.hollow_cassette.linked", gp.pos()));
                    } else {
                        tooltipComponents.add(Component.translatable("tooltip.vista.hollow_cassette.linked_away", gp.dimension()));
                    }
                }
            }
        }
    }
}
