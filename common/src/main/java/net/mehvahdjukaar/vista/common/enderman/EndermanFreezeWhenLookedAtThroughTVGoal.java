package net.mehvahdjukaar.vista.common.enderman;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EndermanFreezeWhenLookedAtThroughTVGoal extends Goal {
    private final EnderMan enderman;
    @Nullable
    private Player target;

    private AbstractEndermanObservationController observer;

    public EndermanFreezeWhenLookedAtThroughTVGoal(EnderMan enderman) {
        this.enderman = enderman;
        this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE, Flag.LOOK));
    }


    private void prime(Player player, AbstractEndermanObservationController observer) {
        this.target = player;
        this.observer = observer;
        this.enderman.setBeingStaredAt();
        this.enderman.setTarget(player);
    }


    @Override
    public boolean canUse() {
        if (observer == null) {
            return false;
        }
        if (observer.isInvalid()) {
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
        return observer.isPlayerLookingAtEnderman(enderman, target);

    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && isCameraViewValid();
    }

    @Override
    public void start() {
        VistaMod.ENDERMAN_CAP.set(this.enderman, true);
        this.enderman.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.enderman.getLookControl().setLookAt(this.target.getX(), this.target.getEyeY(), this.target.getZ());
    }

    @Override
    public void stop() {
        super.stop();
        this.observer = null;
    }

    public static boolean anger(EnderMan man, Player player, AbstractEndermanObservationController observer) {
        EndermanFreezeWhenLookedAtThroughTVGoal goal = findGoal(man);
        if (goal != null) {
            goal.prime(player, observer);
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
