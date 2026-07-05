package io.github.panda17tk.arpg.ui

/**
 * v2.86 開幕バナー — the one-line toast could be missed in a firefight; a wave event now opens
 * with a short cinematic band: a scrim slides across the mid-screen, the line rides in, holds,
 * and lets go. Pure timing state — GameScreen draws it (scrim + fitted text) from [alpha]/[slide].
 */
class EventBanner {
    var text = ""
        private set
    private var t = -1f

    val active: Boolean get() = t >= 0f

    fun start(line: String) {
        text = line
        t = 0f
    }

    fun update(dt: Float) {
        if (t >= 0f) {
            t += dt
            if (t >= LIFE) t = -1f
        }
    }

    /** Scrim/text opacity: rises through the first beat, holds, falls over the last. */
    fun alpha(): Float = when {
        !active -> 0f
        t < RISE -> t / RISE
        t > LIFE - FALL -> ((LIFE - t) / FALL).coerceIn(0f, 1f)
        else -> 1f
    }

    /** Text x-offset (dp): eases in from the right, then drifts slowly left through the hold. */
    fun slide(): Float {
        if (!active) return 0f
        if (t < RISE) { val k = 1f - t / RISE; return k * k * SLIDE_IN }
        return -DRIFT * ((t - RISE) / (LIFE - RISE))
    }

    companion object {
        const val LIFE = 2.4f   // seconds on screen
        const val RISE = 0.25f  // ease-in beat
        const val FALL = 0.45f  // let-go beat
        const val SLIDE_IN = 36f
        const val DRIFT = 10f
    }
}
