package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.item.EquipSlot
import io.github.panda17tk.arpg.item.ItemDef
import io.github.panda17tk.arpg.item.Loadout

/** Snapshot of the player's run state, carried across SPACE⇄SURFACE world rebuilds (Living Planets LP-E). */
data class PlayerCarry(
    val hp: Float, val stamina: Float, val blocks: Int, val wave: Int,
    val ammo9: Int, val ammo12: Int, val ammoBeam: Int, val ammoNade: Int,
    val mags: List<Int>, val curW: Int,
    val gunMul: Float, val fireMul: Float, val meleeMul: Float, val moveMul: Float,
    val ammoMul: Float, val healOnKill: Float, val wallHp: Float,
    val loadout: Loadout = Loadout(), val backpack: List<ItemDef> = emptyList(), // v2.33 gear survives transitions
) {
    fun applyTo(world: World, player: Entity) = with(world) {
        player[Health].hp = hp
        player[Stamina].value = stamina
        player[Materials].blocks = blocks
        val ammo = player[Ammo]
        ammo.ammo9 = ammo9; ammo.ammo12 = ammo12; ammo.ammoBeam = ammoBeam; ammo.ammoNade = ammoNade
        val ars = player[Arsenal]
        mags.forEachIndexed { i, m -> if (i < ars.weapons.size) ars.weapons[i].mag = m }
        ars.curW = curW.coerceIn(0, ars.weapons.size - 1)
        val mods = player[Mods]
        mods.gunMul = gunMul; mods.fireMul = fireMul; mods.meleeMul = meleeMul; mods.moveMul = moveMul
        mods.ammoMul = ammoMul; mods.healOnKill = healOnKill; mods.wallHp = wallHp
        val gear = player[Gear]
        for (slot in EquipSlot.values()) gear.loadout.set(slot, loadout.get(slot))
        gear.backpack.clear(); gear.backpack.addAll(backpack)
    }

    companion object {
        fun of(world: World, player: Entity, wave: Int): PlayerCarry = with(world) {
            val mods = player[Mods]
            val ammo = player[Ammo]
            val ars = player[Arsenal]
            PlayerCarry(
                hp = player[Health].hp, stamina = player[Stamina].value, blocks = player[Materials].blocks, wave = wave,
                ammo9 = ammo.ammo9, ammo12 = ammo.ammo12, ammoBeam = ammo.ammoBeam, ammoNade = ammo.ammoNade,
                mags = ars.weapons.map { it.mag }, curW = ars.curW,
                gunMul = mods.gunMul, fireMul = mods.fireMul, meleeMul = mods.meleeMul, moveMul = mods.moveMul,
                ammoMul = mods.ammoMul, healOnKill = mods.healOnKill, wallHp = mods.wallHp,
                loadout = player[Gear].loadout, backpack = player[Gear].backpack.toList(),
            )
        }
    }
}
