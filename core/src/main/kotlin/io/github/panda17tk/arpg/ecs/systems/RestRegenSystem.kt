package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState

/**
 * v2.83 休息回復 — stand still, put the tools down, and the keeper's frame patches itself:
 * +1 HP per second, but only while genuinely resting (no motion, no inputs, and a quiet
 * spell since the last hit). Any wound or action resets the calm.
 */
class RestRegenSystem(
    private val mobGrid: io.github.panda17tk.arpg.pathfinding.SpatialGrid<Entity>, // v2.157 読む海
) : IteratingSystem(family { all(PlayerTag, Health, Velocity) }) {
    private val input: InputState = world.inject()
    private val boons: io.github.panda17tk.arpg.config.WorkshopBoons = world.inject() // v2.90
    private val difficulty: io.github.panda17tk.arpg.sim.Difficulty = world.inject() // v2.97
    private var sinceHurt = Float.MAX_VALUE / 2f
    private var lastHp = Float.NaN
    private var acc = 0f

    override fun onTickEntity(entity: Entity) {
        val h = entity[Health]
        val v = entity[Velocity]
        if (!lastHp.isNaN() && h.hp < lastHp - 0.001f) sinceHurt = 0f else sinceHurt += deltaTime
        lastHp = h.hp
        // Intent-driven velocity only: gravity keeps tugging driftX/Y even while the keeper
        // floats idle in space, and coasting on that pull is still rest.
        val speedSq = v.vx * v.vx + v.vy * v.vy
        val resting = speedSq < SPEED_EPS * SPEED_EPS &&
            !input.fire && !input.melee && !input.dash && sinceHurt >= REST_DELAY
        if (resting && h.hp < h.hpMax) {
            val modsRegen = entity.getOrNull(io.github.panda17tk.arpg.ecs.components.Mods)?.regenAdd ?: 0f // v2.107 自己修復
            // v2.157 読む海: resting in the island whale's shadow mends half again as fast —
            // the great calm creature makes its water a haven worth finding.
            var whaleCalm = 1f
            entity.getOrNull(io.github.panda17tk.arpg.ecs.components.Transform)?.let { t ->
                mobGrid.forNearby(t.x, t.y, WHALE_CALM_R) { e ->
                    if (whaleCalm == 1f && e.getOrNull(io.github.panda17tk.arpg.ecs.components.Mob)?.def?.id == "isle_whale") whaleCalm = WHALE_CALM_MUL
                }
            }
            acc += deltaTime * (1f + boons.regenPerSec + modsRegen) * difficulty.regenMul * whaleCalm // v2.90 学習 / v2.97 安定運転 / v2.157 鯨の安らぎ
            while (acc >= 1f) { acc -= 1f; h.hp = (h.hp + 1f).coerceAtMost(h.hpMax) }
        } else if (!resting) {
            acc = 0f // moving/fighting breaks the breath — the next point starts over
        }
    }

    companion object {
        const val REST_DELAY = 2.5f // seconds without a wound before the mending starts
        const val WHALE_CALM_R = 300f   // v2.157: the island whale's haven
        const val WHALE_CALM_MUL = 1.5f
        const val SPEED_EPS = 14f   // "standing still", with a little drift tolerance
    }
}
