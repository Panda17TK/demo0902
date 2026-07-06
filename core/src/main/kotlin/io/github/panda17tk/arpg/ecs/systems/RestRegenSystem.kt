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
class RestRegenSystem : IteratingSystem(family { all(PlayerTag, Health, Velocity) }) {
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
            acc += deltaTime * (1f + boons.regenPerSec) * difficulty.regenMul // v2.90 学習 / v2.97 安定運転
            while (acc >= 1f) { acc -= 1f; h.hp = (h.hp + 1f).coerceAtMost(h.hpMax) }
        } else if (!resting) {
            acc = 0f // moving/fighting breaks the breath — the next point starts over
        }
    }

    companion object {
        const val REST_DELAY = 2.5f // seconds without a wound before the mending starts
        const val SPEED_EPS = 14f   // "standing still", with a little drift tolerance
    }
}
