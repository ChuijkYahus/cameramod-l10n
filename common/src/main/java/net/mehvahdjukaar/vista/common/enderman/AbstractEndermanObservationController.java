package net.mehvahdjukaar.vista.common.enderman;

import com.mojang.authlib.GameProfile;
import net.mehvahdjukaar.moonlight.api.util.FakePlayerManager;
import net.mehvahdjukaar.vista.common.tv.TVSpectatorView;
import net.mehvahdjukaar.vista.common.view_finder.EndermanLookResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Common pipeline for any block that lets a player observe endermen indirectly (TV through a
 * remote ViewFinder, mirror via reflection, ...). Subclasses provide:
 * <ul>
 *   <li>{@link #level()}, {@link #fakePlayerProfile()}, distance constants, {@link #isInvalid()}</li>
 *   <li>{@link #openTick()} — per-tick context (screen geometry, endermen anchor, and the
 *   per-hit fake-player orient function). Return null to skip the tick (e.g. TV broadcast not
 *   resolvable).</li>
 * </ul>
 * Also hosts the shared math helpers, exposed as protected statics so subclasses can reuse them
 * when building their {@link TickContext}.
 */
public abstract class AbstractEndermanObservationController {

    public record ScreenBasis(Vec3 center, Vec3 normal, Vec3 right, Vec3 up, float width, float height) {
    }

    @FunctionalInterface
    public interface FakePlayerOrient {
        /**
         * Position + orient the fake player so that {@link EnderMan#isLookingAtMe(Player)}
         * answers "is this enderman in the path of the world ray emerging from the given hit".
         * Return false to skip this hit.
         */
        boolean orient(Player fakePlayer, TVSpectatorView hit);
    }

    protected record TickContext(ScreenBasis screenBasis, BlockPos endermenAnchor, FakePlayerOrient orient) {
    }

    protected abstract Level level();

    protected abstract GameProfile fakePlayerProfile();

    protected abstract float playersScreenDist();

    protected abstract float endermenSearchDist();

    /** True if the controller's backing block is gone — observed endermen should drop the goal. */
    public abstract boolean isInvalid();

    @Nullable
    protected abstract TickContext openTick();

    /** Called from {@link EndermanFreezeWhenLookedAtThroughTVGoal#canContinueToUse()}. */
    public boolean isPlayerLookingAtEnderman(EnderMan enderMan, Player player) {
        TickContext ctx = openTick();
        if (ctx == null) return false;
        TVSpectatorView view = getPlayerHit(player, ctx.screenBasis(), playersScreenDist());
        if (view == null) return false;
        Player fakePlayer = FakePlayerManager.get(fakePlayerProfile(), level());
        return !checkEndermenLookedAt(List.of(view), List.of(enderMan), fakePlayer, ctx.orient()).isEmpty();
    }

    public boolean tick() {
        TickContext ctx = openTick();
        if (ctx == null) return false;
        Level level = level();

        List<TVSpectatorView> views = findPlayersLookingAtScreen(level.players(), ctx.screenBasis(), playersScreenDist());
        if (views.isEmpty()) return false;

        List<EnderMan> enderMen = findEndermenNear(level, ctx.endermenAnchor(), endermenSearchDist());
        if (enderMen.isEmpty()) return false;

        Player fakePlayer = FakePlayerManager.get(fakePlayerProfile(), level);
        List<EndermanLookResult> looks = checkEndermenLookedAt(views, enderMen, fakePlayer, ctx.orient());

        boolean anyAnger = false;
        for (var r : looks) {
            if (EndermanFreezeWhenLookedAtThroughTVGoal.anger(r.enderman(), r.player(), this)) anyAnger = true;
        }
        return anyAnger;
    }

    // --- shared math helpers ---

    protected static List<TVSpectatorView> findPlayersLookingAtScreen(Collection<? extends Player> players,
                                                                      ScreenBasis sb, float maxDist) {
        if (players.isEmpty()) return List.of();
        List<TVSpectatorView> result = new ArrayList<>();
        for (Player p : players) {
            if (p.isCreative()) continue;
            TVSpectatorView vr = getPlayerHit(p, sb, maxDist);
            if (vr != null) result.add(vr);
        }
        return result;
    }

    protected static List<EnderMan> findEndermenNear(Level level, BlockPos anchor, float range) {
        Vec3 center = Vec3.atCenterOf(anchor);
        double rangeSq = (double) range * range;
        AABB aabb = new AABB(anchor).inflate(range);
        return level.getEntitiesOfClass(EnderMan.class, aabb,
                em -> em.distanceToSqr(center.x, center.y, center.z) < rangeSq);
    }

    protected static List<EndermanLookResult> checkEndermenLookedAt(List<TVSpectatorView> views,
                                                                    List<EnderMan> endermen,
                                                                    Player fakePlayer,
                                                                    FakePlayerOrient orient) {
        List<EndermanLookResult> results = new ArrayList<>();
        for (TVSpectatorView vr : views) {
            if (!orient.orient(fakePlayer, vr)) continue;
            for (EnderMan man : endermen) {
                if (man.isLookingAtMe(fakePlayer)) {
                    results.add(new EndermanLookResult(vr.player(), man));
                }
            }
        }
        return results;
    }

    /** Ray-cast the player's view onto a screen rect. Returns local UV in [-0.5, 0.5], or null. */
    @Nullable
    protected static TVSpectatorView getPlayerHit(Player player, ScreenBasis sb, float maxDist) {
        final double EPS = 1e-6;
        Vec3 eyePos = player.getEyePosition(1.0F);

        Vec3 eyeToCenter = sb.center.subtract(eyePos);
        double distSq = eyeToCenter.lengthSqr();
        if (distSq > (maxDist * maxDist)) return null;
        eyeToCenter = eyeToCenter.scale(1.0 / Math.sqrt(distSq));

        if (eyeToCenter.dot(sb.normal) > 0.0) return null;

        Vec3 playerView = player.getViewVector(1.0F).normalize();
        double denom = playerView.dot(sb.normal);
        if (Math.abs(denom) < EPS) return null;
        double t = sb.center.subtract(eyePos).dot(sb.normal) / denom;
        if (t <= 0.0) return null;

        Vec3 hit = eyePos.add(playerView.scale(t));
        Vec3 local = hit.subtract(sb.center);
        double x = local.dot(sb.right);
        double y = local.dot(sb.up);

        if (Math.abs(x) <= (sb.width / 2f) && Math.abs(y) <= (sb.height / 2f)) {
            return new TVSpectatorView(player, new Vec2((float) x / sb.width, (float) y / sb.height), t);
        }
        return null;
    }
}
