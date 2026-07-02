package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.sim.RewardBundle
import kotlin.math.ceil

/**
 * Applies a takeoff [RewardBundle] to the player's components (LP v2.29). Called just before the
 * SURFACE→SPACE transition so PlayerCarry hauls the gifts into space. The ammo top-up rounds up,
 * so any non-empty pool always gains at least one round.
 */
object RewardApply {
    private val AMMO_KINDS = arrayOf("ammo9", "ammo12", "ammoBeam", "ammoNade")

    fun apply(world: World, player: Entity, r: RewardBundle) {
        if (r.isEmpty) return
        with(world) {
            if (r.blocks > 0) player[Materials].blocks += r.blocks
            if (r.med > 0f) {
                val h = player[Health]
                h.hp = minOf(h.hpMax, h.hp + r.med)
            }
            if (r.ammoPct > 0f) {
                val a = player[Ammo]
                for (k in AMMO_KINDS) {
                    val cur = a.get(k)
                    if (cur > 0) a.set(k, cur + ceil(cur * r.ammoPct).toInt())
                }
            }
        }
    }
}
