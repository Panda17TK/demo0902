package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng

/** What flavor a space wave carries (v2.45; v2.48 adds the purge): nothing, a horde, a magnetic
 *  storm, a bounty head, or a custodial purge sweep (惑星サーバーの清掃プロトコル). */
enum class WaveEvent { NONE, HORDE, STORM, BOUNTY, PURGE }

/**
 * v2.45 イベントウェーブ + 賞金首 — pure wave-flavor rules the spawner consults.
 * The schedule is modular on the wave number so a player can LEARN the rhythm:
 * bounties on 4/11/18…, hordes on 3/8/13…, storms on 5/17/29… (bounty, then horde, wins ties).
 */
object WaveEvents {
    fun eventFor(wave: Int): WaveEvent = when {
        wave % 7 == 4 -> WaveEvent.BOUNTY
        wave % 5 == 3 -> WaveEvent.HORDE
        wave % 6 == 5 -> WaveEvent.STORM
        wave % 9 == 7 -> WaveEvent.PURGE // v2.48: 7/16/34… the server sweeps its own halls
        else -> WaveEvent.NONE
    }

    /** The one-line HUD announcement a wave opens with (null = an ordinary wave). */
    fun announceFor(event: WaveEvent, bountyName: String?): String? = when (event) {
        WaveEvent.NONE -> null
        WaveEvent.HORDE -> "大群が接近している"
        WaveEvent.STORM -> "磁気嵐 — 敵は荒ぶり、星屑は多くこぼれる"
        WaveEvent.BOUNTY -> "賞金首『${bountyName ?: "名も無き者"}』が現れた"
        WaveEvent.PURGE -> "清掃プロトコル起動 — 保守機構が展開する"
    }

    // Calm two-part names (落ち着いたトーン): a quiet epithet + a short call sign.
    private val TITLES = listOf("錆の", "静かな", "緋の", "遠雷の", "霧の", "枯野の", "宵の", "白磁の")
    private val NAMES = listOf("カラス", "ヴェガ", "ノクト", "ロッカ", "ハクア", "スズ", "ガラン", "ユキ")

    /** Draw a bounty head's name from the given stream (deterministic for a deterministic sim). */
    fun bountyName(r: Rng): String = TITLES[r.nextInt(TITLES.size)] + NAMES[r.nextInt(NAMES.size)]

    /** The dust pile a bounty head bursts into — worth chasing at any wave, better later. */
    fun bountyReward(wave: Int): Int = 40 + wave * 2

    // HORDE: more bodies, faster trickle. STORM: everyone dashes, kills shed double dust.
    const val HORDE_QUOTA_MUL = 1.6f
    const val HORDE_INTERVAL_MUL = 0.7f
    const val STORM_DASH_CHANCE = 0.9f
    const val STORM_DUST_MUL = 2

    // PURGE (v2.48 惑星サーバー): the wave spawns ONLY the preservation machinery — custodians,
    // indexers, quarantine bodies — as the local server runs a maintenance sweep of its halls.
    val PURGE_KEYS = listOf("custodian", "indexer", "quarantine")
    const val PURGE_QUOTA_MUL = 1.2f
}
