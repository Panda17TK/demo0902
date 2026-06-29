package io.github.panda17tk.arpg.combat

import kotlinx.serialization.Serializable

/** Static weapon stats. magSize null = no magazine (Beam consumes reserve directly). */
@Serializable
data class WeaponDef(
    val id: String,
    val name: String,
    val dmg: Float,
    val fireRate: Float,
    val magSize: Int?,
    val spread: Float,
    val pellets: Int,
    val ammoType: String,
    val reloadTime: Float = 0f, // seconds for an auto reload (manual is faster); 0 = none (beam)
    val infiniteAmmo: Boolean = false, // reload refills the mag without ever depleting reserve
)
