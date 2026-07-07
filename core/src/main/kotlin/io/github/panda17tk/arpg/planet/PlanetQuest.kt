package io.github.panda17tk.arpg.planet

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.Weather
import io.github.panda17tk.arpg.sim.WeatherKind

/** What a planet asks of its visitor (v2.45 星の依頼; v2.68 DUST/CORE; v2.69 PROTECT/OBSERVE). */
enum class QuestKind { ELITES, KILLS, DUST, CORE, PROTECT, OBSERVE }

/**
 * v2.109 天候×依頼: the sky sets the pay. Harder-in-this-weather work earns a quiet premium —
 * observing through thunder, guarding in the rain, hunting through fog. Pure; the quest chip
 * and the settlement read the same table.
 */
object WeatherQuest {
    fun mul(kind: QuestKind, weather: io.github.panda17tk.arpg.sim.WeatherKind): Float = when (weather) {
        io.github.panda17tk.arpg.sim.WeatherKind.THUNDER -> when (kind) {
            QuestKind.OBSERVE -> 1.5f; QuestKind.PROTECT -> 1.25f; else -> 1f
        }
        io.github.panda17tk.arpg.sim.WeatherKind.FOG -> when (kind) {
            QuestKind.KILLS, QuestKind.ELITES -> 1.25f; else -> 1f
        }
        io.github.panda17tk.arpg.sim.WeatherKind.RAIN -> if (kind == QuestKind.PROTECT) 1.25f else 1f
        io.github.panda17tk.arpg.sim.WeatherKind.SNOW -> if (kind == QuestKind.DUST) 1.25f else 1f
        io.github.panda17tk.arpg.sim.WeatherKind.ASH -> if (kind == QuestKind.KILLS) 1.25f else 1f
        io.github.panda17tk.arpg.sim.WeatherKind.AURORA -> when (kind) {
            QuestKind.OBSERVE, QuestKind.CORE -> 1.25f; else -> 1f
        }
        else -> 1f
    }

    fun rewardFor(q: QuestDef, weather: io.github.panda17tk.arpg.sim.WeatherKind): Int =
        (q.rewardDust * mul(q.kind, weather)).toInt()
}

/** One planet's standing request: kind + target count + the dust it pays on takeoff. */
data class QuestDef(val kind: QuestKind, val target: Int, val rewardDust: Int) {
    val line: String
        get() = when (kind) {
            QuestKind.ELITES -> "依頼: 精鋭を${target}体討つ（${rewardDust}屑）"
            QuestKind.KILLS -> "依頼: 外敵を${target}体討つ（${rewardDust}屑）"
            QuestKind.DUST -> "依頼: 記憶片を${target}回収する（${rewardDust}屑）"
            QuestKind.CORE -> "依頼: 記憶核と照合する（${rewardDust}屑）"
            QuestKind.PROTECT -> "依頼: 捕食者を${target}体退ける（${rewardDust}屑）"
            QuestKind.OBSERVE -> "依頼: ${target}秒の定点観測（${rewardDust}屑）"
        }
}

/**
 * v2.45 星の依頼 — each planet carries ONE deterministic request (from its id, like its
 * context/market stock). Fulfil it during the visit and the star pays dust at takeoff;
 * a star whose child you killed pays nothing, same rule as the send-off gift.
 * v2.68: two quieter asks joined the pool — gather the shed memory fragments (DUST), or
 * simply walk to the memory core and be checked against it (CORE).
 */
object PlanetQuest {
    /** v2.72 連鎖: how many requests one visit can chain through. */
    const val CHAIN = 3

    /** The [stage]-th request of a visit (0-based). Stage 0 is byte-identical to the pre-chain
     *  quest, so what a planet asks first never changed across versions. */
    fun questFor(planetId: Long, biome: PlanetBiome, stage: Int = 0): QuestDef {
        val r = Rng(planetId * 131L + biome.ordinal.toLong() * 17L + stage * 7919L + SALT)
        val roll = r.nextFloat()
        return when {
            roll < 0.25f -> {
                val t = 1 + r.nextInt(3) // 1..3 elites
                QuestDef(QuestKind.ELITES, t, 60 + t * 45)
            }
            roll < 0.45f -> {
                val t = 8 + r.nextInt(9) // 8..16 hostiles
                QuestDef(QuestKind.KILLS, t, 30 + t * 5)
            }
            roll < 0.60f -> {
                val t = 20 + r.nextInt(21) // 20..40 dust picked up during the visit
                QuestDef(QuestKind.DUST, t, 40 + t * 2)
            }
            roll < 0.70f -> QuestDef(QuestKind.CORE, 1, 90) // stand before the core once
            // v2.69 護衛: cull the predators pressing on the children. The lonely asteroid has
            // no wildlife to guard against — and on a RAINY world the hunters are already
            // sheltering (v2.75 thins them), so both ask for observation time instead.
            roll < 0.85f -> if (biome == PlanetBiome.LONELY || Weather.kindFor(planetId, biome) in STORMY) {
                observed(r)
            } else {
                // 1..2 predators — never more than the fewest a biome actually fields (v2.75 fix)
                val t = 1 + r.nextInt(2)
                QuestDef(QuestKind.PROTECT, t, 50 + t * 30)
            }
            else -> observed(r) // v2.69 観測: simply stay, and watch
        }
    }

    private fun observed(r: Rng): QuestDef {
        val t = 45 + r.nextInt(46) // 45..90 seconds on the surface
        return QuestDef(QuestKind.OBSERVE, t, 40 + t)
    }

    /** v2.75/77: skies that shelter the hunters — those planets ask for observation instead. */
    private val STORMY = setOf(WeatherKind.RAIN, WeatherKind.THUNDER)

    private const val SALT = 0x51E5_7A2DL
}
