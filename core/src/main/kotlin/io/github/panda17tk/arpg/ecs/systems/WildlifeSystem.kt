package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.FamilyRole
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.config.WildState
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.CircleCollision
import io.github.panda17tk.arpg.sim.Collision
import io.github.panda17tk.arpg.sim.CrashModel
import io.github.panda17tk.arpg.sim.PlanetField
import io.github.panda17tk.arpg.sim.SocietyTuning
import io.github.panda17tk.arpg.sim.WildAI
import io.github.panda17tk.arpg.sim.WorldState
import io.github.panda17tk.arpg.sim.toPressure
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * Drives the wildlife layer — mute wild animals ([LifeKind.WILDLIFE]) that graze, herd, flee and hunt
 * each other instead of swarming the player. Hostile/sapient mobs are left to [AISystem] (which skips
 * wildlife); this system owns wildlife movement + collision entirely, so wildlife never deal contact
 * damage or run attacks. The brain is the pure [WildAI]; here we just sense the world and move.
 */
class WildlifeSystem : IteratingSystem(family { all(Mob, Transform, Velocity, Body, Health, Facing) }) {
    private val map: TileMap = world.inject()
    private val planetField: PlanetField = world.inject()
    private val worldState: WorldState = world.inject()
    private val players by lazy { world.family { all(PlayerTag, Transform) } }
    private val mobs by lazy { world.family { all(Mob, Transform, Health) } }
    private val wanderRng = Rng(0x21D7A11FEL) // private stream: keeps idle wander out of the combat RNG

