package io.github.panda17tk.arpg.map

/** A spawn marker found while parsing a stage (kind + payload). Consumed by Phase 5. */
data class SpawnMarker(val kind: String, val name: String, val worldX: Float, val worldY: Float)

/** Stage definition: char rows + legend mapping marker chars to spawn entries. */
data class StageDef(
    val id: String,
    val name: String,
    val wallHp: Float,
    val rows: List<String>,
    val legend: Map<Char, LegendEntry>,
)

/** What a legend marker char spawns. */
data class LegendEntry(val kind: String, val name: String = "", val amount: Int = 0, val heal: Int = 0)
