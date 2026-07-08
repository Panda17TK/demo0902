package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Collision
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * v2.131 魚群シミュレーション — classic boids for [WildRole.SCHOOL]: separation, alignment
 * and cohesion among same-kind fish, plus a hard flee from the player and from predatory
 * wildlife. WildlifeSystem hands SCHOOL over entirely (its herd logic would fight the flock).
 * A school runs near a hundred fish, so the neighbour pass caches every member's position
 * once per tick and scans only school members — O(n²) at n≈100 is comfortably cheap.
 * Determinism: no rng — the wander phase derives from the entity id and the sim clock.
 */
class SchoolFishSystem : IteratingSystem(family { all(Mob, Transform, Velocity, Body, Facing) }) {
    private val map: TileMap = world.inject()
    private val players by lazy { world.family { all(PlayerTag, Transform) } }

    private var time = 0f
    // per-tick caches: school members (position + heading + kind hash) and predator positions
    private val sx = ArrayList<Float>(128)
    private val sy = ArrayList<Float>(128)
    private val shx = ArrayList<Float>(128)
    private val shy = ArrayList<Float>(128)
    private val skind = ArrayList<Int>(128)
    private val px = ArrayList<Float>(8)
    private val py = ArrayList<Float>(8)
    // v2.135 島鯨: whale positions — the pilot fish's home current
    private val wx = ArrayList<Float>(4)
    private val wy = ArrayList<Float>(4)

