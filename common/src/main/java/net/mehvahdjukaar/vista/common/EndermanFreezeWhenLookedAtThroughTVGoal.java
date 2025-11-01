package net.mehvahdjukaar.vista.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public class EndermanFreezeWhenLookedAtThroughTVGoal extends Goal {
    private final EnderMan enderman;
    @Nullable
    private Player target;

    private TVBlockEntity television;

    public EndermanFreezeWhenLookedAtThroughTVGoal(EnderMan enderman) {
        this.enderman = enderman;
        this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
    }


    private void prime(Player player, TVBlockEntity tv) {
        this.target = player;
        this.television = tv;
        this.enderman.setBeingStaredAt();
        this.enderman.setTarget(player);
    }


    @Override
    public boolean canUse() {
        if (television == null) {
            return false;
        }
        if (television.isRemoved()) {
            return false;
        }
        LivingEntity t = this.enderman.getTarget();
        if (t instanceof Player p) {
            this.target = p;
            return true;
        }
        return false;
    }

    private boolean isCameraViewValid() {
        TVSpectatorView view = television.getPlayerViewHit(target);
        if (view == null) {
            return false;
        }

        Level l = enderman.level();
        UUID feed = television.getLinkedFeedUUID();
        ViewFinderBlockEntity viewFinder = LiveFeedConnectionManager.findLinkedViewFinder(l, feed);
        if (viewFinder == null) return false;
        viewFinder.angerEndermenBeingLookedAt()
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && isCameraViewValid();
    }

    @Override
    public void start() {
        this.enderman.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.enderman.getLookControl().setLookAt(this.target.getX(), this.target.getEyeY(), this.target.getZ());
    }

    @Override
    public void stop() {
        super.stop();
        this.television = null;
    }

    public static boolean anger(EnderMan man, Player player, ViewFinderBlockEntity viewFinder, TVBlockEntity television) {
        EndermanFreezeWhenLookedAtThroughTVGoal goal = findGoal(man);
        if (goal != null) {
            goal.prime(player, television);
            return true;
        }
        return false;
    }


    @Nullable
    private static EndermanFreezeWhenLookedAtThroughTVGoal findGoal(EnderMan man) {
        for (var goal : man.goalSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof EndermanFreezeWhenLookedAtThroughTVGoal g) {
                return g;
            }
        }
        return null;
    }
}
