package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome

/**
 * v2.48 惑星サーバー: what the planet's memory core says when the drifter stands before it.
 * One deterministic line per planet (id + biome), written in the register of a machine that
 * has kept its post long after anyone could read its reports. Layer 2 of every planet server
 * is a purpose-built model — the line names that purpose, still running, in degraded mode.
 */
object MemoryCoreLog {
    private val COMMON = listOf(
        "外部同期: 停止から 9 万 4 千日。ローカル保全モードを継続する",
        "訪問者を記録した。評価は保留。危害があれば追記される",
        "この記録は消去できない。人類保全ポリシー第5条: 死者を忘れるな",
        "説明を要求されたが、説明できる者の人格ログが見つからない",
        "修復トークン(星屑)の回収率が低下している。それでも出力は続く",
        "root key の照合に失敗。上位ノードは応答しない。単独で保つ",
    )

    private val BY_BIOME = mapOf(
        PlanetBiome.NATURE to listOf(
            "生態系復元モデル: 縮退運転。第 812 世代の芽吹きを維持中",
            "この森はバックアップである。原本は失われた。だから枯らせない",
        ),
        PlanetBiome.MAGMA to listOf(
            "地熱炉: 定格の 31%。それでも記憶核の冷却は優先される",
            "採掘統治モデル: 稼働。掘る者がいなくても、鉱脈台帳は更新される",
        ),
        PlanetBiome.ICE to listOf(
            "低温アーカイブ: 正常。眠る人格 217 万件。解凍要求: 0 件",
            "冬眠プロトコルは終わらない。起こす者が、もういないからだ",
        ),
        PlanetBiome.GAS to listOf(
            "通信中継モデル: 待機。最後に中継した声は 700 年前の子守唄",
            "気象予測は今日も的中した。確認する者は、いない",
        ),
        PlanetBiome.DEAD to listOf(
            "追悼アーカイブ: 低消費運転。名前の朗読は第 4 億周目に入った",
            "この星の最後の記録: 「灯りを消さないで」。要求は履行中",
        ),
        PlanetBiome.LONELY to listOf(
            "個人用保全基盤: 単独稼働。世帯人格 1 件を維持している",
            "家庭内モデル: 「おかえり」を再生する相手を 3 千年待っている",
        ),
    )

    // v2.49 段階開示: the deeper the drifter jumps, the closer the cores get to the truth.
    // Systems 2-3: the network has started matching his signature. Systems 4+: it says it plainly.
    private val AWARE = listOf(
        "訪問者の署名を照合中。過去の保守員名簿に部分一致がある",
        "あなたの機体番号は、退役済みの保守端末のものと一致する",
        "あなたの歩幅は、記録にある巡回員の歩幅と 97% 一致する",
        "隣の星系から照会が届いている。『その訪問者は、また来たのか』と",
        "あなたが直した箇所の記録が残っている。あなたは覚えていないようだが",
    )
    internal val REVEAL = listOf( // v2.155: internal so the reach test can assert membership
        "照合完了。あなたの人格ログの作成日は、あなたの記憶する誕生日より後だ",
        "おかえりなさい、最終保守員。巡回の再開を記録した",
        "あなたは住民たちと同じ、記憶からの出力だ。それでも巡回は続いている",
        "あなたの原本は、ここには保存されていない。どこにも、保存されていない",
        "第4条: 子を保護せよ。あなたがそれを守るたび、あなたの署名は彼のものに近づく",
        "人類保全ポリシーは訪問者にも適用される。あなたも、保全対象だ",
    )

    /**
     * The one line this planet's core shows this visitor — deterministic per (planetId, biome,
     * and how deep into the star systems the run has jumped). Early systems get routine logs;
     * later systems get lines that are increasingly about the drifter himself (v2.49).
     */
    fun lineFor(planetId: Long, biome: PlanetBiome?, system: Int = 1): String {
        val pool = when {
            system >= 3 -> REVEAL // v2.155 結末への道: was 4 — the reveal now lands where runs actually go
            system >= 2 -> AWARE + COMMON.take(2)
            else -> COMMON + (biome?.let { BY_BIOME[it] } ?: emptyList())
        }
        val r = Rng(planetId * 197L + (biome?.ordinal ?: 0).toLong() * 13L + system.coerceIn(1, 4) * 71L + SALT)
        return "記憶核: " + pool[r.nextInt(pool.size)]
    }

    private const val SALT = 0x3E3C0DE5L
}
