package io.github.panda17tk.arpg.upgrade

import io.github.panda17tk.arpg.config.UpgradesConfig
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.math.Rng

/**
 * A permanent run upgrade offered between waves (legacy state/upgrades.js UPGRADES).
 * [name] is the Japanese label (shown once Phase 8 ships a JP font); [label] is an ASCII
 * fallback so cards are legible with the default BitmapFont.
 */
data class Upgrade(val id: String, val name: String, val label: String)

/**
 * Pure upgrade catalog + selection + application. Operates on plain component objects so the
 * whole thing is unit-testable without a Fleks world. Mirrors legacy upgrades.js exactly.
 */
object Upgrades {
    val ALL: List<Upgrade> = listOf(
        Upgrade("gun_dmg", "火力強化", "GUN DMG"),
        Upgrade("fire_rate", "連射強化", "FIRE RATE"),
        Upgrade("melee", "近接強化", "MELEE"),
        Upgrade("max_hp", "頑強", "MAX HP"),
        Upgrade("speed", "俊足", "SPEED"),
        Upgrade("ammo", "弾薬調達", "AMMO"),
        Upgrade("lifesteal", "吸血", "LIFESTEAL"),
        Upgrade("engineer", "築城術", "ENGINEER"),
    )

    fun byId(id: String): Upgrade? = ALL.firstOrNull { it.id == id }

    /** Pick [n] distinct upgrades at random (legacy pickUpgradeChoices: splice — no repeats). */
    fun pick(n: Int, rng: Rng): List<Upgrade> {
        val pool = ALL.toMutableList()
        val out = ArrayList<Upgrade>(minOf(n, pool.size))
        repeat(minOf(n, pool.size)) { out.add(pool.removeAt(rng.nextInt(pool.size))) }
        return out
    }

    /** Apply a permanent upgrade to the player's mutable stats (legacy UPGRADES[].apply). */
    fun apply(id: String, cfg: UpgradesConfig, mods: Mods, health: Health, ammo: Ammo, materials: Materials) {
        when (id) {
            "gun_dmg" -> mods.gunMul *= cfg.gunMul
            "fire_rate" -> mods.fireMul *= cfg.fireMul
            "melee" -> mods.meleeMul *= cfg.meleeMul
            "max_hp" -> { health.hpMax += cfg.maxHpAdd; health.hp = health.hpMax }
            "speed" -> mods.moveMul *= cfg.moveMul
            "ammo" -> {
                mods.ammoMul *= cfg.ammoMul
                ammo.ammo9 += cfg.ammoRefill9
                ammo.ammo12 += cfg.ammoRefill12
                ammo.ammoBeam += cfg.ammoRefillBeam
                ammo.ammoNade += cfg.ammoRefillNade
            }
            "lifesteal" -> mods.healOnKill += cfg.lifestealAdd
            "engineer" -> { materials.blocks += cfg.blocksAdd; mods.wallHp += cfg.wallHpAdd }
        }
    }

    /** ASCII card description with the actual magnitudes (numbers render with the default font). */
    fun desc(u: Upgrade, cfg: UpgradesConfig): String = when (u.id) {
        "gun_dmg" -> "射撃ダメージ ×${cfg.gunMul}"
        "fire_rate" -> "発射間隔 ×${cfg.fireMul}"
        "melee" -> "近接ダメージ ×${cfg.meleeMul}"
        "max_hp" -> "最大HP +${cfg.maxHpAdd.toInt()}・全回復"
        "speed" -> "移動速度 ×${cfg.moveMul}"
        "ammo" -> "弾薬補給・ドロップ ×${cfg.ammoMul}"
        "lifesteal" -> "撃破ごとに HP +${cfg.lifestealAdd.toInt()}"
        "engineer" -> "資材 +${cfg.blocksAdd}・壁を強化"
        else -> ""
    }
}
