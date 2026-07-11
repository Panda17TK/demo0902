package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.Reload
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.input.InputState

class ReloadSystem : IteratingSystem(family { all(PlayerTag, Arsenal, Ammo) }) {
    private val input: InputState = world.inject()
    private val config: GameConfig = world.inject()
    private val boons: io.github.panda17tk.arpg.config.WorkshopBoons = world.inject() // v2.90

    override fun onTickEntity(entity: Entity) {
        val arsenal = entity[Arsenal]; val ammo = entity[Ammo]

        // 1) Progress an in-flight reload on EVERY weapon, so it keeps going after switching away.
        for (wr in arsenal.weapons) {
            val sz = wr.def.magSize ?: continue
            if (wr.reloadT > 0f) {
                wr.reloadT -= deltaTime
                if (wr.reloadT <= 0f) {
                    wr.reloadT = 0f
                    if (wr.def.infiniteAmmo) wr.mag = sz
                    else { val r = Reload.reload(sz, wr.mag, ammo.get(wr.def.ammoType)); wr.mag = r.newMag; ammo.set(wr.def.ammoType, r.newReserve) }
                }
            }
        }

        // 2) Start a reload — only for the current weapon (empty → immediate, manual → fast, else auto).
        val w = arsenal.current; val size = w.def.magSize ?: return // beam: no magazine
        if (w.reloadT > 0f) return
        if (w.mag >= size || (!w.def.infiniteAmmo && ammo.get(w.def.ammoType) <= 0)) { w.autoReloadTimer = 0f; return }
        // v2.37: the equipped gun's grade speeds (or slows) its reload while it's the active weapon.
        val gradeReload = entity.getOrNull(Gear)?.loadout?.ranged
            ?.takeIf { it.weaponType == w.def.id }?.reloadMul ?: 1f
        val modsReload = entity.getOrNull(io.github.panda17tk.arpg.ecs.components.Mods)?.reloadMul ?: 1f // v2.107 装填の手際
        val time = (if (w.def.reloadTime > 0f) w.def.reloadTime else config.player.autoReloadDelay) * gradeReload * boons.reloadMul * modsReload // v2.90 装填の手癖
        // Auto-reload ONLY when the magazine is shot dry; a manual reload (R / button) is still allowed any time.
        // No quiet-delay auto-reload — stopping fire with rounds left keeps them chambered.
        when {
            w.mag <= 0 -> { w.reloadT = time; w.autoReloadTimer = 0f }               // out of ammo → auto-reload
            input.reload -> { input.reload = false; w.reloadT = time * MANUAL_MUL; w.autoReloadTimer = 0f } // manual: faster (v2.153: consume)
            else -> w.autoReloadTimer = 0f
        }
    }

    companion object { private const val MANUAL_MUL = 0.45f }
}
