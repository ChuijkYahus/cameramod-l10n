package net.mehvahdjukaar.vista.common.cassette;

import net.mehvahdjukaar.moonlight.api.resources.assets.LangBuilder;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;
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
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        super.onCraftedPostProcess(stack, level);
        assignCustomCassette(stack, level);
    }

    public static void assignCustomCassette(ItemStack stack, Level level) {
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            String name = customName.getString().toLowerCase(Locale.ROOT);
            assignCustomCassette(stack, level, name);
        }

    }

    public static void assignCustomCassette(ItemStack stack, Level level, String name) {
        //supporters cassettes
        for (var h : level.registryAccess().registryOrThrow(VistaMod.CASSETTE_TAPE_REGISTRY_KEY).getTagOrEmpty(VistaMod.SUPPORTER_TAPES_TAG)) {
            var key = h.unwrapKey().get();
            if (key.location().getPath().equals(name)) {
                stack.set(VistaMod.CASSETTE_TAPE_COMPONENT.get(), h);
                stack.remove(DataComponents.CUSTOM_NAME);
                break;
            }
        }
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        var tape = stack.get(VistaMod.CASSETTE_TAPE_COMPONENT.get());
        if (tape != null) {
            tape.unwrapKey().ifPresent((resourceKey) -> {
                ResourceLocation location = resourceKey.location();
                if (tape.is(VistaMod.SUPPORTER_TAPES_TAG)) {
                    tooltipComponents.add(Component.literal(LangBuilder.getReadableName(location.getPath()))
                            .withStyle(ChatFormatting.GRAY));
                } else {
                    tooltipComponents.add(Component.translatable(location.toLanguageKey("cassette_tape", "tooltip")).withStyle(ChatFormatting.GRAY));
                }
            });
        }
    }
}
