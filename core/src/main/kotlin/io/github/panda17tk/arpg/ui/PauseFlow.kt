package io.github.panda17tk.arpg.ui

/** Which blocking overlay is active (pure — no libGDX). INVENTORY (v2.33) is special: it does not
 *  freeze the sim — the world crawls at 0.01× speed behind it. */
enum class Overlay { NONE, PAUSE, HELP, MEMORY, FORGET, INVENTORY }

/** What a pause-overlay tap resolves to; the screen performs the side effects. */
enum class PauseAction { RESUME, RESTART, HELP, MEMORY, FORGET }

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
        3 -> if (hasMemory) PauseAction.MEMORY else PauseAction.FORGET
        4 -> if (hasMemory) PauseAction.FORGET else null
        else -> null
    }
}
