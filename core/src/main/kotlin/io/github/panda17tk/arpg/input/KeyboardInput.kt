package io.github.panda17tk.arpg.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys

/** Polls the libGDX keyboard into [InputState]. F is edge-triggered (placeWall fires once per press). */
object KeyboardInput {
    private var prevF = false
    private var prevJ = false
    private var prevR = false
    private var prevL = false
    private var prevK = false
    private var prevI = false

    fun poll(state: InputState) {
        val k = Gdx.input
        state.left = k.isKeyPressed(Keys.A) || k.isKeyPressed(Keys.LEFT)
        state.right = k.isKeyPressed(Keys.D) || k.isKeyPressed(Keys.RIGHT)
        state.up = k.isKeyPressed(Keys.W) || k.isKeyPressed(Keys.UP)
        state.down = k.isKeyPressed(Keys.S) || k.isKeyPressed(Keys.DOWN)
        state.moveMag = 0f // keyboard is digital: never trips the analog stick dash (use Shift to dash)
        state.dash = k.isKeyPressed(Keys.SHIFT_LEFT) || k.isKeyPressed(Keys.SHIFT_RIGHT)
        val f = k.isKeyPressed(Keys.F)
        state.placeWall = f && !prevF
        prevF = f
        val kFire = k.isKeyPressed(Keys.K)
        state.fire = kFire
        state.fireRelease = !kFire && prevK; prevK = kFire // release edge → manual-fire weapons shoot
        val j = k.isKeyPressed(Keys.J); state.melee = j && !prevJ; prevJ = j
        val r = k.isKeyPressed(Keys.R); state.reload = r && !prevR; prevR = r
        val lKey = k.isKeyPressed(Keys.L); state.land = lKey && !prevL; prevL = lKey // land / take off
        val iKey = k.isKeyPressed(Keys.I); state.inventory = iKey && !prevI; prevI = iKey // inventory (v2.33)
        state.fullThrottle = k.isKeyPressed(Keys.O) // OC thruster full throttle, held (v2.33)
        state.weaponSlot = when {
            k.isKeyPressed(Keys.NUM_1) -> 0
            k.isKeyPressed(Keys.NUM_2) -> 1
            k.isKeyPressed(Keys.NUM_3) -> 2
            k.isKeyPressed(Keys.NUM_4) -> 3
            k.isKeyPressed(Keys.NUM_5) -> 4
            else -> -1
        }
    }
}
