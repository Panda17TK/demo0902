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
 * once per tick and scans only its own flock — O(n²) at n≈100 is comfortably cheap even
 * with dozens of flocks in the sky (v2.144 大群衆: pools split per spawn flock, not per species).
 * Determinism: no rng — the wander phase derives from the entity id and the sim clock.
 */
class SchoolFishSystem : IteratingSystem(family { all(Mob, Transform, Velocity, Body, Facing) }) {
    private val map: TileMap = world.inject()
    private val config: io.github.panda17tk.arpg.config.GameConfig = world.inject() // v2.143: live wild knobs
    private val players by lazy { world.family { all(PlayerTag, Transform) } }

    private var time = 0f
    // per-tick caches. v2.140 bucketed per kind; v2.144 大群衆 buckets per FLOCK — thirty sardine
    // schools must not melt into one n² pool. Unboxed FloatBuf: at ~5000 fish the old
    // ArrayList<Float> would box ~20k floats per tick, pure GC churn on a phone.
    private class FloatBuf {
        var a = FloatArray(128); var n = 0
        fun add(v: Float) { if (n == a.size) a = a.copyOf(a.size * 2); a[n++] = v }
        fun clear() { n = 0 }
    }
    private class IntBuf {
        var a = IntArray(16); var n = 0
        fun add(v: Int) { if (n == a.size) a = a.copyOf(a.size * 2); a[n++] = v }
        fun clear() { n = 0 }
    }
    private class SchoolPool {
        val x = FloatBuf(); val y = FloatBuf()
        val hx = FloatBuf(); val hy = FloatBuf()
        // v2.149 倹約第3弾: flock bounds + the few predators near THIS flock — most flocks
        // swim with no teeth in reach, so their fish skip the flee scan entirely.
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        val preds = IntBuf()
        fun grow(px0: Float, py0: Float) {
            if (px0 < minX) minX = px0; if (px0 > maxX) maxX = px0
            if (py0 < minY) minY = py0; if (py0 > maxY) maxY = py0
        }
        fun clear() {
            x.clear(); y.clear(); hx.clear(); hy.clear(); preds.clear()
            minX = Float.MAX_VALUE; maxX = -Float.MAX_VALUE; minY = Float.MAX_VALUE; maxY = -Float.MAX_VALUE
        }
    }
    private val pools = HashMap<Int, SchoolPool>()
    private val px = FloatBuf()
    private val py = FloatBuf()
    // v2.135 島鯨: whale positions — the pilot fish's home current
    private val wx = ArrayList<Float>(4)
    private val wy = ArrayList<Float>(4)

