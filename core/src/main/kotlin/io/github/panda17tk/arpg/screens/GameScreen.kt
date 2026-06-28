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
import io.github.panda17tk.arpg.config.ConfigStore
import io.github.panda17tk.arpg.core.Constants
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.world.GameWorld
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.input.KeyboardInput
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.pow

/**
 * Fixed-timestep simulation + render interpolation, porting the legacy main.js loop.
 * World is y-down, so the world camera is set up y-down to match.
 */
class GameScreen : ScreenAdapter() {
    private val input = InputState()
    private val configStore = ConfigStore()
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
        configStore.loadFromDisk()
        gw = WorldFactory.create(input, configStore.config)
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
        val px: Float; val py: Float; val fx: Float; val fy: Float; val sta: Float; val staMax: Float
        with(gw.world) {
            val t = gw.player[Transform]; val f = gw.player[Facing]; val s = gw.player[Stamina]
            px = t.prevX + (t.x - t.prevX) * alpha
            py = t.prevY + (t.y - t.prevY) * alpha
            fx = f.x; fy = f.y; sta = s.value; staMax = s.max
        }

        updateCamera(delta, px, py, fx, fy)

        ScreenUtils.clear(0.06f, 0.07f, 0.10f, 1f)

        // world
        worldViewport.apply()
        shapes.projectionMatrix = camera.combined

        // tiles (filled)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val m = gw.map
        for (ty in 0 until m.height) for (tx in 0 until m.width) {
            val t = m.tileAt(tx, ty)
            if (t == Tile.FLOOR) continue
            shapes.color = if (t == Tile.DOOR) Color(0.30f, 0.22f, 0.12f, 1f)
                           else Color(0.22f, 0.24f, 0.32f, 1f)
            shapes.rect(tx * Tuning.TILE, ty * Tuning.TILE, Tuning.TILE, Tuning.TILE)
        }
        // mobs
        with(gw.world) {
            gw.world.family { all(Mob, Transform, Health) }.forEach { e ->
                val mt = e[Transform]; val mm = e[Mob]; val mh = e[Health]
                shapes.color = if (mh.hitFlash > 0f) Color.WHITE else Color.valueOf(mm.def.color)
                shapes.circle(mt.x, mt.y, mm.def.w / 2f, 16)
            }
        }
        // player
        shapes.color = Color(0.45f, 0.85f, 0.95f, 1f)
        shapes.circle(px, py, Tuning.PLAYER_RADIUS, 24)
        // bullets (yellow) + grenades (red)
        with(gw.world) {
            gw.world.family { all(Bullet, Transform) }.forEach { e ->
                val bt = e[Transform]; shapes.color = Color(1f, 0.95f, 0.5f, 1f); shapes.circle(bt.x, bt.y, 3f, 8)
            }
            gw.world.family { all(Grenade, Transform) }.forEach { e ->
                val gt = e[Transform]; shapes.color = Color(1f, 0.5f, 0.4f, 1f); shapes.circle(gt.x, gt.y, 5f, 10)
            }
        }
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color.WHITE
        shapes.line(px, py, px + fx * Tuning.PLAYER_RADIUS * 1.8f, py + fy * Tuning.PLAYER_RADIUS * 1.8f)
        shapes.end()

        // HUD (screen space)
        hudViewport.apply()
        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        val blocks = with(gw.world) { gw.player[Materials].blocks }
        val arsenal = with(gw.world) { gw.player[Arsenal] }
        val ammo = with(gw.world) { gw.player[Ammo] }
        val hp = with(gw.world) { gw.player[Health].hp }
        val w = arsenal.current
        val magStr = w.def.magSize?.let { "${w.mag}/$it" } ?: "∞"
        val reserve = ammo.get(w.def.ammoType)
        font.draw(batch, "${w.def.name} $magStr (res $reserve)  STA ${sta.toInt()}  blk $blocks  HP ${hp.toInt()}  [WASD/J/K/R/1-5/F]", 16f, 28f)
        batch.end()
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.2f, 0.2f, 0.25f, 1f)
        shapes.rect(16f, 40f, 200f, 10f)
        shapes.color = Color(0.95f, 0.8f, 0.3f, 1f)
        shapes.rect(16f, 40f, 200f * (sta / staMax), 10f)
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
