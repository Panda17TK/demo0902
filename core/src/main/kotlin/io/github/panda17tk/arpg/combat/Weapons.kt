package io.github.panda17tk.arpg.combat

object Weapons {
    val ALL: List<WeaponDef> = listOf(
        WeaponDef("pistol", "ピストル", 24f, 0.22f, 16, 0.05f, 1, "ammo9", reloadTime = 0.5f, infiniteAmmo = true), // v2.83: 16発 / 0.5秒装填
        WeaponDef("shotgun", "ショットガン", 16f, 0.60f, 6, 0.25f, 9, "ammo12", reloadTime = 2.2f, infiniteAmmo = true), // v2.80: 9ペレット / 弾薬無限
        WeaponDef("mg", "マシンガン", 24f, 0.08f, 120, 0.12f, 1, "ammo9", reloadTime = 5.0f, infiniteAmmo = true), // v2.83: 威力ピストル並(24) / 120発
        // Beam + grenade are deliberate, aimed shots → manual fire (tap to aim, release to shoot).
        WeaponDef("beam", "ビーム", 80f, 0.60f, null, 0f, 1, "ammoBeam", reloadTime = 0f, infiniteAmmo = true, manualFire = true), // v2.37: 弾薬無限
        WeaponDef("grenade", "グレネード", 0f, 0.90f, 1, 0f, 1, "ammoNade", reloadTime = 1.5f, infiniteAmmo = true, manualFire = true), // v2.80: 弾薬無限
        // v2.38: new weapon TYPES — reached through equipment (装備で武器種が増える), not the number keys.
        WeaponDef("smg", "サブマシンガン", 8f, 0.055f, 30, 0.18f, 1, "ammo9", reloadTime = 2.0f, infiniteAmmo = true),
        WeaponDef("rifle", "ライフル", 60f, 0.75f, 5, 0.01f, 1, "ammo12", reloadTime = 2.0f),
    )
}
