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
import io.github.panda17tk.arpg.audio.Ambience
import io.github.panda17tk.arpg.audio.AmbienceScore
import io.github.panda17tk.arpg.audio.CombatHeat
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
import io.github.panda17tk.arpg.ecs.world.Pickups
import io.github.panda17tk.arpg.ecs.world.PlayerCarry
import io.github.panda17tk.arpg.ecs.world.RewardApply
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.item.EquipSlot
import io.github.panda17tk.arpg.item.GearCraft
import io.github.panda17tk.arpg.item.ItemCatalog
import io.github.panda17tk.arpg.item.ItemDef
import io.github.panda17tk.arpg.item.ItemKind
import io.github.panda17tk.arpg.item.Loadout
import io.github.panda17tk.arpg.item.Market
import io.github.panda17tk.arpg.App
import io.github.panda17tk.arpg.input.Haptics
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.input.ButtonTweak
import io.github.panda17tk.arpg.input.KeyboardInput
import io.github.panda17tk.arpg.input.LayoutTweaks
import io.github.panda17tk.arpg.input.TouchButton
import io.github.panda17tk.arpg.input.TouchControls
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.planet.PlanetQuest
import io.github.panda17tk.arpg.planet.QuestKind
import io.github.panda17tk.arpg.render.Fonts
import io.github.panda17tk.arpg.render.Hud
import io.github.panda17tk.arpg.render.PlayerPose
import io.github.panda17tk.arpg.render.SceneRenderer
import io.github.panda17tk.arpg.render.TouchOverlay
import io.github.panda17tk.arpg.save.Achievement
import io.github.panda17tk.arpg.save.Achievements
import io.github.panda17tk.arpg.save.DeathRelic
import io.github.panda17tk.arpg.save.PlanetMemoryCodec
import io.github.panda17tk.arpg.save.PreferencesMemoryStore
import io.github.panda17tk.arpg.save.PreferencesRelicStore
import io.github.panda17tk.arpg.save.PreferencesRunSaveStore
import io.github.panda17tk.arpg.save.RunSaveDto
import io.github.panda17tk.arpg.save.Scores
import io.github.panda17tk.arpg.sim.DesyncGauge
import io.github.panda17tk.arpg.sim.Drift
import io.github.panda17tk.arpg.sim.Epithet
import io.github.panda17tk.arpg.sim.EventKind
import io.github.panda17tk.arpg.sim.MemoryCoreLog
import io.github.panda17tk.arpg.sim.PlanetCardInfo
import io.github.panda17tk.arpg.sim.PlanetEvent
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
import io.github.panda17tk.arpg.sim.SyncRestoration
import io.github.panda17tk.arpg.sim.TutorialController
import io.github.panda17tk.arpg.sim.TutorialStep
import io.github.panda17tk.arpg.sim.TakeoffReward
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.sim.WorldState
import io.github.panda17tk.arpg.sim.Weather
import io.github.panda17tk.arpg.sim.WeatherKind
import io.github.panda17tk.arpg.sim.WreckLog
import io.github.panda17tk.arpg.ui.HudLayout
import io.github.panda17tk.arpg.ui.InvTab
import io.github.panda17tk.arpg.ui.InventoryLayout
import io.github.panda17tk.arpg.ui.Logbook
import io.github.panda17tk.arpg.ui.Modals
import io.github.panda17tk.arpg.ui.Onboarding
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
// v2.44/v2.52: base gate-shard cost; a well-restored network (SyncRestoration) asks one less.
private const val GATE_TAP_R = 96f       // v2.44: world-px radius that counts as tapping the jump gate
private const val TRAINING_SEED = 4242L   // v2.53: the simulation always rebuilds the same arena
private const val HINT_TOP = 138f          // v2.54: hint panels start below the top HUD band
private const val SETTINGS_PREFS = "drift-settings" // v2.39: device-level settings (not run state)
private const val SETTINGS_SWAP = "controlSwap"
private const val SETTINGS_ONBOARD = "onboardDone" // v2.47: the first-run walkthrough ran once
private const val SETTINGS_LAYOUT = "buttonLayout" // v2.56: the layout editor's saved tweaks
private const val SETTINGS_SOUND = "soundOn"       // v2.59: title-screen toggles
private const val SETTINGS_TUTORIAL = "tutorialDone" // v2.60: the boot diagnostic ran (or was skipped)
private const val SETTINGS_HAPTICS = "hapticsOn"
private const val SETTINGS_LEFTY = "leftHanded"    // v2.65: mirror the touch layout
private const val SETTINGS_HINTS = "controlHintsOn" // v2.66: how-to guidance (landing, onboarding)
private const val SETTINGS_LORE = "loreHintsOn"     // v2.66: the world speaking (core logs, wrecks)
private const val SAVED_NOTE_TIME = 2f // seconds the 「セーブした」 flash stays on the SAVE tab

/**
 * Fixed-timestep simulation + render interpolation, porting the legacy main.js loop.
 * World is y-down, so the world camera is set up y-down to match.
 * Scene painting is delegated to render/SceneRenderer (world) + render/Hud + render/TouchOverlay
 * (screen space); this class owns run state, input routing, the sim loop and the camera.
 */
