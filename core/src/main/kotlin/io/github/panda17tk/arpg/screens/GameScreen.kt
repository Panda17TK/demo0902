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
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.world.GameWorld
import io.github.panda17tk.arpg.ecs.world.GearOps
import io.github.panda17tk.arpg.ecs.world.ItemUse
import io.github.panda17tk.arpg.ecs.world.PlayerCarry
import io.github.panda17tk.arpg.ecs.world.RewardApply
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.item.EquipSlot
import io.github.panda17tk.arpg.item.ItemCatalog
import io.github.panda17tk.arpg.item.ItemDef
import io.github.panda17tk.arpg.item.ItemKind
import io.github.panda17tk.arpg.item.Loadout
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
import io.github.panda17tk.arpg.save.PlanetMemoryCodec
import io.github.panda17tk.arpg.save.PreferencesMemoryStore
import io.github.panda17tk.arpg.save.PreferencesRunSaveStore
import io.github.panda17tk.arpg.save.RunSaveDto
import io.github.panda17tk.arpg.save.Scores
import io.github.panda17tk.arpg.sim.Drift
import io.github.panda17tk.arpg.sim.PlanetCardInfo
import io.github.panda17tk.arpg.sim.PlanetContext
import io.github.panda17tk.arpg.sim.PlanetLexicon
import io.github.panda17tk.arpg.sim.PlanetScan
import io.github.panda17tk.arpg.sim.PlanetSocietyState
import io.github.panda17tk.arpg.sim.RunSession
import io.github.panda17tk.arpg.sim.SocietyMemorySummary
import io.github.panda17tk.arpg.sim.ReturnVisitEffects
import io.github.panda17tk.arpg.sim.RewardBundle
import io.github.panda17tk.arpg.sim.SurfaceGoals
import io.github.panda17tk.arpg.sim.SurfaceObjective
import io.github.panda17tk.arpg.sim.TakeoffReward
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.sim.WorldState
import io.github.panda17tk.arpg.ui.HudLayout
import io.github.panda17tk.arpg.ui.InvTab
import io.github.panda17tk.arpg.ui.InventoryLayout
import io.github.panda17tk.arpg.ui.Modals
import io.github.panda17tk.arpg.ui.Overlay
import io.github.panda17tk.arpg.ui.PauseAction
import io.github.panda17tk.arpg.ui.PauseFlow
import io.github.panda17tk.arpg.ui.TransitionFade
import io.github.panda17tk.arpg.upgrade.Upgrade
import io.github.panda17tk.arpg.upgrade.Upgrades
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow

