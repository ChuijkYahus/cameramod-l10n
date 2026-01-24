package net.mehvahdjukaar.vista.common.tv;

import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class TVItem extends BlockItem {

    public TVItem(Block block, Properties properties) {
        super(block, properties);
    }

    @ForgeOverride
    public boolean isEnderMask(ItemStack stack, Player player, EnderMan enderMan) {
        return true;
    }
}
