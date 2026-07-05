package io.github.panda17tk.arpg.planet

import io.github.panda17tk.arpg.math.Rng

/** What a planet asks of its visitor (v2.45 星の依頼; v2.68 DUST/CORE; v2.69 PROTECT/OBSERVE). */
enum class QuestKind { ELITES, KILLS, DUST, CORE, PROTECT, OBSERVE }

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
    fun questFor(planetId: Long, biome: PlanetBiome): QuestDef {
        val r = Rng(planetId * 131L + biome.ordinal.toLong() * 17L + SALT)
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
            // no wildlife to guard against, so it asks for observation time instead.
            roll < 0.85f -> if (biome == PlanetBiome.LONELY) {
                observed(r)
            } else {
                val t = 2 + r.nextInt(2) // 2..3 predators
                QuestDef(QuestKind.PROTECT, t, 50 + t * 30)
            }
            else -> observed(r) // v2.69 観測: simply stay, and watch
        }
    }

    private fun observed(r: Rng): QuestDef {
        val t = 45 + r.nextInt(46) // 45..90 seconds on the surface
        return QuestDef(QuestKind.OBSERVE, t, 40 + t)
    }

    private const val SALT = 0x51E5_7A2DL
}