/** Half-extent of the toroidal box the drifting debris wraps within (matches WorldFactory's seed range). */
private const val DRIFT_RANGE = 1400f
private const val REMEMBERED_TIME = 5f // seconds the HUD greets a returning player on a remembered planet
private const val TOAST_TIME = 3f      // seconds the takeoff send-off toast rides the space HUD (LP v2.29)
private const val INV_TIME_SCALE = 0.01f // v2.33: the world crawls at this speed behind the inventory
private const val PLANET_TAP_PAD = 48f   // v2.38: extra world-px around a planet that still counts as tapping it
private const val SETTINGS_PREFS = "drift-settings" // v2.39: device-level settings (not run state)
private const val SETTINGS_SWAP = "controlSwap"
private const val SAVED_NOTE_TIME = 2f // seconds the 「セーブした」 flash stays on the SAVE tab

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
    private var rawTapX = 0f // raw screen coords, re-unprojected into world space for planet taps (v2.38)
    private var rawTapY = 0f
    private val tmpTap = Vector3()

    // Static help text shown on the pause → 操作説明 overlay (keyboard + touch bindings).
    private val HELP_LINES = listOf(
        "移動：左スティック / WASD",
        "エイム＆射撃：右スティック / 矢印 + K",
        "ダッシュ：ボタン / Shift",
        "近接：ボタン / J",
        "リロード：ボタン / R",
        "武器切替・壁設置：ボタン / 数字",
        "インベントリ：持物ボタン / I",
        "フルスロットル：全開ボタン / O（OCスラスター装備時）",
        "ポーズ：右上ボタン / Esc・P",
    )

    // Visual port: animation clock + run timer.
    private var animTime = 0f
    private var runTime = 0f
    private val glyphLayout = GlyphLayout()
    private var uiScale = 1f

    // Living Planets run state (R1): seeds + per-planet memory live in the pure RunSession; this class
    // only executes the transitions it plans. rememberedT is a draw-side timer, so it stays here.
    private val session = RunSession(store = PreferencesMemoryStore())
    private var rememberedT = 0f // seconds left to show the return-visit greeting in the HUD

    // Inventory overlay (v2.33): tab state + the run-save backend + a brief note flash (セーブした /
    // a consumable's effect). readingLore (v2.34) is the readable currently open on the ITEMS tab.
    private val runStore = PreferencesRunSaveStore()
    private var invTab = InvTab.EQUIP
    private var invNote: String? = null
    private var invNoteT = 0f
    private var readingLore: ItemDef? = null
    private var worldSeed = 1L // the seed the CURRENT world was built with (goes into the run save)
    // v2.39: swap the touch roles — melee on the right stick, gun on the ML button (persisted).
    private var controlSwap = false

    // Takeoff send-off toast (LP v2.29): one line in the SPACE HUD for a few seconds after leaving.
    private var rewardToast: String? = null
    private var rewardToastT = 0f

    // Memory tint per planet id (LP v2.30/10c) — rebuilt only when memory can change (transitions/forget).
    private var memoryTones: Map<Long, Int> = emptyMap()

    // Landing/takeoff fade (10b): OUT → swap the world behind black → IN. Gameplay input pauses meanwhile.
    private val fade = TransitionFade()

    // Pre-landing scan card (LP v2.23), rebuilt only when the latched candidate's id changes (FR-1.6).
    private var lastCardId: Long? = null
    private var cachedCard: PlanetCardInfo? = null

    // Surface goal chips (LP v2.26), rebuilt only when their inputs change (§14.2 — no per-frame strings).
    private var chipsKey = -1
    private var cachedChips: List<String> = emptyList()

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
        session.restore() // LP v2.28: the universe remembers you across runs and restarts
        touchEnabled = Gdx.app.type == Application.ApplicationType.Android
        controlSwap = try { Gdx.app.getPreferences(SETTINGS_PREFS).getBoolean(SETTINGS_SWAP, false) } catch (_: Throwable) { false }
        if (!tryRestoreRun()) newRun() // v2.33: a saved run resumes where it left off
    }

    /** Build (or rebuild) the run and reset per-run screen state (Phase 7 restart). */
    private fun newRun() {
        session.reset() // a fresh run forgets every planet
        runStore.clear() // v2.33: restarting abandons the saved run
        worldSeed = session.spaceSeed
        gw = WorldFactory.create(input, configStore.config, seed = session.spaceSeed)
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
        rebuildMemoryTones()
    }

    /** LP v2.30/10c: per-planet memory tints, recomputed only when the memory can have changed. */
    private fun rebuildMemoryTones() {
        memoryTones = session.memory.memories
            .mapValues { (_, s) -> ReturnVisitEffects.memoryTone(s) }
            .filterValues { it != 0 }
    }

    /** Whether a landing/takeoff would actually happen right now (used to gate the fade, 10b). */
    private fun canTransitionNow(): Boolean = if (gw.worldState.mode == WorldMode.SPACE) {
        gw.worldState.landingCandidate != null
    } else {
        playerOnEscapePad()
    }

    /** Land on the hovered planet (SPACE) or take off from the escape pad (SURFACE), carrying run state across. */
    private fun handleLanding() {
        val ws = gw.worldState
        if (ws.mode == WorldMode.SPACE) {
            val cand = ws.landingCandidate ?: return
            val plan = session.planLanding(cand) // seeds, memory recall and the greeting all decided in one place
            // R2: the remembered society goes INTO the factory, so spawn-time consumers see it from tick 0.
            transitionWorld(WorldMode.SURFACE, plan.biome, plan.seed, null, plan.context, plan.society)
            // Return-visit payoff: a remembered planet greets the player by reputation (shown briefly in the HUD).
            gw.worldState.rememberedPlanet = plan.known
            gw.worldState.returnVisitGreeting = plan.greeting
            rememberedT = if (plan.showGreeting) REMEMBERED_TIME else 0f
        } else {
            if (!playerOnEscapePad()) return // must stand on the escape pad to leave the surface
            // Takeoff send-off (LP v2.29): the star's parting gift, applied BEFORE the transition so
            // PlayerCarry hauls it into space. A child-killer gets nothing, and is told so.
            val soc = gw.worldState.society
            val reward = gw.worldState.biome?.let {
                TakeoffReward.compute(soc, it, gw.worldState.context ?: PlanetContext.NEUTRAL)
            } ?: RewardBundle()
            RewardApply.apply(gw.world, gw.player, reward)
            rewardToast = TakeoffReward.toastFor(reward, soc.childKilled)
            rewardToastT = if (rewardToast != null) TOAST_TIME else 0f
            val (seed, spawn) = session.completeTakeoff(soc) // fold the visit back into memory
            transitionWorld(WorldMode.SPACE, null, seed, spawn) // same system, beside the planet we left
        }
    }

    private fun transitionWorld(
        mode: WorldMode, biome: PlanetBiome?, seed: Long, spawn: Pair<Float, Float>?,
        context: PlanetContext? = null, society: PlanetSocietyState? = null,
    ) {
        val carry = PlayerCarry.of(gw.world, gw.player, gw.waveState.num)
        worldSeed = seed
        gw = WorldFactory.create(input, configStore.config, seed, mode, biome, carry, spawn, context, society)
        gw.waveState.num = carry.wave
        accumulator = 0f; camInit = false; overlay = Overlay.NONE
        choosing = false; offered = false; choices = emptyList(); lastHp = Float.NaN
        lastCardId = null; cachedCard = null // memory may have changed across the transition → rebuild the scan card
        chipsKey = -1; cachedChips = emptyList()
        rebuildMemoryTones()
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

        pollGameplayTouch(overlay != Overlay.NONE || fade.blocksInput)
        // v2.33: the I key / INV button toggles the inventory. Unlike pause it does not freeze the
        // sim — the world crawls at INV_TIME_SCALE behind the panel (装備・持物・マップ・セーブ).
        if (input.inventory && !choosing && !gw.gameOver.isOver &&
            (overlay == Overlay.NONE || overlay == Overlay.INVENTORY)
        ) {
            overlay = if (overlay == Overlay.INVENTORY) Overlay.NONE else Overlay.INVENTORY
            readingLore = null
        }
        val paused = overlay != Overlay.NONE && overlay != Overlay.INVENTORY // sim-freezing overlays
        val simDelta = delta * if (overlay == Overlay.INVENTORY) INV_TIME_SCALE else 1f
        if (invNoteT > 0f) invNoteT -= delta

        // Living Planets: land on / leave a planet (L key or the touch LAND button). The fade wraps the
        // rebuild: OUT → (world swap behind black) → IN, and gameplay input is ignored meanwhile (10b).
        if (overlay == Overlay.NONE && !choosing && !gw.gameOver.isOver && !fade.blocksInput && input.land && canTransitionNow()) {
            fade.start()
            Sfx.play(if (gw.worldState.mode == WorldMode.SPACE) "land" else "takeoff")
        }
        if (!paused && fade.update(delta)) handleLanding() // the OUT leg just completed → swap behind black

        if (!advanceSim(simDelta, paused)) return // restarted this frame — draw from the fresh run next frame
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
        if (!paused) gw.worldState.drift?.let { Drift.advance(it, px, py, DRIFT_RANGE, simDelta) }
        // v2.33/39: everything the screen shows is mapped — the visible viewport rect, not just the
        // player's immediate surroundings (the inventory MAP tab draws only these tiles).
        if (!paused) {
            val vhw = worldViewport.worldWidth / 2f; val vhh = worldViewport.worldHeight / 2f
            gw.visited.markRect(
                floor((camX - vhw) / Tuning.TILE).toInt(), floor((camY - vhh) / Tuning.TILE).toInt(),
                floor((camX + vhw) / Tuning.TILE).toInt(), floor((camY + vhh) / Tuning.TILE).toInt(),
            )
        }
        // The return-visit greeting fades after a few seconds, then the surface objective reverts to normal.
        if (rememberedT > 0f && !paused) { rememberedT -= simDelta; if (rememberedT <= 0f) gw.worldState.rememberedPlanet = false }
        if (rewardToastT > 0f && !paused) { rewardToastT -= simDelta; if (rewardToastT <= 0f) rewardToast = null }
        val playerHit = pit > 0f && ((pit * 20f).toInt() % 2 == 0)
        // v2.37: the gear look — the active weapon shapes the drawn gun, armor tints the suit, OC burns blue.
        val gearLook = with(gw.world) { gw.player[Gear].loadout }
        val activeWeapon = with(gw.world) { gw.player[Arsenal].current.def.id }
        val pose = PlayerPose(
            px, py, fx, fy, dashing = input.dash && sta > 0f, hit = playerHit, muzzle = input.fire,
            weaponType = activeWeapon, armorId = gearLook.armor?.id, oc = gearLook.hasOverclockThruster,
        )

        updateCamera(delta, px, py, fx, fy)
        if (gw.fx.shakeMag > 0f) { camera.position.add(gw.fx.shakeX(), gw.fx.shakeY(), 0f); camera.update() }

        ScreenUtils.clear(0.06f, 0.07f, 0.10f, 1f)

        // world (procedural sprites — ported from legacy renderer.js + enemy-sprites.js)
        worldViewport.apply()
        scene.draw(shapes, batch, font, camera, gw, animTime, pose, memoryTones)

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
            val hasOverclock = with(gw.world) { gw.player[Gear].loadout.hasOverclockThruster }
            touch.poll(input, hudViewport, tBlocks, tw.mag, tw.def.magSize, canLand, hasOverclock, controlSwap)
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
                session.persist() // LP v2.28: a death checkpoints the universe's memory too
                runStore.clear() // v2.33: death consumes the saved run (roguelite — no retry from the save)
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
        // Surface event feed (LP v2.24): drawn whenever on a surface; aging freezes with the sim while paused.
        if (gw.worldState.mode == WorldMode.SURFACE) Hud.eventFeed(batch, font, hudViewport, gw.worldState.recentEvents)

        if (touchEnabled && overlay == Overlay.NONE && !choosing && !gw.gameOver.isOver) {
            val landLabel = if (gw.worldState.mode == WorldMode.SURFACE) "発進" else "着陸"
            TouchOverlay.draw(shapes, batch, font, hudViewport, touch, landLabel, controlSwap)
        }
        if (overlay == Overlay.NONE && !choosing && !gw.gameOver.isOver) Hud.pauseButton(shapes, hudViewport, Modals.pauseButton(hudW, hudH))
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
        if (overlay == Overlay.INVENTORY) drawInventory()
        if (overlay == Overlay.PAUSE) Hud.pause(shapes, batch, font, Fonts.title, hudViewport, Modals.pauseButtons(hudW, hudH, pauseHasMemory()))
        if (overlay == Overlay.HELP) Hud.help(shapes, batch, font, Fonts.title, hudViewport, Modals.helpButtons(hudW, hudH).first(), HELP_LINES)
        if (overlay == Overlay.FORGET) Hud.forget(shapes, batch, font, Fonts.title, hudViewport, Modals.forgetButtons(hudW, hudH))
        if (overlay == Overlay.MEMORY) {
            val ws = gw.worldState
            val soc = ws.society
            var elites = 0
            with(gw.world) { gw.world.family { all(Mob) }.forEach { if (it[Mob].def.tier != "normal") elites++ } }
            val goals = ws.biome?.let { SurfaceGoals.allChips(it, ws.context ?: PlanetContext.NEUTRAL, soc, elites) } ?: emptyList()
            Hud.memory(
                shapes, batch, font, Fonts.title, hudViewport,
                PlanetLexicon.traitLine(ws.context ?: PlanetContext.NEUTRAL),
                SocietyMemorySummary.factLines(soc), SocietyMemorySummary.gauges(soc),
                Modals.helpButtons(hudW, hudH).first(), goals,
            )
        }
        Hud.fade(shapes, hudViewport, fade.alpha) // landing/takeoff scrim covers everything (10b)
    }

    /** Living Planets: surface exploration objective, or the pre-landing scan card in space (HUD space). */
    private fun drawObjectiveHint(paused: Boolean, hudW: Float, hudH: Float) {
        val ws = gw.worldState
        // SPACE: a latched landing candidate shows the scan card instead of the old one-line hint (LP v2.23).
        if (ws.mode == WorldMode.SPACE) {
            // The takeoff send-off toast (LP v2.29) rides the top of the space HUD for a few seconds.
            if (rewardToastT > 0f && rewardToast != null && !paused && !choosing && !gw.gameOver.isOver) {
                batch.projectionMatrix = hudViewport.camera.combined
                batch.begin()
                glyphLayout.setText(font, rewardToast)
                font.draw(batch, glyphLayout, (hudW - glyphLayout.width) / 2f, hudH - 12f)
                batch.end()
            }
            val cand = ws.landingCandidate ?: run {
                lastCardId = null; cachedCard = null
                // v2.38/39: even with no planet latched, space tells you HOW landing works AND
                // points at the nearest planet with a live distance — no more searching blind.
                if (!paused && !choosing && !gw.gameOver.isOver) {
                    val idle = if (touchEnabled) "惑星に近づくとカードが出る　惑星をタップで着陸" else "惑星に近づいて [L] で着陸"
                    val (ppx, ppy) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
                    val nearest = gw.planets.minByOrNull { hypot(it.cx - ppx, it.cy - ppy) }
                    val nav = nearest?.let {
                        val dx = it.cx - ppx; val dy = it.cy - ppy
                        val dist = (hypot(dx, dy) - it.radius).coerceAtLeast(0f).toInt()
                        // World is y-down: dy<0 = up on screen. Pick the dominant cardinal arrow.
                        val arrow = if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                            if (dx >= 0f) "→" else "←"
                        } else {
                            if (dy >= 0f) "↓" else "↑"
                        }
                        "最寄りの惑星 $arrow $dist"
                    }
                    batch.projectionMatrix = hudViewport.camera.combined
                    batch.begin()
                    glyphLayout.setText(font, idle)
                    font.draw(batch, glyphLayout, (hudW - glyphLayout.width) / 2f, hudH - 12f)
                    if (nav != null) {
                        glyphLayout.setText(font, nav)
                        font.draw(batch, glyphLayout, (hudW - glyphLayout.width) / 2f, hudH - 34f)
                    }
                    batch.end()
                }
                return
            }
            if (paused || choosing || gw.gameOver.isOver) return
            if (cand.id != lastCardId) { // rebuild only when the candidate changes (FR-1.6)
                cachedCard = PlanetScan.cardFor(cand, session.memory.knows(cand.id), session.memory.recall(cand.id))
                lastCardId = cand.id
                Sfx.play("scan") // 10a: a fresh scan pings once per newly latched planet
            }
            cachedCard?.let {
                val hint = if (touchEnabled) "惑星かこのカードをタップで着陸" else "[L] 着陸" // v2.34/38: planet or card = the landing button
                Hud.planetScanCard(shapes, batch, font, Fonts.title, hudViewport, it, hint)
            }
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
        // Goal chips (LP v2.26) right under the main objective line, rebuilt only when inputs change.
        val chips = if (biome != null) surfaceChips(biome, ws, elites) else emptyList()
        if (!paused && !choosing) {
            batch.projectionMatrix = hudViewport.camera.combined
            batch.begin()
            glyphLayout.setText(font, hint)
            font.draw(batch, glyphLayout, (hudW - glyphLayout.width) / 2f, hudH - 12f)
            if (chips.isNotEmpty()) {
                val joined = chips.joinToString("　　")
                glyphLayout.setText(font, joined)
                font.draw(batch, glyphLayout, (hudW - glyphLayout.width) / 2f, hudH - 34f)
            }
            batch.end()
        }
    }

    /** The cached surface goal chips (max 2); rebuilt only when the deciding inputs change (§14.2). */
    private fun surfaceChips(biome: PlanetBiome, ws: WorldState, elites: Int): List<String> {
        val s = ws.society
        fun b(v: Boolean) = if (v) 1 else 0
        val key = (elites shl 5) or (b(s.relicClaimed) shl 4) or (b(s.childKilled) shl 3) or
            (b(s.childHarmed) shl 2) or (b(s.apexKilled) shl 1) or b(s.leaderDefeated)
        if (key != chipsKey) {
            cachedChips = SurfaceGoals.chipsFor(biome, ws.context ?: PlanetContext.NEUTRAL, s, elites)
            chipsKey = key
        }
        return cachedChips
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
            rawTapX = Gdx.input.x.toFloat(); rawTapY = Gdx.input.y.toFloat() // kept for world-space hits (v2.38)
            tmpTap.set(rawTapX, rawTapY, 0f)
            hudViewport.unproject(tmpTap)
            tapX = tmpTap.x; tapY = tmpTap.y
        }
    }

    /** v2.38: did this frame's tap land on the landing candidate planet itself (world space)? */
    private fun tapHitsCandidatePlanet(): Boolean {
        val cand = gw.worldState.landingCandidate ?: return false
        tmpTap.set(rawTapX, rawTapY, 0f)
        worldViewport.unproject(tmpTap)
        return hypot(tmpTap.x - cand.cx, tmpTap.y - cand.cy) < cand.radius + PLANET_TAP_PAD
    }

    /** Whether the pause carries the 4th 「この星の記憶」 entry (surface only — LP v2.25). */
    private fun pauseHasMemory(): Boolean = gw.worldState.mode == WorldMode.SURFACE

    /** Route a tap to the pause/help/memory overlays, or open pause from the in-play ⏸ button (spec §5.2). */
    private fun handlePauseTaps() {
        if (!tapped) return
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        when (overlay) {
            Overlay.PAUSE -> {
                val hasMemory = pauseHasMemory()
                when (PauseFlow.action(Modals.hitModal(Modals.pauseButtons(w, h, hasMemory), tapX, tapY) ?: -1, hasMemory)) {
                    PauseAction.RESUME -> overlay = Overlay.NONE
                    PauseAction.RESTART -> { newRun(); overlay = Overlay.NONE }
                    PauseAction.HELP -> overlay = Overlay.HELP
                    PauseAction.MEMORY -> overlay = Overlay.MEMORY
                    PauseAction.FORGET -> overlay = Overlay.FORGET
                    null -> {}
                }
            }
            Overlay.HELP -> if (Modals.hitModal(Modals.helpButtons(w, h), tapX, tapY) != null) overlay = Overlay.PAUSE
            Overlay.MEMORY -> if (Modals.hitModal(Modals.helpButtons(w, h), tapX, tapY) != null) overlay = Overlay.PAUSE
            Overlay.FORGET -> when (Modals.hitModal(Modals.forgetButtons(w, h), tapX, tapY)) {
                0 -> { // confirmed: every star forgets you (memory + disk)
                    session.forgetUniverse()
                    lastCardId = null; cachedCard = null // the scan card must re-read the blank memory
                    rebuildMemoryTones()
                    overlay = Overlay.PAUSE
                }
                1 -> overlay = Overlay.PAUSE
                else -> {}
            }
            Overlay.INVENTORY -> handleInventoryTap(w, h)
            Overlay.NONE -> if (!choosing && !gw.gameOver.isOver) {
                if (Modals.hitModal(listOf(Modals.pauseButton(w, h)), tapX, tapY) != null) {
                    overlay = Overlay.PAUSE
                } else if (touchEnabled && !fade.blocksInput && gw.worldState.mode == WorldMode.SPACE &&
                    canTransitionNow() && (
                        cachedCard?.let { HudLayout.planetCard(w, h, it.lines.size).contains(tapX, tapY) } == true ||
                            tapHitsCandidatePlanet() // v2.38: the planet ITSELF is the biggest landing button of all
                        )
                ) {
                    // v2.34: on touch, tapping the scan card lands — the card is a big, obvious target where
                    // the player is already looking, so landing no longer depends on reaching the LAND button.
                    fade.start()
                    Sfx.play("land")
                }
            }
        }
    }

    /** Route a tap inside the inventory overlay (v2.33): tabs, slot cycling, item use/read, save, close. */
    private fun handleInventoryTap(w: Float, h: Float) {
        Modals.hitModal(InventoryLayout.tabs(w, h), tapX, tapY)?.let {
            invTab = InvTab.entries[it]; readingLore = null; return
        }
        if (InventoryLayout.closeButton(w, h).contains(tapX, tapY)) { overlay = Overlay.NONE; readingLore = null; return }
        when (invTab) {
            InvTab.EQUIP -> {
                if (InventoryLayout.controlToggle(w, h).contains(tapX, tapY)) { toggleControlSwap(); return }
                val row = Modals.hitModal(InventoryLayout.slotRows(w, h), tapX, tapY) ?: return
                GearOps.cycleSlot(gw.world, gw.player, InventoryLayout.SLOT_ORDER[row])
            }
            InvTab.ITEMS -> handleItemsTap(w, h)
            InvTab.SAVE -> if (InventoryLayout.saveButton(w, h).contains(tapX, tapY)) saveRun()
            InvTab.MAP -> {}
        }
    }

    /** ITEMS tab tap (v2.34): reading → back; a consumable row → use one; a lore row → open it. */
    private fun handleItemsTap(w: Float, h: Float) {
        if (readingLore != null) { readingLore = null; return } // any tap closes the reading view
        val gear = with(gw.world) { gw.player[Gear] }
        val groups = ItemCatalog.grouped(gear.backpack)
        val idx = Modals.hitModal(InventoryLayout.itemRows(w, h, groups.size), tapX, tapY) ?: return
        val item = groups[idx].first
        when (item.kind) {
            ItemKind.CONSUMABLE -> {
                val note = ItemUse.use(gw.world, gw.player, item) ?: return // wasted use → keep the item
                val at = gear.backpack.indexOfFirst { it.id == item.id }
                if (at >= 0) gear.backpack.removeAt(at)
                invNote = note; invNoteT = SAVED_NOTE_TIME
            }
            ItemKind.LORE -> readingLore = item
            // Equipment swaps in from the EQUIP tab's slots, not from the list.
            ItemKind.THRUSTER, ItemKind.ARMOR, ItemKind.RANGED_WEAPON, ItemKind.MELEE_WEAPON, ItemKind.ACCESSORY -> {}
        }
    }

    /** The inventory overlay's view model → Hud.inventory (v2.33). */
    private fun drawInventory() {
        val gear = with(gw.world) { gw.player[Gear] }
        val slotTexts = InventoryLayout.SLOT_ORDER.mapIndexed { i, slot ->
            "${InventoryLayout.SLOT_LABELS[i]}：${gear.loadout.get(slot)?.name ?: "（なし）"}"
        }
        // The backpack, grouped so duplicates read as ×N; a marker tells apart what a tap does (v2.34).
        val itemLines = ItemCatalog.grouped(gear.backpack).map { (d, n) ->
            val count = if (n > 1) "　×$n" else ""
            when (d.kind) {
                ItemKind.CONSUMABLE -> "【使】${d.name}$count　─　${d.desc}"
                ItemKind.LORE -> "【読】『${d.name}』$count　─　${d.desc}"
                else -> if (d.desc.isEmpty()) "${d.name}$count" else "${d.name}$count　─　${d.desc}"
            }
        }
        val (ptx, pty) = with(gw.world) {
            val t = gw.player[Transform]
            floor(t.x / Tuning.TILE).toInt() to floor(t.y / Tuning.TILE).toInt()
        }
        Hud.inventory(
            shapes, batch, font, Fonts.title, hudViewport, invTab,
            slotTexts, itemLines, gw.visited, ptx, pty,
            if (invNoteT > 0f) invNote else null,
            loreTitle = readingLore?.name, loreLines = readingLore?.lore?.split("\n") ?: emptyList(),
            controlLabel = "操作: " + (if (controlSwap) "銃=ボタン / 近接=右スティック" else "近接=ボタン / 銃=右スティック") + "　(タップで入替)",
        )
    }

    /** v2.39: flip the touch roles of the ML button and the right stick, and persist the choice. */
    private fun toggleControlSwap() {
        controlSwap = !controlSwap
        try {
            val p = Gdx.app.getPreferences(SETTINGS_PREFS)
            p.putBoolean(SETTINGS_SWAP, controlSwap)
            p.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
        invNote = if (controlSwap) "入替: 銃=ボタン / 近接=右スティック" else "入替: 近接=ボタン / 銃=右スティック"
        invNoteT = SAVED_NOTE_TIME
    }

    /** v2.33 SAVE tab: snapshot the run (world identity + player state + gear) and persist it. */
    private fun saveRun() {
        val ws = gw.worldState
        val dto = with(gw.world) {
            val t = gw.player[Transform]; val hlt = gw.player[Health]; val sta = gw.player[Stamina]
            val ammo = gw.player[Ammo]; val mods = gw.player[Mods]
            val ars = gw.player[Arsenal]; val gear = gw.player[Gear]
            RunSaveDto(
                mode = ws.mode.name, biome = ws.biome?.name,
                spaceSeed = session.spaceSeed, surfSeed = session.surfSeed, worldSeed = worldSeed,
                landedPlanetId = session.landedPlanetId,
                returnX = session.returnSpawn?.first, returnY = session.returnSpawn?.second,
                wave = gw.waveState.num,
                px = t.x, py = t.y,
                hp = hlt.hp, hpMax = hlt.hpMax, stamina = sta.value,
                ammo9 = ammo.ammo9, ammo12 = ammo.ammo12, ammoBeam = ammo.ammoBeam, ammoNade = ammo.ammoNade,
                blocks = gw.player[Materials].blocks,
                mags = ars.weapons.map { it.mag },
                gunMul = mods.gunMul, fireMul = mods.fireMul, meleeMul = mods.meleeMul, moveMul = mods.moveMul,
                ammoMul = mods.ammoMul, healOnKill = mods.healOnKill, wallHp = mods.wallHp,
                loadout = EquipSlot.entries.mapNotNull { s -> gear.loadout.get(s)?.let { s.name to it.id } }.toMap(),
                backpack = gear.backpack.map { it.id },
                curWeapon = ars.curW,
                society = if (ws.mode == WorldMode.SURFACE) PlanetMemoryCodec.dtoOf(ws.society) else null,
            )
        }
        runStore.save(dto)
        session.persist() // the universe's memory checkpoints alongside the run
        invNote = "セーブした"; invNoteT = SAVED_NOTE_TIME
    }

    /**
     * v2.33: rebuild the saved run, if one exists and parses. The world regenerates deterministically
     * from its saved seed (enemy positions reset — the save keeps the player, gear and progress).
     * Returns false when there is no usable save, in which case the caller starts a fresh run.
     */
    private fun tryRestoreRun(): Boolean {
        val dto = runStore.load() ?: return false
        val mode = WorldMode.entries.firstOrNull { it.name == dto.mode } ?: return false
        val biome = dto.biome?.let { b -> PlanetBiome.entries.firstOrNull { it.name == b } }
        if (mode == WorldMode.SURFACE && biome == null) return false
        session.spaceSeed = dto.spaceSeed
        session.surfSeed = dto.surfSeed
        session.landedPlanetId = dto.landedPlanetId
        val rx = dto.returnX; val ry = dto.returnY
        session.returnSpawn = if (rx != null && ry != null) rx to ry else null
        // The planet's soul is deterministic from its id + biome — no need to have saved it.
        val context = if (mode == WorldMode.SURFACE && dto.landedPlanetId != null && biome != null) {
            PlanetContext.contextFor(dto.landedPlanetId, biome)
        } else null
        val society = dto.society?.let { PlanetMemoryCodec.stateOf(it) }
        worldSeed = dto.worldSeed
        gw = WorldFactory.create(
            input, configStore.config, dto.worldSeed, mode, biome,
            carry = null, playerSpawn = dto.px to dto.py, context = context, society = society,
        )
        gw.waveState.num = dto.wave
        with(gw.world) {
            val hlt = gw.player[Health]
            hlt.hpMax = dto.hpMax; hlt.hp = dto.hp.coerceIn(0.1f, dto.hpMax)
            gw.player[Stamina].value = dto.stamina
            val ammo = gw.player[Ammo]
            ammo.ammo9 = dto.ammo9; ammo.ammo12 = dto.ammo12; ammo.ammoBeam = dto.ammoBeam; ammo.ammoNade = dto.ammoNade
            gw.player[Materials].blocks = dto.blocks
            val mods = gw.player[Mods]
            mods.gunMul = dto.gunMul; mods.fireMul = dto.fireMul; mods.meleeMul = dto.meleeMul; mods.moveMul = dto.moveMul
            mods.ammoMul = dto.ammoMul; mods.healOnKill = dto.healOnKill; mods.wallHp = dto.wallHp
            val ars = gw.player[Arsenal]
            dto.mags.forEachIndexed { i, m -> if (i < ars.weapons.size) ars.weapons[i].mag = m }
            ars.curW = dto.curWeapon.coerceIn(0, ars.weapons.size - 1)
            val gear = gw.player[Gear]
            for (slot in EquipSlot.entries) {
                val item = dto.loadout[slot.name]?.let { ItemCatalog.byId(it) } ?: continue
                if (Loadout.compatible(slot, item.kind)) gear.loadout.set(slot, item)
            }
            gear.backpack.clear()
            dto.backpack.mapNotNullTo(gear.backpack) { ItemCatalog.byId(it) }
        }
        accumulator = 0f; camInit = false; choosing = false; offered = false; choices = emptyList()
        overlay = Overlay.NONE; lastHp = Float.NaN; lastKills = 0; prevOver = false; newBest = false
        lastCardId = null; cachedCard = null; chipsKey = -1; cachedChips = emptyList()
        rebuildMemoryTones()
        return true
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
