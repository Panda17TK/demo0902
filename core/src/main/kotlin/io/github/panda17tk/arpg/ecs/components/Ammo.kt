package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.sim.Tuning

/** Reserve ammo by pool (legacy player.inv). */
class Ammo(
    var ammo9: Int = Tuning.START_AMMO9,
    var ammo12: Int = Tuning.START_AMMO12,
    var ammoBeam: Int = Tuning.START_AMMO_BEAM,
    var ammoNade: Int = Tuning.START_AMMO_NADE,
) : Component<Ammo> {
    fun get(pool: String): Int = when (pool) {
        "ammo9" -> ammo9; "ammo12" -> ammo12; "ammoBeam" -> ammoBeam; "ammoNade" -> ammoNade; else -> 0
    }
    fun set(pool: String, value: Int) { when (pool) {
        "ammo9" -> ammo9 = value; "ammo12" -> ammo12 = value; "ammoBeam" -> ammoBeam = value; "ammoNade" -> ammoNade = value
    } }
    override fun type() = Ammo
    companion object : ComponentType<Ammo>()
}
