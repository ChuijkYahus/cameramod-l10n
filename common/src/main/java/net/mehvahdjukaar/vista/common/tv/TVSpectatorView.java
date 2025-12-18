package net.mehvahdjukaar.vista.common.tv;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;

public record TVSpectatorView(Player player, Vec2 localHit, double distance) {
}
