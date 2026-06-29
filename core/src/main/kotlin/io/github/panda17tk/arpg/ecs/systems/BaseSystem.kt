package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.WaveState
import io.github.panda17tk.arpg.ecs.world.MobFactory
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.BaseField
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tribe strongholds ("stars") periodically reinforce: every [INTERVAL]s each base within range of
 * the player spawns one of its own tribe's mobs. Bounded by a global cap so the huge maps stay sane.
 */
class BaseSystem : IntervalSystem() {
    private val field: BaseField = world.inject()
    private val config: GameConfig = world.inject()
    private val rng: Rng = world.inject()
    private val wave: WaveState = world.inject()
    private var timer = 0f
    private val players by lazy { world.family { all(PlayerTag, Transform) } }
    private val mobs by lazy { world.family { all(Mob) } }
    private val normalKeys = config.enemies.filterValues { it.tier == "normal" }.keys.toList()

    override fun onTick() {
        timer -= deltaTime
        if (timer > 0f || field.bases.isEmpty() || normalKeys.isEmpty()) return
        timer = INTERVAL
        with(world) {
            var px = 0f; var py = 0f; var has = false
            players.forEach { if (!has) { val t = it[Transform]; px = t.x; py = t.y; has = true } }
            if (!has || mobs.numEntities >= CAP) return
            for (base in field.bases) {
                val dx = base.x - px; val dy = base.y - py
                if (dx * dx + dy * dy > RANGE * RANGE) continue
                val def = config.enemies[normalKeys[rng.nextInt(normalKeys.size)]] ?: continue
                val a = rng.nextFloat() * TAU; val r = rng.nextFloat() * base.radius
                MobFactory.spawn(world, def, base.x + cos(a) * r, base.y + sin(a) * r, wave.num, config.waves.hpScalePerWave, config.waves.speedScalePerWave, tribe = base.tribe)
            }
        }
    }

    companion object {
        private const val INTERVAL = 4f
        private const val CAP = 120
        private val RANGE = Tuning.TILE * 60f
        private const val TAU = 6.2831855f
    }
}
