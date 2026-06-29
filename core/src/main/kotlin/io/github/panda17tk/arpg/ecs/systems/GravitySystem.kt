package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Cluster
import io.github.panda17tk.arpg.sim.Gravity
import io.github.panda17tk.arpg.sim.GravityField
import io.github.panda17tk.arpg.sim.PlanetField
import io.github.panda17tk.arpg.sim.PlanetGravity
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WallGravity
import kotlin.math.floor

/**
 * Weak gravity toward nearby destructible wall clusters (radius ≥ 2 tiles). For the player and mobs
 * gravity is added to their momentum (drift), so they fall into orbit and can be flung; the player's
 * pull is weakened while dashing (dash-escape). Grenades get a positional curve. Bullets/beam/enemy
 * bullets fly straight (readability). Clusters are detected in a window around the player on a
 * throttle, so cost is O(window²) regardless of map size.
 */
class GravitySystem : IntervalSystem() {
    private val map: TileMap = world.inject()
    private val gravityField: GravityField = world.inject()
    private val planetField: PlanetField = world.inject()
    private var timer = 0f
    private var clusters: List<Cluster> = emptyList()
    private val players by lazy { world.family { all(PlayerTag, Transform, Velocity) } }
    private val mobs by lazy { world.family { all(Mob, Transform, Velocity) } }
    private val grenades by lazy { world.family { all(Grenade, Transform) } }

    override fun onTick() {
        val dt = deltaTime
        with(world) {
            timer -= dt
            if (timer <= 0f) {
                timer = RECOMPUTE
                var px = 0f; var py = 0f; var has = false
                players.forEach { if (!has) { val t = it[Transform]; px = t.x; py = t.y; has = true } }
                if (has) {
                    val ptx = floor(px / TILE).toInt(); val pty = floor(py / TILE).toInt()
                    val raw = WallGravity.detect(
                        maxOf(0, ptx - WIN), minOf(map.width - 1, ptx + WIN),
                        maxOf(0, pty - WIN), minOf(map.height - 1, pty + WIN), map::destructibleAt,
                    )
                    clusters = raw.map { Cluster(it.cx * TILE, it.cy * TILE, it.radius * TILE, it.count) }
                    gravityField.clusters = clusters
                }
            }
            if (clusters.isEmpty() && planetField.planets.isEmpty()) return
            // Player: gravity into drift, weakened while dashing (dash-escape).
            players.forEach { e ->
                applyGravity(e[Transform], e[Velocity], 1f, if (e[PlayerTag].dashing) DASH_GRAVITY_MUL else 1f, dt)
            }
            // Mobs: gravity into drift, scaled by each archetype's gravityResponse (0 = ignores gravity).
            mobs.forEach { e -> applyGravity(e[Transform], e[Velocity], e[Mob].def.gravityResponse, 1f, dt) }
            // Grenades: positional curve (no drift integrator of their own).
            grenades.forEach { pullPos(it[Transform], dt) }
        }
    }

    /** Add cluster gravity to an entity's momentum, scaled by response and dash-escape. */
    private fun applyGravity(t: Transform, v: Velocity, response: Float, dashMul: Float, dt: Float) {
        val (ax, ay) = PlanetGravity.combinedGravityAccel(planetField.planets, clusters, t.x, t.y, RANGE, STRENGTH)
        if (ax == 0f && ay == 0f) return
        val (ndx, ndy) = Gravity.applyToDrift(v.driftX, v.driftY, ax, ay, response, dashMul, dt)
        v.driftX = ndx; v.driftY = ndy
    }

    /** Positional pull (grenades): nudge the transform unless it would embed in a wall. */
    private fun pullPos(t: Transform, dt: Float) {
        val (ax, ay) = PlanetGravity.combinedGravityAccel(planetField.planets, clusters, t.x, t.y, RANGE, STRENGTH)
        if (ax == 0f && ay == 0f) return
        val nx = t.x + ax * dt; val ny = t.y + ay * dt
        if (!map.solidAt(floor(nx / TILE).toInt(), floor(ny / TILE).toInt())) { t.x = nx; t.y = ny }
    }

    companion object {
        private val TILE = Tuning.TILE
        private const val WIN = 50
        private const val RECOMPUTE = 1.5f
        private val RANGE = Tuning.TILE * 12f // influence reaches 12 blocks, decaying per block
        private const val STRENGTH = 13f // ~1/3 of the previous pull
        private const val DASH_GRAVITY_MUL = 0.25f // dashing weakens gravity to 1/4 (escape with effort)
    }
}
