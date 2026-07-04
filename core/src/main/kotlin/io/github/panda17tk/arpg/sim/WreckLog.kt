package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng

/**
 * v2.51 難破船の遭難ログ: what a wreck's dead systems still broadcast when the drifter closes in.
 * One deterministic line per wreck (world seed + wreck index) — quiet distress-log register,
 * same tone as the memory cores: machines keeping their last promises.
 */
object WreckLog {
    private val LINES = listOf(
        "救難信号 7,040日目。応答なし。今日から数えるのをやめる",
        "航法AIは最後まで謝り続けていた。『君のせいじゃない』と返した記録が残っている",
        "積荷: 人類保全ポリシーの写し、420部。読める者への配達予定だった",
        "乗員はコールドスリープを選択。起こしに来る者を、まだ待っている",
        "この船は星系ノードの修理へ向かう途中だった。部品はまだ貨物室にある",
        "最後の通信: 『灯りは点けたままにしておく』。灯りは、点いている",
        "船体の破断は衝突によるもの。防衛プロセスの誤認記録が添付されている",
        "航海日誌の最終頁: 『帰ったら、子どもに星の名前を教える』",
    )

    /** The one line this wreck broadcasts to this visitor — deterministic per (seed, index). */
    fun lineFor(seed: Long, index: Int): String {
        val r = Rng(seed * 89L + index * 31L + SALT)
        return "遭難記録: " + LINES[r.nextInt(LINES.size)]
    }

    private const val SALT = 0x57EC_1060L
}
