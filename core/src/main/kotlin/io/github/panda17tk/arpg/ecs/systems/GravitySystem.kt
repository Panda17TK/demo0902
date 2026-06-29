package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.EBullet
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Cluster
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WallGravity
import kotlin.math.floor

/**
 * Weak gravity toward nearby destructible wall clusters (radius ≥ 2 tiles). Affects the player,
 * mobs, and non-beam projectiles (bullets / enemy bullets / grenades). Clusters are detected in a
 * window around the player on a throttle, so cost is O(window²) regardless of map size. Gravity is
 * applied positionally and wall-blocked, so it never embeds entities or fights their own movers.
 */
class GravitySystem : IntervalSystem() {
    private val map: TileMap = world.inject()
    private var timer = 0f
    private var clusters: List<Cluster> = emptyList()
    private val players by lazy { world.family { all(PlayerTag, Transform) } }
    private val mobs by lazy { world.family { all(Mob, Transform) } }
    private val bullets by lazy { world.family { all(Bullet, Transform) } }
    private val ebullets by lazy { world.family { all(EBullet, Transform) } }
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
                }
            }
            if (clusters.isEmpty()) return
            players.forEach { pull(it[Transform], dt) }
            mobs.forEach { pull(it[Transform], dt) }
            bullets.forEach { pull(it[Transform], dt) }
            ebullets.forEach { pull(it[Transform], dt) }
            grenades.forEach { pull(it[Transform], dt) }
        }
    }

    private fun pull(t: Transform, dt: Float) {
        val (ax, ay) = WallGravity.gravityAt(clusters, t.x, t.y, RANGE, STRENGTH)
        if (ax == 0f && ay == 0f) return
        val nx = t.x + ax * dt; val ny = t.y + ay * dt
        if (!map.solidAt(floor(nx / TILE).toInt(), floor(ny / TILE).toInt())) { t.x = nx; t.y = ny }
    }

    companion object {
        private val TILE = Tuning.TILE
        private const val WIN = 50
        private const val RECOMPUTE = 1.5f
        private val RANGE = Tuning.TILE * 12f // influence reaches 12 blocks, decaying per block
        private const val STRENGTH = 38f
    }
}
