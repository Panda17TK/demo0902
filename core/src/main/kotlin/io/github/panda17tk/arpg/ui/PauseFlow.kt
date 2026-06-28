package io.github.panda17tk.arpg.ui

/** Which blocking overlay is active (pure — no libGDX). */
enum class Overlay { NONE, PAUSE, HELP }

/** What a pause-overlay tap resolves to; the screen performs the side effects. */
enum class PauseAction { RESUME, RESTART, HELP }

/**
 * Pure pause state machine. Esc / P / the ⏸ button toggle between play and pause; from any
 * overlay the toggle returns to play. Pause-overlay taps map by index to an action the screen
 * then carries out (resume, restart, or open help).
 */
object PauseFlow {
    fun toggle(cur: Overlay): Overlay = if (cur == Overlay.NONE) Overlay.PAUSE else Overlay.NONE

    fun action(index: Int): PauseAction? = when (index) {
        0 -> PauseAction.RESUME
        1 -> PauseAction.RESTART
        2 -> PauseAction.HELP
        else -> null
    }
}
