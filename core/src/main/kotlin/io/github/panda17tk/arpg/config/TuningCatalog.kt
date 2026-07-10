package io.github.panda17tk.arpg.config

import kotlin.math.abs

/**
 * v2.98/2.99 調整モード — one live balance knob: a name, the pristine default (基準), a step,
 * DEBUG-wide bounds, and accessors into the shared [GameConfig] (whose fields are `var` for
 * exactly this). Nudging clamps; [reset] returns to the shipped value; [changed] marks drift.
 */
class TuneParam(
    val name: String,
    val def: Float,
    val step: Float,
    val min: Float,
    val max: Float,
    val get: () -> Float,
    val set: (Float) -> Unit,
) {
    /** One tap: ±step. [big] (the ≪/≫ buttons) rides ×10. */
    fun nudge(dir: Int, big: Boolean = false) {
        set((get() + dir * step * (if (big) 10f else 1f)).coerceIn(min, max))
    }

    fun reset() = set(def)

    fun changed(): Boolean = abs(get() - def) > 1e-4f

    /** Integers print clean; fractions keep two places. */
    fun display(): String = fmt(get())

    fun displayDef(): String = fmt(def)

    private fun fmt(v: Float): String = if (step >= 1f) v.toInt().toString() else String.format("%.2f", v)
}

/**
 * The catalog the tuning popup pages through — every player field, every weapon field, the
 * whole wave/AI table, and roster-wide enemy multipliers. Defaults come from a pristine
 * [GameConfig], so 基準 stays honest no matter how far the knobs have wandered.
 */
