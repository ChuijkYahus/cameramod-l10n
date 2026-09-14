package net.mehvahdjukaar.vista.platform;

import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.common.cassette.ITvCassette;
import net.mehvahdjukaar.vista.common.tv.TVBlock;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.mehvahdjukaar.vista.configs.CommonConfigs;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class TvEnergyHandler implements IEnergyStorage {

    private static final int MIN_CAPACITY = 1600;
    private static final int BUFFER_TICKS = 40;
    private static final int TURN_ON_TICKS = 20;

    private final TVBlockEntity tv;
    private int stored = 0;
    private boolean running = false;

    public static TvEnergyHandler getOrCreate(TVBlockEntity tv) {
        if (tv.energyCap == null){
            tv.energyCap = new TvEnergyHandler(tv);
        }
        return (TvEnergyHandler) tv.energyCap;
    }

    public TvEnergyHandler(TVBlockEntity tv) {
        this.tv = tv;
    }

    private int getConsumptionRate() {
        Vec2i v = tv.getConnectedCount();
        return CommonConfigs.TV_ENERGY_CONSUMPTION_RATE.get() * Math.max(1, v.x()) * Math.max(1, v.y());
    }

    public void tick() {
        int cost = getConsumptionRate();
        stored = Math.min(stored, getMaxEnergyStored()); //tv could have shrunk
        if (running) running = stored >= cost;
        else running = stored >= cost * TURN_ON_TICKS;

        boolean hasCassette = !tv.getDisplayedItem().isEmpty() &&
                tv.getDisplayedItem().getItem() instanceof ITvCassette;
        boolean isPowered = tv.getBlockState().getValue(TVBlock.POWER_STATE).isOn();
        if (running && hasCassette && isPowered) stored -= cost;

        tv.setHasEnergy(running);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int accepted = Math.min(maxReceive, getMaxEnergyStored() - stored);
        if (accepted <= 0) return 0;
        if (!simulate) stored += accepted;
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return stored;
    }

    @Override
    public int getMaxEnergyStored() {
        return Math.max(MIN_CAPACITY, getConsumptionRate() * BUFFER_TICKS);
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
