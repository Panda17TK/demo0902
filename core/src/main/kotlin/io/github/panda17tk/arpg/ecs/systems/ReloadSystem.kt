package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.Reload
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.input.InputState

class ReloadSystem : IteratingSystem(family { all(PlayerTag, Arsenal, Ammo) }) {
    private val input: InputState = world.inject()
    private val config: GameConfig = world.inject()

    override fun onTickEntity(entity: Entity) {
        val arsenal = entity[Arsenal]; val ammo = entity[Ammo]
        val w = arsenal.current; val size = w.def.magSize ?: return // beam: no magazine

        // A reload in progress finishes (refills the mag) when its timer elapses.
        if (w.reloadT > 0f) {
            w.reloadT -= deltaTime
            if (w.reloadT <= 0f) {
                w.reloadT = 0f
                val r = Reload.reload(size, w.mag, ammo.get(w.def.ammoType))
                w.mag = r.newMag; ammo.set(w.def.ammoType, r.newReserve)
            }
            return
        }
        if (w.mag >= size || ammo.get(w.def.ammoType) <= 0) { w.autoReloadTimer = 0f; return }
        val time = if (w.def.reloadTime > 0f) w.def.reloadTime else config.player.autoReloadDelay
        if (input.reload) { w.reloadT = time * MANUAL_MUL; w.autoReloadTimer = 0f; return } // manual: faster
        if (!input.fire) { // auto-reload after a quiet delay
            w.autoReloadTimer += deltaTime
            if (w.autoReloadTimer >= config.player.autoReloadDelay) { w.reloadT = time; w.autoReloadTimer = 0f }
        } else {
            w.autoReloadTimer = 0f
        }
    }

    companion object { private const val MANUAL_MUL = 0.45f }
}
