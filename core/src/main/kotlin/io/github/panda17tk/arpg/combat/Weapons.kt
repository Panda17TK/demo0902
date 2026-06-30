package io.github.panda17tk.arpg.combat

object Weapons {
    val ALL: List<WeaponDef> = listOf(
        WeaponDef("pistol", "ピストル", 24f, 0.22f, 12, 0.05f, 1, "ammo9", reloadTime = 1.0f, infiniteAmmo = true),
        WeaponDef("shotgun", "ショットガン", 16f, 0.60f, 6, 0.25f, 6, "ammo12", reloadTime = 2.2f),
        WeaponDef("mg", "マシンガン", 12f, 0.08f, 40, 0.12f, 1, "ammo9", reloadTime = 5.0f),
        // Beam + grenade are deliberate, aimed shots → manual fire (tap to aim, release to shoot).
        WeaponDef("beam", "ビーム", 80f, 0.60f, null, 0f, 1, "ammoBeam", reloadTime = 0f, manualFire = true),
        WeaponDef("grenade", "グレネード", 0f, 0.90f, 1, 0f, 1, "ammoNade", reloadTime = 1.5f, manualFire = true),
    )
}
