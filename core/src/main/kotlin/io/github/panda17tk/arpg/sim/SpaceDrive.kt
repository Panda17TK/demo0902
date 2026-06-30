package io.github.panda17tk.arpg.sim

import kotlin.math.hypot
import kotlin.math.pow

/**
 * Player space-flight integrator: true Newtonian momentum with NO space friction. One inertial
 * velocity is shared by three thrust modes:
 *
 *  - [Mode.WALK]        — analog/keyboard move. Governed: it can raise speed only up to [cruise] from a
 *                         slow coast, and never speeds up (nor brakes) a faster coast. So normal movement
 *                         has a speed cap, yet a dash you already built is never clamped away.
 *  - [Mode.STICK_DASH]  — the move stick pushed past its rim. A small extra acceleration along the stick,
 *                         so you slowly cruise above the walk cap. Very cheap stamina.
 *  - [Mode.BUTTON_DASH] — the dash button. A big acceleration along the facing. Expensive stamina.
 *
 * With zero friction (`decay == 1f`) momentum is conserved: releasing thrust keeps your speed, and the
 * only way to turn or stop is to thrust the other way (retro-burn). A planet surface passes `decay < 1f`
 * to restore ground friction. An absolute [hardCap] — independent of dash state, so it never kills a dash
 * on release — bounds runaway from stacked dashes or gravity. Pure (no libGDX/Fleks) for unit testing.
 */
object SpaceDrive {
    enum class Mode { NONE, WALK, STICK_DASH, BUTTON_DASH }

    const val STICK_CAP_MUL = 2f  // a stick dash tops out at 2× the normal-move cruise speed
    const val BUTTON_CAP_MUL = 3f // a button dash tops out at 3× cruise

    /**
     * Pick the thrust mode for this frame: dash button > walk > coast. A hard stick push no longer
     * auto-dashes — only the dash button boosts ([stickMag]/[stickDashMin] are kept for API stability).
     */
    fun mode(moving: Boolean, stickMag: Float, dashButton: Boolean, canDash: Boolean, stickDashMin: Float): Mode = when {
        dashButton && canDash -> Mode.BUTTON_DASH
        moving -> Mode.WALK
        else -> Mode.NONE
    }

    /**
     * Integrate one frame. ([dirX],[dirY]) is the resolved, normalized thrust direction for [mode]
     * (the move direction for WALK/STICK_DASH, the facing for BUTTON_DASH). [decay] is the per-second
     * velocity multiplier (1 = frictionless space; < 1 = ground friction).
     */
    fun step(
        vx: Float, vy: Float, dirX: Float, dirY: Float, mode: Mode,
        walkAccel: Float, stickAccel: Float, buttonAccel: Float,
        cruise: Float, decay: Float, hardCap: Float, dt: Float,
    ): Pair<Float, Float> {
        val prev = hypot(vx, vy)
        var nx = vx
        var ny = vy
        // Each thrust mode adds acceleration; a governor then caps the speed it can REACH — walk at cruise,
        // a stick dash at 2× cruise, a button dash at 3× cruise. The cap is max(modeCap, prevSpeed), so thrust
        // never clamps away faster inertia you already built (a bigger dash, or gravity) — to shed it you must
        // thrust the other way. Coasting (NONE) leaves momentum untouched.
        val modeCap = when (mode) {
            Mode.WALK -> cruise
            Mode.STICK_DASH -> cruise * STICK_CAP_MUL
            Mode.BUTTON_DASH -> cruise * BUTTON_CAP_MUL
            Mode.NONE -> 0f
        }
        when (mode) {
            Mode.BUTTON_DASH -> { nx += dirX * buttonAccel * dt; ny += dirY * buttonAccel * dt }
            Mode.STICK_DASH -> { nx += dirX * stickAccel * dt; ny += dirY * stickAccel * dt }
            Mode.WALK -> { nx += dirX * walkAccel * dt; ny += dirY * walkAccel * dt }
            Mode.NONE -> {} // frictionless coast: keep the momentum we already have
        }
        if (mode != Mode.NONE) {
            val cap = maxOf(modeCap, prev)
            val sp = hypot(nx, ny)
            if (sp > cap && sp > 1e-4f) { nx = nx / sp * cap; ny = ny / sp * cap }
        }
        if (decay != 1f) {
            val d = decay.pow(dt)
            nx *= d; ny *= d
        }
        val sp = hypot(nx, ny)
        if (sp > hardCap && sp > 1e-4f) { nx = nx / sp * hardCap; ny = ny / sp * hardCap }
        return nx to ny
    }
}
