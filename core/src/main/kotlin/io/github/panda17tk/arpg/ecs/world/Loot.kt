package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.math.Rng

/**
 * Pure wall-break drop table. Always yields some materials plus 1–3 weighted extras, so breaking
 * walls is rewarding and varied: common ammo, the occasional medkit, and rare consumables
 * (smoke bomb / timed infinite-stamina / dash-speed boost). Deterministic given [Rng].
 */
object Loot {
    val KINDS = setOf("blocks", "ammo9", "ammo12", "ammoBeam", "ammoNade", "med", "smoke", "staminaInf", "dashUp")
    private val AMMO = arrayOf("ammo9" to 12, "ammo12" to 4, "ammoBeam" to 2, "ammoNade" to 1)

    fun wallDrops(rng: Rng): List<Pair<String, Int>> {
        val out = ArrayList<Pair<String, Int>>()
        out.add("blocks" to (1 + rng.nextInt(2)))
        repeat(1 + rng.nextInt(3)) {
            val r = rng.nextFloat()
            out.add(
                when {
                    r < 0.42f -> AMMO[rng.nextInt(AMMO.size)]
                    r < 0.62f -> "med" to 20
                    r < 0.76f -> "blocks" to (1 + rng.nextInt(3))
                    r < 0.88f -> "smoke" to 1
                    r < 0.95f -> "dashUp" to 1
                    else -> "staminaInf" to 1
                },
            )
        }
        return out
    }
}
