package net.mehvahdjukaar.vista.common;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Optional;

public class CassetteItem extends Item {

    public CassetteItem(Properties properties) {
        super(properties);
    }


    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return super.getTooltipImage(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        var tape = stack.get(VistaMod.CASSETTE_TAPE_COMPONENT.get());
        if(tape != null){
            tape.unwrapKey().ifPresent((resourceKey) -> {
                tooltipComponents.add(Component.translatable(resourceKey.location().toLanguageKey("cassette_tape", "title")).withStyle(ChatFormatting.YELLOW));
                tooltipComponents.add(Component.translatable(resourceKey.location().toLanguageKey("cassette_tape", "author")).withStyle(ChatFormatting.GRAY));
            });
        }
    }
}
