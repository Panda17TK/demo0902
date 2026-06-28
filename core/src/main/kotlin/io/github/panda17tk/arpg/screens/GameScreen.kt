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
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.ecs.world.GameWorld
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.Haptics
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.input.KeyboardInput
import io.github.panda17tk.arpg.input.TouchButton
import io.github.panda17tk.arpg.input.TouchControls
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.render.Actors
import io.github.panda17tk.arpg.render.Draw
import io.github.panda17tk.arpg.render.Fonts
import io.github.panda17tk.arpg.render.WorldView
import io.github.panda17tk.arpg.save.Scores
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.upgrade.Upgrade
import io.github.panda17tk.arpg.upgrade.Upgrades
import kotlin.math.hypot
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
    private val cPanel = Color(0.05f, 0.07f, 0.10f, 0.72f)
    private val cBarBg = Color(0.2f, 0.2f, 0.25f, 0.9f)
    private val cSta = Color.valueOf("4da6ff")
    private val cHpHi = Color.valueOf("7fe08a")
    private val cHpLo = Color.valueOf("e0786a")
    private val glyphLayout = GlyphLayout()
    private var uiScale = 1f

    override fun show() {
        configStore.loadFromDisk()
        uiScale = Gdx.graphics.density.coerceIn(1f, 4f)
        shapes = ShapeRenderer()
        batch = SpriteBatch()
        Fonts.load(uiScale)
        font = Fonts.ui
        camera = OrthographicCamera().apply { setToOrtho(true, Tuning.VIEW_W, Tuning.VIEW_H) } // y-down
        worldViewport = ExtendViewport(Tuning.VIEW_H, Tuning.VIEW_H, camera) // square min, extends to fill (portrait/landscape)
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
        lastHp = Float.NaN
        lastKills = 0
        prevOver = false
        newBest = false
    }

    override fun render(delta: Float) {
        KeyboardInput.poll(input)
        if (touchEnabled) touch.poll(input, hudViewport)
        if (gw.gameOver.isOver) {
            accumulator = 0f
            if (!prevOver) {
                newBest = Scores.record(gw.waveState.num, gw.gameOver.kills)
                Sfx.play("dead"); Haptics.buzz(140)
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) { newRun(); return }
        } else {
            updateUpgradeFlow(delta)
            trackPlayerHitShake()
        }
        prevOver = gw.gameOver.isOver
        gw.fx.update(delta)
        animTime += delta
        if (!gw.gameOver.isOver && !choosing) runTime += delta
        val alpha = (accumulator / Constants.FIXED_DT).coerceIn(0f, 1f)

        // interpolated player position + state
        val px: Float; val py: Float; val fx: Float; val fy: Float; val sta: Float; val staMax: Float; val pit: Float
        with(gw.world) {
            val t = gw.player[Transform]; val f = gw.player[Facing]; val s = gw.player[Stamina]
            px = t.prevX + (t.x - t.prevX) * alpha
            py = t.prevY + (t.y - t.prevY) * alpha
            fx = f.x; fy = f.y; sta = s.value; staMax = s.max; pit = gw.player[Health].iTime
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
        WorldView.draw(shapes, gw.map)
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
        shapes.end()

        // charge-melee telegraph rings
        shapes.begin(ShapeRenderer.ShapeType.Line)
        with(gw.world) {
            gw.world.family { all(Mob, Transform, MobAction) }.forEach { e ->
                val mt = e[Transform]; val ma = e[MobAction]
                if (ma.charging) { shapes.color = cTelegraph; shapes.circle(mt.x, mt.y, 14f + ma.chargeT * 30f, 16) }
            }
        }
        shapes.end()

        // HUD (screen space) — panel + bars + stats
        hudViewport.apply()
        val hudW = hudViewport.worldWidth; val hudH = hudViewport.worldHeight
        val blocks = with(gw.world) { gw.player[Materials].blocks }
        val arsenal = with(gw.world) { gw.player[Arsenal] }
        val ammo = with(gw.world) { gw.player[Ammo] }
        val hp = with(gw.world) { gw.player[Health].hp }
        val hpMax = with(gw.world) { gw.player[Health].hpMax }
        val foes = gw.world.family { all(Mob) }.numEntities
        val w = arsenal.current
        val magStr = w.def.magSize?.let { "${w.mag}/$it" } ?: "INF"
        val mins = (runTime / 60f).toInt(); val secs = (runTime % 60f).toInt()
        val barW = minOf(220f, hudW - 70f)

        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cPanel; shapes.rect(8f, hudH - 120f, minOf(420f, hudW - 16f), 112f)
        shapes.color = cBarBg; shapes.rect(12f, hudH - 88f, barW, 9f)
        shapes.color = cSta; shapes.rect(12f, hudH - 88f, barW * (if (staMax > 0f) (sta / staMax).coerceIn(0f, 1f) else 0f), 9f)
        shapes.color = cBarBg; shapes.rect(12f, hudH - 104f, barW, 9f)
        shapes.color = if (hp > hpMax * 0.3f) cHpHi else cHpLo; shapes.rect(12f, hudH - 104f, barW * (if (hpMax > 0f) (hp / hpMax).coerceIn(0f, 1f) else 0f), 9f)
        shapes.end()

        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        font.draw(batch, "ウェーブ ${gw.waveState.num}    残り $foes    ${w.def.name} $magStr", 14f, hudH - 16f)
        font.draw(batch, "予備 9mm${ammo.ammo9} 12g${ammo.ammo12} ﾋﾞｰﾑ${ammo.ammoBeam} 榴${ammo.ammoNade}", 14f, hudH - 44f)
        font.draw(batch, "時間 %d:%02d    撃破 %d    資材 %d".format(mins, secs, gw.gameOver.kills, blocks), 14f, hudH - 70f)
        font.draw(batch, "スタ", 18f + barW, hudH - 86f)
        font.draw(batch, "HP ${hp.toInt()}/${hpMax.toInt()}", 18f + barW, hudH - 102f)
        batch.end()

        if (touchEnabled && !choosing && !gw.gameOver.isOver) drawTouchControls()
        if (choosing) drawUpgradeCards()
        if (gw.gameOver.isOver) drawGameOver()
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

    private fun drawUpgradeCards() {
        if (choices.isEmpty()) return
        val cfg = configStore.config.upgrades
        val w = hudViewport.worldWidth
        val h = hudViewport.worldHeight
        val cardW = minOf(360f, w * 0.85f)
        val cardH = 64f
        val gap = 16f
        val totalH = choices.size * cardH + (choices.size - 1) * gap
        val x = (w - cardW) / 2f
        val top = (h + totalH) / 2f - cardH // bottom-left y of the first (top) card

        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.6f)
        shapes.rect(0f, 0f, w, h)
        shapes.color = Color(0.16f, 0.17f, 0.22f, 1f)
        choices.indices.forEach { i -> shapes.rect(x, top - i * (cardH + gap), cardW, cardH) }
        shapes.end()

        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        font.draw(batch, "ウェーブ ${gw.waveState.num} クリア！  強化を選択 (1 / 2 / 3)", x, top + cardH + 28f)
        choices.forEachIndexed { i, u ->
            val cy = top - i * (cardH + gap)
            font.draw(batch, "${i + 1})  ${u.name}", x + 14f, cy + cardH - 14f)
            font.draw(batch, Upgrades.desc(u, cfg), x + 14f, cy + 24f)
        }
        batch.end()
    }

    private fun center(f: BitmapFont, s: String, screenW: Float, y: Float) {
        glyphLayout.setText(f, s)
        f.draw(batch, glyphLayout, (screenW - glyphLayout.width) / 2f, y)
    }

    private fun drawGameOver() {
        val w = hudViewport.worldWidth
        val h = hudViewport.worldHeight
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.72f)
        shapes.rect(0f, 0f, w, h)
        shapes.end()
        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        center(Fonts.title, "ゲームオーバー", w, h / 2f + 70f)
        center(font, "ウェーブ ${gw.waveState.num}    撃破 ${gw.gameOver.kills}", w, h / 2f + 24f)
        center(font, if (newBest) "自己ベスト更新！  ウェーブ ${Scores.bestWave}" else "ベスト  ウェーブ ${Scores.bestWave}  撃破 ${Scores.bestKills}", w, h / 2f - 6f)
        center(font, "R で再挑戦", w, h / 2f - 36f)
        batch.end()
    }

    private fun drawTouchControls() {
        val l = touch.layout
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(1f, 1f, 1f, 0.10f); shapes.circle(l.stickCx, l.stickCy, l.stickRadius, 28)
        if (touch.stickActive) {
            shapes.color = Color(1f, 1f, 1f, 0.30f)
            var kx = touch.knobX - touch.baseX; var ky = touch.knobY - touch.baseY
            val len = hypot(kx, ky); val max = l.stickRadius
            if (len > max) { kx = kx / len * max; ky = ky / len * max }
            shapes.circle(l.stickCx + kx, l.stickCy + ky, l.stickRadius * 0.42f, 18)
        }
        // right aim+fire stick — floats at the thumb, reddish = firing
        if (touch.aimActive) {
            shapes.color = Color(1f, 0.5f, 0.4f, 0.16f); shapes.circle(touch.aimBaseX, touch.aimBaseY, l.stickRadius, 28)
            shapes.color = Color(1f, 0.5f, 0.4f, 0.5f)
            var ax = touch.aimKnobX - touch.aimBaseX; var ay = touch.aimKnobY - touch.aimBaseY
            val al = hypot(ax, ay); val am = l.stickRadius
            if (al > am) { ax = ax / al * am; ay = ay / al * am }
            shapes.circle(touch.aimBaseX + ax, touch.aimBaseY + ay, l.stickRadius * 0.42f, 18)
        }
        shapes.color = Color(1f, 1f, 1f, 0.14f)
        for (b in l.all()) shapes.circle(l.centerX(b), l.centerY(b), l.buttonRadius, 22)
        shapes.end()
        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        for (b in l.all()) { glyphLayout.setText(font, labelOf(b)); font.draw(batch, glyphLayout, l.centerX(b) - glyphLayout.width / 2f, l.centerY(b) + 7f) }
        batch.end()
    }

    private fun labelOf(b: TouchButton): String = when (b) {
        TouchButton.FIRE -> "射撃"
        TouchButton.MELEE -> "近接"
        TouchButton.DASH -> "回避"
        TouchButton.RELOAD -> "装填"
        TouchButton.WALL -> "壁"
        TouchButton.WEAPON -> "武器"
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
