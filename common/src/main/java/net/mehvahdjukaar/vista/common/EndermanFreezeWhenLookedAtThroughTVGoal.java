package net.mehvahdjukaar.vista.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EndermanFreezeWhenLookedAtThroughTVGoal extends Goal {
    private final EnderMan enderman;
    @Nullable
    private LivingEntity target;

    public EndermanFreezeWhenLookedAtThroughTVGoal(EnderMan enderman) {
        this.enderman = enderman;
        this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        this.target = this.enderman.getTarget();
        if (!(this.target instanceof Player)) {
            return false;
        } else {
            double d = this.target.distanceToSqr(this.enderman);
            return !(d > 256.0) && this.enderman.isLookingAtMe((Player) this.target);
        }
    }

    @Override
    public void start() {
        this.enderman.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.enderman.getLookControl().setLookAt(this.target.getX(), this.target.getEyeY(), this.target.getZ());
    }


    public static void anger(Player player, ViewFinderBlockEntity viewFinderBlockEntity) {

    }
}
