package net.mehvahdjukaar.vista.common.mob_gaze;

import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class TVEndermanLook {

    public final IntAnimationState animation = new IntAnimationState(20, 20, 0.6f);
    private boolean angeredEndermanOnScreen = false;
    private int markedTicks = 0;

    public void markAngeredEndermanOnScreen() {
        this.markedTicks = 2;
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        boolean onScreen = markedTicks > 0;
        if (onScreen) markedTicks--;
        if (onScreen != angeredEndermanOnScreen) {
            angeredEndermanOnScreen = onScreen;
            level.blockEvent(pos, state.getBlock(), 1, onScreen ? 1 : 0);
        }
    }

    public void clientTick() {
        if (angeredEndermanOnScreen) {
            animation.increment();
        } else {
            animation.decrement();
        }
    }

    public void onBlockEvent(int param) {
        this.angeredEndermanOnScreen = param > 0;
    }
}
