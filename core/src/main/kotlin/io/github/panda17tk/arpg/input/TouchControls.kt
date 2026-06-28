package io.github.panda17tk.arpg.input

import com.badlogic.gdx.Gdx

/**
 * Maps multi-touch into [InputState] and tracks the virtual stick knob for rendering (Phase 8).
 * OR-merges into InputState after the keyboard poll, so a held finger never clears a key. Edge
 * buttons (melee/reload/wall/weapon) keep their own previous-frame state to fire once per tap.
 */
class TouchControls {
    val layout = TouchLayout()
    var stickActive = false; private set
    var baseX = 0f; private set
    var baseY = 0f; private set
    var knobX = 0f; private set
    var knobY = 0f; private set

    private var stickPointer = -1
    private var prevMelee = false
    private var prevReload = false
    private var prevWall = false
    private var prevWeapon = false
    private var weaponIdx = 0

    fun poll(input: InputState, screenW: Float, screenH: Float) {
        layout.screenW = screenW; layout.screenH = screenH
        var stick = false
        var fire = false; var dash = false
        var melee = false; var reload = false; var wall = false; var weapon = false

        for (i in 0 until MAX_POINTERS) {
            if (!Gdx.input.isTouched(i)) continue
            val x = Gdx.input.getX(i).toFloat()
            val y = screenH - Gdx.input.getY(i).toFloat() // libGDX getY is top-down; HUD is y-up
            if (layout.isInStickZone(x, y)) {
                if (stickPointer == -1) { stickPointer = i; baseX = x; baseY = y }
                if (stickPointer == i) { knobX = x; knobY = y; stick = true }
            } else when (layout.button(x, y)) {
                TouchButton.FIRE -> fire = true
                TouchButton.DASH -> dash = true
                TouchButton.MELEE -> melee = true
                TouchButton.RELOAD -> reload = true
                TouchButton.WALL -> wall = true
                TouchButton.WEAPON -> weapon = true
                null -> {}
            }
        }
        if (!stick) stickPointer = -1
        stickActive = stick

        if (stick) {
            val dead = layout.stickRadius * 0.30f
            val dx = knobX - baseX; val dy = knobY - baseY
            if (dx < -dead) input.left = true
            if (dx > dead) input.right = true
            if (dy > dead) input.up = true   // finger pushed up the screen == keyboard W
            if (dy < -dead) input.down = true
        }
        if (fire) input.fire = true
        if (dash) input.dash = true
        if (melee && !prevMelee) input.melee = true
        if (reload && !prevReload) input.reload = true
        if (wall && !prevWall) input.placeWall = true
        if (weapon && !prevWeapon) { input.weaponSlot = weaponIdx; weaponIdx = (weaponIdx + 1) % WEAPON_SLOTS }
        prevMelee = melee; prevReload = reload; prevWall = wall; prevWeapon = weapon
    }

    companion object {
        private const val MAX_POINTERS = 10
        private const val WEAPON_SLOTS = 5
    }
}
