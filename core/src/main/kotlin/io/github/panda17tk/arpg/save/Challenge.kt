package io.github.panda17tk.arpg.save

/**
 * v2.102 検証ラン: the weekly proving run — one fixed sky, one fixed loadout, every keeper
 * on the same footing. Pure time→seed math: the week turns on Monday (UTC), and the seed is
 * a stable mix of the week index, so「今週の宙域」is the same for everyone all week.
 */
object Challenge {
    /** Week index for a wall-clock instant; weeks turn on Monday 00:00 UTC. */
    fun weekOf(epochMillis: Long): Long = (epochMillis / 86_400_000L + 3) / 7 // 1970-01-01 was a Thursday

    /** The week's sky. Always positive and never ≤ 1 (seed 1 is the home system; ≤1 = no trait). */
    fun seedFor(week: Long): Long {
        var z = week * -0x61c8_8646_80b5_83ebL + 0x9E37_79B9L
        z = (z xor (z ushr 31)) and Long.MAX_VALUE
        return if (z <= 1L) z + 0x7EC1L else z
    }

    /** v2.119: days until the sky turns (Monday 00:00 UTC) — 1..7; 1 means within 24 hours. */
    fun daysLeft(epochMillis: Long): Int {
        val day = epochMillis / 86_400_000L + 3 // aligned so week boundaries land on multiples of 7
        return (7 - (day % 7)).toInt()
    }

    /** The short code the HUD and the records both wear (calm — a designation, not a fanfare). */
    fun codeFor(week: Long): String = "W$week"

    // v2.180 今日の宙域: the daily sibling — a fresh fixed sky every UTC midnight, for the
    // keeper who wants a new proving ground before the week turns.
    fun dayOf(epochMillis: Long): Long = epochMillis / 86_400_000L

    /** The day's sky — a different salt from the weekly mix, so D-skies never mirror W-skies. */
    fun seedForDay(day: Long): Long {
        var z = day * -0x7ee3_623a_03d3_92e9L + 0x1234_5678L
        z = (z xor (z ushr 29)) and Long.MAX_VALUE
        return if (z <= 1L) z + 0x7EC1L else z
    }

    fun codeForDay(day: Long): String = "D$day"

    /** v2.180 記録コード: one shareable line — code, wave, kills, and a short checksum so a
     *  typo'd or doctored code is at least awkward. Lands on the clipboard at game over. */
    fun resultCode(code: String, wave: Int, kills: Int): String {
        val sum = (code.hashCode() * 31 + wave * 131 + kills * 7) and 0xFFF
        return "$code-w$wave-k$kills-%03x".format(sum)
    }
}