class GameScreen(
    private val startFresh: Boolean = false, // v2.58 タイトル「はじめから」: abandon the saved run
    private val startInTraining: Boolean = false, // v2.58 タイトル「旧式戦闘訓練」
) : ScreenAdapter() {
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
    private val ambHeat = CombatHeat() // v2.67 状況反応: combat drives the pulse layer
    private var weatherT = 0f // v2.74 天候: cosmetic time, never sim time
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
    // v2.46 遺品回収: what the last death left floating in the void (half the dust, every shard).
    private val relicStore = PreferencesRelicStore()
    private var invTab = InvTab.EQUIP
    private var invNote: String? = null
    private var invNoteT = 0f
    private var readingLore: ItemDef? = null
    private var worldSeed = 1L // the seed the CURRENT world was built with (goes into the run save)
    // v2.39: swap the touch roles — melee on the right stick, gun on the ML button (persisted).
    private var controlSwap = false
    // v2.47: the very first run walks the basics once; completion persists across launches.
    private var onboardDone = true
    private var controlHints = true // v2.66 操作ヒント: how-to guidance on/off
    private var loreHints = true    // v2.66 世界観ヒント: the world's asides on/off
    // v2.60 チュートリアル: the keeper-boot diagnostic (layer 1). Null once done or skipped.
    private var tutorial: TutorialController? = null
    private var tutPrevPx = Float.NaN
    private var tutPrevPy = Float.NaN
    private var tutPrevKills = 0
    private var tutPrevDust = -1

    // v2.56 ボタン配置エディタ: drag to move, toolbar to resize; saved as screen fractions.
    private var layoutEditing = false
    private var editTarget: TouchButton? = null
    private var layoutDragging = false

    // v2.53 旧式戦闘訓練: the walled-off wave simulation. Entering snapshots the real run's
    // player (gear/materials/wave); exiting restores it — training gains stay in the training.
    private var simMode = false
    private var preSimCarry: PlayerCarry? = null
    // v2.43: stall slots already bought this landing (the stock is per-planet deterministic).
    private val marketSold = mutableSetOf<Int>()

    // Takeoff send-off toast (LP v2.29): one line in the SPACE HUD for a few seconds after leaving.
    private var rewardToast: String? = null
    private var rewardToastT = 0f

    // v2.44: the fade that is currently running ends in a system jump, not a landing/takeoff.
    private var pendingJump = false

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
    // v2.45 星の依頼: the quest chip cache (progress-keyed, same no-per-frame-strings policy).
    private var questChipKey = -1
    private var questChip: String? = null

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
        Achievements.load() // v2.62
        session.restore() // LP v2.28: the universe remembers you across runs and restarts
        touchEnabled = Gdx.app.type == Application.ApplicationType.Android
        controlSwap = try { Gdx.app.getPreferences(SETTINGS_PREFS).getBoolean(SETTINGS_SWAP, false) } catch (_: Throwable) { false }
        onboardDone = try { Gdx.app.getPreferences(SETTINGS_PREFS).getBoolean(SETTINGS_ONBOARD, false) } catch (_: Throwable) { true }
        try { // v2.59 設定: sound / haptics master switches (title-screen toggles)
            val sp = Gdx.app.getPreferences(SETTINGS_PREFS)
            Sfx.enabled = sp.getBoolean(SETTINGS_SOUND, true)
            Haptics.enabled = sp.getBoolean(SETTINGS_HAPTICS, true)
        } catch (_: Throwable) { /* defaults stay on */ }
        touch.layout.tweaks = try {
            LayoutTweaks.fromJson(Gdx.app.getPreferences(SETTINGS_PREFS).getString(SETTINGS_LAYOUT, ""))
        } catch (_: Throwable) { emptyMap() }
        touch.layout.mirrored = try { // v2.65 左利き配置
            Gdx.app.getPreferences(SETTINGS_PREFS).getBoolean(SETTINGS_LEFTY, false)
        } catch (_: Throwable) { false }
        try { // v2.66: the two hint channels from the settings panel
            val sp = Gdx.app.getPreferences(SETTINGS_PREFS)
            controlHints = sp.getBoolean(SETTINGS_HINTS, true)
            loreHints = sp.getBoolean(SETTINGS_LORE, true)
        } catch (_: Throwable) { /* defaults stay on */ }
        if (startFresh) runStore.clear() // v2.58: タイトルの「はじめから」は前のランを置いていく
        if (startFresh || !tryRestoreRun()) newRun() // v2.33: a saved run resumes where it left off
        if (startInTraining && !simMode) toggleTraining() // v2.58: straight into the simulation
        // v2.60: the boot diagnostic runs once per install (never inside the training sim).
        val tutDone = try { Gdx.app.getPreferences(SETTINGS_PREFS).getBoolean(SETTINGS_TUTORIAL, false) } catch (_: Throwable) { true }
        if (!tutDone && !simMode) tutorial = TutorialController()
        Ambience.setEnabled(Sfx.enabled) // v2.63: same サウンド switch gates the ambient loop
        syncAmbience()
    }

    /** v2.63 生成オーディオ: hum the track this scene wants (space / biome / training sim). */
    private fun syncAmbience() {
        Ambience.play(AmbienceScore.trackFor(gw.worldState.mode, gw.worldState.biome, simMode))
    }

    /** v2.53: enter/exit the old-style combat simulation. The universe's memory, the saved run
     *  and the real player snapshot are all untouched — a simulation leaves no trace but skill. */
    private fun toggleTraining() {
        if (!simMode) {
            simMode = true
            preSimCarry = PlayerCarry.of(gw.world, gw.player, gw.waveState.num)
            worldSeed = TRAINING_SEED
            gw = WorldFactory.create(input, configStore.config, seed = TRAINING_SEED, carry = preSimCarry)
        } else {
            simMode = false
            worldSeed = session.spaceSeed
            gw = WorldFactory.create(input, configStore.config, seed = session.spaceSeed, carry = preSimCarry)
            gw.waveState.num = preSimCarry?.wave ?: 1
            preSimCarry = null
        }
        accumulator = 0f; camInit = false; choosing = false; offered = false; choices = emptyList()
        lastHp = Float.NaN; lastCardId = null; cachedCard = null
        chipsKey = -1; cachedChips = emptyList(); questChipKey = -1; questChip = null
        marketSold.clear(); pendingJump = false
        rewardToast = if (simMode) "旧式戦闘シミュレーション起動 — 成果は持ち出せない" else "訓練環境を終了した"
        rewardToastT = TOAST_TIME
        rebuildMemoryTones()
        syncAmbience() // v2.63: the sim has its own hum
    }

    /** Build (or rebuild) the run and reset per-run screen state (Phase 7 restart). */
    private fun newRun() {
        simMode = false; preSimCarry = null // v2.53: a real restart always leaves the simulation
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
        pendingJump = false
        lastCardId = null; cachedCard = null
        questChipKey = -1; questChip = null
        marketSold.clear()
        // v2.46 遺品回収: the previous death's bundle drifts where you fell (a fresh run rebuilds
        // the same first system, so the spot is real). A surface death washes up near the start.
        relicStore.load()?.let { r ->
            val (sx, sy) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
            val rx = if (r.space) r.x else sx + 380f
            val ry = if (r.space) r.y else sy
            if (r.dust > 0) Pickups.spawn(gw.world, "dust", r.dust, rx, ry)
            if (r.shards > 0) Pickups.spawn(gw.world, "shard", r.shards, rx + 14f, ry)
            relicStore.clear()
            rewardToast = "遺品を検知 — 前回の星屑${r.dust}" + (if (r.shards > 0) "とゲート鍵${r.shards}" else "") + "が漂っている"
            rewardToastT = TOAST_TIME
        }
        rebuildMemoryTones()
        syncAmbience() // v2.63: a fresh run starts back in space
    }

    /** LP v2.30/10c: per-planet memory tints, recomputed only when the memory can have changed. */
    private fun rebuildMemoryTones() {
        memoryTones = session.memory.memories
            .mapValues { (_, s) -> ReturnVisitEffects.memoryTone(s) }
            .filterValues { it != 0 }
    }

    /** Whether a landing/takeoff would actually happen right now (used to gate the fade, 10b). */
    private fun canTransitionNow(): Boolean = if (simMode) {
        false // v2.53: the simulation has no planets worth the name — no landings
    } else if (gw.worldState.mode == WorldMode.SPACE) {
        gw.worldState.landingCandidate != null
    } else {
        playerOnEscapePad()
    }

    /** Land on the hovered planet (SPACE) or take off from the escape pad (SURFACE), carrying run state across. */
    private fun handleLanding() {
        val ws = gw.worldState
        if (ws.mode == WorldMode.SPACE) {
            val cand = ws.landingCandidate ?: return
            tutorial?.onLanded() // v2.60: the first touchdown completes the boot diagnostic
            tryUnlock(Achievement.FIRST_LANDING) // v2.62
            val plan = session.planLanding(cand) // seeds, memory recall and the greeting all decided in one place
            // R2: the remembered society goes INTO the factory, so spawn-time consumers see it from tick 0.
            transitionWorld(
                WorldMode.SURFACE, plan.biome, plan.seed, null, plan.context, plan.society,
                Weather.kindFor(cand.id, plan.biome), // v2.75: the planet's own sky rides in
            )
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
            // v2.72: requests now settle on the spot (依頼の連鎖) — takeoff only hands over the gift.
            val toast = TakeoffReward.toastFor(reward, soc.childKilled)
            rewardToast = toast
            rewardToastT = if (rewardToast != null) TOAST_TIME else 0f
            tutorial?.onTakeoff() // v2.61: lifting off the first star completes the diagnostic
            val (seed, spawn) = session.completeTakeoff(soc) // fold the visit back into memory
            // v2.62 実績: the epithet + restoration milestones settle at takeoff, when memory folds.
            when (Epithet.of(session.memory.memories.values)) {
                "星還し" -> tryUnlock(Achievement.STAR_RETURNER)
                "王殺し" -> tryUnlock(Achievement.KING_SLAYER)
            }
            if (syncPercent() >= 50) tryUnlock(Achievement.SYNC_50)
            if (syncPercent() >= 90) tryUnlock(Achievement.SYNC_90) // v2.68
            if (session.memory.memories.values.any { it.relicClaimed }) tryUnlock(Achievement.RELIC_KEEPER) // v2.68
            if (gw.worldState.questKills == 0) tryUnlock(Achievement.QUIET_VISIT) // v2.70: no hostile fell this visit
            if (session.memory.memories.values.count { it.apexKilled } >= 2) tryUnlock(Achievement.BEAST_HUNTER) // v2.70
            transitionWorld(WorldMode.SPACE, null, seed, spawn) // same system, beside the planet we left
        }
    }

    private fun transitionWorld(
        mode: WorldMode, biome: PlanetBiome?, seed: Long, spawn: Pair<Float, Float>?,
        context: PlanetContext? = null, society: PlanetSocietyState? = null,
        weather: WeatherKind = WeatherKind.CLEAR,
    ) {
        val carry = PlayerCarry.of(gw.world, gw.player, gw.waveState.num)
        worldSeed = seed
        gw = WorldFactory.create(input, configStore.config, seed, mode, biome, carry, spawn, context, society, weather)
        gw.waveState.num = carry.wave
        accumulator = 0f; camInit = false; overlay = Overlay.NONE
        choosing = false; offered = false; choices = emptyList(); lastHp = Float.NaN
        lastCardId = null; cachedCard = null // memory may have changed across the transition → rebuild the scan card
        chipsKey = -1; cachedChips = emptyList()
        questChipKey = -1; questChip = null
        marketSold.clear()
        rebuildMemoryTones()
        syncAmbience() // v2.63: space ↔ surface swap the ambient loop
    }

    /** v2.74 天候: purely cosmetic surface weather — deterministic per planet, pure in time. */
    private fun drawWeather(delta: Float) {
        if (simMode || gw.worldState.mode != WorldMode.SURFACE) return
        val weatherKind = gw.worldState.weather // v2.75: the factory's single source of truth
        if (weatherKind == WeatherKind.CLEAR) return
        weatherT += delta
        val p = Weather.paramsFor(weatherKind)
        hudViewport.apply()
        shapes.projectionMatrix = hudViewport.camera.combined
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        when (weatherKind) { // a faint wash so the whole scene reads the climate
            WeatherKind.RAIN -> shapes.setColor(0.10f, 0.15f, 0.30f, 0.06f)
            WeatherKind.SNOW -> shapes.setColor(0.85f, 0.90f, 1f, 0.045f)
            WeatherKind.ASH -> shapes.setColor(0.25f, 0.18f, 0.12f, 0.06f)
            WeatherKind.DUSTWIND -> shapes.setColor(0.50f, 0.42f, 0.25f, 0.05f)
            WeatherKind.CLEAR -> {}
        }
        shapes.rect(0f, 0f, w, h)
        when (weatherKind) {
            WeatherKind.RAIN -> shapes.setColor(0.60f, 0.75f, 0.95f, 0.35f)
            WeatherKind.SNOW -> shapes.setColor(0.95f, 0.97f, 1f, 0.50f)
            WeatherKind.ASH -> shapes.setColor(0.55f, 0.50f, 0.48f, 0.40f)
            WeatherKind.DUSTWIND -> shapes.setColor(0.75f, 0.68f, 0.50f, 0.30f)
            WeatherKind.CLEAR -> {}
        }
        for (i in 0 until p.count) {
            val (fx, fy) = Weather.pos(i, weatherT, p)
            val x = fx * w + Weather.sway(i, weatherT, p)
            val y = fy * h
            if (p.streak) { // a short trail along the motion of the last ~60ms
                shapes.rectLine(x, y, x - p.driftPerSec * w * 0.06f, y + p.fallPerSec * h * 0.06f, p.size)
            } else {
                shapes.circle(x, y, p.size, 6)
            }
        }
        shapes.end()
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
            !choosing && !gw.gameOver.isOver && !layoutEditing) overlay = PauseFlow.toggle(overlay)
        if (layoutEditing) handleLayoutEdit() // v2.56: the editor swallows all input while open
        if (!layoutEditing && !handleTutorialTaps()) handlePauseTaps() // v2.60: diagnostic taps first

        pollGameplayTouch(
            overlay != Overlay.NONE || fade.blocksInput || layoutEditing ||
                tutorial?.step == TutorialStep.BOOT_PROMPT,
        )
        // v2.33: the I key / INV button toggles the inventory. Unlike pause it does not freeze the
        // sim — the world crawls at INV_TIME_SCALE behind the panel (装備・持物・マップ・セーブ).
        if (input.inventory && !choosing && !gw.gameOver.isOver && !layoutEditing &&
            (overlay == Overlay.NONE || overlay == Overlay.INVENTORY)
        ) {
            overlay = if (overlay == Overlay.INVENTORY) Overlay.NONE else Overlay.INVENTORY
            readingLore = null
        }
        val paused = (overlay != Overlay.NONE && overlay != Overlay.INVENTORY) || layoutEditing ||
            tutorial?.step == TutorialStep.BOOT_PROMPT // sim-freezing overlays (+ the boot prompt)
        val simDelta = delta * if (overlay == Overlay.INVENTORY) INV_TIME_SCALE else 1f
        if (invNoteT > 0f) invNoteT -= delta

        // Living Planets: land on / leave a planet (L key or the touch LAND button). The fade wraps the
        // rebuild: OUT → (world swap behind black) → IN, and gameplay input is ignored meanwhile (10b).
        if (overlay == Overlay.NONE && !choosing && !gw.gameOver.isOver && !fade.blocksInput && input.land) {
            if (canJumpNow()) { // v2.44: standing at the gate with enough shards → jump beats landing
                fade.start(); pendingJump = true
                Sfx.play("takeoff")
            } else if (canTransitionNow()) {
                fade.start()
                Sfx.play(if (gw.worldState.mode == WorldMode.SPACE) "land" else "takeoff")
            }
        }
        // The OUT leg just completed → swap behind black (jump or landing/takeoff).
        if (!paused && fade.update(delta)) { if (pendingJump) performJump() else handleLanding() }

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
        // v2.60: feed the diagnostic's sensors — movement, kills, dust, dash, scan.
        tutorial?.let { t ->
            if (!t.done && !paused) {
                if (!tutPrevPx.isNaN()) t.onMoved(hypot(px - tutPrevPx, py - tutPrevPy))
                tutPrevPx = px; tutPrevPy = py
                if (gw.gameOver.kills > tutPrevKills) t.onKill()
                tutPrevKills = gw.gameOver.kills
                val dustNow = with(gw.world) { gw.player[Materials].dust }
                if (tutPrevDust in 0 until dustNow) t.onDustPicked()
                tutPrevDust = dustNow
                if (input.dash) t.onDash()
                if (cachedCard != null) t.onScan()
                // v2.61 Layer 2: the first surface — observe, the child, the star writing it down.
                if (gw.worldState.mode == WorldMode.SURFACE) {
                    t.onSurfaceTick(simDelta)
                    val soc = gw.worldState.society
                    if (soc.childHarmed || soc.childKilled) t.onSocietyEvent(protected = false)
                    else if (soc.predatorKilledNearChild || soc.mercy > 0.05f) t.onSocietyEvent(protected = true)
                }
                if (t.done) finishTutorial()
            }
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
        // v2.45: the sim leaves one-shot announcements (event waves, boss telegraph, bounty kill)
        // on the WaveState; the screen turns them into the same toast the send-off gift uses.
        if (!paused) gw.waveState.announce?.let {
            rewardToast = it; rewardToastT = TOAST_TIME
            gw.waveState.announce = null
            Sfx.play("scan")
            if (it.contains("賞金首を討ち取った") && !simMode) tryUnlock(Achievement.BOUNTY_HUNTER) // v2.62
        }
        // v2.62 実績: surviving deep into the desync (the real run only).
        if (!paused && !simMode && gw.waveState.num >= 15) tryUnlock(Achievement.DEEP_SURGE)
        // v2.70 実績: a real hoard of memory fragments, held all at once.
        if (!paused && !simMode && with(gw.world) { gw.player[Materials].dust } >= 500) {
            tryUnlock(Achievement.DUST_RICH)
        }
        // v2.51: a wreck the drifter closes in on broadcasts its distress log — once per wreck.
        // v2.66 世界観ヒント OFF leaves the log unread (and unmarked, so it can speak later).
        if (!paused && !simMode && loreHints && gw.worldState.mode == WorldMode.SPACE) {
            val (wpx, wpy) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
            gw.worldState.wrecks.forEachIndexed { i, w ->
                if (i !in gw.worldState.wreckLogShown && hypot(wpx - w.first, wpy - w.second) < Tuning.TILE * 3f) {
                    gw.worldState.wreckLogShown.add(i)
                    rewardToast = WreckLog.lineFor(worldSeed, i)
                    rewardToastT = TOAST_TIME
                    Sfx.play("scan")
                }
            }
        }
        // v2.48 惑星サーバー: standing before the memory core makes it speak — once per landing,
        // into the surface event feed (the same channel the society's memory already uses).
        if (!paused && gw.worldState.mode == WorldMode.SURFACE && (!gw.worldState.coreVisited || !gw.worldState.coreLogShown)) {
            gw.worldState.memoryCore?.let { core ->
                val (cpx, cpy) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
                if (hypot(cpx - core.first, cpy - core.second) < Tuning.TILE * 2f) {
                    gw.worldState.coreVisited = true // v2.68: the CORE quest counts the visit itself
                    if (!loreHints || gw.worldState.coreLogShown) return@let
                    gw.worldState.coreLogShown = true
                    gw.worldState.recentEvents.add(
                        PlanetEvent(
                            MemoryCoreLog.lineFor(session.landedPlanetId ?: 0L, gw.worldState.biome, session.spaceSeed.toInt()),
                            EventKind.NEUTRAL,
                        ),
                    )
                    Sfx.play("scan")
                }
            }
        }
        // v2.72 依頼の連鎖: a fulfilled request settles ON THE SPOT — the star pays, thanks you
        // through the event feed, and (up to 3 times a visit) hands over the next thing it needs.
        // A star whose child you killed stops asking, the same rule as the send-off gift.
        if (!paused && !simMode && gw.worldState.mode == WorldMode.SURFACE) {
            val ws = gw.worldState
            val pid = session.landedPlanetId
            val qb = ws.biome
            if (pid != null && qb != null && ws.questStage < PlanetQuest.CHAIN && !ws.society.childKilled) {
                val q = PlanetQuest.questFor(pid, qb, ws.questStage)
                if (questProgress(q.kind, ws) >= q.target) {
                    with(gw.world) { gw.player[Materials].dust += q.rewardDust }
                    ws.questStage++
                    ws.questBaseKills = ws.questKills; ws.questBaseElites = ws.questElites
                    ws.questBaseDust = ws.questDust; ws.questBasePredators = ws.questPredators
                    ws.questBaseTime = ws.questTime
                    questChipKey = -1 // the chip rebuilds for the next request
                    ws.recentEvents.add(
                        PlanetEvent(
                            if (ws.questStage < PlanetQuest.CHAIN) "依頼を果たした +${q.rewardDust}屑 — 次の頼みが届いた"
                            else "依頼を果たした +${q.rewardDust}屑 — この星の頼みはすべて済んだ",
                            EventKind.MERCY,
                        ),
                    )
                    Sfx.play("levelup")
                    tryUnlock(Achievement.QUEST_PATRON)
                    if (ws.questStage >= PlanetQuest.CHAIN) tryUnlock(Achievement.CHAIN_PATRON) // v2.75
                    when (q.kind) { // v2.70: the quiet professions get their own lines
                        QuestKind.PROTECT -> tryUnlock(Achievement.GUARDIAN)
                        QuestKind.OBSERVE -> tryUnlock(Achievement.OBSERVER)
                        else -> {}
                    }
                }
            }
        }
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

        drawWeather(delta) // v2.74: the planet's climate, between the world and the HUD
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
                // v2.53: a death inside the simulation is a simulated death — no REAL records.
                // v2.62: but the training hall keeps its own scoreboard.
                newBest = if (!simMode) {
                    Scores.record(gw.waveState.num, gw.gameOver.kills)
                } else {
                    Scores.recordSim(gw.waveState.num, gw.gameOver.kills)
                }
                session.persist() // LP v2.28: a death checkpoints the universe's memory too
                if (!simMode) runStore.clear() // v2.33: death consumes the saved run (roguelite)
                // v2.46 遺品: death leaves half the dust and every gate shard where you fell —
                // the NEXT run can fly back and reclaim the bundle.
                if (!simMode) {
                    val relic = with(gw.world) {
                        val t = gw.player[Transform]; val m = gw.player[Materials]
                        DeathRelic.of(t.x, t.y, m.dust, m.shards, gw.worldState.mode == WorldMode.SPACE)
                    }
                    if (relic != null) relicStore.save(relic) else relicStore.clear()
                }
                Sfx.play("dead"); Haptics.buzz(140)
            }
            val goHit = if (tapped) {
                Modals.hitModal(Modals.gameOverButtons(hudViewport.worldWidth, hudViewport.worldHeight), tapX, tapY)
            } else null
            if (Gdx.input.isKeyJustPressed(Input.Keys.R) || goHit == 0) { newRun(); return false }
            if (goHit == 1) { (Gdx.app.applicationListener as? App)?.showTitle(); return false } // v2.59
        } else if (paused) {
            accumulator = 0f // freeze the sim while paused; skip stepping & the upgrade flow
        } else {
            updateUpgradeFlow(delta)
            trackPlayerHitShake()
        }
        prevOver = gw.gameOver.isOver
        // v2.67 状況反応: combat heat breathes the pulse in; the memory core, the shimmer.
        ambHeat.tick(delta)
        val shimmerLevel = if (gw.worldState.mode == WorldMode.SURFACE) {
            gw.worldState.memoryCore?.let { core ->
                val (apx, apy) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
                AmbienceScore.shimmerFor(hypot(apx - core.first, apy - core.second), Tuning.TILE)
            } ?: 0f
        } else 0f
        Ambience.setLayers(ambHeat.value, shimmerLevel)
        gw.fx.update(delta)
        animTime += delta
        if (!gw.gameOver.isOver && !choosing && !paused) runTime += delta
        // v2.69 観測依頼: surface time only ticks while actually playing the surface.
        if (!gw.gameOver.isOver && !choosing && !paused && gw.worldState.mode == WorldMode.SURFACE) {
            gw.worldState.questTime += delta
        }
        return true
    }

    /** HUD (screen space) — P2 live HUD delegated to render/Hud (geometry from ui/HudLayout). */
    private fun drawHud(paused: Boolean, sta: Float, staMax: Float, overheat: Boolean) {
        hudViewport.apply()
        val hudW = hudViewport.worldWidth; val hudH = hudViewport.worldHeight
        val blocks = with(gw.world) { gw.player[Materials].blocks }
        val dust = with(gw.world) { gw.player[Materials].dust }
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
            runTime, gw.gameOver.kills, blocks, dust,
            simMode = simMode,
        )
        val tut = tutorial
        if (tut != null && !tut.done) drawTutorial(hudW, hudH) else drawObjectiveHint(paused, hudW, hudH)
        if (tut == null) drawOnboarding(paused, hudW) // v2.60: the diagnostic supersedes onboarding
        // Surface event feed (LP v2.24): drawn whenever on a surface; aging freezes with the sim while paused.
        if (gw.worldState.mode == WorldMode.SURFACE) Hud.eventFeed(batch, font, hudViewport, gw.worldState.recentEvents)

        if (layoutEditing) { // v2.56: the editor shows EVERY button, a halo on the selection, and its toolbar
            val landLabel = if (gw.worldState.mode == WorldMode.SURFACE) "発進" else "着陸"
            TouchOverlay.draw(shapes, batch, font, hudViewport, touch, landLabel, controlSwap, showAll = true, editTarget = editTarget)
            Hud.buttonRow(shapes, batch, font, hudViewport, Modals.layoutEditButtons(hudW, hudH))
            Hud.hintPanel(
                shapes, batch, font, hudViewport,
                listOf("ボタン配置エディタ", "ドラッグで移動　選択して[大きく/小さく]　[完了]で保存"),
                hudH - HINT_TOP,
            )
        } else if (touchEnabled && overlay == Overlay.NONE && !choosing && !gw.gameOver.isOver) {
            val landLabel = if (gw.worldState.mode == WorldMode.SURFACE) "発進" else "着陸"
            TouchOverlay.draw(shapes, batch, font, hudViewport, touch, landLabel, controlSwap)
        }
        if (overlay == Overlay.NONE && !choosing && !gw.gameOver.isOver && !layoutEditing) Hud.pauseButton(shapes, hudViewport, Modals.pauseButton(hudW, hudH))
        if (choosing) {
            val cfg = configStore.config.upgrades
            Hud.upgradeCards(
                shapes, batch, font, hudViewport, gw.waveState.num,
                Modals.upgradeCards(hudW, hudH, choices.size),
                choices.map { it.name }, choices.map { Upgrades.desc(it, cfg) },
            )
        }
        if (gw.gameOver.isOver) {
            val bestText = when {
                simMode && newBest -> "訓練記録更新！  ウェーブ ${Scores.simBestWave}" // v2.62
                simMode -> "訓練記録  ウェーブ ${Scores.simBestWave}  撃破 ${Scores.simBestKills}"
                newBest -> "自己ベスト更新！  汚染深度 ${Scores.bestWave}"
                else -> "ベスト  汚染深度 ${Scores.bestWave}  撃破 ${Scores.bestKills}"
            }
            val mins = (runTime / 60f).toInt(); val secs = (runTime % 60f).toInt()
            Hud.gameOver(
                shapes, batch, font, Fonts.title, hudViewport,
                gw.waveState.num, gw.gameOver.kills, bestText, Modals.gameOverButtons(hudW, hudH),
                summary = listOf( // v2.59: the run's obituary
                    "第${session.spaceSeed}星系　星間同期復旧 ${syncPercent()}%",
                    "呼び名 『${Epithet.of(session.memory.memories.values)}』　航行時間 %d:%02d".format(mins, secs),
                ),
            )
        }
        if (overlay == Overlay.INVENTORY) drawInventory()
        if (overlay == Overlay.PAUSE) Hud.pause(shapes, batch, font, Fonts.title, hudViewport, Modals.pauseButtons(hudW, hudH, pauseHasMemory(), simMode))
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

    /** v2.47 オンボーディング: the first run's four timed hints, low on the screen, then never again. */
    private fun drawOnboarding(paused: Boolean, hudW: Float) {
        if (!controlHints) return // v2.66 操作ヒント OFF — the walkthrough stays quiet
        if (onboardDone || paused || choosing || gw.gameOver.isOver || overlay != Overlay.NONE) return
        val line = Onboarding.lineFor(runTime, touchEnabled)
        if (line == null) {
            if (runTime >= Onboarding.END) {
                onboardDone = true
                try {
                    val p = Gdx.app.getPreferences(SETTINGS_PREFS)
                    p.putBoolean(SETTINGS_ONBOARD, true)
                    p.flush()
                } catch (_: Throwable) { /* persist best-effort */ }
            }
            return
        }
        Hud.hintPanel(shapes, batch, font, hudViewport, listOf(line), 236f)
    }

    /** Living Planets: surface exploration objective, or the pre-landing scan card in space (HUD space). */
    private fun drawObjectiveHint(paused: Boolean, hudW: Float, hudH: Float) {
        val ws = gw.worldState
        // SPACE: a latched landing candidate shows the scan card instead of the old one-line hint (LP v2.23).
        if (ws.mode == WorldMode.SPACE) {
            // v2.54: every space hint (toast / sim banner / idle guidance) lives on ONE glass
            // panel BELOW the top HUD band — no more text colliding with the bars and badges.
            val busy = paused || choosing || gw.gameOver.isOver
            val toast = if (rewardToastT > 0f && !busy) rewardToast else null
            if (simMode) { // v2.53: the simulation is combat only — say so, plainly
                if (!busy) {
                    Hud.hintPanel(
                        shapes, batch, font, hudViewport,
                        listOfNotNull(toast, "訓練環境 — 模擬戦闘のみ（ポーズから終了）"), hudH - HINT_TOP,
                    )
                }
                return
            }
            val cand = ws.landingCandidate ?: run {
                lastCardId = null; cachedCard = null
                // v2.38/39: even with no planet latched, space tells you HOW landing works AND
                // points at the nearest planet with a live distance — no more searching blind.
                if (!busy) {
                    // v2.66 操作ヒント OFF drops the how-to line; the nav/gate compasses stay.
                    val idle = if (!controlHints) null
                    else if (touchEnabled) "惑星に近づくとカードが出る　惑星をタップで着陸" else "惑星に近づいて [L] で着陸"
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
                    // v2.44: the gate line — shard progress while collecting, a live compass once ready.
                    val shards = with(gw.world) { gw.player[Materials].shards }
                    val gateLine = ws.gate?.let { g ->
                        if (shards < gateNeed()) {
                            "ゲート鍵 $shards/${gateNeed()}（強敵が落とす）"
                        } else {
                            val dx = g.first - ppx; val dy = g.second - ppy
                            val dist = hypot(dx, dy).toInt()
                            val arrow = if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                                if (dx >= 0f) "→" else "←"
                            } else {
                                if (dy >= 0f) "↓" else "↑"
                            }
                            "ジャンプゲート $arrow $dist　接近して跳躍"
                        }
                    }
                    Hud.hintPanel(
                        shapes, batch, font, hudViewport,
                        listOfNotNull(toast, idle, nav, gateLine), hudH - HINT_TOP,
                    )
                }
                return
            }
            if (busy) return
            if (cand.id != lastCardId) { // rebuild only when the candidate changes (FR-1.6)
                cachedCard = PlanetScan.cardFor(cand, session.memory.knows(cand.id), session.memory.recall(cand.id))
                lastCardId = cand.id
                Sfx.play("scan") // 10a: a fresh scan pings once per newly latched planet
            }
            cachedCard?.let {
                val hint = when { // v2.34/38: planet or card = the landing button (v2.66: mutable)
                    !controlHints -> ""
                    touchEnabled -> "惑星かこのカードをタップで着陸"
                    else -> "[L] 着陸"
                }
                Hud.planetScanCard(shapes, batch, font, Fonts.title, hudViewport, it, hint)
                // v2.54: a live toast slots under the card instead of over the HUD.
                if (toast != null) {
                    Hud.hintPanel(shapes, batch, font, hudViewport, listOf(toast), HudLayout.planetCard(hudW, hudH, it.lines.size).y - 8f)
                }
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
        // v2.45: the planet's standing request (星の依頼) rides the same chip row with live progress.
        val chips = (if (biome != null) surfaceChips(biome, ws, elites) else emptyList()) +
            listOfNotNull(questChip(ws))
        if (!paused && !choosing) {
            val lines = listOf(hint) + (if (chips.isEmpty()) emptyList() else listOf(chips.joinToString("　　")))
            Hud.hintPanel(shapes, batch, font, hudViewport, lines, hudH - HINT_TOP)
        }
    }

    /** v2.45/68 星の依頼: this visit's progress toward a request of [kind]. */
    private fun questProgress(kind: QuestKind, ws: WorldState): Int = when (kind) {
        // v2.72 連鎖: each stage counts from the snapshot taken when the previous one settled.
        QuestKind.ELITES -> ws.questElites - ws.questBaseElites
        QuestKind.KILLS -> ws.questKills - ws.questBaseKills
        QuestKind.DUST -> ws.questDust - ws.questBaseDust
        QuestKind.CORE -> if (ws.coreVisited) 1 else 0
        QuestKind.PROTECT -> ws.questPredators - ws.questBasePredators // v2.69
        QuestKind.OBSERVE -> (ws.questTime - ws.questBaseTime).toInt() // v2.69: whole seconds
    }

    /** v2.45/72 星の依頼: the chip shows the CURRENT link of the chain, or the satisfied star. */
    private fun questChip(ws: WorldState): String? {
        val pid = session.landedPlanetId ?: return null
        val b = ws.biome ?: return null
        if (ws.questStage >= PlanetQuest.CHAIN) return "依頼 完了 — この星は満ちている"
        val q = PlanetQuest.questFor(pid, b, ws.questStage)
        val prog = questProgress(q.kind, ws)
        val key = prog.coerceAtMost(q.target) + ws.questStage * 1000
        if (key != questChipKey) {
            questChip = "依頼${ws.questStage + 1}/${PlanetQuest.CHAIN}　${q.line}　$prog/${q.target}"
            questChipKey = key
        }
        return questChip
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
        if (!lastHp.isNaN() && hp < lastHp - 0.01f) {
            gw.fx.addShake(0.18f, 6f); Sfx.play("hit"); Haptics.buzz(25)
            ambHeat.onPlayerHit() // v2.67: taking fire quickens the pulse
        }
        lastHp = hp
        val kills = gw.gameOver.kills
        if (kills > lastKills) { Sfx.play("kill"); ambHeat.onKill() }
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

    /** v2.44: did this frame's tap land on the jump gate itself (world space)? */
    private fun tapHitsGate(): Boolean {
        val g = gw.worldState.gate ?: return false
        tmpTap.set(rawTapX, rawTapY, 0f)
        worldViewport.unproject(tmpTap)
        return hypot(tmpTap.x - g.first, tmpTap.y - g.second) < GATE_TAP_R
    }

    /** v2.44: the player is close enough to the gate ring to enter it. */
    private fun nearGate(): Boolean {
        val g = gw.worldState.gate ?: return false
        val (ppx, ppy) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        return hypot(ppx - g.first, ppy - g.second) < Tuning.TILE * 4f
    }

    /** v2.52 同期復旧度: derived progression — systems jumped + planets known + planets trusting. */
    private fun syncPercent(): Int = SyncRestoration.percent(session.spaceSeed.toInt(), session.memory.memories.values)

    /** How many shards THIS jump costs (a 60%+ restored network recognizes the keeper). */
    private fun gateNeed(): Int = SyncRestoration.gateShardsNeeded(syncPercent())

    /** v2.44: a jump is possible right now — in space, at the gate, holding enough gate shards. */
    private fun canJumpNow(): Boolean = !simMode && gw.worldState.mode == WorldMode.SPACE && nearGate() &&
        with(gw.world) { gw.player[Materials].shards } >= gateNeed()

    /**
     * v2.44 ジャンプゲート: spend the shards and move the whole run to the NEXT star system.
     * A fresh spaceSeed regenerates planets/enemies/the gate; the player keeps everything they
     * carry (PlayerCarry hauls gear, materials and wave across, so difficulty keeps climbing).
     */
    private fun performJump() {
        pendingJump = false
        with(gw.world) {
            val m = gw.player[Materials]
            m.shards = (m.shards - gateNeed()).coerceAtLeast(0)
        }
        session.spaceSeed += 1
        session.surfSeed = session.spaceSeed * 100
        session.landedPlanetId = null
        session.returnSpawn = null
        transitionWorld(WorldMode.SPACE, null, session.spaceSeed, null)
        with(gw.world) { val h = gw.player[Health]; h.hp = h.hpMax } // the jump mends the hull
        tryUnlock(Achievement.FIRST_JUMP) // v2.62
        if (syncPercent() >= 50) tryUnlock(Achievement.SYNC_50)
        if (syncPercent() >= 90) tryUnlock(Achievement.SYNC_90) // v2.68
        if (session.spaceSeed >= 3) tryUnlock(Achievement.SYSTEM_3) // v2.70
        rewardToast = "第${session.spaceSeed}星系に到達した"
        rewardToastT = TOAST_TIME
        Sfx.play("takeoff")
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
                when (PauseFlow.action(Modals.hitModal(Modals.pauseButtons(w, h, hasMemory, simMode), tapX, tapY) ?: -1, hasMemory)) {
                    PauseAction.RESUME -> overlay = Overlay.NONE
                    PauseAction.RESTART -> { newRun(); overlay = Overlay.NONE }
                    PauseAction.HELP -> overlay = Overlay.HELP
                    PauseAction.MEMORY -> overlay = Overlay.MEMORY
                    PauseAction.SIM -> { toggleTraining(); overlay = Overlay.NONE } // v2.53
                    PauseAction.TITLE -> { // v2.58: auto-save the real run, then the front door
                        if (!simMode && !gw.gameOver.isOver) saveRun()
                        (Gdx.app.applicationListener as? App)?.showTitle()
                        return
                    }
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
                } else if (touchEnabled && !fade.blocksInput && canJumpNow() && tapHitsGate()) {
                    // v2.44: the gate itself is the jump button — same pattern as planet-tap landing.
                    fade.start(); pendingJump = true
                    Sfx.play("takeoff")
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
                // v2.56: the layout-editor strip sits just above the control toggle.
                if (InventoryLayout.layoutEditToggle(w, h).contains(tapX, tapY)) {
                    layoutEditing = true; editTarget = null; overlay = Overlay.NONE; readingLore = null
                    return
                }
                if (InventoryLayout.controlToggle(w, h).contains(tapX, tapY)) { toggleControlSwap(); return }
                val row = Modals.hitModal(InventoryLayout.slotRows(w, h), tapX, tapY) ?: return
                GearOps.cycleSlot(gw.world, gw.player, InventoryLayout.SLOT_ORDER[row])
            }
            InvTab.ITEMS -> handleItemsTap(w, h)
            InvTab.MARKET -> handleMarketTap(w, h)
            InvTab.SAVE -> if (InventoryLayout.saveButton(w, h).contains(tapX, tapY)) {
                if (simMode) { invNote = "訓練環境ではセーブできない"; invNoteT = SAVED_NOTE_TIME } else saveRun()
            }
            InvTab.MAP -> {}
            InvTab.LOG -> {} // v2.46: the logbook is read-only
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
            // v2.47 合成: tapping a stacked weapon row (×2以上) hones two copies into one "+1".
            // Other equipment swaps in from the EQUIP tab's slots, not from the list.
            ItemKind.RANGED_WEAPON, ItemKind.MELEE_WEAPON -> {
                if (groups[idx].second >= 2 && GearCraft.craftable(item)) {
                    repeat(2) {
                        val at = gear.backpack.indexOfFirst { it.id == item.id }
                        if (at >= 0) gear.backpack.removeAt(at)
                    }
                    val up = GearCraft.honed(item)
                    gear.backpack.add(up)
                    tryUnlock(Achievement.FIRST_HONE) // v2.62
                    if (GearCraft.level(up.id) >= GearCraft.MAX_LEVEL) tryUnlock(Achievement.HONED_MAX) // v2.68
                    invNote = "合成: ${item.name} ×2 → ${up.name}"; invNoteT = SAVED_NOTE_TIME
                    Sfx.play("levelup")
                }
            }
            ItemKind.THRUSTER, ItemKind.ARMOR, ItemKind.ACCESSORY -> {}
        }
    }

    /** The inventory overlay's view model → Hud.inventory (v2.33). */
    /** v2.60 起動診断: the current prompt panel (+ boot choice / the always-there skip). */
    private fun drawTutorial(hudW: Float, hudH: Float) {
        val t = tutorial ?: return
        if (choosing || gw.gameOver.isOver || overlay != Overlay.NONE || layoutEditing) return
        Hud.hintPanel(shapes, batch, font, hudViewport, t.prompt(touchEnabled), hudH - HINT_TOP)
        if (t.step == TutorialStep.BOOT_PROMPT) {
            Hud.buttonRow(shapes, batch, font, hudViewport, Modals.tutorialBootButtons(hudW, hudH))
        } else {
            Hud.buttonRow(shapes, batch, font, hudViewport, listOf(Modals.tutorialSkipButton(hudW, hudH)))
        }
    }

    /** v2.60: diagnostic taps — boot choice or the skip button. True = the tap was consumed. */
    private fun handleTutorialTaps(): Boolean {
        val t = tutorial ?: return false
        if (!tapped || overlay != Overlay.NONE || choosing || gw.gameOver.isOver) return false
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        if (t.step == TutorialStep.BOOT_PROMPT) {
            when (Modals.hitModal(Modals.tutorialBootButtons(w, h), tapX, tapY)) {
                0 -> t.begin()
                1 -> { t.skip(); finishTutorial() }
            }
            return true // the boot prompt owns every tap while it is up
        }
        if (Modals.tutorialSkipButton(w, h).contains(tapX, tapY)) {
            t.skip(); finishTutorial()
            return true
        }
        return false
    }

    /** v2.62 実績: unlock + one-line toast the first time only. */
    private fun tryUnlock(a: Achievement) {
        if (Achievements.unlock(a)) {
            rewardToast = "実績解除『${a.title}』 — ${a.desc}"
            rewardToastT = TOAST_TIME
            Sfx.play("levelup")
        }
    }

    /** v2.60: same ending either way — the reward never depends on finishing vs skipping. */
    private fun finishTutorial() {
        val t = tutorial ?: return
        with(gw.world) { gw.player[Materials].dust += TutorialController.REWARD_DUST }
        rewardToast = t.completionToast()
        rewardToastT = TOAST_TIME
        onboardDone = true
        try {
            val p = Gdx.app.getPreferences(SETTINGS_PREFS)
            p.putBoolean(SETTINGS_TUTORIAL, true)
            p.putBoolean(SETTINGS_ONBOARD, true)
            p.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
        tutorial = null
        Sfx.play("levelup")
    }

    /** v2.56 ボタン配置エディタ: drag any button to move it; the toolbar resizes/resets/saves. */
    private fun handleLayoutEdit() {
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        val toolbar = Modals.layoutEditButtons(w, h)
        if (tapped) {
            when (Modals.hitModal(toolbar, tapX, tapY)) {
                0 -> editTarget?.let { nudgeScale(it, +0.1f) }
                1 -> editTarget?.let { nudgeScale(it, -0.1f) }
                2 -> { touch.layout.tweaks = emptyMap(); editTarget = null; saveLayoutTweaks() }
                3 -> { saveLayoutTweaks(); layoutEditing = false; layoutDragging = false; return }
                else -> {}
            }
        }
        if (Gdx.input.isTouched(0)) {
            tmpTap.set(Gdx.input.getX(0).toFloat(), Gdx.input.getY(0).toFloat(), 0f)
            hudViewport.unproject(tmpTap)
            val x = tmpTap.x; val y = tmpTap.y
            if (!layoutDragging && Modals.hitModal(toolbar, x, y) == null) {
                val l = touch.layout
                val hit = l.all().firstOrNull { hypot(x - l.centerX(it), y - l.centerY(it)) <= l.radiusOf(it) + 8f }
                if (hit != null) { layoutDragging = true; editTarget = hit }
            }
            if (layoutDragging) editTarget?.let { b ->
                val cur = touch.layout.tweaks[b]
                setTweak(b, x / w, y / h, cur?.scale ?: 1f)
            }
        } else {
            layoutDragging = false
        }
    }

    private fun nudgeScale(b: TouchButton, d: Float) {
        val cur = touch.layout.tweaks[b]
        val fx = cur?.fx ?: (touch.layout.centerX(b) / hudViewport.worldWidth)
        val fy = cur?.fy ?: (touch.layout.centerY(b) / hudViewport.worldHeight)
        setTweak(b, fx, fy, (cur?.scale ?: 1f) + d)
    }

    private fun setTweak(b: TouchButton, fx: Float, fy: Float, scale: Float) {
        touch.layout.tweaks = touch.layout.tweaks + (b to LayoutTweaks.sanitize(ButtonTweak(fx, fy, scale)))
    }

    private fun saveLayoutTweaks() {
        try {
            val prefs = Gdx.app.getPreferences(SETTINGS_PREFS)
            prefs.putString(SETTINGS_LAYOUT, LayoutTweaks.toJson(touch.layout.tweaks))
            prefs.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
    }

    private fun drawInventory() {
        val gear = with(gw.world) { gw.player[Gear] }
        val slotTexts = InventoryLayout.SLOT_ORDER.mapIndexed { i, slot ->
            "${InventoryLayout.SLOT_LABELS[i]}：${gear.loadout.get(slot)?.name ?: "（なし）"}"
        }
        // The backpack, grouped so duplicates read as ×N; a marker tells apart what a tap does (v2.34).
        val itemLines = ItemCatalog.grouped(gear.backpack).map { (d, n) ->
            val count = if (n > 1) "　×$n" else ""
            // v2.47 合成: a stacked, still-honable weapon advertises the merge its tap performs.
            val craft = if (n >= 2 && GearCraft.craftable(d)) "【合成可】" else ""
            when (d.kind) {
                ItemKind.CONSUMABLE -> "【使】${d.name}$count　─　${d.desc}"
                ItemKind.LORE -> "【読】『${d.name}』$count　─　${d.desc}"
                else -> if (d.desc.isEmpty()) "$craft${d.name}$count" else "$craft${d.name}$count　─　${d.desc}"
            }
        }
        val (ptx, pty) = with(gw.world) {
            val t = gw.player[Transform]
            floor(t.x / Tuning.TILE).toInt() to floor(t.y / Tuning.TILE).toInt()
        }
        val (marketLines, marketFooter) = marketView()
        Hud.inventory(
            shapes, batch, font, Fonts.title, hudViewport, invTab,
            slotTexts, itemLines, gw.visited, ptx, pty,
            if (invNoteT > 0f) invNote else null,
            loreTitle = readingLore?.name, loreLines = readingLore?.lore?.split("\n") ?: emptyList(),
            controlLabel = "操作: " + (if (controlSwap) "銃=ボタン / 近接=右スティック" else "近接=ボタン / 銃=右スティック") + "　(タップで入替)",
            marketLines = marketLines, marketFooter = marketFooter,
            logLines = if (invTab == InvTab.LOG) logbookLines() else emptyList(),
            layoutEditLabel = "ボタン配置を編集　(タップで開く)", // v2.56
        )
    }

    /** v2.46 航海日誌: the run's present tense + all-time bests + what each visited star remembers. */
    private fun logbookLines(): List<String> {
        val (dust, shards) = with(gw.world) {
            val m = gw.player[Materials]; m.dust to m.shards
        }
        val planetLines = session.memory.memories.entries.take(10).map { (id, s) ->
            val facts = SocietyMemorySummary.factLines(s)
            "星$id　" + (facts.firstOrNull()?.first ?: "記憶は薄い")
        }
        return Logbook.lines(
            session.spaceSeed.toInt(), gw.waveState.num, gw.gameOver.kills, dust, shards,
            Scores.bestWave, Scores.bestKills, planetLines,
            epithet = Epithet.of(session.memory.memories.values),
            stability = DesyncGauge.stability(gw.waveState.num),
            syncPercent = syncPercent(),
        ) + Achievements.logLines() // v2.62: the service record closes the book
    }

    /** v2.43 市: rows + footer for the MARKET tab. A hostile world's stalls are simply shut. */
    private fun marketView(): Pair<List<String>, String> {
        val ws = gw.worldState
        val planetId = session.landedPlanetId
        if (ws.mode != WorldMode.SURFACE || planetId == null) {
            return emptyList<String>() to "市は惑星の地表でのみ開かれる"
        }
        if (!Market.isOpen(ws.society.hostility)) {
            return emptyList<String>() to "この星はあなたと取引しない"
        }
        val dust = with(gw.world) { gw.player[Materials].dust }
        val lines = Market.stockFor(planetId).mapIndexed { i, item ->
            if (i in marketSold) "─ 売約済 ─"
            else "${'$'}{item.name}　【${'$'}{Market.priceFor(item, ws.society.mercy)}屑】"
        }
        return lines to "所持 星屑 ${'$'}dust　行をタップで購入"
    }

    /** v2.43: buy the tapped stall slot if it's still there and the dust covers it. */
    private fun handleMarketTap(w: Float, h: Float) {
        val ws = gw.worldState
        val planetId = session.landedPlanetId ?: return
        if (ws.mode != WorldMode.SURFACE || !Market.isOpen(ws.society.hostility)) return
        val stock = Market.stockFor(planetId)
        val idx = Modals.hitModal(InventoryLayout.marketRows(w, h, stock.size), tapX, tapY) ?: return
        if (idx in marketSold) return
        val item = stock[idx]
        val price = Market.priceFor(item, ws.society.mercy)
        with(gw.world) {
            val mats = gw.player[Materials]
            if (mats.dust < price) {
                invNote = "星屑が足りない（${'$'}price 必要）"; invNoteT = SAVED_NOTE_TIME
                return
            }
            mats.dust -= price
            gw.player[Gear].backpack.add(item)
            marketSold.add(idx)
            invNote = "${'$'}{item.name} を購入した（-${'$'}price屑）"; invNoteT = SAVED_NOTE_TIME
        }
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
                dust = gw.player[Materials].dust,
                shards = gw.player[Materials].shards,
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
            weather = if (mode == WorldMode.SURFACE && dto.landedPlanetId != null && biome != null) {
                Weather.kindFor(dto.landedPlanetId, biome) // v2.75: same sky after a restore
            } else WeatherKind.CLEAR,
        )
        gw.waveState.num = dto.wave
        with(gw.world) {
            val hlt = gw.player[Health]
            hlt.hpMax = dto.hpMax; hlt.hp = dto.hp.coerceIn(0.1f, dto.hpMax)
            gw.player[Stamina].value = dto.stamina
            val ammo = gw.player[Ammo]
            ammo.ammo9 = dto.ammo9; ammo.ammo12 = dto.ammo12; ammo.ammoBeam = dto.ammoBeam; ammo.ammoNade = dto.ammoNade
            gw.player[Materials].blocks = dto.blocks
            gw.player[Materials].dust = dto.dust
            gw.player[Materials].shards = dto.shards
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
        marketSold.clear()
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