    override fun onTick() {
        time += deltaTime
        pools.values.forEach { it.clear() }
        px.clear(); py.clear(); wx.clear(); wy.clear()
        family.forEach { e ->
            val m = e[Mob]
            if (m.def.lifeKind != LifeKind.WILDLIFE) return@forEach
            if (m.def.id == WHALE_KIND) { val t = e[Transform]; wx.add(t.x); wy.add(t.y) }
            when (m.def.wildRole) {
                WildRole.SCHOOL -> {
                    val t = e[Transform]; val f = e[Facing]
                    val pool = pools.getOrPut(poolKey(m)) { SchoolPool() }
                    pool.x.add(t.x); pool.y.add(t.y); pool.hx.add(f.x); pool.hy.add(f.y)
                    pool.grow(t.x, t.y)
                }
                WildRole.PREDATOR, WildRole.APEX -> {
                    val t = e[Transform]; px.add(t.x); py.add(t.y)
                }
                else -> {}
            }
        }
        // v2.149: hand each flock only the predators inside its padded bounds
        pools.values.forEach { pool ->
            if (pool.x.n == 0) return@forEach
            for (i in 0 until px.n) {
                if (px.a[i] < pool.minX - FLEE_R || px.a[i] > pool.maxX + FLEE_R) continue
                if (py.a[i] < pool.minY - FLEE_R || py.a[i] > pool.maxY + FLEE_R) continue
                pool.preds.add(i)
            }
        }
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val m = entity[Mob]
        if (m.def.lifeKind != LifeKind.WILDLIFE || m.def.wildRole != WildRole.SCHOOL) return
        val t = entity[Transform]; val v = entity[Velocity]; val b = entity[Body]; val f = entity[Facing]
        val dt = deltaTime
        val myKey = poolKey(m)

        // knockback / flung momentum bleeds off like every other creature
        v.vx *= 0.02f.pow(dt); v.vy *= 0.02f.pow(dt)
        v.driftX *= 0.5f.pow(dt); v.driftY *= 0.5f.pow(dt)

        // --- the three boid urges, over same-kind school mates (v2.140: own pool only) ---
        var cohX = 0f; var cohY = 0f; var alX = 0f; var alY = 0f; var sepX = 0f; var sepY = 0f; var n = 0
        val pool = pools[myKey]
        if (pool != null) for (i in 0 until pool.x.n) {
            val dx = pool.x.a[i] - t.x; val dy = pool.y.a[i] - t.y
            val d2 = dx * dx + dy * dy
            if (d2 <= 0.0001f || d2 > NEIGHBOR_R2) continue
            cohX += pool.x.a[i]; cohY += pool.y.a[i]; alX += pool.hx.a[i]; alY += pool.hy.a[i]; n++
            if (d2 < SEP_R2) { sepX -= dx / d2; sepY -= dy / d2 }
        }
        var steerX = 0f; var steerY = 0f
        if (n > 0) {
            val cx = cohX / n - t.x; val cy = cohY / n - t.y
            val cl = hypot(cx, cy).coerceAtLeast(0.0001f)
            steerX += cx / cl * config.wild.schoolCohesion + alX / n * config.wild.schoolAlign
            steerY += cy / cl * config.wild.schoolCohesion + alY / n * config.wild.schoolAlign
        }
        steerX += sepX * config.wild.schoolSeparate; steerY += sepY * config.wild.schoolSeparate

        // --- fear: the player and any predator scatter the school ---
        players.forEach { e ->
            val pt = e[Transform]
            val dx = t.x - pt.x; val dy = t.y - pt.y
            val d2 = dx * dx + dy * dy
            if (d2 < FLEE_R2 && d2 > 0.0001f) {
                val d = hypot(dx, dy)
                steerX += dx / d * config.wild.schoolFlee; steerY += dy / d * config.wild.schoolFlee
            }
        }
        if (pool != null) for (k in 0 until pool.preds.n) {
            val i = pool.preds.a[k]
            val dx = t.x - px.a[i]; val dy = t.y - py.a[i]
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
        steerX += cos(phase) * config.wild.schoolWander; steerY += sin(phase) * config.wild.schoolWander

        // --- steer the heading with inertia, then swim ---
        var hx = f.x + steerX * TURN * dt
        var hy = f.y + steerY * TURN * dt
        val hl = hypot(hx, hy)
        if (hl > 0.0001f) { hx /= hl; hy /= hl } else { hx = 1f; hy = 0f }
        f.x = hx; f.y = hy
        val r1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, hx * m.speed * dt + (v.vx + v.driftX) * dt, 0f)
        if (r1.hitX) { v.driftX = 0f; f.x = -f.x } // v2.136: bounce the heading off the rock, not pin against it
        val r2 = Collision.moveAndCollide(map, r1.x, r1.y, b.halfW, b.halfH, 0f, hy * m.speed * dt + (v.vy + v.driftY) * dt)
        if (r2.hitY) { v.driftY = 0f; f.y = -f.y }
        t.x = r2.x; t.y = r2.y
    }

    // v2.144 大群衆: a school is its spawn flock, not the whole species. group 0 (hand-spawned
    // strays) still pools kind-wide, so a lone test sardine keeps its old behaviour.
    private fun poolKey(m: Mob): Int = if (m.schoolGroup == 0) m.kind.hashCode() else m.schoolGroup * -0x61c88647

    companion object {
        private const val NEIGHBOR_R2 = 52f * 52f // mates inside this ring pull the fish along
        private const val SEP_R2 = 13f * 13f      // closer than this pushes apart (no stacking)
        private const val FLEE_R = 130f
        private const val FLEE_R2 = FLEE_R * FLEE_R   // the player / a predator scatters the school
        private const val W_FLEE_PRED = 2.4f // v2.143: the tunable weights moved to WildConfig
        private const val TURN = 4.5f // heading inertia: how fast the urge bends the swim line
        // v2.135 島鯨: the retinue and its island
        private const val WHALE_KIND = "isle_whale"
        private const val PILOT_KIND = "pilot_minnow"
        private const val WHALE_SENSE2 = 900f * 900f // a pilot fish feels its whale this far out
        private const val WHALE_KEEP2 = 90f * 90f    // ...and rests once it swims alongside
        private const val W_WHALE = 1.4f
    }
}
