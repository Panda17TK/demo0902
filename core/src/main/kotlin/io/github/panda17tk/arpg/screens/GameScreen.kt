package io.github.panda17tk.arpg.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
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
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.world.GameWorld
import io.github.panda17tk.arpg.ecs.world.PlayerCarry
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.Haptics
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.input.KeyboardInput
import io.github.panda17tk.arpg.input.TouchControls
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.render.Fonts
import io.github.panda17tk.arpg.render.Hud
import io.github.panda17tk.arpg.render.PlayerPose
import io.github.panda17tk.arpg.render.SceneRenderer
import io.github.panda17tk.arpg.render.TouchOverlay
import io.github.panda17tk.arpg.save.Scores
import io.github.panda17tk.arpg.sim.Drift
import io.github.panda17tk.arpg.sim.PlanetCardInfo
import io.github.panda17tk.arpg.sim.PlanetContext
import io.github.panda17tk.arpg.sim.PlanetMemoryBook
import io.github.panda17tk.arpg.sim.PlanetScan
import io.github.panda17tk.arpg.sim.ReturnSpawn
import io.github.panda17tk.arpg.sim.ReturnVisitLine
import io.github.panda17tk.arpg.sim.SocietySpeechLines
import io.github.panda17tk.arpg.sim.SurfaceObjective
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.ui.Modals
import io.github.panda17tk.arpg.ui.Overlay
import io.github.panda17tk.arpg.ui.PauseAction
import io.github.panda17tk.arpg.ui.PauseFlow
import io.github.panda17tk.arpg.upgrade.Upgrade
import io.github.panda17tk.arpg.upgrade.Upgrades
import kotlin.math.hypot
import kotlin.math.pow

/** Half-extent of the toroidal box the drifting debris wraps within (matches WorldFactory's seed range). */
private const val DRIFT_RANGE = 1400f
private const val REMEMBERED_TIME = 5f // seconds the HUD greets a returning player on a remembered planet

