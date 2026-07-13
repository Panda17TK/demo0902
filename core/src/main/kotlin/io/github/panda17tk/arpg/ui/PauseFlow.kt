package io.github.panda17tk.arpg.ui

/** Which blocking overlay is active (pure — no libGDX). INVENTORY (v2.33) is special: it does not
 *  freeze the sim — the world crawls at 0.01× speed behind it. */
enum class Overlay { NONE, PAUSE, HELP, MEMORY, FORGET, INVENTORY, TRADER, TUNING, PHOTO } // v2.187 写真モード

/** What a pause-overlay tap resolves to; the screen performs the side effects. */
enum class PauseAction { RESUME, RESTART, HELP, MEMORY, SIM, TITLE, FORGET, PHOTO } // v2.187 写真モード

/**
 * Pure pause state machine. Esc / P / the ⏸ button toggle between play and pause; from any
 * overlay the toggle returns to play. Pause-overlay taps map by index to an action the screen
 * then carries out (resume, restart, open help — and on a surface, the planet-memory summary).
 */
object PauseFlow {
    fun toggle(cur: Overlay): Overlay = if (cur == Overlay.NONE) Overlay.PAUSE else Overlay.NONE

    /**
     * [hasMemory] adds the 「この星の記憶」 entry (surface pauses only — LP v2.25); the destructive
     * 「宇宙の記憶を消す」 entry (LP v2.28) always sits last, behind its own confirmation screen.
     */
    fun action(index: Int, hasMemory: Boolean = false): PauseAction? = when (index) {
        0 -> PauseAction.RESUME
        1 -> PauseAction.RESTART
        2 -> PauseAction.HELP
        3 -> if (hasMemory) PauseAction.MEMORY else PauseAction.SIM // v2.53: 旧式戦闘訓練
        4 -> if (hasMemory) PauseAction.SIM else PauseAction.TITLE // v2.58: タイトルへ
        // v2.187 写真モード: space pauses gain a 写真モード entry before FORGET (surface pauses,
        // already carrying 記憶, stay compact — the entry is space-only). Surface mapping unchanged.
        5 -> if (hasMemory) PauseAction.TITLE else PauseAction.PHOTO
        6 -> PauseAction.FORGET
        else -> null
    }
}
