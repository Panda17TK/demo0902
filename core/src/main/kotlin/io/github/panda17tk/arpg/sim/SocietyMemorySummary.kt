package io.github.panda17tk.arpg.sim

/** How a fact reads on the memory summary: a deed done, a hint still open, or a wound inflicted. */
enum class Mark { DONE, NONE, BAD }

/**
 * The planet-memory summary shown from the surface pause (LP v2.25): the facts the society holds
 * about this visit and its three feelings as gauges. Read-only over [PlanetSocietyState]; the words
 * live only here (§14.3). Facts come out gravest-first (same order as SocietySpeechLines.triggerFor),
 * deeds-done only — except the relic, whose 「−」 line stays visible as an exploration hint (FR-4.3).
 */
object SocietyMemorySummary {
    const val MAX_FACTS = 6

    fun factLines(s: PlanetSocietyState): List<Pair<String, Mark>> {
        val out = ArrayList<Pair<String, Mark>>(MAX_FACTS)
        if (s.childKilled) out.add("子を殺した" to Mark.BAD)
        else if (s.childHarmed) out.add("子を傷つけた" to Mark.BAD)
        if (s.apexKilled) out.add("星の主を倒した" to Mark.BAD)
        if (s.nestMotherKilled) out.add("巣の守り手を倒した" to Mark.BAD)
        if (s.hatchlingKilled) out.add("巣の子を失わせた" to Mark.BAD)
        if (s.predatorKilledNearChild) out.add("捕食者を退けた" to Mark.DONE)
        if (s.leaderDefeated) out.add("主を倒した" to Mark.DONE)
        out.add(if (s.relicClaimed) "遺物を手にした" to Mark.DONE else "遺物は取っていない" to Mark.NONE)
        return out.take(MAX_FACTS)
    }

    fun gauges(s: PlanetSocietyState): List<Pair<String, Float>> = listOf(
        "敵意" to s.hostility,
        "感謝" to s.mercy,
        "乱れ" to s.ecologicalDisruption,
    )
}
