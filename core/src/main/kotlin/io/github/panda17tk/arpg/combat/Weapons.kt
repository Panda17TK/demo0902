package io.github.panda17tk.arpg.combat

object Weapons {
    val ALL: List<WeaponDef> = listOf(
        WeaponDef("pistol", "Pistol", 24f, 0.22f, 12, 0.05f, 1, "ammo9"),
        WeaponDef("shotgun", "Shotgun", 16f, 0.60f, 6, 0.25f, 6, "ammo12"),
        WeaponDef("mg", "MG", 12f, 0.08f, 40, 0.12f, 1, "ammo9"),
        WeaponDef("beam", "Beam", 80f, 0.60f, null, 0f, 1, "ammoBeam"),
        WeaponDef("grenade", "Grenade", 0f, 0.90f, 1, 0f, 1, "ammoNade"),
    )
}
