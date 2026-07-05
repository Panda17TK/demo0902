package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.hypot
import kotlin.math.sin

/**
 * v2.79 水域 — lakes and rivers as world-space bodies (no map-format change, no collision):
 * you can wade straight through, but open water slows the walk. Deterministic per landing;
 * frozen worlds freeze their ponds solid (pretty, not wet). Pure math throughout.
 */
data class Lake(val cx: Float, val cy: Float, val rx: Float, val ry: Float)

/** A meandering strip from the west edge to the east edge, sampled as a polyline. */
data class River(val points: List<Pair<Float, Float>>, val width: Float)

data class WaterBodies(val lakes: List<Lake>, val rivers: List<River>, val frozen: Boolean) {
    val isEmpty: Boolean get() = lakes.isEmpty() && rivers.isEmpty()
    companion object { val NONE = WaterBodies(emptyList(), emptyList(), frozen = false) }
}

object SurfaceWater {
    const val WADE_SLOW = 0.60f // walking through open water: cruise cap multiplier

    /**
     * The landing's water, deterministic per ([biome],[seed]). Nature worlds carry lakes and
     * rivers; ice worlds freeze theirs; dead worlds rarely hold a still black pool. Magma,
     * gas and the lonely asteroid stay dry.
     */
    fun generate(biome: PlanetBiome, seed: Long, worldW: Float, worldH: Float): WaterBodies {
        val r = Rng(seed xor 0x0A9_0A7E5L)
        val cx = worldW / 2f; val cy = worldH / 2f
        val clearR = Tuning.TILE * 4.5f // the pad and its approach stay dry

        fun lake(maxR: Float): Lake? {
            repeat(8) { // a few tries to find a spot away from the pad
                val lx = worldW * (0.15f + r.nextFloat() * 0.7f)
                val ly = worldH * (0.15f + r.nextFloat() * 0.7f)
                val rx = Tuning.TILE * (2.2f + r.nextFloat() * (maxR - 2.2f))
                val ry = rx * (0.6f + r.nextFloat() * 0.5f)
                if (hypot(lx - cx, ly - cy) > clearR + rx) return Lake(lx, ly, rx, ry)
            }
            return null
        }

        fun river(): River {
            // Meander across the arena, based in the top or bottom third so the pad stays dry.
            val base = if (r.nextFloat() < 0.5f) worldH * 0.22f else worldH * 0.78f
            val amp = worldH * (0.05f + r.nextFloat() * 0.06f)
            val phase = r.nextFloat() * 6.28f
            val waves = 1.5f + r.nextFloat() * 1.5f
            val pts = ArrayList<Pair<Float, Float>>()
            var x = 0f
            while (x <= worldW) {
                pts.add(x to base + sin(x / worldW * waves * 6.28f + phase) * amp)
                x += Tuning.TILE * 1.5f
            }
            return River(pts, Tuning.TILE * (0.9f + r.nextFloat() * 0.5f))
        }

        return when (biome) {
            PlanetBiome.NATURE -> WaterBodies(
                lakes = listOfNotNull(if (r.nextFloat() < 0.65f) lake(5f) else null),
                rivers = if (r.nextFloat() < 0.55f) listOf(river()) else emptyList(),
                frozen = false,
            )
            PlanetBiome.ICE -> WaterBodies( // frozen ponds — solid, bright, harmless
                lakes = listOfNotNull(lake(4f), if (r.nextFloat() < 0.4f) lake(3f) else null),
                rivers = emptyList(),
                frozen = true,
            )
            PlanetBiome.DEAD -> WaterBodies( // rarely, one still black pool
                lakes = listOfNotNull(if (r.nextFloat() < 0.30f) lake(3.2f) else null),
                rivers = emptyList(),
                frozen = false,
            )
            PlanetBiome.MAGMA, PlanetBiome.GAS, PlanetBiome.LONELY -> WaterBodies.NONE
        }
    }

    /** True when world point ([x],[y]) stands in open (unfrozen slows; frozen never wets). */
    fun inWater(w: WaterBodies, x: Float, y: Float): Boolean {
        if (w.isEmpty) return false
        for (l in w.lakes) {
            val dx = (x - l.cx) / l.rx; val dy = (y - l.cy) / l.ry
            if (dx * dx + dy * dy <= 1f) return true
        }
        for (rv in w.rivers) {
            val half = rv.width / 2f
            for (i in 0 until rv.points.size - 1) {
                val (ax, ay) = rv.points[i]; val (bx, by) = rv.points[i + 1]
                if (x < minOf(ax, bx) - half || x > maxOf(ax, bx) + half) continue
                // segment steps are short and mostly horizontal — distance to segment, cheaply
                val t = (((x - ax) * (bx - ax) + (y - ay) * (by - ay)) /
                    ((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + 1e-6f)).coerceIn(0f, 1f)
                val px = ax + (bx - ax) * t; val py = ay + (by - ay) * t
                if (hypot(x - px, y - py) <= half) return true
            }
        }
        return false
    }

    /** Wading only in liquid water — a frozen pond is just ground that glitters. */
    fun wadingAt(w: WaterBodies, x: Float, y: Float): Boolean = !w.frozen && inWater(w, x, y)
}