    override fun onTick() {
        time += deltaTime
        sx.clear(); sy.clear(); shx.clear(); shy.clear(); skind.clear(); px.clear(); py.clear()
        wx.clear(); wy.clear()
        family.forEach { e ->
            val m = e[Mob]
            if (m.def.lifeKind != LifeKind.WILDLIFE) return@forEach
            if (m.def.id == WHALE_KIND) { val t = e[Transform]; wx.add(t.x); wy.add(t.y) }
            when (m.def.wildRole) {
                WildRole.SCHOOL -> {
                    val t = e[Transform]; val f = e[Facing]
                    sx.add(t.x); sy.add(t.y); shx.add(f.x); shy.add(f.y); skind.add(m.kind.hashCode())
                }
                WildRole.PREDATOR, WildRole.APEX -> {
                    val t = e[Transform]; px.add(t.x); py.add(t.y)
                }
                else -> {}
            }
        }
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val m = entity[Mob]
        if (m.def.lifeKind != LifeKind.WILDLIFE || m.def.wildRole != WildRole.SCHOOL) return
        val t = entity[Transform]; val v = entity[Velocity]; val b = entity[Body]; val f = entity[Facing]
        val dt = deltaTime
        val myKind = m.kind.hashCode()

        // knockback / flung momentum bleeds off like every other creature
        v.vx *= 0.02f.pow(dt); v.vy *= 0.02f.pow(dt)
        v.driftX *= 0.5f.pow(dt); v.driftY *= 0.5f.pow(dt)

        // --- the three boid urges, over same-kind school mates ---
        var cohX = 0f; var cohY = 0f; var alX = 0f; var alY = 0f; var sepX = 0f; var sepY = 0f; var n = 0
        for (i in sx.indices) {
            if (skind[i] != myKind) continue
            val dx = sx[i] - t.x; val dy = sy[i] - t.y
            val d2 = dx * dx + dy * dy
            if (d2 <= 0.0001f || d2 > NEIGHBOR_R2) continue
            cohX += sx[i]; cohY += sy[i]; alX += shx[i]; alY += shy[i]; n++
            if (d2 < SEP_R2) { sepX -= dx / d2; sepY -= dy / d2 }
        }
        var steerX = 0f; var steerY = 0f
        if (n > 0) {
            val cx = cohX / n - t.x; val cy = cohY / n - t.y
            val cl = hypot(cx, cy).coerceAtLeast(0.0001f)
            steerX += cx / cl * W_COHESION + alX / n * W_ALIGN
            steerY += cy / cl * W_COHESION + alY / n * W_ALIGN
        }
        steerX += sepX * W_SEPARATE; steerY += sepY * W_SEPARATE

        // --- fear: the player and any predator scatter the school ---
        players.forEach { e ->
            val pt = e[Transform]
            val dx = t.x - pt.x; val dy = t.y - pt.y
            val d2 = dx * dx + dy * dy
            if (d2 < FLEE_R2 && d2 > 0.0001f) {
                val d = hypot(dx, dy)
                steerX += dx / d * W_FLEE; steerY += dy / d * W_FLEE
            }
        }
        for (i in px.indices) {
            val dx = t.x - px[i]; val dy = t.y - py[i]
            val d2 = dx * dx + dy * dy
            if (d2 < FLEE_R2 && d2 > 0.0001f) {
                val d = hypot(dx, dy)
                steerX += dx / d * W_FLEE_PRED; steerY += dy / d * W_FLEE_PRED
            }
        }

        // --- v2.135 島鯨: a pilot fish is pulled home to the nearest whale ---
        if (m.def.id == PILOT_KIND) {
            var bd2 = Float.MAX_VALUE; var bi = -1
            for (i in wx.indices) {
                val dx = wx[i] - t.x; val dy = wy[i] - t.y
                val d2 = dx * dx + dy * dy
                if (d2 < bd2) { bd2 = d2; bi = i }
            }
            if (bi >= 0 && bd2 < WHALE_SENSE2 && bd2 > WHALE_KEEP2) {
                val d = hypot(wx[bi] - t.x, wy[bi] - t.y)
                steerX += (wx[bi] - t.x) / d * W_WHALE; steerY += (wy[bi] - t.y) / d * W_WHALE
            }
        }

        // --- a gentle wander so a calm school still drifts and shimmers ---
        val phase = time * 0.9f + (entity.id % 97) * 1.7f
        steerX += cos(phase) * W_WANDER; steerY += sin(phase) * W_WANDER

        // --- steer the heading with inertia, then swim ---
        var hx = f.x + steerX * TURN * dt
        var hy = f.y + steerY * TURN * dt
        val hl = hypot(hx, hy)
        if (hl > 0.0001f) { hx /= hl; hy /= hl } else { hx = 1f; hy = 0f }
        f.x = hx; f.y = hy
        val r1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, hx * m.speed * dt + (v.vx + v.driftX) * dt, 0f)
        if (r1.hitX) v.driftX = 0f
        val r2 = Collision.moveAndCollide(map, r1.x, r1.y, b.halfW, b.halfH, 0f, hy * m.speed * dt + (v.vy + v.driftY) * dt)
        if (r2.hitY) v.driftY = 0f
        t.x = r2.x; t.y = r2.y
    }

    companion object {
        private const val NEIGHBOR_R2 = 52f * 52f // mates inside this ring pull the fish along
        private const val SEP_R2 = 13f * 13f      // closer than this pushes apart (no stacking)
        private const val FLEE_R2 = 130f * 130f   // the player / a predator scatters the school
        private const val W_COHESION = 0.9f
        private const val W_ALIGN = 0.7f
        private const val W_SEPARATE = 26f
        private const val W_FLEE = 3.2f
        private const val W_FLEE_PRED = 2.4f
        private const val W_WANDER = 0.35f
        private const val TURN = 4.5f // heading inertia: how fast the urge bends the swim line
        // v2.135 島鯨: the retinue and its island
        private const val WHALE_KIND = "isle_whale"
        private const val PILOT_KIND = "pilot_minnow"
        private const val WHALE_SENSE2 = 900f * 900f // a pilot fish feels its whale this far out
        private const val WHALE_KEEP2 = 90f * 90f    // ...and rests once it swims alongside
        private const val W_WHALE = 1.4f
    }
}