object TuningCatalog {
    fun paramsFor(config: GameConfig): List<TuneParam> = buildList {
        val base = GameConfig() // the shipped values — the 基準 column
        val p = config.player
        val bp = base.player
        fun add(name: String, def: Float, step: Float, min: Float, max: Float, get: () -> Float, set: (Float) -> Unit) =
            add(TuneParam(name, def, step, min, max, get, set))

        // ── プレイヤー (PlayerConfig 全項目) ─────────────────────────────
        add("最大HP", bp.hpMax, 10f, 1f, 99999f, { p.hpMax }, { p.hpMax = it })
        add("最大スタミナ", bp.staMax, 20f, 1f, 99999f, { p.staMax }, { p.staMax = it })
        add("移動速度", bp.baseSpeed, 5f, 1f, 3000f, { p.baseSpeed }, { p.baseSpeed = it })
        add("移動速度倍率", bp.speedMul, 0.1f, 0.01f, 50f, { p.speedMul }, { p.speedMul = it })
        add("ダッシュ倍率", bp.dashMul, 0.1f, 0.01f, 50f, { p.dashMul }, { p.dashMul = it })
        add("ダッシュ推力", bp.dashThrust, 20f, 0f, 20000f, { p.dashThrust }, { p.dashThrust = it })
        add("スタミナ消費", bp.staDrain, 2f, 0f, 2000f, { p.staDrain }, { p.staDrain = it })
        add("スタミナ回復", bp.staRegen, 2f, 0f, 2000f, { p.staRegen }, { p.staRegen = it })
        add("近接ダメージ", bp.meleeDmg, 2f, 0f, 9999f, { p.meleeDmg }, { p.meleeDmg = it })
        add("近接リーチ", bp.meleeReach, 3f, 1f, 2000f, { p.meleeReach }, { p.meleeReach = it })
        add("近接間隔", bp.meleeCd, 0.02f, 0.01f, 10f, { p.meleeCd }, { p.meleeCd = it })
        add("近接斬撃ダメージ", bp.meleeSlashDmg, 1f, 0f, 9999f, { p.meleeSlashDmg }, { p.meleeSlashDmg = it })
        add("近接弱化しきい値", bp.meleeStaWeakBelow, 0.05f, 0f, 1f, { p.meleeStaWeakBelow }, { p.meleeStaWeakBelow = it })
        add("剣最低スタミナ", bp.meleeStaSwordMin, 0.05f, 0f, 1f, { p.meleeStaSwordMin }, { p.meleeStaSwordMin = it })
        add("近接弱化倍率", bp.meleeWeakMul, 0.05f, 0.01f, 5f, { p.meleeWeakMul }, { p.meleeWeakMul = it })
        add("素手ダメージ", bp.fistDmg, 1f, 0f, 9999f, { p.fistDmg }, { p.fistDmg = it })
        add("近接スタミナ消費", bp.meleeStaCost, 2f, 0f, 2000f, { p.meleeStaCost }, { p.meleeStaCost = it })
        add("弾速", bp.bulletSpeed, 20f, 10f, 20000f, { p.bulletSpeed }, { p.bulletSpeed = it })
        add("弾寿命", bp.bulletLife, 0.1f, 0.05f, 60f, { p.bulletLife }, { p.bulletLife = it })
        add("グレネード弾速", bp.grenadeSpeed, 8f, 5f, 5000f, { p.grenadeSpeed }, { p.grenadeSpeed = it })
        add("グレネード信管", bp.grenadeFuse, 0.1f, 0.05f, 30f, { p.grenadeFuse }, { p.grenadeFuse = it })
        add("自動装填遅延", bp.autoReloadDelay, 0.1f, 0f, 30f, { p.autoReloadDelay }, { p.autoReloadDelay = it })
        add("爆発半径", bp.explodeRadius, 10f, 5f, 5000f, { p.explodeRadius }, { p.explodeRadius = it })
        add("爆発ダメージ", bp.explodeDmg, 10f, 0f, 99999f, { p.explodeDmg }, { p.explodeDmg = it })
        add("爆発自傷", bp.explodeSelfDmg, 5f, 0f, 9999f, { p.explodeSelfDmg }, { p.explodeSelfDmg = it })
        add("爆発壁ダメージ", bp.explodeWallDmg, 10f, 0f, 99999f, { p.explodeWallDmg }, { p.explodeWallDmg = it })

        // ── 武器 (WeaponDef 全調整項目 × 全武器) ────────────────────────
        for (w in config.weapons) {
            val bw = base.weapons.first { it.id == w.id }
            add("${w.name} 威力", bw.dmg, 1f, 0f, 9999f, { w.dmg }, { w.dmg = it })
            add("${w.name} 連射間隔", bw.fireRate, 0.01f, 0.005f, 30f, { w.fireRate }, { w.fireRate = it })
            add("${w.name} 拡散", bw.spread, 0.01f, 0f, 3f, { w.spread }, { w.spread = it })
            if (w.magSize != null) {
                add("${w.name} 装弾数", (bw.magSize ?: 0).toFloat(), 1f, 1f, 99999f, { (w.magSize ?: 0).toFloat() }, { w.magSize = it.toInt() })
            }
            add("${w.name} 発射数", bw.pellets.toFloat(), 1f, 1f, 200f, { w.pellets.toFloat() }, { w.pellets = it.toInt() })
            if (w.reloadTime > 0f) {
                add("${w.name} リロード", bw.reloadTime, 0.1f, 0.05f, 60f, { w.reloadTime }, { w.reloadTime = it })
            }
        }

        // ── 敵 (全ロースター一括倍率 — 基準は 1.00) ─────────────────────
        val zombieHp = base.enemies.getValue("zombie").hp
        val zombieSpeed = base.enemies.getValue("zombie").speed
        add("全敵 HP倍率", 1f, 0.05f, 0.01f, 100f,
            { config.enemies.getValue("zombie").hp / zombieHp },
            { f -> for ((k, d) in config.enemies) d.hp = base.enemies.getValue(k).hp * f })
        add("全敵 速度倍率", 1f, 0.05f, 0.01f, 20f,
            { config.enemies.getValue("zombie").speed / zombieSpeed },
            { f -> for ((k, d) in config.enemies) d.speed = base.enemies.getValue(k).speed * f })
        val zombieDmg = base.enemies.getValue("zombie").attacks.first { it.dmg > 0f }.dmg
        add("全敵 攻撃力倍率", 1f, 0.05f, 0f, 100f,
            { config.enemies.getValue("zombie").attacks.first { a -> a.dmg > 0f }.dmg / zombieDmg },
            { f ->
                for ((k, d) in config.enemies) {
                    val bAtk = base.enemies.getValue(k).attacks
                    d.attacks.forEachIndexed { i, atk -> bAtk.getOrNull(i)?.let { atk.dmg = it.dmg * f } }
                }
            })
        add("全敵 攻撃間隔倍率", 1f, 0.05f, 0.05f, 20f,
            { config.enemies.getValue("zombie").attacks.first().cd / base.enemies.getValue("zombie").attacks.first().cd },
            { f ->
                for ((k, d) in config.enemies) {
                    val bAtk = base.enemies.getValue(k).attacks
                    d.attacks.forEachIndexed { i, atk -> bAtk.getOrNull(i)?.let { atk.cd = it.cd * f } }
                }
            })

        // ── ウェーブ (WaveConfig 全項目) ─────────────────────────────────
        val wv = config.waves
        val bwv = base.waves
        add("初回ウェーブ遅延", bwv.firstWaveDelay, 0.5f, 0f, 120f, { wv.firstWaveDelay }, { wv.firstWaveDelay = it })
        add("幕間", bwv.intermission, 0.5f, 0f, 120f, { wv.intermission }, { wv.intermission = it })
        add("基本物量", bwv.baseQuota.toFloat(), 1f, 1f, 999f, { wv.baseQuota.toFloat() }, { wv.baseQuota = it.toInt() })
        add("物量/波", bwv.quotaPerWave.toFloat(), 1f, 0f, 200f, { wv.quotaPerWave.toFloat() }, { wv.quotaPerWave = it.toInt() })
        add("物量上限", bwv.maxQuota.toFloat(), 5f, 1f, 9999f, { wv.maxQuota.toFloat() }, { wv.maxQuota = it.toInt() })
        add("敵HP成長/波", bwv.hpScalePerWave, 0.01f, 0f, 5f, { wv.hpScalePerWave }, { wv.hpScalePerWave = it })
        add("敵速度成長/波", bwv.speedScalePerWave, 0.01f, 0f, 5f, { wv.speedScalePerWave }, { wv.speedScalePerWave = it })
        add("同時生存 基本", bwv.liveCapBase.toFloat(), 1f, 1f, 999f, { wv.liveCapBase.toFloat() }, { wv.liveCapBase = it.toInt() })
        add("同時生存/波", bwv.liveCapPerWave.toFloat(), 1f, 0f, 100f, { wv.liveCapPerWave.toFloat() }, { wv.liveCapPerWave = it.toInt() })
        add("同時生存上限", bwv.maxLiveCap.toFloat(), 5f, 1f, 999f, { wv.maxLiveCap.toFloat() }, { wv.maxLiveCap = it.toInt() })
        add("湧き間隔 基本", bwv.spawnIntervalBase, 0.05f, 0.01f, 30f, { wv.spawnIntervalBase }, { wv.spawnIntervalBase = it })
        add("湧き間隔短縮/波", bwv.spawnIntervalPerWave, 0.01f, 0f, 5f, { wv.spawnIntervalPerWave }, { wv.spawnIntervalPerWave = it })
        add("湧き間隔下限", bwv.minSpawnInterval, 0.01f, 0.01f, 30f, { wv.minSpawnInterval }, { wv.minSpawnInterval = it })
        add("中ボス周期", bwv.midBossEvery.toFloat(), 1f, 1f, 99f, { wv.midBossEvery.toFloat() }, { wv.midBossEvery = it.toInt() })
        add("ボス周期", bwv.bossEvery.toFloat(), 1f, 1f, 99f, { wv.bossEvery.toFloat() }, { wv.bossEvery = it.toInt() })

        // ── AI (AiConfig 全項目) ─────────────────────────────────────────
        val ai = config.ai
        val bai = base.ai
        add("分離半径", bai.sepRadius, 2f, 0f, 500f, { ai.sepRadius }, { ai.sepRadius = it })
        add("低HP鈍化倍率", bai.hpSlowMul, 0.05f, 0.01f, 5f, { ai.hpSlowMul }, { ai.hpSlowMul = it })
        add("徘徊速度倍率", bai.wanderSlow, 0.05f, 0.01f, 5f, { ai.wanderSlow }, { ai.wanderSlow = it })
        add("徘徊詰まり", bai.wanderStuck, 0.1f, 0.05f, 10f, { ai.wanderStuck }, { ai.wanderStuck = it })
        add("経路再計算間隔", bai.flowRebuildInterval, 0.05f, 0.05f, 10f, { ai.flowRebuildInterval }, { ai.flowRebuildInterval = it })
        add("接触ノックバック", bai.playerKnockback, 20f, 0f, 5000f, { ai.playerKnockback }, { ai.playerKnockback = it })
        add("接触無敵時間", bai.iFrameContact, 0.05f, 0f, 10f, { ai.iFrameContact }, { ai.iFrameContact = it })
        // v2.143 野生/魚群 — the ecology's knobs (defaults = the shipped boid/predation constants)
        val wl = config.wild
        val bwl = base.wild
        add("群れ結集", bwl.schoolCohesion, 0.05f, 0f, 10f, { wl.schoolCohesion }, { wl.schoolCohesion = it })
        add("群れ整列", bwl.schoolAlign, 0.05f, 0f, 10f, { wl.schoolAlign }, { wl.schoolAlign = it })
        add("群れ分離", bwl.schoolSeparate, 1f, 0f, 200f, { wl.schoolSeparate }, { wl.schoolSeparate = it })
        add("群れ逃走", bwl.schoolFlee, 0.1f, 0f, 20f, { wl.schoolFlee }, { wl.schoolFlee = it })
        add("群れ徘徊", bwl.schoolWander, 0.05f, 0f, 5f, { wl.schoolWander }, { wl.schoolWander = it })
        add("噛みつき溜め", bwl.biteWindup, 0.05f, 0.05f, 3f, { wl.biteWindup }, { wl.biteWindup = it })
    }
}

/**
 * v2.99 書き出し — the current knob table as plain text, made to be handed to Claude:
 * one line per knob, the shipped 基準 beside every value, drifted lines flagged with `*`.
 */
object TuningExport {
    fun render(title: String, params: List<TuneParam>): String = buildString {
        appendLine("# $title")
        appendLine("# 形式: 項目 = 現在値 (基準 既定値)   ※基準から変更した行の先頭に *")
        appendLine("# このファイルを Claude に渡すと、変更点(*)を読み取ってゲーム調整に反映できます")
        val changed = params.count { it.changed() }
        appendLine("# 変更 $changed / ${params.size} 項目")
        appendLine()
        for (p in params) {
            appendLine("${if (p.changed()) "*" else " "} ${p.name} = ${p.display()} (基準 ${p.displayDef()})")
        }
    }
}
