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
        // v2.107 強化カード拡充: eight more, so two runs stop feeling like the same build.
        Upgrade("reload_fast", "装填の手際", "RELOAD"),
        Upgrade("stamina_up", "推進拡張", "STAMINA"),
        Upgrade("blast_up", "爆風拡大", "BLAST"),
        Upgrade("regen_up", "自己修復", "REGEN"),
        Upgrade("dash_eff", "縮地の呼吸", "DASH ECO"),
        Upgrade("bullet_speed", "弾速強化", "VELOCITY"),
        Upgrade("armor_up", "装甲圧延", "ARMOR"),
        Upgrade("magnet_up", "回収の手", "MAGNET"),
    )

    fun byId(id: String): Upgrade? = ALL.firstOrNull { it.id == id }

    /** Pick [n] distinct upgrades at random (legacy pickUpgradeChoices: splice — no repeats). */
    fun pick(n: Int, rng: Rng): List<Upgrade> {
        val pool = ALL.toMutableList()
        val out = ArrayList<Upgrade>(minOf(n, pool.size))
        repeat(minOf(n, pool.size)) { out.add(pool.removeAt(rng.nextInt(pool.size))) }
        return out
    }

    /** Apply a permanent upgrade to the player's mutable stats (legacy UPGRADES[].apply).
     *  [stamina] is optional for source compatibility — only 推進拡張 touches it. */
    fun apply(id: String, cfg: UpgradesConfig, mods: Mods, health: Health, ammo: Ammo, materials: Materials, stamina: io.github.panda17tk.arpg.ecs.components.Stamina? = null) {
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
            // v2.107 強化カード拡充
            "reload_fast" -> mods.reloadMul *= cfg.reloadFastMul
            "stamina_up" -> stamina?.let { it.max += cfg.staminaAdd; it.value = (it.value + cfg.staminaAdd).coerceAtMost(it.max) }
            "blast_up" -> mods.blastMul *= cfg.blastUpMul
            "regen_up" -> mods.regenAdd += cfg.regenAddUp
            "dash_eff" -> mods.dashCostMul *= cfg.dashEffMul
            "bullet_speed" -> mods.bulletSpeedMul *= cfg.bulletSpeedUpMul
            "armor_up" -> mods.armorMul *= cfg.armorUpMul
            "magnet_up" -> mods.pickupRange += cfg.magnetAdd
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
        // v2.107
        "reload_fast" -> "リロード時間 ×${cfg.reloadFastMul}"
        "stamina_up" -> "スタミナ上限 +${cfg.staminaAdd.toInt()}"
        "blast_up" -> "爆風半径 ×${cfg.blastUpMul}"
        "regen_up" -> "休息回復 +${cfg.regenAddUp}/秒"
        "dash_eff" -> "ダッシュ消費 ×${cfg.dashEffMul}"
        "bullet_speed" -> "弾速 ×${cfg.bulletSpeedUpMul}"
        "armor_up" -> "被ダメージ ×${cfg.armorUpMul}"
        "magnet_up" -> "回収範囲 +${cfg.magnetAdd.toInt()}"
        else -> ""
    }

    /** v2.107: what the card would change, in the player's CURRENT numbers — 「×1.25 → ×1.56」. */
    fun preview(u: Upgrade, cfg: UpgradesConfig, mods: Mods, health: Health, stamina: io.github.panda17tk.arpg.ecs.components.Stamina): String = when (u.id) {
        "gun_dmg" -> "×%.2f → ×%.2f".format(mods.gunMul, mods.gunMul * cfg.gunMul)
        "fire_rate" -> "×%.2f → ×%.2f".format(mods.fireMul, mods.fireMul * cfg.fireMul)
        "melee" -> "×%.2f → ×%.2f".format(mods.meleeMul, mods.meleeMul * cfg.meleeMul)
        "max_hp" -> "%d → %d".format(health.hpMax.toInt(), (health.hpMax + cfg.maxHpAdd).toInt())
        "speed" -> "×%.2f → ×%.2f".format(mods.moveMul, mods.moveMul * cfg.moveMul)
        "ammo" -> "×%.2f → ×%.2f".format(mods.ammoMul, mods.ammoMul * cfg.ammoMul)
        "lifesteal" -> "+%.0f → +%.0f".format(mods.healOnKill, mods.healOnKill + cfg.lifestealAdd)
        "engineer" -> "壁HP %.0f → %.0f".format(mods.wallHp, mods.wallHp + cfg.wallHpAdd)
        "reload_fast" -> "×%.2f → ×%.2f".format(mods.reloadMul, mods.reloadMul * cfg.reloadFastMul)
        "stamina_up" -> "%d → %d".format(stamina.max.toInt(), (stamina.max + cfg.staminaAdd).toInt())
        "blast_up" -> "×%.2f → ×%.2f".format(mods.blastMul, mods.blastMul * cfg.blastUpMul)
        "regen_up" -> "+%.1f/s → +%.1f/s".format(mods.regenAdd, mods.regenAdd + cfg.regenAddUp)
        "dash_eff" -> "×%.2f → ×%.2f".format(mods.dashCostMul, mods.dashCostMul * cfg.dashEffMul)
        "bullet_speed" -> "×%.2f → ×%.2f".format(mods.bulletSpeedMul, mods.bulletSpeedMul * cfg.bulletSpeedUpMul)
        "armor_up" -> "×%.2f → ×%.2f".format(mods.armorMul, mods.armorMul * cfg.armorUpMul)
        "magnet_up" -> "+%.0f → +%.0f".format(mods.pickupRange, mods.pickupRange + cfg.magnetAdd)
        else -> ""
    }
}
