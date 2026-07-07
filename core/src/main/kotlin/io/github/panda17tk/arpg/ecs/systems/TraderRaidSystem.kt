package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.WaveState
import io.github.panda17tk.arpg.ecs.world.MobFactory
import io.github.panda17tk.arpg.sim.Tribes
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.sim.WorldState
import kotlin.math.cos
import kotlin.math.sin

/**
 * v2.110 行商船襲撃 — once the desync deepens (wave [RAID_WAVE]), rogues descend on the trading
 * vessel. Whoever puts them down — the keeper, a tribe, anyone — settles the raid; a defended
 * vessel thanks its rescuer with a 20% discount ([WorldState.traderRescued], read by the shop).
 * Deterministic: fixed spawn ring, no RNG; the raiders are drifters so waves never stall on them.
 */
class TraderRaidSystem : IntervalSystem() {
    private val worldState: WorldState = world.inject()
    private val waveState: WaveState = world.inject()
    private val config: GameConfig = world.inject()
    private val mobs = world.family { all(Mob) }

    override fun onTick() {
        val trader = worldState.trader ?: return
        if (worldState.mode != WorldMode.SPACE) return
        when (worldState.traderRaid) {
            0 -> if (waveState.num >= RAID_WAVE) {
                val def = config.enemies["rogue_drifter"] ?: return
                for (k in 0 until RAIDERS) {
                    val ang = k * (TAU / RAIDERS)
                    val e = MobFactory.spawn(
                        world, def,
                        trader.first + cos(ang) * RING, trader.second + sin(ang) * RING,
                        tribe = Tribes.ROGUE, dashes = true, drifter = true,
                    )
                    with(world) { e[Mob].raider = true }
                }
                worldState.traderRaid = 1
                waveState.announce = "行商船が襲われている — ならず者を退けろ"
            }
            1 -> {
                var alive = 0
                mobs.forEach { e -> if (e[Mob].raider) alive++ }
                if (alive == 0) {
                    worldState.traderRaid = 2
                    worldState.traderRescued = true
                    waveState.announce = "行商船を守り抜いた — 店の品に感謝の値引き"
                }
            }
        }
    }

    companion object {
        const val RAID_WAVE = 4  // the desync depth that draws the rogues out
        const val RAIDERS = 3
        private const val RING = 130f
        private const val TAU = (Math.PI * 2.0).toFloat()
    }
}
