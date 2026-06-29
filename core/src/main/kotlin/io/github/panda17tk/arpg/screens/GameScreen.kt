package io.github.panda17tk.arpg.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import io.github.panda17tk.arpg.audio.Sfx
import io.github.panda17tk.arpg.config.ConfigStore
import io.github.panda17tk.arpg.core.Constants
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.EBullet
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Smoke
import io.github.panda17tk.arpg.ecs.components.Speech
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.ecs.world.GameWorld
import io.github.panda17tk.arpg.ecs.world.PlayerCarry
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.Haptics
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.input.KeyboardInput
import io.github.panda17tk.arpg.input.TouchButton
import io.github.panda17tk.arpg.input.TouchControls
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.render.Actors
import io.github.panda17tk.arpg.render.Draw
import io.github.panda17tk.arpg.render.Fonts
import io.github.panda17tk.arpg.render.Hud
import io.github.panda17tk.arpg.render.WorldView
import io.github.panda17tk.arpg.save.Scores
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.ui.Modals
import io.github.panda17tk.arpg.ui.Overlay
import io.github.panda17tk.arpg.ui.PauseAction
import io.github.panda17tk.arpg.ui.PauseFlow
import io.github.panda17tk.arpg.upgrade.Upgrade
import io.github.panda17tk.arpg.upgrade.Upgrades
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

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
    private lateinit var worldViewport: ExtendViewport
    private lateinit var hudViewport: ScreenViewport

    private var accumulator = 0f
    private var camX = Tuning.VIEW_W / 2f
    private var camY = Tuning.VIEW_H / 2f
    private var camInit = false

    // Phase 6b: between-wave upgrade selection (modal — sim freezes until a card is picked).
    private val upgradeRng = Rng(System.nanoTime())
    private var choosing = false
    private var offered = false
    private var choices: List<Upgrade> = emptyList()

    // Phase 7: screen-shake trigger — compare HP frame-to-frame to detect the player taking damage.
    private var lastHp = Float.NaN
    private var lastKills = 0
    private var prevOver = false
    private var newBest = false

    // Phase 8: on-screen controls (Android only) + audio service.
    private val touch = TouchControls()
    private var touchEnabled = false

    // P1: blocking overlay (pause / help) + the per-frame HUD tap, unprojected into dp space.
    private var overlay = Overlay.NONE
    private var tapped = false
    private var tapX = 0f
    private var tapY = 0f
    private val tmpTap = Vector3()

    // Static help text shown on the pause → 操作説明 overlay (keyboard + touch bindings).
    private val HELP_LINES = listOf(
        "移動：左スティック / WASD",
        "エイム＆射撃：右スティック / 矢印 + K",
        "ダッシュ：ボタン / Shift",
        "近接：ボタン / J",
        "リロード：ボタン / R",
        "武器切替・壁設置：ボタン / 数字",
        "ポーズ：右上ボタン / Esc・P",
    )

    // Visual port: animation clock + run timer + cached projectile/UI colors (avoid per-frame alloc).
    private var animTime = 0f
    private var runTime = 0f
    private val cTrail = Color(0.63f, 0.82f, 1f, 0.5f)
    private val cBulletCore = Color.valueOf("eaf4ff")
    private val cEbGlow = Color(1f, 0.55f, 0.55f, 0.5f)
    private val cEbCore = Color.valueOf("ffd0d0")
    private val cEbMine = Color.valueOf("ff6b6b")
    private val cGren = Color.valueOf("5b6b3a")
    private val cFuseOn = Color.valueOf("ff5a3a")
    private val cFuseOff = Color.valueOf("7a2a1a")
    private val cTelegraph = Color(1f, 0.3f, 0.2f, 0.9f)
    private val glyphLayout = GlyphLayout()
    private var uiScale = 1f
    private val tmpC = Color()
    private val cBlink = Color(0.72f, 0.52f, 1f, 0.9f)
    private val cAmmo9 = Color.valueOf("ffe066")
    private val cAmmo12 = Color.valueOf("ff9a4d")
    private val cAmmoBeam = Color.valueOf("66e0ff")
    private val cAmmoNade = Color.valueOf("ff6b6b")
    private val cBlocks = Color.valueOf("b48a5a")
    private val cMed = Color.valueOf("7fe08a")
    private val tribeColors = arrayOf(
        Color.valueOf("ff6b6b"), Color.valueOf("66e0ff"), Color.valueOf("7fe08a"), Color.valueOf("ffd166"), Color.valueOf("c08bff"),
    )

    override fun show() {
        configStore.loadFromDisk()
        uiScale = Gdx.graphics.density.coerceIn(1f, 4f)
        shapes = ShapeRenderer()
        batch = SpriteBatch()
        Fonts.load(uiScale)
        font = Fonts.ui
        camera = OrthographicCamera().apply { setToOrtho(true, Tuning.VIEW_W, Tuning.VIEW_H) } // y-down
        worldViewport = ExtendViewport(384f, 384f, camera) // square min (~12 tiles), extends to fill; closer than 480
        hudViewport = ScreenViewport()
        hudViewport.setUnitsPerPixel(1f / uiScale)
        Sfx.init()
        Scores.load()
        touchEnabled = Gdx.app.type == Application.ApplicationType.Android
        newRun()
    }

    /** Build (or rebuild) the run and reset per-run screen state (Phase 7 restart). */
    private fun newRun() {
        gw = WorldFactory.create(input, configStore.config)
        accumulator = 0f
        camInit = false
        choosing = false
        offered = false
        choices = emptyList()
        overlay = Overlay.NONE
        lastHp = Float.NaN
        lastKills = 0
        prevOver = false
        newBest = false
    }

    private var landSeed = 100L

    /** Land on the hovered planet (SPACE) or take off back to space (SURFACE), carrying run state across the rebuild. */
    private fun handleLanding() {
        val ws = gw.worldState
        if (ws.mode == WorldMode.SPACE) {
            val cand = ws.landingCandidate ?: return
            transitionWorld(WorldMode.SURFACE, cand.biome)
        } else {
            transitionWorld(WorldMode.SPACE, null)
        }
    }

    private fun transitionWorld(mode: WorldMode, biome: PlanetBiome?) {
        val carry = PlayerCarry.of(gw.world, gw.player, gw.waveState.num)
        landSeed += 1
        gw = WorldFactory.create(input, configStore.config, landSeed, mode, biome, carry)
        gw.waveState.num = carry.wave
        accumulator = 0f; camInit = false; overlay = Overlay.NONE
        choosing = false; offered = false; choices = emptyList(); lastHp = Float.NaN
    }

    override fun render(delta: Float) {
        KeyboardInput.poll(input)
        pollTap()
        if ((Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) &&
            !choosing && !gw.gameOver.isOver) overlay = PauseFlow.toggle(overlay)
        handlePauseTaps()
        val paused = overlay != Overlay.NONE
        // Living Planets: land on / leave a planet with L (rebuilds the world for the surface stage).
        if (!paused && !choosing && !gw.gameOver.isOver && Gdx.input.isKeyJustPressed(Input.Keys.L)) handleLanding()

        // Gameplay touch (twin-stick / fire) runs only when no modal blocks it (spec §5.2).
        // Pass the player context so the action buttons can show/hide by relevance (P3).
        if (touchEnabled && !paused && !choosing && !gw.gameOver.isOver) {
            val tw = with(gw.world) { gw.player[Arsenal] }.current
            val tBlocks = with(gw.world) { gw.player[Materials].blocks }
            touch.poll(input, hudViewport, tBlocks, tw.mag, tw.def.magSize)
        }

        if (gw.gameOver.isOver) {
            accumulator = 0f
            if (!prevOver) {
                newBest = Scores.record(gw.waveState.num, gw.gameOver.kills)
                Sfx.play("dead"); Haptics.buzz(140)
            }
            val restartTapped = tapped &&
                Modals.hitModal(Modals.gameOverButtons(hudViewport.worldWidth, hudViewport.worldHeight), tapX, tapY) != null
            if (Gdx.input.isKeyJustPressed(Input.Keys.R) || restartTapped) { newRun(); return }
        } else if (paused) {
            accumulator = 0f // freeze the sim while paused; skip stepping & the upgrade flow
        } else {
            updateUpgradeFlow(delta)
            trackPlayerHitShake()
        }
        prevOver = gw.gameOver.isOver
        gw.fx.update(delta)
        animTime += delta
        if (!gw.gameOver.isOver && !choosing && !paused) runTime += delta
        val alpha = (accumulator / Constants.FIXED_DT).coerceIn(0f, 1f)

        // interpolated player position + state
        val px: Float; val py: Float; val fx: Float; val fy: Float; val sta: Float; val staMax: Float; val pit: Float; val overheat: Boolean
        with(gw.world) {
            val t = gw.player[Transform]; val f = gw.player[Facing]; val s = gw.player[Stamina]
            px = t.prevX + (t.x - t.prevX) * alpha
            py = t.prevY + (t.y - t.prevY) * alpha
            fx = f.x; fy = f.y; sta = s.value; staMax = s.max; pit = gw.player[Health].iTime; overheat = s.overheat
        }
        val playerHit = pit > 0f && ((pit * 20f).toInt() % 2 == 0)
        val dashing = input.dash && sta > 0f
        val muzzle = input.fire

        updateCamera(delta, px, py, fx, fy)
        if (gw.fx.shakeMag > 0f) { camera.position.add(gw.fx.shakeX(), gw.fx.shakeY(), 0f); camera.update() }

        ScreenUtils.clear(0.06f, 0.07f, 0.10f, 1f)

        // world
        worldViewport.apply()
        shapes.projectionMatrix = camera.combined

        // world (procedural sprites — ported from legacy renderer.js + enemy-sprites.js)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        // cull tiles to the visible camera region (big maps)
        val vt = Tuning.TILE
        val vhw = camera.viewportWidth / 2f + vt; val vhh = camera.viewportHeight / 2f + vt
        val minTx = maxOf(0, ((camera.position.x - vhw) / vt).toInt())
        val maxTx = minOf(gw.map.width - 1, ((camera.position.x + vhw) / vt).toInt())
        val minTy = maxOf(0, ((camera.position.y - vhh) / vt).toInt())
        val maxTy = minOf(gw.map.height - 1, ((camera.position.y + vhh) / vt).toInt())
        WorldView.draw(shapes, gw.map, minTx, maxTx, minTy, maxTy)
        with(gw.world) {
            gw.world.family { all(Mob, Transform, Health, Facing, Velocity, MobAction) }.forEach { e ->
                val mt = e[Transform]; val mm = e[Mob]; val mh = e[Health]; val mf = e[Facing]; val mv = e[Velocity]; val ma = e[MobAction]
                val moving = (mt.x - mt.prevX) * (mt.x - mt.prevX) + (mt.y - mt.prevY) * (mt.y - mt.prevY) > 0.0025f || (mv.vx * mv.vx + mv.vy * mv.vy) > 4f
                val chargeProg = if (ma.chargeT > 0f) {
                    val windup = mm.def.attacks.firstOrNull { it.type == "charge_melee" }?.windup ?: 0.7f
                    (1f - ma.chargeT.coerceAtLeast(0f) / windup).coerceIn(0f, 1f)
                } else -1f
                val hpFrac = if (mh.hpMax > 0f) mh.hp / mh.hpMax else 1f
                Actors.drawMob(shapes, mm.kind, mm.tier, mt.x, mt.y, mm.def.w, mm.def.h, Color.valueOf(mm.def.color), mf.x, mf.y, moving, mh.hitFlash > 0f, ma.dodgeT > 0f, chargeProg, ma.enrageT > 0f, hpFrac, e.id * 1.3f, animTime)
            }
        }
        Actors.drawPlayer(shapes, px, py, fx, fy, dashing, playerHit, muzzle)
        with(gw.world) {
            gw.world.family { all(Bullet, Transform) }.forEach { e ->
                val bt = e[Transform]; val b = e[Bullet]
                val sp = hypot(b.vx, b.vy).let { if (it == 0f) 1f else it }
                shapes.color = cTrail; Draw.orientedRect(shapes, bt.x, bt.y, b.vx / sp, b.vy / sp, -8f, 8f, 1.5f)
                shapes.color = cBulletCore; shapes.circle(bt.x, bt.y, 2f, 8)
            }
            gw.world.family { all(Grenade, Transform) }.forEach { e ->
                val gt = e[Transform]; shapes.color = cGren; shapes.circle(gt.x, gt.y, 4f, 10)
                shapes.color = if (((animTime / 0.12f).toInt() % 2) == 0) cFuseOn else cFuseOff; shapes.circle(gt.x, gt.y - 1f, 1.6f, 6)
            }
            gw.world.family { all(EBullet, Transform) }.forEach { e ->
                val et = e[Transform]; val eb = e[EBullet]
                val r = if (eb.mine) 6f else 4f
                shapes.color = cEbGlow; shapes.circle(et.x, et.y, r, 10)
                shapes.color = if (eb.mine) cEbMine else cEbCore; shapes.circle(et.x, et.y, 2f, 6)
            }
        }
        // death-burst particles (gibs shrink as they expire)
        gw.fx.particles.forEach { p ->
            val k = 1f - p.t / p.life
            if (k > 0f) { shapes.color = p.color; shapes.circle(p.x, p.y, p.size * k, 8) }
        }
        gw.fx.afters.forEach { a ->
            val k = 1f - a.t / a.life
            if (k > 0f) { tmpC.set(a.color); tmpC.a = 0.3f * k; shapes.color = tmpC; Draw.roundedRect(shapes, a.x - a.w / 2f, a.y - a.h / 2f, a.w, a.h, 5f) }
        }
        gw.fx.beams.forEach { bm ->
            val dx = bm.ex - bm.sx; val dy = bm.ey - bm.sy
            val len = hypot(dx, dy).let { if (it == 0f) 1f else it }
            val ux = dx / len; val uy = dy / len
            val k = 1f - bm.t / bm.life
            shapes.color = tmpC.set(0.5f, 0.85f, 1f, 0.32f * k); Draw.orientedRect(shapes, bm.sx, bm.sy, ux, uy, 0f, len, 6f)
            shapes.color = tmpC.set(0.92f, 0.98f, 1f, 0.9f * k); Draw.orientedRect(shapes, bm.sx, bm.sy, ux, uy, 0f, len, 1.8f)
        }
        with(gw.world) {
            gw.world.family { all(Pickup, Transform) }.forEach { e ->
                val pt = e[Transform]; val pk = e[Pickup]
                val bob = sin(animTime * 4f + pt.x * 0.05f) * 2f
                shapes.color = pickupColor(pk.kind)
                shapes.circle(pt.x, pt.y + bob, 4f, 10)
            }
        }
        // smoke clouds — overlapping puffs of varying white/grey + opacity, fading over life
        with(gw.world) {
            gw.world.family { all(Smoke, Transform) }.forEach { e ->
                val st = e[Transform]; val sm = e[Smoke]
                val k = (1f - sm.t / sm.life).coerceIn(0f, 1f)
                for (i in 0 until 8) {
                    val ang = i * 2.39996f + sm.t * 0.5f
                    val rr = sm.radius * (0.2f + 0.55f * ((i * 0.137f) % 1f))
                    val grey = 0.6f + 0.4f * ((i * 0.31f) % 1f)
                    tmpC.set(grey, grey, grey, 0.16f * k)
                    shapes.color = tmpC
                    shapes.circle(st.x + cos(ang) * rr, st.y + sin(ang) * rr, sm.radius * 0.5f, 14)
                }
            }
        }
        // planets — large solid bodies tinted by their ecological biome
        for (p in gw.planets) {
            when (p.biome) {
                PlanetBiome.NATURE -> tmpC.set(0.28f, 0.52f, 0.30f, 1f)
                PlanetBiome.MAGMA -> tmpC.set(0.62f, 0.24f, 0.18f, 1f)
                PlanetBiome.ICE -> tmpC.set(0.82f, 0.88f, 0.95f, 1f)
                PlanetBiome.GAS -> tmpC.set(0.74f, 0.62f, 0.40f, 1f)
                PlanetBiome.DEAD -> tmpC.set(0.42f, 0.40f, 0.38f, 1f)
                PlanetBiome.LONELY -> tmpC.set(0.30f, 0.30f, 0.34f, 1f)
            }
            shapes.color = tmpC
            shapes.circle(p.cx, p.cy, p.radius, 44)
        }
        // tribe strongholds ("stars") — a tribe-coloured aura + pulsing core
        for (base in gw.bases) {
            val col = tribeColors[base.tribe % tribeColors.size]
            tmpC.set(col.r, col.g, col.b, 0.16f); shapes.color = tmpC
            shapes.circle(base.x, base.y, base.radius + 8f, 28)
            tmpC.set(col.r, col.g, col.b, 0.5f + 0.25f * sin(animTime * 2.2f)); shapes.color = tmpC
            shapes.circle(base.x, base.y, 7f, 12)
        }
        shapes.end()

        // charge-melee telegraph rings
        shapes.begin(ShapeRenderer.ShapeType.Line)
        // gravity wells: concentric procedural rings around each wall cluster's gravity centre
        for (c in gw.gravityField.clusters) {
            tmpC.set(0.55f, 0.52f, 0.66f, 0.45f); shapes.color = tmpC
            for (k in 1..3) shapes.circle(c.cx, c.cy, c.radius * k / 3f, 30)
            tmpC.set(0.45f, 0.5f, 0.72f, 0.16f); shapes.color = tmpC
            shapes.circle(c.cx, c.cy, c.radius + Tuning.TILE * 1.4f, 36)
        }
        // planet gravity wells — faint range rings around each planet
        for (p in gw.planets) {
            tmpC.set(0.5f, 0.55f, 0.72f, 0.18f); shapes.color = tmpC
            shapes.circle(p.cx, p.cy, p.gravityRange, 48)
            shapes.circle(p.cx, p.cy, p.radius + Tuning.TILE * 1.2f, 40)
        }
        with(gw.world) {
            gw.world.family { all(Mob, Transform, MobAction) }.forEach { e ->
                val mt = e[Transform]; val ma = e[MobAction]
                if (ma.charging) { shapes.color = cTelegraph; shapes.circle(mt.x, mt.y, 14f + ma.chargeT * 30f, 16) }
                if (ma.blinkChargeT > 0f) { shapes.color = cBlink; shapes.circle(mt.x, mt.y, 10f + ma.blinkChargeT * 50f, 14) }
            }
        }
        gw.fx.slashes.forEach { sl ->
            val k = 1f - sl.t / sl.life
            shapes.color = tmpC.set(1f, 1f, 1f, 0.75f * k)
            val a0 = sl.ang - 1.0f; val a1 = sl.ang + 1.0f; val r = 32f; val steps = 8
            for (i in 0 until steps) {
                val b0 = a0 + (a1 - a0) * i / steps; val b1 = a0 + (a1 - a0) * (i + 1) / steps
                shapes.line(sl.x + cos(b0) * r, sl.y + sin(b0) * r, sl.x + cos(b1) * r, sl.y + sin(b1) * r)
            }
        }
        shapes.end()

        // speech bubbles — short lines above creatures (world space; y-down → negative scaleY flips glyphs upright)
        batch.projectionMatrix = camera.combined
        batch.begin()
        val bubScaleX = font.data.scaleX; val bubScaleY = font.data.scaleY
        font.data.setScale(0.2f, -0.2f) // small world-unit text; tune on device
        with(gw.world) {
            gw.world.family { all(Mob, Transform, Speech) }.forEach { e ->
                val sp = e[Speech]
                if (sp.remaining > 0f && sp.text.isNotEmpty()) {
                    val mt = e[Transform]
                    glyphLayout.setText(font, sp.text)
                    font.draw(batch, glyphLayout, mt.x - glyphLayout.width / 2f, mt.y - 20f)
                }
            }
        }
        font.data.setScale(bubScaleX, bubScaleY)
        batch.end()

        // HUD (screen space) — P2 live HUD delegated to render/Hud (geometry from ui/HudLayout)
        hudViewport.apply()
        val hudW = hudViewport.worldWidth; val hudH = hudViewport.worldHeight
        val blocks = with(gw.world) { gw.player[Materials].blocks }
        val ammo = with(gw.world) { gw.player[Ammo] }
        val hp = with(gw.world) { gw.player[Health].hp }
        val hpMax = with(gw.world) { gw.player[Health].hpMax }
        val foes = gw.world.family { all(Mob) }.numEntities
        val wpn = with(gw.world) { gw.player[Arsenal] }.current
        val reloadFrac = if (wpn.reloadT > 0f && wpn.def.reloadTime > 0f) (wpn.reloadT / wpn.def.reloadTime).coerceIn(0f, 1f) else 0f
        val reserveStr = if (wpn.def.infiniteAmmo) "無限" else "${ammo.get(wpn.def.ammoType)}"
        Hud.liveHud(
            shapes, batch, font, Fonts.title, hudViewport,
            gw.waveState.num, foes,
            hp, hpMax, sta, staMax, overheat,
            wpn.def.name, wpn.mag, wpn.def.magSize, reloadFrac, reserveStr,
            runTime, gw.gameOver.kills, blocks,
        )
        // Living Planets: landing / takeoff prompt (HUD space).
        run {
            val ws = gw.worldState
            val hint = when {
                ws.mode == WorldMode.SURFACE -> "[L] 離陸して宇宙へ"
                ws.landingCandidate != null -> "[L] 着陸: ${ws.landingCandidate!!.biome.displayName}"
                else -> null
            }
            if (hint != null && !paused && !choosing) {
                batch.projectionMatrix = hudViewport.camera.combined
                batch.begin()
                glyphLayout.setText(font, hint)
                font.draw(batch, glyphLayout, (hudW - glyphLayout.width) / 2f, hudH - 12f)
                batch.end()
            }
        }

        if (touchEnabled && !paused && !choosing && !gw.gameOver.isOver) drawTouchControls()
        if (!paused && !choosing && !gw.gameOver.isOver) Hud.pauseButton(shapes, hudViewport, Modals.pauseButton(hudW, hudH))
        if (choosing) {
            val cfg = configStore.config.upgrades
            Hud.upgradeCards(
                shapes, batch, font, hudViewport, gw.waveState.num,
                Modals.upgradeCards(hudW, hudH, choices.size),
                choices.map { it.name }, choices.map { Upgrades.desc(it, cfg) },
            )
        }
        if (gw.gameOver.isOver) {
            val bestText = if (newBest) "自己ベスト更新！  ウェーブ ${Scores.bestWave}"
                else "ベスト  ウェーブ ${Scores.bestWave}  撃破 ${Scores.bestKills}"
            Hud.gameOver(
                shapes, batch, font, Fonts.title, hudViewport,
                gw.waveState.num, gw.gameOver.kills, bestText, Modals.gameOverButtons(hudW, hudH).first(),
            )
        }
        if (overlay == Overlay.PAUSE) Hud.pause(shapes, batch, font, Fonts.title, hudViewport, Modals.pauseButtons(hudW, hudH))
        if (overlay == Overlay.HELP) Hud.help(shapes, batch, font, Fonts.title, hudViewport, Modals.helpButtons(hudW, hudH).first(), HELP_LINES)
    }

    private fun trackPlayerHitShake() {
        val hp = with(gw.world) { gw.player[Health].hp }
        if (!lastHp.isNaN() && hp < lastHp - 0.01f) { gw.fx.addShake(0.18f, 6f); Sfx.play("hit"); Haptics.buzz(25) }
        lastHp = hp
        val kills = gw.gameOver.kills
        if (kills > lastKills) Sfx.play("kill")
        lastKills = kills
    }

    /**
     * Phase 6b: when a wave is cleared the sim enters "intermission"; we freeze it and offer
     * 3 random upgrade cards. Number keys 1/2/3 pick one, apply it permanently, then resume.
     */
    private fun updateUpgradeFlow(delta: Float) {
        if (choosing) {
            accumulator = 0f // keep the sim frozen while the player chooses
            val sel = when {
                Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) -> 0
                Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) -> 1
                Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) -> 2
                tapped -> Modals.hitModal(
                    Modals.upgradeCards(hudViewport.worldWidth, hudViewport.worldHeight, choices.size), tapX, tapY,
                ) ?: -1
                else -> -1
            }
            if (sel in choices.indices) { applyUpgrade(choices[sel]); choosing = false; offered = true }
        } else {
            step(delta)
            if (gw.waveState.phase == "intermission") {
                if (!offered) { choices = Upgrades.pick(3, upgradeRng); choosing = true }
            } else {
                offered = false
            }
        }
    }

    private fun applyUpgrade(u: Upgrade) {
        val cfg = configStore.config.upgrades
        with(gw.world) {
            Upgrades.apply(u.id, cfg, gw.player[Mods], gw.player[Health], gw.player[Ammo], gw.player[Materials])
        }
        Sfx.play("levelup")
    }

    /** Capture this frame's HUD tap (a desktop mouse click counts too), unprojected into dp space. */
    private fun pollTap() {
        tapped = Gdx.input.justTouched()
        if (tapped) {
            tmpTap.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            hudViewport.unproject(tmpTap)
            tapX = tmpTap.x; tapY = tmpTap.y
        }
    }

    /** Route a tap to the pause/help overlays, or open pause from the in-play ⏸ button (spec §5.2). */
    private fun handlePauseTaps() {
        if (!tapped) return
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        when (overlay) {
            Overlay.PAUSE -> when (PauseFlow.action(Modals.hitModal(Modals.pauseButtons(w, h), tapX, tapY) ?: -1)) {
                PauseAction.RESUME -> overlay = Overlay.NONE
                PauseAction.RESTART -> { newRun(); overlay = Overlay.NONE }
                PauseAction.HELP -> overlay = Overlay.HELP
                null -> {}
            }
            Overlay.HELP -> if (Modals.hitModal(Modals.helpButtons(w, h), tapX, tapY) != null) overlay = Overlay.PAUSE
            Overlay.NONE -> if (!choosing && !gw.gameOver.isOver &&
                Modals.hitModal(listOf(Modals.pauseButton(w, h)), tapX, tapY) != null) overlay = Overlay.PAUSE
        }
    }

    private fun drawTouchControls() {
        val l = touch.layout
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(1f, 1f, 1f, 0.06f); shapes.circle(l.stickCx, l.stickCy, l.stickRadius, 28)
        if (touch.stickActive) {
            shapes.color = Color(1f, 1f, 1f, 0.30f)
            var kx = touch.knobX - touch.baseX; var ky = touch.knobY - touch.baseY
            val len = hypot(kx, ky); val max = l.stickRadius
            if (len > max) { kx = kx / len * max; ky = ky / len * max }
            shapes.circle(l.stickCx + kx, l.stickCy + ky, l.stickRadius * 0.42f, 18)
        }
        // right aim+fire stick — floats at the thumb when active, reddish = firing;
        // otherwise a fixed "rest here to aim" guide ring sits on the right (P3).
        if (touch.aimActive) {
            shapes.color = Color(1f, 0.5f, 0.4f, 0.16f); shapes.circle(touch.aimBaseX, touch.aimBaseY, l.stickRadius, 28)
            shapes.color = Color(1f, 0.5f, 0.4f, 0.5f)
            var ax = touch.aimKnobX - touch.aimBaseX; var ay = touch.aimKnobY - touch.aimBaseY
            val al = hypot(ax, ay); val am = l.stickRadius
            if (al > am) { ax = ax / al * am; ay = ay / al * am }
            shapes.circle(touch.aimBaseX + ax, touch.aimBaseY + ay, l.stickRadius * 0.42f, 18)
        } else {
            shapes.color = Color(1f, 0.5f, 0.4f, 0.06f); shapes.circle(l.aimGuideCx, l.aimGuideCy, l.aimGuideRadius, 24)
        }
        // action buttons — only the contextually-visible ones; pressed ones brighten + grow (P3).
        for (b in l.all()) {
            if (b !in touch.visibleButtons) continue
            val pressed = b in touch.pressedButtons
            shapes.color = if (pressed) Color(1f, 1f, 1f, 0.34f) else Color(1f, 1f, 1f, 0.14f)
            shapes.circle(l.centerX(b), l.centerY(b), l.radiusOf(b) * (if (pressed) 1.16f else 1f), 22)
        }
        shapes.end()
        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        for (b in l.all()) {
            if (b !in touch.visibleButtons) continue
            glyphLayout.setText(font, labelOf(b)); font.draw(batch, glyphLayout, l.centerX(b) - glyphLayout.width / 2f, l.centerY(b) + 7f)
        }
        batch.end()
    }

    private fun labelOf(b: TouchButton): String = when (b) {
        TouchButton.FIRE -> "射撃"
        TouchButton.MELEE -> "近接"
        TouchButton.DASH -> "ダッシュ"
        TouchButton.RELOAD -> "装填"
        TouchButton.WALL -> "壁"
        TouchButton.WEAPON -> "武器"
    }

    private fun pickupColor(kind: String): Color = when (kind) {
        "ammo9" -> cAmmo9
        "ammo12" -> cAmmo12
        "ammoBeam" -> cAmmoBeam
        "ammoNade" -> cAmmoNade
        "blocks" -> cBlocks
        "med" -> cMed
        else -> Color.WHITE
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
        Sfx.dispose()
    }
}
