package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/**
 * Permanent per-run player upgrades (legacy player.mods). All multipliers default to the
 * identity (1) so an un-upgraded player behaves exactly as before. fireMul is an *interval*
 * multiplier: smaller = faster (legacy CONFIG.upgrades.fireMul = 0.85).
 */
class Mods(
    var gunMul: Float = 1f,
    var fireMul: Float = 1f,
    var meleeMul: Float = 1f,
    var moveMul: Float = 1f,
    var ammoMul: Float = 1f,
    var healOnKill: Float = 0f,
    var wallHp: Float = 70f,
    // v2.107 強化カード拡充: the new dials (all neutral by default; saved with the run)
    var reloadMul: Float = 1f,      // 装填の手際 — multiplies every reload duration
    var blastMul: Float = 1f,       // 爆風拡大 — grenade + beam-impact blast radius
    var regenAdd: Float = 0f,       // 自己修復 — added to the rest-mend rate (hp/s)
    var dashCostMul: Float = 1f,    // 縮地の呼吸 — multiplies dash/blink stamina costs
    var bulletSpeedMul: Float = 1f, // 弾速強化 — multiplies muzzle velocity
    var armorMul: Float = 1f,       // 装甲圧延 — multiplies damage TAKEN (smaller = tougher)
    var pickupRange: Float = 0f,    // 回収の手 — px added to the pickup radius
) : Component<Mods> {
    override fun type() = Mods
    companion object : ComponentType<Mods>()
}