/**
 * Fixed-timestep simulation + render interpolation, porting the legacy main.js loop.
 * World is y-down, so the world camera is set up y-down to match.
 * Scene painting is delegated to render/SceneRenderer (world) + render/Hud + render/TouchOverlay
 * (screen space); this class owns run state, input routing, the sim loop and the camera.
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
    private val scene = SceneRenderer()

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

    // Visual port: animation clock + run timer.
    private var animTime = 0f
    private var runTime = 0f
    private val glyphLayout = GlyphLayout()
    private var uiScale = 1f

    // Living Planets run state: the star system's seed, surface seed, and per-planet memory.
    private var spaceSeed = 1L            // the current star system's seed (stable, so round-trips return to it)
    private var surfSeed = 100L           // surface seed, varied per landing
    private var returnSpawn: Pair<Float, Float>? = null // where to re-emerge in space after taking off
    private val planetMemory = PlanetMemoryBook() // per-planet society memory, persists across landings this run
    private var landedPlanetId: Long? = null      // id of the planet we're on, so takeoff folds memory back in
    private var rememberedT = 0f                  // seconds left to show the return-visit greeting in the HUD

    // Pre-landing scan card (LP v2.23), rebuilt only when the latched candidate's id changes (FR-1.6).
    private var lastCardId: Long? = null
    private var cachedCard: PlanetCardInfo? = null

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
        spaceSeed = 1L; surfSeed = 100L; returnSpawn = null
        planetMemory.memories.clear(); landedPlanetId = null // a fresh run forgets every planet
        gw = WorldFactory.create(input, configStore.config, seed = spaceSeed)
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
        lastCardId = null; cachedCard = null
    }

    /** Land on the hovered planet (SPACE) or take off from the escape pad (SURFACE), carrying run state across. */
    private fun handleLanding() {
        val ws = gw.worldState
        if (ws.mode == WorldMode.SPACE) {
            val cand = ws.landingCandidate ?: return
            returnSpawn = ReturnSpawn.beside(cand) // remember where to re-emerge on takeoff
            surfSeed += 1
            landedPlanetId = cand.id
            val known = planetMemory.knows(cand.id)
            val recalled = planetMemory.recall(cand.id)
            transitionWorld(WorldMode.SURFACE, cand.biome, surfSeed, null, cand.context)
            gw.worldState.society = recalled // seed this visit from the planet's remembered state
            // Return-visit payoff: a remembered planet greets the player by reputation (shown briefly in the HUD).
            gw.worldState.rememberedPlanet = known
            gw.worldState.returnVisitGreeting = if (known) SocietySpeechLines.returnGreeting(recalled) else null
            rememberedT = if (known && ReturnVisitLine.hudLine(recalled) != null) REMEMBERED_TIME else 0f
        } else {
            if (!playerOnEscapePad()) return // must stand on the escape pad to leave the surface
            landedPlanetId?.let { planetMemory.remember(it, gw.worldState.society) } // fold the visit back into memory
            transitionWorld(WorldMode.SPACE, null, spaceSeed, returnSpawn) // same system, beside the planet we left
        }
    }

    private fun transitionWorld(mode: WorldMode, biome: PlanetBiome?, seed: Long, spawn: Pair<Float, Float>?, context: PlanetContext? = null) {
        val carry = PlayerCarry.of(gw.world, gw.player, gw.waveState.num)
        gw = WorldFactory.create(input, configStore.config, seed, mode, biome, carry, spawn, context)
        gw.waveState.num = carry.wave
        accumulator = 0f; camInit = false; overlay = Overlay.NONE
        choosing = false; offered = false; choices = emptyList(); lastHp = Float.NaN
        lastCardId = null; cachedCard = null // memory may have changed across the transition → rebuild the scan card
    }

    /** True when the player is standing on the surface escape pad (the return point). */
    private fun playerOnEscapePad(): Boolean {
        val pad = gw.worldState.escapePad ?: return false
        val (ppx, ppy) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        return hypot(ppx - pad.first, ppy - pad.second) < Tuning.TILE * 1.5f
    }

    override fun render(delta: Float) {
        KeyboardInput.poll(input)
        pollTap()
        if ((Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) &&
            !choosing && !gw.gameOver.isOver) overlay = PauseFlow.toggle(overlay)
        handlePauseTaps()
        val paused = overlay != Overlay.NONE

        pollGameplayTouch(paused)
        // Living Planets: land on / leave a planet (L key or the touch LAND button) — rebuilds the world.
        if (!paused && !choosing && !gw.gameOver.isOver && input.land) handleLanding()

        if (!advanceSim(delta, paused)) return // restarted this frame — draw from the fresh run next frame
        val alpha = (accumulator / Constants.FIXED_DT).coerceIn(0f, 1f)

        // interpolated player position + state
        val px: Float; val py: Float; val fx: Float; val fy: Float; val sta: Float; val staMax: Float; val pit: Float; val overheat: Boolean
        with(gw.world) {
            val t = gw.player[Transform]; val f = gw.player[Facing]; val s = gw.player[Stamina]
            px = t.prevX + (t.x - t.prevX) * alpha
            py = t.prevY + (t.y - t.prevY) * alpha
            fx = f.x; fy = f.y; sta = s.value; staMax = s.max; pit = gw.player[Health].iTime; overheat = s.overheat
        }
        // flow the cosmetic debris/asteroid field around the player (space only; wraps toroidally)
        if (!paused) gw.worldState.drift?.let { Drift.advance(it, px, py, DRIFT_RANGE, delta) }
        // The return-visit greeting fades after a few seconds, then the surface objective reverts to normal.
        if (rememberedT > 0f && !paused) { rememberedT -= delta; if (rememberedT <= 0f) gw.worldState.rememberedPlanet = false }
        val playerHit = pit > 0f && ((pit * 20f).toInt() % 2 == 0)
        val pose = PlayerPose(px, py, fx, fy, dashing = input.dash && sta > 0f, hit = playerHit, muzzle = input.fire)

        updateCamera(delta, px, py, fx, fy)
        if (gw.fx.shakeMag > 0f) { camera.position.add(gw.fx.shakeX(), gw.fx.shakeY(), 0f); camera.update() }

        ScreenUtils.clear(0.06f, 0.07f, 0.10f, 1f)

        // world (procedural sprites — ported from legacy renderer.js + enemy-sprites.js)
        worldViewport.apply()
        scene.draw(shapes, batch, font, camera, gw, animTime, pose)

        drawHud(paused, sta, staMax, overheat)
    }

    /** Gameplay touch (twin-stick / fire) runs only when no modal blocks it (spec §5.2). */
    private fun pollGameplayTouch(paused: Boolean) {
        // Pass the player context so the action buttons can show/hide by relevance (P3). The LAND button
        // appears only when landing is possible: near a planet in space, or standing on the escape pad.
        if (touchEnabled && !paused && !choosing && !gw.gameOver.isOver) {
            val tw = with(gw.world) { gw.player[Arsenal] }.current
            val tBlocks = with(gw.world) { gw.player[Materials].blocks }
            val ws = gw.worldState
            val canLand = (ws.mode == WorldMode.SPACE && ws.landingCandidate != null) ||
                (ws.mode == WorldMode.SURFACE && playerOnEscapePad())
            touch.poll(input, hudViewport, tBlocks, tw.mag, tw.def.magSize, canLand)
        }
    }

    /**
     * Step the fixed-timestep sim, or freeze it for game over / pause / the upgrade modal.
     * Returns false when a game-over restart rebuilt the run (the frame should not be drawn).
     */
    private fun advanceSim(delta: Float, paused: Boolean): Boolean {
        if (gw.gameOver.isOver) {
            accumulator = 0f
            if (!prevOver) {
                newBest = Scores.record(gw.waveState.num, gw.gameOver.kills)
                Sfx.play("dead"); Haptics.buzz(140)
            }
            val restartTapped = tapped &&
                Modals.hitModal(Modals.gameOverButtons(hudViewport.worldWidth, hudViewport.worldHeight), tapX, tapY) != null
            if (Gdx.input.isKeyJustPressed(Input.Keys.R) || restartTapped) { newRun(); return false }
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
        return true
    }

    /** HUD (screen space) — P2 live HUD delegated to render/Hud (geometry from ui/HudLayout). */
    private fun drawHud(paused: Boolean, sta: Float, staMax: Float, overheat: Boolean) {
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
        drawObjectiveHint(paused, hudW, hudH)

        if (touchEnabled && !paused && !choosing && !gw.gameOver.isOver) {
            val landLabel = if (gw.worldState.mode == WorldMode.SURFACE) "発進" else "着陸"
            TouchOverlay.draw(shapes, batch, font, hudViewport, touch, landLabel)
        }
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

    /** Living Planets: surface exploration objective, or the pre-landing scan card in space (HUD space). */
    private fun drawObjectiveHint(paused: Boolean, hudW: Float, hudH: Float) {
        val ws = gw.worldState
        // SPACE: a latched landing candidate shows the scan card instead of the old one-line hint (LP v2.23).
        if (ws.mode == WorldMode.SPACE) {
            val cand = ws.landingCandidate ?: run { lastCardId = null; cachedCard = null; return }
            if (paused || choosing || gw.gameOver.isOver) return
            if (cand.id != lastCardId) { // rebuild only when the candidate changes (FR-1.6)
                cachedCard = PlanetScan.cardFor(cand, planetMemory.knows(cand.id), planetMemory.recall(cand.id))
                lastCardId = cand.id
            }
            cachedCard?.let { Hud.planetScanCard(shapes, batch, font, Fonts.title, hudViewport, it, "[L] 着陸") }
            return
        }
        var elites = 0
        with(gw.world) {
            gw.world.family { all(Mob) }.forEach { if (it[Mob].def.tier != "normal") elites++ }
        }
        val biome = ws.biome
        val onPad = playerOnEscapePad()
        val hint = when {
            onPad -> "[L] 脱出パッドから離陸して宇宙へ"
            biome != null -> SurfaceObjective.hudLine(biome, elites, ws.society, ws.context ?: PlanetContext.NEUTRAL, ws.rememberedPlanet)
            else -> "[L] 離陸して宇宙へ"
        }
        if (!paused && !choosing) {
            batch.projectionMatrix = hudViewport.camera.combined
            batch.begin()
            glyphLayout.setText(font, hint)
            font.draw(batch, glyphLayout, (hudW - glyphLayout.width) / 2f, hudH - 12f)
            batch.end()
        }
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
