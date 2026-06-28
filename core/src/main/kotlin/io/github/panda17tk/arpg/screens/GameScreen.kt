package io.github.panda17tk.arpg.screens

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import io.github.panda17tk.arpg.core.Constants
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.world.GameWorld
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.input.KeyboardInput
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.pow

/**
 * Fixed-timestep simulation + render interpolation, porting the legacy main.js loop.
 * World is y-down, so the world camera is set up y-down to match.
 */
class GameScreen : ScreenAdapter() {
    private val input = InputState()
    // Built in show() (not the constructor) so any future libGDX resource access
    // inside the ECS world happens after Gdx.app is available on Android.
    private lateinit var gw: GameWorld

    private lateinit var shapes: ShapeRenderer
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var camera: OrthographicCamera
    private lateinit var worldViewport: FitViewport
    private lateinit var hudViewport: ScreenViewport

    private var accumulator = 0f
    private var camX = Tuning.VIEW_W / 2f
    private var camY = Tuning.VIEW_H / 2f
    private var camInit = false

    override fun show() {
        gw = WorldFactory.create(input)
        shapes = ShapeRenderer()
        batch = SpriteBatch()
        font = BitmapFont()
        camera = OrthographicCamera().apply { setToOrtho(true, Tuning.VIEW_W, Tuning.VIEW_H) } // y-down
        worldViewport = FitViewport(Tuning.VIEW_W, Tuning.VIEW_H, camera)
        hudViewport = ScreenViewport()
    }

    override fun render(delta: Float) {
        KeyboardInput.poll(input)
        step(delta)
        val alpha = (accumulator / Constants.FIXED_DT).coerceIn(0f, 1f)

        // interpolated player position
        val px: Float; val py: Float; val fx: Float; val fy: Float; val sta: Float
        with(gw.world) {
            val t = gw.player[Transform]; val f = gw.player[Facing]; val s = gw.player[Stamina]
            px = t.prevX + (t.x - t.prevX) * alpha
            py = t.prevY + (t.y - t.prevY) * alpha
            fx = f.x; fy = f.y; sta = s.value
        }

        updateCamera(delta, px, py, fx, fy)

        ScreenUtils.clear(0.06f, 0.07f, 0.10f, 1f)

        // world
        worldViewport.apply()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color(0.16f, 0.18f, 0.24f, 1f)
        drawGrid()
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.45f, 0.85f, 0.95f, 1f)
        shapes.circle(px, py, Tuning.PLAYER_RADIUS, 24)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color.WHITE
        shapes.line(px, py, px + fx * Tuning.PLAYER_RADIUS * 1.8f, py + fy * Tuning.PLAYER_RADIUS * 1.8f)
        shapes.end()

        // HUD (screen space)
        hudViewport.apply()
        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        font.draw(batch, "WASD/Arrows: move   Shift: dash   STA ${sta.toInt()}/${Tuning.STA_MAX.toInt()}", 16f, 28f)
        batch.end()
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.2f, 0.2f, 0.25f, 1f)
        shapes.rect(16f, 40f, 200f, 10f)
        shapes.color = Color(0.95f, 0.8f, 0.3f, 1f)
        shapes.rect(16f, 40f, 200f * (sta / Tuning.STA_MAX), 10f)
        shapes.end()
    }

    private fun step(delta: Float) {
        val dt = minOf(Constants.MAX_DT, delta)
        accumulator += dt
        var steps = 0
        while (accumulator >= Constants.FIXED_DT && steps < Constants.MAX_STEPS) {
            gw.world.update(Constants.FIXED_DT)
            accumulator -= Constants.FIXED_DT
            steps++
        }
        if (steps >= Constants.MAX_STEPS) accumulator = 0f
    }

    private fun updateCamera(delta: Float, px: Float, py: Float, fx: Float, fy: Float) {
        val tgX = px + fx * Tuning.CAM_LOOK_AHEAD
        val tgY = py + fy * Tuning.CAM_LOOK_AHEAD
        if (!camInit) { camX = tgX; camY = tgY; camInit = true }
        val k = 1f - 0.0001f.pow(delta) // legacy smoothing
        camX += (tgX - camX) * k
        camY += (tgY - camY) * k
        camera.position.set(camX, camY, 0f)
        camera.update()
    }

    private fun drawGrid() {
        val t = Tuning.TILE
        var gx = camX - Tuning.VIEW_W
        val endX = camX + Tuning.VIEW_W
        gx = (gx / t).toInt() * t
        while (gx < endX) { shapes.line(gx, camY - Tuning.VIEW_H, gx, camY + Tuning.VIEW_H); gx += t }
        var gy = ((camY - Tuning.VIEW_H) / t).toInt() * t
        val endY = camY + Tuning.VIEW_H
        while (gy < endY) { shapes.line(camX - Tuning.VIEW_W, gy, camX + Tuning.VIEW_W, gy); gy += t }
    }

    override fun resize(width: Int, height: Int) {
        worldViewport.update(width, height)
        hudViewport.update(width, height, true)
    }

    override fun dispose() {
        shapes.dispose()
        batch.dispose()
        font.dispose()
    }
}
