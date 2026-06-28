package io.github.panda17tk.arpg.combat

/** Static weapon stats. magSize null = no magazine (Beam consumes reserve directly). */
data class WeaponDef(
    val id: String,
    val name: String,
    val dmg: Float,
    val fireRate: Float,
    val magSize: Int?,
    val spread: Float,
    val pellets: Int,
    val ammoType: String,
)
