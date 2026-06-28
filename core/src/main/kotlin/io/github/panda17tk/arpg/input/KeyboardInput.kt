package io.github.panda17tk.arpg.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys

/** Polls the libGDX keyboard into [InputState]. F is edge-triggered (placeWall fires once per press). */
object KeyboardInput {
    private var prevF = false
    private var prevJ = false
    private var prevR = false

    fun poll(state: InputState) {
        val k = Gdx.input
        state.left = k.isKeyPressed(Keys.A) || k.isKeyPressed(Keys.LEFT)
        state.right = k.isKeyPressed(Keys.D) || k.isKeyPressed(Keys.RIGHT)
        state.up = k.isKeyPressed(Keys.W) || k.isKeyPressed(Keys.UP)
        state.down = k.isKeyPressed(Keys.S) || k.isKeyPressed(Keys.DOWN)
        state.dash = k.isKeyPressed(Keys.SHIFT_LEFT) || k.isKeyPressed(Keys.SHIFT_RIGHT)
        val f = k.isKeyPressed(Keys.F)
        state.placeWall = f && !prevF
        prevF = f
        state.fire = k.isKeyPressed(Keys.K)
        val j = k.isKeyPressed(Keys.J); state.melee = j && !prevJ; prevJ = j
        val r = k.isKeyPressed(Keys.R); state.reload = r && !prevR; prevR = r
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
