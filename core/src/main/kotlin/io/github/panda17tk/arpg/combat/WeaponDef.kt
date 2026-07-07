package io.github.panda17tk.arpg.combat

import kotlinx.serialization.Serializable

/** Static weapon stats. magSize null = no magazine (Beam consumes reserve directly). */
@Serializable
data class WeaponDef(
    val id: String,
    val name: String,
    var dmg: Float, // v2.98: tunable live
    var fireRate: Float, // v2.98: tunable live
    var magSize: Int?, // v2.98: tunable live
    var spread: Float, // v2.98: tunable live
    var pellets: Int, // v2.98: tunable live
    val ammoType: String,
    var reloadTime: Float = 0f, // v2.98: tunable live — // seconds for an auto reload (manual is faster); 0 = none (beam)
    val infiniteAmmo: Boolean = false, // reload refills the mag without ever depleting reserve
    val manualFire: Boolean = false, // tap-to-aim, release-to-fire (deliberate, aimed weapons) instead of hold-to-fire
)
