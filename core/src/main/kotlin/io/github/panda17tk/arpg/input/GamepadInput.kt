package io.github.panda17tk.arpg.input

import com.badlogic.gdx.controllers.Controllers

/**
 * v2.188 ゲームパッド: reads the first connected controller into [InputState] — ADDITIVELY. It only
 * sets a field when its stick/button is active, so it never clobbers the frame's keyboard/touch
 * values; poll it AFTER them. Untested like KeyboardInput/TouchControls (it touches the device
 * boundary and gdx-controllers); the mapping math lives in the pure, unit-tested [GamepadMap].
 *
 * Mapping (Xbox layout): left stick = move, right stick = aim + auto-fire (release fires the
 * manual-fire beam/grenade along the held aim), A = dash (held), R1 = full throttle (held),
 * X = melee, Y = reload, B = land/takeoff. Weapon switch stays on the on-screen button for now.
 *
 * Aim relies on [KeyboardInput] resetting `aiming=false` first each frame (it always runs), so a
 * centred right stick leaves no stuck aim and this source only ever SETS aiming when active.
 */
object GamepadInput {
    /** True while a controller is connected — a screen may surface a quiet "パッド接続" note. */
    var present = false
        private set

    private var prevX = false
    private var prevY = false
    private var prevB = false
    private var prevAiming = false
    private var lastAimX = 1f
    private var lastAimY = 0f

    fun poll(state: InputState, suppress: Boolean) {
        val arr = try { Controllers.getControllers() } catch (_: Throwable) { null }
        val pad = if (arr != null && arr.size > 0) arr.first() else null
        present = pad != null
        if (pad == null) return
        val m = try { pad.mapping } catch (_: Throwable) { null } ?: return

        fun axis(idx: Int): Float = if (idx >= 0) try { pad.getAxis(idx) } catch (_: Throwable) { 0f } else 0f
        fun button(idx: Int): Boolean = idx >= 0 && (try { pad.getButton(idx) } catch (_: Throwable) { false })

        if (suppress) { // a modal frame: keep the edge trackers in sync, emit nothing
            prevX = button(m.buttonX); prevY = button(m.buttonY); prevB = button(m.buttonB)
            prevAiming = false
            return
        }

        // Left stick → move (additive: set-true only; magnitude only when pushed → stick dash near rim).
        val lx = axis(m.axisLeftX); val ly = axis(m.axisLeftY)
        val move = GamepadMap.stick(lx, ly, GamepadMap.MOVE_DEAD)
        if (move.active) {
            if (lx < -GamepadMap.MOVE_DEAD) state.left = true
            if (lx > GamepadMap.MOVE_DEAD) state.right = true
            if (ly < -GamepadMap.MOVE_DEAD) state.up = true // stick up == move up the screen
            if (ly > GamepadMap.MOVE_DEAD) state.down = true
            state.moveMag = move.mag
        }
        if (button(m.buttonA)) state.dash = true // held
        if (button(m.buttonR1)) state.fullThrottle = true // held

        // Right stick → aim + auto-fire; the release edge fires manual-fire weapons along the held aim.
        val aim = GamepadMap.stick(axis(m.axisRightX), axis(m.axisRightY), GamepadMap.AIM_DEAD)
        val aimingNow = aim.active
        if (aimingNow) {
            state.aiming = true
            state.aimX = aim.x; state.aimY = aim.y // gamepad y is +down; the world is y-down → no flip
            lastAimX = aim.x; lastAimY = aim.y
            state.fire = true
        }
        if (prevAiming && !aimingNow) { // released → the beam/grenade shoots where you last pointed
            state.fireRelease = true
            state.aiming = true; state.aimX = lastAimX; state.aimY = lastAimY
        }
        prevAiming = aimingNow

        // Edge buttons — set-true-only, per the InputState buffered-edge contract.
        val bx = button(m.buttonX); if (bx && !prevX) state.melee = true; prevX = bx
        val by = button(m.buttonY); if (by && !prevY) state.reload = true; prevY = by
        val bb = button(m.buttonB); if (bb && !prevB) state.land = true; prevB = bb
    }
}
