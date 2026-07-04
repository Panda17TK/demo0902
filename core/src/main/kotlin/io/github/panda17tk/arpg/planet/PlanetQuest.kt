package io.github.panda17tk.arpg.planet

import io.github.panda17tk.arpg.math.Rng

/** What a planet asks of its visitor (v2.45 星の依頼). */
enum class QuestKind { ELITES, KILLS }

/** One planet's standing request: kind + target count + the dust it pays on takeoff. */
data class QuestDef(val kind: QuestKind, val target: Int, val rewardDust: Int) {
    val line: String
        get() = when (kind) {
            QuestKind.ELITES -> "依頼: 精鋭を${target}体討つ（${rewardDust}屑）"
            QuestKind.KILLS -> "依頼: 外敵を${target}体討つ（${rewardDust}屑）"
        }
}

/**
 * v2.45 星の依頼 — each planet carries ONE deterministic request (from its id, like its
 * context/market stock). Fulfil it during the visit and the star pays dust at takeoff;
 * a star whose child you killed pays nothing, same rule as the send-off gift.
 */
object PlanetQuest {
    fun questFor(planetId: Long, biome: PlanetBiome): QuestDef {
        val r = Rng(planetId * 131L + biome.ordinal.toLong() * 17L + SALT)
        return if (r.nextFloat() < 0.45f) {
            val t = 1 + r.nextInt(3) // 1..3 elites
            QuestDef(QuestKind.ELITES, t, 60 + t * 45)
        } else {
            val t = 8 + r.nextInt(9) // 8..16 hostiles
            QuestDef(QuestKind.KILLS, t, 30 + t * 5)
        }
    }

    private const val SALT = 0x51E5_7A2DL
}
