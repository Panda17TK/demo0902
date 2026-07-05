package io.github.panda17tk.arpg.audio

/**
 * v2.67 状況反応: a tiny leaky integrator for "how much fighting is happening right now".
 * Kills and taken hits push it up, time bleeds it away; the value drives the PULSE layer's
 * volume. Pure state — no Gdx, no clock of its own.
 */
class CombatHeat {
    var value = 0f
        private set

    fun onKill() { value = (value + 0.4f).coerceAtMost(1f) }
    fun onPlayerHit() { value = (value + 0.5f).coerceAtMost(1f) }

    /** Call once per frame with the frame delta; heat fades over ~4 seconds of quiet. */
    fun tick(delta: Float) { value = (value - delta * DECAY).coerceAtLeast(0f) }

    companion object { const val DECAY = 0.25f }
}
