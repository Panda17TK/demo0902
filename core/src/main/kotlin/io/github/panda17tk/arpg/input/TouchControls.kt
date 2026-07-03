package io.github.panda17tk.arpg.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.Viewport
import kotlin.math.hypot

/**
 * Twin-stick touch (left hand moves, right hand aims+fires):
 *  - Left side  → move stick (8-way into InputState).
 *  - Right side → aim+fire stick: push the right thumb toward where you want to shoot; while held
 *    past the dead-zone the player faces that way and auto-fires.
 *  - A small top-right cluster keeps the secondary actions (dash/melee/reload/wall/weapon).
 * Pointers are locked to their role on touch-down, so dragging one stick across the screen never
 * leaks into the other zone. Coordinates come unprojected through the HUD viewport (dp space).
 */
class TouchControls {
    val layout = TouchLayout()

    var stickActive = false; private set
    var baseX = 0f; private set
    var baseY = 0f; private set
    var knobX = 0f; private set
    var knobY = 0f; private set

    var aimActive = false; private set
    var aimBaseX = 0f; private set
    var aimBaseY = 0f; private set
    var aimKnobX = 0f; private set
    var aimKnobY = 0f; private set

    private var stickPointer = -1
    private var aimPointer = -1
    private var prevMelee = false
    private var prevReload = false
    private var prevWall = false
    private var prevWeapon = false
    private var prevLand = false
    private var prevInv = false
    private var prevAiming = false // for manual-fire weapons: detect the frame the aim stick is released
    private var lastAimX = 1f
    private var lastAimY = 0f
    private var weaponIdx = 0
    private val tmpVec = Vector3()

    // P3: contextual visibility + press highlight (exposed for drawing) + haptic edge tracker.
    var visibleButtons: Set<TouchButton> = emptySet(); private set
    var pressedButtons: Set<TouchButton> = emptySet(); private set
    private var prevPressed: Set<TouchButton> = emptySet()

    fun poll(input: InputState, viewport: Viewport, blocks: Int, mag: Int, magSize: Int?, canLand: Boolean, hasOverclock: Boolean = false) {
        layout.screenW = viewport.worldWidth; layout.screenH = viewport.worldHeight
        val vis = TouchButtons.visible(blocks, mag, magSize, canLand, hasOverclock)
        visibleButtons = vis
        input.aiming = false
        input.moveMag = 0f
        var stick = false; var aim = false
        var dash = false; var melee = false; var reload = false; var wall = false; var weapon = false; var land = false
        var inv = false; var full = false

        for (i in 0 until MAX_POINTERS) {
            if (!Gdx.input.isTouched(i)) continue
            tmpVec.set(Gdx.input.getX(i).toFloat(), Gdx.input.getY(i).toFloat(), 0f)
            viewport.unproject(tmpVec)
            val x = tmpVec.x; val y = tmpVec.y
            when {
                i == stickPointer -> { knobX = x; knobY = y; stick = true }
                i == aimPointer -> { aimKnobX = x; aimKnobY = y; aim = true }
                layout.isInStickZone(x, y) && stickPointer == -1 -> {
                    stickPointer = i; baseX = x; baseY = y; knobX = x; knobY = y; stick = true
                }
                // Hidden (context-gated) buttons fall through to the aim stick.
                else -> when (layout.button(x, y)?.takeIf { it in vis }) {
                    TouchButton.DASH -> dash = true
                    TouchButton.MELEE -> melee = true
                    TouchButton.RELOAD -> reload = true
                    TouchButton.WALL -> wall = true
                    TouchButton.WEAPON -> weapon = true
                    TouchButton.LAND -> land = true
                    TouchButton.INV -> inv = true
                    TouchButton.FULL -> full = true
                    // Never let a tap on the LAND button's spot get stolen by the aim stick (which would lock the
                    // pointer to fire and make landing impossible on touch). Ignore it on the frames it's not landable.
                    else -> if (aimPointer == -1 && !layout.isInStickZone(x, y) && layout.button(x, y) != TouchButton.LAND) {
                        aimPointer = i; aimBaseX = x; aimBaseY = y; aimKnobX = x; aimKnobY = y; aim = true
                    }
                }
            }
        }
        if (!stick) stickPointer = -1
        if (!aim) aimPointer = -1
        stickActive = stick; aimActive = aim

        if (stick) {
            val dead = layout.stickRadius * MOVE_DEAD
            val dx = knobX - baseX; val dy = knobY - baseY
            if (dx < -dead) input.left = true
            if (dx > dead) input.right = true
            if (dy > dead) input.up = true   // finger pushed up the screen == keyboard W
            if (dy < -dead) input.down = true
            // Analog deflection (0..1): pushing the stick to its rim trips a stick dash.
            input.moveMag = (hypot(dx, dy) / layout.stickRadius).coerceIn(0f, 1f)
        }
        if (aim) {
            val dx = aimKnobX - aimBaseX; val dy = aimKnobY - aimBaseY
            val len = hypot(dx, dy)
            if (len > layout.stickRadius * AIM_DEAD) {
                input.aiming = true
                input.aimX = dx / len; input.aimY = -dy / len // touch is y-up, world is y-down → flip Y
                lastAimX = input.aimX; lastAimY = input.aimY
                input.fire = true
            }
        }
        // Manual-fire weapons (beam/grenade) shoot when you RELEASE the aim stick: emit a one-frame release edge
        // and hold the last aim so Facing (and thus the shot) stays pointed where you were aiming.
        val aimingNow = input.aiming
        if (prevAiming && !aimingNow) {
            input.fireRelease = true
            input.aiming = true
            input.aimX = lastAimX; input.aimY = lastAimY
        }
        prevAiming = aimingNow
        if (dash) input.dash = true
        if (melee && !prevMelee) input.melee = true
        if (reload && !prevReload) input.reload = true
        if (wall && !prevWall) input.placeWall = true
        if (weapon && !prevWeapon) { input.weaponSlot = weaponIdx; weaponIdx = (weaponIdx + 1) % WEAPON_SLOTS }
        if (land && !prevLand) input.land = true // edge: fire landing once per tap
        if (inv && !prevInv) input.inventory = true // edge: open/close the inventory once per tap (v2.33)
        if (full) input.fullThrottle = true // held: OC full throttle (v2.33)
        prevMelee = melee; prevReload = reload; prevWall = wall; prevWeapon = weapon; prevLand = land; prevInv = inv

        // P3: pressed set drives the highlight; a fresh press edge fires a short haptic tick.
        val pressed = buildSet {
            if (dash) add(TouchButton.DASH)
            if (melee) add(TouchButton.MELEE)
            if (reload) add(TouchButton.RELOAD)
            if (wall) add(TouchButton.WALL)
            if (weapon) add(TouchButton.WEAPON)
            if (land) add(TouchButton.LAND) // feedback parity: LAND also buzzes + highlights on press
            if (inv) add(TouchButton.INV)
            if (full) add(TouchButton.FULL)
        }
        if ((pressed - prevPressed).isNotEmpty()) Haptics.buzz(18)
        prevPressed = pressed
        pressedButtons = pressed
    }

    companion object {
        private const val MAX_POINTERS = 10
        private const val WEAPON_SLOTS = 5
        private const val MOVE_DEAD = 0.26f // P3: slightly tighter dead-zones for snappier response
        private const val AIM_DEAD = 0.18f
    }
}
