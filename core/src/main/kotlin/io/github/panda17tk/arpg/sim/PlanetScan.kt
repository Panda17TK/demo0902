package io.github.panda17tk.arpg.sim

/**
 * The pre-landing scan card (Living Planets v2.23): what kind of world this is, and whether it
 * remembers you — shown while a landing candidate is latched in space, before the player commits.
 * Built purely from the planet's deterministic context + the run's memory book, so the same planet
 * always scans the same (and GameScreen can cache by planet id). Pure → unit-testable.
 */
data class PlanetCardInfo(
    val title: String,      // biome display name
    val traitLine: String,  // 「気質　◯◯　　聖　◯◯」
    val omenLine: String?,  // 「兆候　◯◯」 (null when the planet carries no story seed)
    val memoryLine: String, // 未訪問 / ReturnVisitLine's greeting / faint memory
) {
    /** Body lines under the title (the hint row is drawn separately by Hud). */
    val lines: List<String> get() = listOfNotNull(traitLine, omenLine, memoryLine)
}

object PlanetScan {
    const val UNVISITED = "未訪問"
    const val FAINT_MEMORY = "訪問済み　記憶は薄い"

    /**
     * Compose the card for a landing candidate. [known]/[memory] come from the run's PlanetMemoryBook;
     * the memory line reuses ReturnVisitLine verbatim (single source of truth for the greeting).
     */
    fun cardFor(p: PlanetBody, known: Boolean, memory: PlanetSocietyState): PlanetCardInfo = PlanetCardInfo(
        title = p.biome.displayName,
        traitLine = PlanetLexicon.traitLine(p.context),
        omenLine = PlanetLexicon.omen(p.context.storySeed)?.let { "兆候　$it" },
        memoryLine = if (!known) UNVISITED else ReturnVisitLine.hudLine(memory, (p.id and 0x7fffffff).toInt()) ?: FAINT_MEMORY,
    )
}
