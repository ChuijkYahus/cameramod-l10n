package net.mehvahdjukaar.vista.common;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record TvViewHitResult(Player player, Vec2 localHit, double distance) {
}
