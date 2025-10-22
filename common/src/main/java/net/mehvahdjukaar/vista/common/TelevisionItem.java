package net.mehvahdjukaar.vista.common;

import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class TelevisionItem extends BlockItem {

    public TelevisionItem(Block block, Properties properties) {
        super(block, properties);
    }

    @ForgeOverride
    public boolean isEnderMask(ItemStack stack, Player player, EnderMan enderMan) {
        return true;
    }
}
