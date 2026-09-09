package net.mehvahdjukaar.vista.common.mob_gaze;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EndermanFreezeWhenLookedAtThroughTVGoal extends Goal {
    private final EnderMan enderman;
    @Nullable
    private GazeRedirect.ScreenGaze gaze;

    public EndermanFreezeWhenLookedAtThroughTVGoal(EnderMan enderman) {
        this.enderman = enderman;
        this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.enderman.getRandom().nextInt(reducedTickDelay(10)) != 0) return false;
        this.gaze = GazeRedirect.findWatcherThroughScreens(this.enderman.level(), this.enderman);
        return this.gaze != null;
    }

    @Override
    public boolean canContinueToUse() {
        this.gaze = GazeRedirect.traceGazeTo(this.enderman.level(), this.gaze.player(), this.enderman);
        return this.gaze != null;
    }

    @Override
    public void start() {
        VistaMod.ENDERMAN_CAP.set(this.enderman, true);
        this.enderman.setBeingStaredAt();
        this.enderman.setTarget(this.gaze.player());
        this.enderman.getNavigation().stop();
    }

    @Override
    public void tick() {
        Player watcher = this.gaze.player();
        this.enderman.getLookControl().setLookAt(watcher.getX(), watcher.getEyeY(), watcher.getZ());
        for (TVBlockEntity tv : this.gaze.tvsPassed()) {
            tv.endermanLook.markAngeredEndermanOnScreen();
        }
    }

    @Override
    public void stop() {
        this.gaze = null;
    }
}
