package io.github.panda17tk.arpg.sim

/** A tribe stronghold ("star") sited on a large block cluster; spawns its tribe periodically. */
data class Base(val x: Float, val y: Float, val tribe: Int, val radius: Float)

/** Injected per-run set of tribe strongholds (built in WorldFactory, spawned from by BaseSystem). */
class BaseField(val bases: List<Base>)

/** Pure base selection: the biggest block clusters become tribe strongholds. */
object Bases {
    /** The [k] largest clusters (by block count) with at least [minCount] blocks, biggest first. */
    fun pickLargest(clusters: List<Cluster>, k: Int, minCount: Int): List<Cluster> =
        clusters.filter { it.count >= minCount }.sortedByDescending { it.count }.take(k)
}
