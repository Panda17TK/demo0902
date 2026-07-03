package io.github.panda17tk.arpg.ui

/**
 * The landing/takeoff fade (LP v2.30/10b as v2.31 UI/UX): FADE_OUT → (build the new world) → FADE_IN,
 * so the world swap happens behind black instead of an instant cut. Pure state machine — GameScreen
 * calls [update] each frame and performs the transition on the single frame update() returns true.
 * Input is meant to be ignored while [blocksInput].
 */
class TransitionFade(private val leg: Float = LEG) {
    enum class Phase { NONE, OUT, IN }

    var phase = Phase.NONE
        private set
    private var t = 0f

    /** Begin a fade-out toward a transition; refused (false) while one is already running. */
    fun start(): Boolean {
        if (phase != Phase.NONE) return false
        phase = Phase.OUT
        t = 0f
        return true
    }

    /**
     * Advance the fade. Returns true on exactly the frame the OUT leg completes — the caller
     * performs the world transition on that frame, behind full black, and the IN leg follows.
     */
    fun update(dt: Float): Boolean {
        when (phase) {
            Phase.NONE -> return false
            Phase.OUT -> {
                t += dt
                if (t >= leg) { phase = Phase.IN; t = 0f; return true }
            }
            Phase.IN -> {
                t += dt
                if (t >= leg) { phase = Phase.NONE; t = 0f }
            }
        }
        return false
    }

    /** The black scrim's opacity this frame: rises through OUT, falls through IN, 0 when idle. */
    val alpha: Float
        get() = when (phase) {
            Phase.NONE -> 0f
            Phase.OUT -> (t / leg).coerceIn(0f, 1f)
            Phase.IN -> (1f - t / leg).coerceIn(0f, 1f)
        }

    /** Gameplay input (movement/fire/land) is ignored while a fade runs. */
    val blocksInput: Boolean get() = phase != Phase.NONE

    companion object { const val LEG = 0.35f } // seconds per leg (out, then in)
}
