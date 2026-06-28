package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.Reload
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.Tuning

class ReloadSystem : IteratingSystem(family { all(PlayerTag, Arsenal, Ammo) }) {
    private val input: InputState = world.inject()
    override fun onTickEntity(entity: Entity) {
        val arsenal = entity[Arsenal]; val ammo = entity[Ammo]
        val w = arsenal.current; val size = w.def.magSize ?: return // beam: no reload
        fun doReload() {
            val r = Reload.reload(size, w.mag, ammo.get(w.def.ammoType))
            w.mag = r.newMag; ammo.set(w.def.ammoType, r.newReserve); w.autoReloadTimer = 0f
        }
        if (input.reload) { doReload(); return }
        // auto-reload: when not firing and the mag isn't full, count up to the delay
        if (w.mag < size && !input.fire) {
            w.autoReloadTimer += deltaTime
            if (w.autoReloadTimer >= Tuning.AUTO_RELOAD_DELAY) doReload()
        } else {
            w.autoReloadTimer = 0f
        }
    }
}
