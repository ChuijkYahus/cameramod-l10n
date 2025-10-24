package net.mehvahdjukaar.vista.integration;

import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PictureTapeItem extends Item {
    public PictureTapeItem(Item.Properties properties) {
        super(properties);
    }

    public List<ItemStack> getContent(ItemStack stack) {
        return stack.getOrDefault(ExposureCompat.ITEM_LIST_COMPONENT.get(), Collections.emptyList());
    }

    public void setContent(ItemStack stack, List<ItemStack> content) {
        stack.set(ExposureCompat.ITEM_LIST_COMPONENT.get(), content);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        if (player instanceof ServerPlayer serverPlayer) {
            int albumSlot = usedHand == InteractionHand.OFF_HAND ? 40 : player.getInventory().selected;
            this.open(serverPlayer, itemStack, albumSlot);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    public void open(ServerPlayer player, final ItemStack albumStack, final int albumSlot) {
        /*
        MenuProvider menuProvider = new MenuProvider(this) {
            public @NotNull Component getDisplayName() {
                return albumStack.getHoverName();
            }

            public @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
                return new AlbumMenu(containerId, playerInventory, albumSlot);
            }
        };

        PlatformHelper.openMenu(player, menuProvider, (buffer) -> {
            buffer.writeVarInt(albumSlot);
        });
        */
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int photographsCount = this.getContent(stack).size();
        if (photographsCount > 0) {
            tooltipComponents.add(Component.translatable("item.exposure.album.tooltip.photos_count", photographsCount));
        }
    }

    @ForgeOverride
    public boolean shouldPlayEquipAnimation(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() != newStack.getItem();
    }

    //TODO. faric override

}