    override fun onTickEntity(entity: Entity) {
        val m = entity[Mob]
        if (m.def.lifeKind != LifeKind.WILDLIFE) return // only wild animals; AISystem handles the rest
        if (m.def.wildRole == WildRole.SCHOOL) return // v2.131: boid fish belong to SchoolFishSystem

        val t = entity[Transform]
        val v = entity[Velocity]
        val b = entity[Body]
        val f = entity[Facing]
        val h = entity[Health]
        val dt = deltaTime

        // Knockback + flung-momentum bleed off (same feel as AISystem) so a struck animal coasts then recovers.
        v.vx *= KNOCK_DECAY.pow(dt); v.vy *= KNOCK_DECAY.pow(dt)
        v.driftX *= DRIFT_DECAY.pow(dt); v.driftY *= DRIFT_DECAY.pow(dt)
        if (h.hitFlash > 0f) h.hitFlash -= dt

        // --- Sense: player, nearest predator, nearest prey, herd centroid ---
        var px = t.x; var py = t.y; var hasPlayer = false
        players.forEach { e -> if (!hasPlayer) { val pt = e[Transform]; px = pt.x; py = pt.y; hasPlayer = true } }
        val pdist = hypot(px - t.x, py - t.y)

        var predatorNear = false; var predX = 0f; var predY = 0f; var predBest = Float.MAX_VALUE
        var preyNear = false; var preyX = 0f; var preyY = 0f; var preyBest = Float.MAX_VALUE
        var herdSumX = 0f; var herdSumY = 0f; var herdCount = 0
        mobs.forEach { other ->
            if (other == entity) return@forEach
            val ot = other[Transform]; val od = other[Mob].def
            val od2 = (ot.x - t.x) * (ot.x - t.x) + (ot.y - t.y) * (ot.y - t.y)
            if (od.lifeKind == LifeKind.WILDLIFE && (od.wildRole == WildRole.PREDATOR || od.wildRole == WildRole.APEX)) {
                if (od2 < PRED_SENSE2 && od2 < predBest) { predBest = od2; predatorNear = true; predX = ot.x; predY = ot.y }
            }
            val huntable = (od.lifeKind == LifeKind.WILDLIFE && od.wildRole in HUNTABLE) || od.familyRole == FamilyRole.CHILD ||
                (od.lifeKind == LifeKind.SAPIENT && m.def.bravery >= io.github.panda17tk.arpg.sim.Predation.BRAVE &&
                    (m.def.wildRole == WildRole.PREDATOR || m.def.wildRole == WildRole.APEX)) // v2.132 敵対
            if (huntable && od2 < PREY_SENSE2 && od2 < preyBest) { preyBest = od2; preyNear = true; preyX = ot.x; preyY = ot.y }
            if (other[Mob].kind == m.kind && od2 < HERD_SENSE2) { herdSumX += ot.x; herdSumY += ot.y; herdCount++ }
        }
        val herdCx = if (herdCount > 0) herdSumX / herdCount else t.x
        val herdCy = if (herdCount > 0) herdSumY / herdCount else t.y
        val homeDist = hypot(m.homeX - t.x, m.homeY - t.y)
        val isNestGuard = m.def.wildRole == WildRole.NEST_GUARD
        // A nest-guard "regroups" to its nest; a herd animal regroups to the herd centre.
        val herdSeparated = if (isNestGuard) homeDist > HOME_LEASH
        else herdCount > 0 && hypot(herdCx - t.x, herdCy - t.y) > HERD_SEPARATION
        val territory = m.def.territoryRadius
        // The nest is threatened when the player enters the territory, or a wild predator prowls nearby.
        val nestThreatened = territory > 0f && (pdist < territory || predatorNear)

        // --- Decide ---
        // Living Planets: a shaken food web spooks prey sooner; a slain apex / hungry world starves predators faster.
        val wildP = worldState.context?.let { SocietyTuning.wild(worldState.society.toPressure(it)) }
        m.hunger = (m.hunger + dt * HUNGER_RATE * (wildP?.hungerMul ?: 1f)).coerceIn(0f, 1f)
        val effFear = (m.def.fear * (wildP?.fearMul ?: 1f)).coerceIn(0f, 1f)
        val state = WildAI.nextState(
            m.def.wildRole, h.hp / h.hpMax, pdist,
            predatorNear, preyNear, nestThreatened, herdSeparated, m.hunger, effFear,
            fleeSuppressed = worldState.spawnTweaks.fleeSuppressed, // LP v2.27: gratitude calms herds/hatchlings
        )
        m.wildState = state
        if (state == WildState.Feed || (state == WildState.Chase && preyNear && preyBest < FEED_DIST2)) {
            m.hunger = (m.hunger - dt * FEED_RATE).coerceAtLeast(0f) // a meal sates it
        }

        // --- Move ---
        var dx = 0f; var dy = 0f; var mul = 0f
        when (state) {
            WildState.Graze, WildState.Feed, WildState.Sleep -> mul = 0f
            WildState.Threaten -> { if (pdist > 1e-3f) { f.x = (px - t.x) / pdist; f.y = (py - t.y) / pdist }; mul = 0f }
            WildState.GuardNest -> { // hold the nest: stray back if we've wandered off, else face the threat and stand
                if (homeDist > GUARD_LEASH) { val l = hypot(m.homeX - t.x, m.homeY - t.y); if (l > 1e-3f) { dx = (m.homeX - t.x) / l; dy = (m.homeY - t.y) / l }; mul = HERD_MUL }
                else if (pdist > 1e-3f) { f.x = (px - t.x) / pdist; f.y = (py - t.y) / pdist; mul = 0f }
            }
            WildState.Wander -> { rollWander(m, f, dt); dx = f.x; dy = f.y; mul = WANDER_MUL }
            WildState.Hunt -> { rollWander(m, f, dt); dx = f.x; dy = f.y; mul = HUNT_MUL }
            WildState.Flee -> {
                val tx = if (predatorNear) predX else px; val ty = if (predatorNear) predY else py
                val l = hypot(t.x - tx, t.y - ty); if (l > 1e-3f) { dx = (t.x - tx) / l; dy = (t.y - ty) / l }; mul = FLEE_MUL
                // v2.131 縮地: a blink-swimmer vanishes a stride away instead of merely running
                // (MobActionSystem executes the teleport + afterimages; dodgeCd doubles as its cooldown).
                if (m.def.canBlink && (dx != 0f || dy != 0f)) {
                    entity.getOrNull(io.github.panda17tk.arpg.ecs.components.MobAction)?.let { a ->
                        if (a.dodgeCd <= 0f && a.blinkT <= 0f && a.blinkChargeT <= 0f) {
                            a.blinkDx = dx * BLINK_FLEE_DIST; a.blinkDy = dy * BLINK_FLEE_DIST
                            a.blinkTotal = 0.12f; a.blinkT = 0.12f
                            a.dodgeCd = BLINK_FLEE_CD
                        }
                    }
                }
            }
            WildState.Herd -> {
                val l = hypot(herdCx - t.x, herdCy - t.y); if (l > 1e-3f) { dx = (herdCx - t.x) / l; dy = (herdCy - t.y) / l }; mul = HERD_MUL
            }
            WildState.ReturnNest -> { // head back to the nest/home
                val l = hypot(m.homeX - t.x, m.homeY - t.y); if (l > 1e-3f) { dx = (m.homeX - t.x) / l; dy = (m.homeY - t.y) / l }; mul = HERD_MUL
            }
            WildState.Stalk -> { val l = hypot(preyX - t.x, preyY - t.y); if (l > 1e-3f) { dx = (preyX - t.x) / l; dy = (preyY - t.y) / l }; mul = STALK_MUL }
            WildState.Chase -> {
                val tx = if (preyNear) preyX else px; val ty = if (preyNear) preyY else py
                val l = hypot(tx - t.x, ty - t.y); if (l > 1e-3f) { dx = (tx - t.x) / l; dy = (ty - t.y) / l }; mul = CHASE_MUL
                // v2.132 縮地: a blink-hunter closes the last stretch of a chase in one stride,
                // landing a pace short of the mark (MobActionSystem executes the teleport).
                if (m.def.canBlink && l > BLINK_STRIKE_MIN && l < BLINK_STRIKE_MAX) {
                    entity.getOrNull(io.github.panda17tk.arpg.ecs.components.MobAction)?.let { a ->
                        if (a.dodgeCd <= 0f && a.blinkT <= 0f && a.blinkChargeT <= 0f) {
                            a.blinkDx = dx * (l - 28f); a.blinkDy = dy * (l - 28f)
                            // v2.138 公正な野生: the strike CHARGES first (MobActionSystem's telegraph),
                            // a readable beat before the teleport — the flee-blink stays instant.
                            a.blinkTotal = 0.10f; a.blinkChargeT = BLINK_STRIKE_WINDUP
                            a.dodgeCd = BLINK_FLEE_CD
                        }
                    }
                }
            }
        }
        // v2.136 泳ぐ魚: a space fish never hangs dead in the void — where a land animal would
        // stand (graze, threaten, feed, sleep), the fish keeps swimming a slow wander instead.
        if (mul == 0f && m.def.swims) { rollWander(m, f, dt); dx = f.x; dy = f.y; mul = WANDER_MUL }
        if (dx != 0f || dy != 0f) { val l = hypot(dx, dy); if (l > 1e-3f) { dx /= l; dy /= l; f.x = dx; f.y = dy } }

        // Apply movement riding on any leftover drift, stopping at walls (no crash/fall damage), same as AISystem.
        val moveEff = m.speed * mul
        val r1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, dx * moveEff * dt + (v.vx + v.driftX) * dt, 0f)
        if (r1.hitX) { v.driftX = 0f; if (m.def.swims) f.x = -f.x } // v2.136: a fish turns off the rock, not pins against it
        val r2 = Collision.moveAndCollide(map, r1.x, r1.y, b.halfW, b.halfH, 0f, dy * moveEff * dt + (v.vy + v.driftY) * dt)
        if (r2.hitY) { v.driftY = 0f; if (m.def.swims) f.y = -f.y }
        t.x = r2.x; t.y = r2.y
        val pc = CircleCollision.resolve(t.x, t.y, b.halfW, v.vx + v.driftX, v.vy + v.driftY, planetField.planets)
        if (pc.hit) {
            t.x = pc.x; t.y = pc.y
            val (rdx, rdy) = CrashModel.rebound(v.driftX, v.driftY, pc.normalX, pc.normalY, CRASH_RESTITUTION)
            v.driftX = rdx; v.driftY = rdy
        }
    }

    /** Re-roll a slow idle heading every couple of seconds so wanderers (and prowling hunters) amble naturally. */
    private fun rollWander(m: Mob, f: Facing, dt: Float) {
        m.wanderCd -= dt
        if (m.wanderCd <= 0f) {
            val a = wanderRng.nextFloat() * TAU
            f.x = cos(a); f.y = sin(a)
            m.wanderCd = WANDER_REROLL_MIN + wanderRng.nextFloat() * WANDER_REROLL_VAR
        }
    }

    companion object {
        private val HUNTABLE = setOf(WildRole.PREY, WildRole.HERD, WildRole.HATCHLING, WildRole.SWARM, WildRole.SCHOOL) // v2.131: teeth follow the schools
        private const val BLINK_FLEE_DIST = 110f // v2.131 縮地: one stride of vanish
        private const val BLINK_FLEE_CD = 2.6f
        private const val BLINK_STRIKE_MIN = 80f  // v2.132 縮地: too close needs no stride...
        private const val BLINK_STRIKE_MAX = 220f // ...too far and the hunter keeps padding
        private const val BLINK_STRIKE_WINDUP = 0.22f // v2.138: the offensive blink telegraphs this long
        private const val TAU = 6.2831855f
        private const val KNOCK_DECAY = 0.02f      // knockback velocity bleeds fast
        private const val DRIFT_DECAY = 0.8f       // gravity/crash momentum bleeds ~20%/s
        private const val CRASH_RESTITUTION = 0.35f
        private const val PRED_SENSE2 = 220f * 220f // prey notice a predator within ~7 tiles
        private const val PREY_SENSE2 = 260f * 260f // a predator smells prey within ~8 tiles
        private const val HERD_SENSE2 = 220f * 220f // herd-mates within this count toward the centre
        private const val HERD_SEPARATION = 130f    // drift this far from the herd centre → regroup
        private const val HOME_LEASH = 220f         // a nest-guard this far from home heads back
        private const val GUARD_LEASH = 90f         // while guarding, edge back if beyond this from the nest
        private const val FEED_DIST2 = 40f * 40f    // close enough to a kill to feed
        private const val HUNGER_RATE = 0.06f       // hunger/sec (full appetite in ~8s)
        private const val FEED_RATE = 0.5f          // hunger drained/sec while feeding
        private const val WANDER_MUL = 0.35f
        private const val HUNT_MUL = 0.65f
        private const val STALK_MUL = 0.5f
        private const val HERD_MUL = 0.7f
        private const val FLEE_MUL = 1.25f
        private const val CHASE_MUL = 1.1f
        private const val WANDER_REROLL_MIN = 1.4f
        private const val WANDER_REROLL_VAR = 2.0f
    }
}
