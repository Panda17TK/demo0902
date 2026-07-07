package io.github.panda17tk.arpg.config

/**
 * v2.98 調整モード — one live balance knob: a name, a step, bounds, and the accessors that
 * reach into the shared [GameConfig] (whose fields are `var` for exactly this). Nudging is
 * clamped, so the popup can never push a value somewhere the sim can't survive.
 */
class TuneParam(
    val name: String,
    val step: Float,
    val min: Float,
    val max: Float,
    val get: () -> Float,
    val set: (Float) -> Unit,
) {
    fun nudge(dir: Int) {
        set((get() + dir * step).coerceIn(min, max))
    }

    /** Integers print clean; fractions keep two places. */
    fun display(): String {
        val v = get()
        return if (step >= 1f) v.toInt().toString() else String.format("%.2f", v)
    }
}

/** The catalog the tuning popup pages through — player balance first, then every weapon. */
object TuningCatalog {
    fun paramsFor(config: GameConfig): List<TuneParam> = buildList {
        val p = config.player
        add(TuneParam("最大HP", 10f, 10f, 999f, { p.hpMax }, { p.hpMax = it }))
        add(TuneParam("最大スタミナ", 20f, 50f, 999f, { p.staMax }, { p.staMax = it }))
        add(TuneParam("移動速度", 5f, 20f, 400f, { p.baseSpeed }, { p.baseSpeed = it }))
        add(TuneParam("ダッシュ倍率", 0.1f, 1f, 8f, { p.dashMul }, { p.dashMul = it }))
        add(TuneParam("近接ダメージ", 2f, 1f, 500f, { p.meleeDmg }, { p.meleeDmg = it }))
        add(TuneParam("近接リーチ", 3f, 10f, 300f, { p.meleeReach }, { p.meleeReach = it }))
        add(TuneParam("近接間隔", 0.02f, 0.05f, 2f, { p.meleeCd }, { p.meleeCd = it }))
        add(TuneParam("弾速", 20f, 60f, 1500f, { p.bulletSpeed }, { p.bulletSpeed = it }))
        add(TuneParam("弾寿命", 0.1f, 0.2f, 8f, { p.bulletLife }, { p.bulletLife = it }))
        add(TuneParam("爆発半径", 10f, 30f, 600f, { p.explodeRadius }, { p.explodeRadius = it }))
        add(TuneParam("爆発ダメージ", 10f, 10f, 999f, { p.explodeDmg }, { p.explodeDmg = it }))
        add(TuneParam("グレネード弾速", 8f, 40f, 600f, { p.grenadeSpeed }, { p.grenadeSpeed = it }))
        add(TuneParam("スタミナ消費", 2f, 0f, 200f, { p.staDrain }, { p.staDrain = it }))
        add(TuneParam("スタミナ回復", 2f, 0f, 200f, { p.staRegen }, { p.staRegen = it }))
        for (w in config.weapons) {
            add(TuneParam("${w.name} 威力", 1f, 1f, 500f, { w.dmg }, { w.dmg = it }))
            add(TuneParam("${w.name} 連射間隔", 0.01f, 0.02f, 3f, { w.fireRate }, { w.fireRate = it }))
            if (w.magSize != null) {
                add(TuneParam("${w.name} 装弾数", 1f, 1f, 999f, { (w.magSize ?: 0).toFloat() }, { w.magSize = it.toInt() }))
            }
            if (w.pellets > 1) {
                add(TuneParam("${w.name} 発射数", 1f, 1f, 30f, { w.pellets.toFloat() }, { w.pellets = it.toInt() }))
            }
            if (w.reloadTime > 0f) {
                add(TuneParam("${w.name} リロード", 0.1f, 0.1f, 10f, { w.reloadTime }, { w.reloadTime = it }))
            }
        }
    }
}
