package net.mehvahdjukaar.vista.common.picture_tape;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.cassette.ITvCassette;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A roll of tape that stores up to {@link #MAX_MAPS} filled maps. Right-clicking opens a
 * horizontally scrolling gallery where maps can be added, viewed and taken back out.
 */
public class PictureTapeItem extends Item implements ITvCassette {

    public static final int MAX_MAPS = 16;

    public PictureTapeItem(Properties properties) {
        super(properties);
    }

    public static PictureTapeContent getContent(ItemStack stack) {
        return stack.getOrDefault(VistaMod.PICTURE_TAPE_CONTENT.get(), PictureTapeContent.EMPTY);
    }

    public static void setContent(ItemStack stack, PictureTapeContent content) {
        stack.set(VistaMod.PICTURE_TAPE_CONTENT.get(), content);
    }

    /**
     * Whether the given stack may be stored on the tape. Filled maps are always allowed; other
     * picture types (e.g. Exposure photographs) are contributed by integrations via
     * {@link PictureTapeEntries}.
     */
    public static boolean isValidEntry(ItemStack stack) {
        return PictureTapeEntries.isValid(stack);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = usedHand == InteractionHand.OFF_HAND ? 40 : player.getInventory().selected;
            open(serverPlayer, stack, slot);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public void open(ServerPlayer player, final ItemStack tapeStack, final int tapeSlot) {
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return tapeStack.getHoverName();
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player p) {
                return new PictureTapeMenu(containerId, playerInventory, tapeSlot);
            }
        };
        PlatHelper.openCustomMenu(player, menuProvider, buffer -> buffer.writeVarInt(tapeSlot));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int count = getContent(stack).size();
        if (count > 0) {
            tooltip.add(Component.translatable("item.vista.picture_tape.pictures_count", count)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public int getAnalogSignalStrength(ItemStack stack) {
        return Math.min(15, getContent(stack).size());
    }
}
