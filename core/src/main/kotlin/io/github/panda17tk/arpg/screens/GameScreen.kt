package io.github.panda17tk.arpg.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
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
import io.github.panda17tk.arpg.item.Trader
import io.github.panda17tk.arpg.item.TraderGoodKind
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
import io.github.panda17tk.arpg.save.Challenge
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
import io.github.panda17tk.arpg.sim.SystemTrait
import io.github.panda17tk.arpg.sim.SystemTraits
import io.github.panda17tk.arpg.sim.WaveEvent
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
import io.github.panda17tk.arpg.ui.TraderPanel
import io.github.panda17tk.arpg.ui.TransitionFade
import io.github.panda17tk.arpg.upgrade.Upgrade
import io.github.panda17tk.arpg.upgrade.Upgrades
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.pow

/** Half-extent of the toroidal box the drifting debris wraps within (matches WorldFactory's seed range). */
internal const val DRIFT_RANGE = 1400f
internal const val REMEMBERED_TIME = 5f // seconds the HUD greets a returning player on a remembered planet
internal const val TOAST_TIME = 3f      // seconds the takeoff send-off toast rides the space HUD (LP v2.29)
internal const val INV_TIME_SCALE = 0.01f // v2.33: the world crawls at this speed behind the inventory
internal const val PLANET_TAP_PAD = 48f   // v2.38: extra world-px around a planet that still counts as tapping it
// v2.44/v2.52: base gate-shard cost; a well-restored network (SyncRestoration) asks one less.
internal const val GATE_TAP_R = 96f       // v2.44: world-px radius that counts as tapping the jump gate
internal const val TRAINING_SEED = 4242L   // v2.53: the simulation always rebuilds the same arena
internal const val HINT_TOP = 138f          // v2.54: hint panels start below the top HUD band
internal const val BOSSBAR_RANGE = 680f     // v2.88: a heavy inside this range owns the boss bar
internal const val SELL_UNDO_TIME = 8f     // v2.118 戻す: how long the last sale can be taken back
internal const val SETTINGS_PREFS = "drift-settings" // v2.39: device-level settings (not run state)
internal const val SETTINGS_SWAP = "controlSwap"
internal const val SETTINGS_ONBOARD = "onboardDone" // v2.47: the first-run walkthrough ran once
internal const val SETTINGS_LAYOUT = "buttonLayout" // v2.56: the layout editor's saved tweaks
internal const val SETTINGS_SOUND = "soundOn"       // v2.59: title-screen toggles
internal const val SETTINGS_TUTORIAL = "tutorialDone" // v2.60: the boot diagnostic ran (or was skipped)
internal const val SETTINGS_HAPTICS = "hapticsOn"
internal const val SETTINGS_LEFTY = "leftHanded"    // v2.65: mirror the touch layout
internal const val SETTINGS_HINTS = "controlHintsOn" // v2.66: how-to guidance (landing, onboarding)
internal const val SETTINGS_LORE = "loreHintsOn"     // v2.66: the world speaking (core logs, wrecks)
internal const val SAVED_NOTE_TIME = 2f // seconds the 「セーブした」 flash stays on the SAVE tab

/**
 * Fixed-timestep simulation + render interpolation, porting the legacy main.js loop.
 * World is y-down, so the world camera is set up y-down to match.
 * Scene painting is delegated to render/SceneRenderer (world) + render/Hud + render/TouchOverlay
 * (screen space); this class owns run state, input routing, the sim loop and the camera.
 */
class GameScreen(
    internal val startFresh: Boolean = false, // v2.58 タイトル「はじめから」: abandon the saved run
    internal val startInTraining: Boolean = false, // v2.58 タイトル「旧式戦闘訓練」
    internal val startInChallenge: Boolean = false, // v2.102 タイトル「検証ラン」: 今週の宙域
    internal val slot: Int = 0, // v2.103 セーブスロット: which journey this screen plays
) : ScreenAdapter() {
    internal val input = InputState()
    internal val configStore = ConfigStore()
    // Built in show() (not the constructor) so any future libGDX resource access
    // inside the ECS world happens after Gdx.app is available on Android.
    internal lateinit var gw: GameWorld

    internal lateinit var shapes: ShapeRenderer
    internal lateinit var batch: SpriteBatch
    internal lateinit var font: BitmapFont
    internal lateinit var camera: OrthographicCamera
    internal lateinit var worldViewport: ExtendViewport
    internal lateinit var hudViewport: ScreenViewport
    internal val scene = SceneRenderer()

    internal var accumulator = 0f
    internal var camX = Tuning.VIEW_W / 2f
    internal var camY = Tuning.VIEW_H / 2f
    internal var camInit = false

    // Phase 6b: between-wave upgrade selection (modal — sim freezes until a card is picked).
    internal val upgradeRng = Rng(System.nanoTime())
    internal var choosing = false
    internal var offered = false
    internal var choices: List<Upgrade> = emptyList()

    // Phase 7: screen-shake trigger — compare HP frame-to-frame to detect the player taking damage.
    internal var lastHp = Float.NaN
    internal var lastKills = 0
    internal val ambHeat = CombatHeat() // v2.67 状況反応: combat drives the pulse layer
    internal var weatherT = 0f // v2.74 天候: cosmetic time, never sim time
    internal var prevOver = false
    internal var newBest = false

    // Phase 8: on-screen controls (Android only) + audio service.
    internal val touch = TouchControls()
    internal var touchEnabled = false

    // P1: blocking overlay (pause / help) + the per-frame HUD tap, unprojected into dp space.
    internal var overlay = Overlay.NONE
    internal var tapped = false
    internal var tapX = 0f
    internal var tapY = 0f
    internal var rawTapX = 0f // raw screen coords, re-unprojected into world space for planet taps (v2.38)
    internal var rawTapY = 0f
    internal val tmpTap = Vector3()

    // Static help text shown on the pause → 操作説明 overlay (keyboard + touch bindings).
    internal val HELP_LINES = listOf(
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
    internal var animTime = 0f
    internal var runTime = 0f
    internal val glyphLayout = GlyphLayout()
    internal var uiScale = 1f

    // Living Planets run state (R1): seeds + per-planet memory live in the pure RunSession; this class
    // only executes the transitions it plans. rememberedT is a draw-side timer, so it stays here.
    internal val session = RunSession(store = PreferencesMemoryStore(slot)) // v2.103: per-slot universe
    internal var rememberedT = 0f // seconds left to show the return-visit greeting in the HUD

    // Inventory overlay (v2.33): tab state + the run-save backend + a brief note flash (セーブした /
    // a consumable's effect). readingLore (v2.34) is the readable currently open on the ITEMS tab.
    internal val runStore = PreferencesRunSaveStore(slot) // v2.103: per-slot run save
    // v2.46 遺品回収: what the last death left floating in the void (half the dust, every shard).
    internal val relicStore = PreferencesRelicStore(slot) // v2.103: each journey's own relic
    internal var invTab = InvTab.EQUIP
    internal var invNote: String? = null
    internal var invNoteT = 0f
    internal var readingLore: ItemDef? = null
    internal var worldSeed = 1L // the seed the CURRENT world was built with (goes into the run save)
    // v2.39: swap the touch roles — melee on the right stick, gun on the ML button (persisted).
    internal var controlSwap = false
    // v2.47: the very first run walks the basics once; completion persists across launches.
    internal var onboardDone = true
    internal var controlHints = true // v2.66 操作ヒント: how-to guidance on/off
    internal var loreHints = true    // v2.66 世界観ヒント: the world's asides on/off
    // v2.60 チュートリアル: the keeper-boot diagnostic (layer 1). Null once done or skipped.
    internal var tutorial: TutorialController? = null
    internal var tutPrevPx = Float.NaN
    internal var tutPrevPy = Float.NaN
    internal var tutPrevKills = 0
    internal var tutPrevDust = -1

    // v2.56 ボタン配置エディタ: drag to move, toolbar to resize; saved as screen fractions.
    internal var layoutEditing = false
    internal var editTarget: TouchButton? = null
    internal var layoutDragging = false

    // v2.53 旧式戦闘訓練: the walled-off wave simulation. Entering snapshots the real run's
    // player (gear/materials/wave); exiting restores it — training gains stay in the training.
    internal var simMode = false
    internal var preSimCarry: PlayerCarry? = null
    // v2.102 検証ラン: this week's proving run — simMode's walls (no landings, no real records,
    // no achievements) plus a fixed weekly seed and the standard-issue loadout.
    internal var challengeMode = false
    internal var challengeWeek = 0L
    // v2.43: stall slots already bought this landing (the stock is per-planet deterministic).
    internal val marketSold = mutableSetOf<Int>()
    // v2.100 行商船: shelf slots bought under this sky, the approach latch (re-arms after pulling
    // away), and the shop's inline note (bought / not enough dust).
    internal val traderSold = mutableSetOf<Int>()
    internal var traderGreeted = false
    internal var traderNote: String? = null
    internal var traderNoteT = 0f
    internal var traderSelling = false // v2.114 買い取り: the stall's second face
    internal var sellPage = 0
    internal var sellUndoItem: io.github.panda17tk.arpg.item.ItemDef? = null // v2.118 戻す
    internal var sellUndoPay = 0
    internal var sellUndoIdx = 0
    internal var sellUndoT = 0f

    // Takeoff send-off toast (LP v2.29): one line in the SPACE HUD for a few seconds after leaving.
    internal var rewardToast: String? = null
    internal var rewardToastT = 0f

    // v2.44: the fade that is currently running ends in a system jump, not a landing/takeoff.
    internal var pendingJump = false
    internal var fadeZoomDir = 0f // v2.85: landing dives in (-), takeoff/jump pulls back (+)
    internal val eventBanner = io.github.panda17tk.arpg.ui.EventBanner() // v2.86 開幕バナー
    internal val bannerGlyph = com.badlogic.gdx.graphics.g2d.GlyphLayout()
    internal val cEventTmp = Color()
    internal val cQuestGold = Color.valueOf("ffd980") // v2.87: the star's answer, dust gold
    internal val cQuestPale = Color.valueOf("fff2c8")
    internal var eventFxT = 0f // v2.86: clock for the event-flavored screen fx
    internal val bossBar = io.github.panda17tk.arpg.ui.BossBar() // v2.88
    internal var duckLevel = 1f // v2.89: the running ambient duck
    internal var metaBoons = io.github.panda17tk.arpg.config.WorkshopBoons.NONE // v2.90 工房
    internal var shakeOn = true    // v2.96: motion comfort — gates shake + recoil kick
    internal var runDifficulty = io.github.panda17tk.arpg.sim.Difficulty.NORMAL // v2.97
    // v2.98 調整モード: the popup's state (open flag, current page, the live knob list).
    internal var tunePage = 0
    internal var lowHpT = 0f // v2.112 低HP警告: the red rim's breathing clock
    internal var tuneParams: List<io.github.panda17tk.arpg.config.TuneParam> = emptyList()
    internal var softFlash = false // v2.96: photosensitivity — dims the white-outs
    internal var prevWaveNum = 0 // v2.92: to notice a wave ending (流星群を生き延びた)
    internal var prevWaveEvent = WaveEvent.NONE
    // v2.93 エンディング: 0=off, 1..pages=dialogue, pages+1=choice, pages+2=epilogue.
    internal var endingStage = 0
    internal var endingSeenThisWorld = false // 「切断」した空では核はもう問わない

    // Memory tint per planet id (LP v2.30/10c) — rebuilt only when memory can change (transitions/forget).
    internal var memoryTones: Map<Long, Int> = emptyMap()

    // Landing/takeoff fade (10b): OUT → swap the world behind black → IN. Gameplay input pauses meanwhile.
    internal val fade = TransitionFade()

    // Pre-landing scan card (LP v2.23), rebuilt only when the latched candidate's id changes (FR-1.6).
    internal var lastCardId: Long? = null
    internal var cachedCard: PlanetCardInfo? = null

    // Surface goal chips (LP v2.26), rebuilt only when their inputs change (§14.2 — no per-frame strings).
    internal var chipsKey = -1
    internal var cachedChips: List<String> = emptyList()
    // v2.45 星の依頼: the quest chip cache (progress-keyed, same no-per-frame-strings policy).
    internal var questChipKey = -1
    internal var questChip: String? = null

    override fun show() {
        io.github.panda17tk.arpg.save.Workshop.load() // v2.90 工房: the run starts with its boons
        metaBoons = io.github.panda17tk.arpg.save.Workshop.boons()
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
        io.github.panda17tk.arpg.save.Stats.load() // v2.123
        session.restore() // LP v2.28: the universe remembers you across runs and restarts
        touchEnabled = Gdx.app.type == Application.ApplicationType.Android
        controlSwap = try { Gdx.app.getPreferences(SETTINGS_PREFS).getBoolean(SETTINGS_SWAP, false) } catch (_: Throwable) { false }
        onboardDone = try { Gdx.app.getPreferences(SETTINGS_PREFS).getBoolean(SETTINGS_ONBOARD, false) } catch (_: Throwable) { true }
        try { // v2.59 設定: sound / haptics master switches (title-screen toggles)
            val sp = Gdx.app.getPreferences(SETTINGS_PREFS)
            Sfx.enabled = sp.getBoolean(SETTINGS_SOUND, true)
            Haptics.enabled = sp.getBoolean(SETTINGS_HAPTICS, true)
            // v2.96 快適設定: master volume + motion/photosensitivity comfort
            Sfx.volume = sp.getFloat("masterVolume", 1f).coerceIn(0f, 1f)
            Ambience.setMaster(Sfx.volume)
            shakeOn = sp.getBoolean("shakeOn", true)
            softFlash = sp.getBoolean("softFlash", false)
            runDifficulty = io.github.panda17tk.arpg.sim.Difficulty.byName(sp.getString("difficulty", "NORMAL")) // v2.97
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
            input.aimAssist = sp.getBoolean("aimAssist", true) // v2.112 エイム補助
            io.github.panda17tk.arpg.i18n.Lang.en = sp.getBoolean("langEn", false) // v2.115 English表示
        } catch (_: Throwable) { /* defaults stay on */ }
        if (startFresh) runStore.clear() // v2.58: タイトルの「はじめから」は前のランを置いていく
        if (startFresh || !tryRestoreRun()) newRun(countSortie = !startInTraining && !startInChallenge) // v2.33/v2.125
        if (startInTraining && !simMode) toggleTraining() // v2.58: straight into the simulation
        if (startInChallenge && !challengeMode) enterChallenge() // v2.102: 今週の宙域へ直行
        // v2.60: the boot diagnostic runs once per install (never inside the training sim).
        val tutDone = try { Gdx.app.getPreferences(SETTINGS_PREFS).getBoolean(SETTINGS_TUTORIAL, false) } catch (_: Throwable) { true }
        if (!tutDone && !simMode) tutorial = TutorialController()
        Ambience.setEnabled(Sfx.enabled) // v2.63: same サウンド switch gates the ambient loop
        syncAmbience()
    }

    /** v2.63 生成オーディオ: hum the track this scene wants (space / biome / training sim). */
    internal fun syncAmbience() {
        Ambience.play(AmbienceScore.trackFor(gw.worldState.mode, gw.worldState.biome, simMode))
        // v2.76 天候アンビエント: the surface brings its sky's sound; everywhere else is silent.
        Ambience.playWeather(
            if (!simMode && gw.worldState.mode == WorldMode.SURFACE) gw.worldState.weather
            else WeatherKind.CLEAR,
        )
    }

    /** v2.53: enter/exit the old-style combat simulation. The universe's memory, the saved run
     *  and the real player snapshot are all untouched — a simulation leaves no trace but skill. */
    internal fun toggleTraining() {
        if (!simMode) {
            foldBestiary() // v2.113: the real world's tallies, before the sim
            simMode = true
            preSimCarry = PlayerCarry.of(gw.world, gw.player, gw.waveState.num)
            worldSeed = TRAINING_SEED
            gw = WorldFactory.create(input, configStore.config, seed = TRAINING_SEED, carry = preSimCarry, boons = metaBoons, difficulty = runDifficulty)
        } else {
            simMode = false
            challengeMode = false // v2.102: leaving the walled-off world ends the proving run too
            worldSeed = session.spaceSeed
            gw = WorldFactory.create(input, configStore.config, seed = session.spaceSeed, carry = preSimCarry, boons = metaBoons, trait = SystemTraits.traitFor(session.spaceSeed), difficulty = runDifficulty)
            gw.waveState.num = preSimCarry?.wave ?: 1
            preSimCarry = null
        }
        accumulator = 0f; camInit = false; choosing = false; offered = false; choices = emptyList()
        lastHp = Float.NaN; lastCardId = null; cachedCard = null
        chipsKey = -1; cachedChips = emptyList(); questChipKey = -1; questChip = null
        marketSold.clear(); traderSold.clear(); traderGreeted = false; pendingJump = false
        rewardToast = if (simMode) "旧式戦闘シミュレーション起動 — 成果は持ち出せない" else "訓練環境を終了した"
        rewardToastT = TOAST_TIME
        rebuildMemoryTones()
        syncAmbience() // v2.63: the sim has its own hum
    }

    /** v2.102 検証ラン: enter (or retry) this week's proving run — fixed sky, standard-issue
     *  loadout, no boons, NORMAL. The real run's snapshot is taken once and kept across retries,
     *  so leaving the challenge always restores exactly what was left behind. */
    internal fun enterChallenge() {
        if (::gw.isInitialized) foldBestiary() // v2.117: entering the proving run is a seam like the training door
        if (!challengeMode) preSimCarry = PlayerCarry.of(gw.world, gw.player, gw.waveState.num)
        simMode = true; challengeMode = true
        // v2.106 公正化: the proving run starts from the shipped numbers — session knobs all reset,
        // and the tuning popup (if open) closes; the tune door itself stays shut while inside.
        io.github.panda17tk.arpg.config.TuningCatalog.paramsFor(configStore.config).forEach { it.reset() }
        if (overlay == Overlay.TUNING) overlay = Overlay.NONE
        challengeWeek = Challenge.weekOf(System.currentTimeMillis())
        worldSeed = Challenge.seedFor(challengeWeek)
        gw = WorldFactory.create(
            input, configStore.config, seed = worldSeed,
            trait = SystemTraits.traitFor(worldSeed),
            difficulty = io.github.panda17tk.arpg.sim.Difficulty.NORMAL,
        )
        accumulator = 0f; camInit = false; choosing = false; offered = false; choices = emptyList()
        overlay = Overlay.NONE; lastHp = Float.NaN; prevOver = false; newBest = false
        lastCardId = null; cachedCard = null
        chipsKey = -1; cachedChips = emptyList(); questChipKey = -1; questChip = null
        marketSold.clear(); traderSold.clear(); traderGreeted = false; pendingJump = false
        rewardToast = "検証ラン ${Challenge.codeFor(challengeWeek)} — 全員同じ宙域・同じ装備。残り${Challenge.daysLeft(System.currentTimeMillis())}日（補助設定は有効）"
        rewardToastT = TOAST_TIME
        rebuildMemoryTones()
        syncAmbience()
    }

    /** Build (or rebuild) the run and reset per-run screen state (Phase 7 restart).
     *  [countSortie] is false only when show() builds a throwaway world on the way into the
     *  training sim or the proving run — that departure is not a real sortie (v2.125). */
    internal fun newRun(countSortie: Boolean = true) {
        // v2.117 図鑑: an abandoned run still counts (a death already folded at game over).
        if (::gw.isInitialized && !gw.gameOver.isOver) foldBestiary()
        simMode = false; preSimCarry = null // v2.53: a real restart always leaves the simulation
        challengeMode = false // v2.102
        if (countSortie) io.github.panda17tk.arpg.save.Stats.addSortie() // v2.123: a fresh real run leaves the hangar
        session.reset() // a fresh run forgets every planet
        runStore.clear() // v2.33: restarting abandons the saved run
        worldSeed = session.spaceSeed
        gw = WorldFactory.create(input, configStore.config, seed = session.spaceSeed, boons = metaBoons, trait = SystemTraits.traitFor(session.spaceSeed), difficulty = runDifficulty)
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
        marketSold.clear(); traderSold.clear(); traderGreeted = false
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
    internal fun rebuildMemoryTones() {
        memoryTones = session.memory.memories
            .mapValues { (_, s) -> ReturnVisitEffects.memoryTone(s) }
            .filterValues { it != 0 }
    }

    /** Whether a landing/takeoff would actually happen right now (used to gate the fade, 10b). */
    internal fun canTransitionNow(): Boolean = if (simMode) {
        false // v2.53: the simulation has no planets worth the name — no landings
    } else if (gw.worldState.mode == WorldMode.SPACE) {
        gw.worldState.landingCandidate != null
    } else {
        playerOnEscapePad()
    }

    /** Land on the hovered planet (SPACE) or take off from the escape pad (SURFACE), carrying run state across. */
    internal fun handleLanding() {
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
            when (gw.worldState.weather) { // v2.77: the rare skies leave a line in the record
                WeatherKind.THUNDER -> tryUnlock(Achievement.STORM_WATCHER)
                WeatherKind.AURORA -> tryUnlock(Achievement.AURORA_GAZER)
                else -> {}
            }
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

    internal fun transitionWorld(
        mode: WorldMode, biome: PlanetBiome?, seed: Long, spawn: Pair<Float, Float>?,
        context: PlanetContext? = null, society: PlanetSocietyState? = null,
        weather: WeatherKind = WeatherKind.CLEAR,
    ) {
        foldBestiary() // v2.113: fold before the swap
        val carry = PlayerCarry.of(gw.world, gw.player, gw.waveState.num)
        worldSeed = seed
        gw = WorldFactory.create(input, configStore.config, seed, mode, biome, carry, spawn, context, society, weather, boons = metaBoons, trait = if (mode == WorldMode.SPACE) SystemTraits.traitFor(session.spaceSeed) else SystemTrait.NONE, difficulty = runDifficulty)
        gw.waveState.num = carry.wave
        accumulator = 0f; camInit = false; overlay = Overlay.NONE
        choosing = false; offered = false; choices = emptyList(); lastHp = Float.NaN
        lastCardId = null; cachedCard = null // memory may have changed across the transition → rebuild the scan card
        chipsKey = -1; cachedChips = emptyList()
        questChipKey = -1; questChip = null
        marketSold.clear(); traderSold.clear(); traderGreeted = false
        rebuildMemoryTones()
        syncAmbience() // v2.63: space ↔ surface swap the ambient loop
    }


    /** True when the player is standing on the surface escape pad (the return point). */
    internal fun playerOnEscapePad(): Boolean {
        val pad = gw.worldState.escapePad ?: return false
        val (ppx, ppy) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        return hypot(ppx - pad.first, ppy - pad.second) < Tuning.TILE * 1.5f
    }

    override fun render(delta: Float) {
        KeyboardInput.poll(input)
        pollTap()
        if ((Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) &&
            !choosing && !gw.gameOver.isOver && !layoutEditing && endingStage == 0) overlay = PauseFlow.toggle(overlay)
        if (layoutEditing) handleLayoutEdit() // v2.56: the editor swallows all input while open
        if (endingStage > 0) handleEndingTaps() // v2.93: the final dialogue owns every tap
        else if (!layoutEditing && !handleTutorialTaps()) handlePauseTaps() // v2.60: diagnostic taps first

        pollGameplayTouch(
            overlay != Overlay.NONE || fade.blocksInput || layoutEditing || endingStage > 0 ||
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
        // v2.98 調整モード: the 調整 button (left of 持物) opens the knob popup.
        // v2.111: tuning rides the Overlay register (like 持物) — the popup owns the screen,
        // and every overlay-aware guard (touch buttons, HUD chips, taps) excludes it for free.
        if (input.tune && io.github.panda17tk.arpg.save.TuneMode.active && !challengeMode && !choosing && !gw.gameOver.isOver &&
            !layoutEditing && endingStage == 0 && (overlay == Overlay.NONE || overlay == Overlay.TUNING)
        ) { // v2.106 公正化: the proving run takes no knobs
            overlay = if (overlay == Overlay.TUNING) Overlay.NONE else Overlay.TUNING
            if (overlay == Overlay.TUNING) {
                tuneParams = io.github.panda17tk.arpg.config.TuningCatalog.paramsFor(configStore.config)
                tunePage = 0
            }
            Sfx.play("scan")
        }
        val paused = (overlay != Overlay.NONE && overlay != Overlay.INVENTORY) || layoutEditing ||
            endingStage > 0 || // v2.93: the final dialogue holds the world still (TUNING pauses as an overlay)
            tutorial?.step == TutorialStep.BOOT_PROMPT // sim-freezing overlays (+ the boot prompt)
        val simDelta = delta * if (overlay == Overlay.INVENTORY) INV_TIME_SCALE else 1f
        if (invNoteT > 0f) invNoteT -= delta

        // Living Planets: land on / leave a planet (L key or the touch LAND button). The fade wraps the
        // rebuild: OUT → (world swap behind black) → IN, and gameplay input is ignored meanwhile (10b).
        if (overlay == Overlay.NONE && !choosing && !gw.gameOver.isOver && !fade.blocksInput && input.land) {
            if (canJumpNow()) { // v2.44: standing at the gate with enough shards → jump beats landing
                fade.start(); pendingJump = true
                fadeZoomDir = 0.14f // v2.85: a jump pulls the camera back
                Sfx.play("takeoff")
            } else if (canTransitionNow()) {
                fade.start()
                // v2.85: landing dives toward the ground; takeoff pulls back into the void.
                fadeZoomDir = if (gw.worldState.mode == WorldMode.SPACE) -0.10f else 0.14f
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
                // v2.61/v2.146 Layer 2: the first surface — a single observe beat, then home.
                if (gw.worldState.mode == WorldMode.SURFACE) t.onSurfaceTick(simDelta)
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
            eventBanner.start(it) // v2.86: the moment lines ride the cinematic band, not a toast
            gw.waveState.announce = null
            Sfx.play("banner") // v2.89: the band has its own stinger
            if (it.contains("賞金首を討ち取った") && !simMode) tryUnlock(Achievement.BOUNTY_HUNTER) // v2.62
        }
        // v2.62 実績: surviving deep into the desync (the real run only).
        if (!paused && !simMode && gw.waveState.num >= 15) tryUnlock(Achievement.DEEP_SURGE)
        // v2.92 実績: the sim counts feats; the screen turns counts into unlocks.
        if (!paused && !simMode) {
            if (gw.gameOver.rogueKills > 0) tryUnlock(Achievement.ROGUE_SLAYER)
            if (gw.gameOver.rageKills > 0) tryUnlock(Achievement.RAGE_BREAKER)
            if (gw.gameOver.grandKills > 0) tryUnlock(Achievement.GRAND_RITUAL)
            if (gw.fx.comboStep >= io.github.panda17tk.arpg.combat.MeleeCombo.MAX_STEP) tryUnlock(Achievement.COMBO_MASTER)
            if (with(gw.world) { gw.player[Materials].shards } >= gateNeed()) tryUnlock(Achievement.GATE_READY)
            // surviving a meteor wave: the wave number moved on while the sky was falling
            if (gw.waveState.num > prevWaveNum && prevWaveEvent == WaveEvent.METEOR && !gw.gameOver.isOver) {
                tryUnlock(Achievement.METEOR_SURVIVOR)
            }
            prevWaveNum = gw.waveState.num
            prevWaveEvent = gw.waveState.event
        }
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
        // v2.100 行商船: drifting up to the vessel opens its stall — once per approach; the latch
        // re-arms only after pulling clearly away, so the shop never pins the player in place.
        if (!paused && !simMode && gw.worldState.mode == WorldMode.SPACE && overlay == Overlay.NONE &&
            !choosing && !gw.gameOver.isOver && endingStage == 0 && !fade.blocksInput
        ) {
            gw.worldState.trader?.let { tr ->
                val (tpx, tpy) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
                val d = hypot(tpx - tr.first, tpy - tr.second)
                if (d > Tuning.TILE * 5f) {
                    traderGreeted = false
                } else if (d < Tuning.TILE * 2.5f && !traderGreeted) {
                    traderGreeted = true
                    traderNote = null; traderNoteT = 0f
                    traderSelling = false; sellPage = 0 // v2.114: the shop opens on its shelves
                    sellUndoItem = null; sellUndoT = 0f // v2.118: a new visit starts with a clean ledger
                    overlay = Overlay.TRADER
                    Sfx.play("scan")
                }
            }
        }
        // v2.110 生存者救助: one wreck may shelter a survivor — drawing close IS the rescue.
        if (!paused && !simMode && gw.worldState.mode == WorldMode.SPACE &&
            gw.worldState.survivorWreck >= 0 && !gw.worldState.survivorRescued
        ) {
            gw.worldState.wrecks.getOrNull(gw.worldState.survivorWreck)?.let { wk ->
                val (spx2, spy2) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
                if (hypot(spx2 - wk.first, spy2 - wk.second) < Tuning.TILE * 2f) {
                    gw.worldState.survivorRescued = true
                    with(gw.world) { gw.player[Materials].dust += 40 }
                    eventBanner.start("漂流者を救助した — 礼にと星屑40を分けてくれた")
                    Sfx.play("levelup")
                    tryUnlock(Achievement.LIFELINE)
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
                    val pay = io.github.panda17tk.arpg.planet.WeatherQuest.rewardFor(q, ws.weather) // v2.109 天候×依頼
                    with(gw.world) { gw.player[Materials].dust += pay }
                    ws.questStage++
                    ws.questBaseKills = ws.questKills; ws.questBaseElites = ws.questElites
                    ws.questBaseDust = ws.questDust; ws.questBasePredators = ws.questPredators
                    ws.questBaseTime = ws.questTime
                    questChipKey = -1 // the chip rebuilds for the next request
                    ws.recentEvents.add(
                        PlanetEvent(
                            if (ws.questStage < PlanetQuest.CHAIN) "依頼を果たした +${pay}屑 — 次の頼みが届いた"
                            else "依頼を果たした +${pay}屑 — この星の頼みはすべて済んだ",
                            EventKind.MERCY,
                        ),
                    )
                    // v2.87 星の答え: a column of light + gold rain over the keeper, and the band tells it.
                    with(gw.world) {
                        val pt2 = gw.player[Transform]
                        gw.fx.spawnPillar(pt2.x, pt2.y)
                        gw.fx.spawnSparks(pt2.x, pt2.y - 10f, 16, cQuestGold)
                        gw.fx.spawnSparks(pt2.x, pt2.y - 10f, 10, cQuestPale)
                    }
                    eventBanner.start(
                        if (ws.questStage < PlanetQuest.CHAIN) "星が応えた — 次の頼みが届いた"
                        else "星が応えた — この星の頼みはすべて済んだ",
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
        // v2.93 管制核: when the sync tops out, the core surfaces beside the gate — once per sky.
        if (!simMode && gw.worldState.mode == WorldMode.SPACE && !endingSeenThisWorld &&
            gw.worldState.controlCore == null && io.github.panda17tk.arpg.sim.Endgame.ready(syncPercent())
        ) {
            gw.worldState.gate?.let { gw.worldState.controlCore = io.github.panda17tk.arpg.sim.Endgame.corePos(it) }
        }
        if (!paused && endingStage == 0 && !gw.gameOver.isOver) {
            gw.worldState.controlCore?.let { (ccx, ccy) ->
                if (hypot(px - ccx, py - ccy) < Tuning.TILE * 2f) {
                    endingStage = 1
                    Sfx.play("scan")
                }
            }
        }
        // v2.95 地下遺構: crossing into the chamber marks the visit once.
        if (!paused && gw.worldState.mode == WorldMode.SURFACE && !gw.worldState.vaultEntered) {
            gw.worldState.vault?.let { (vx2, vy2) ->
                if (hypot(px - vx2, py - vy2) < Tuning.TILE * 2f) {
                    gw.worldState.vaultEntered = true
                    gw.worldState.recentEvents.add(PlanetEvent("封じられた遺構に踏み入った", EventKind.NEUTRAL))
                    if (!simMode) tryUnlock(Achievement.VAULT_DELVER)
                    Sfx.play("scan")
                }
            }
        }
        val playerHit = pit > 0f && ((pit * 20f).toInt() % 2 == 0)
        // v2.37: the gear look — the active weapon shapes the drawn gun, armor tints the suit, OC burns blue.
        val gearLook = with(gw.world) { gw.player[Gear].loadout }
        val activeWeapon = with(gw.world) { gw.player[Arsenal].current.def.id }
        val pMoving = with(gw.world) { // v2.85: drives the walk bob (interp distance this frame)
            val t = gw.player[Transform]; hypot(t.x - t.prevX, t.y - t.prevY) > 0.06f
        }
        val pose = PlayerPose(
            px, py, fx, fy, dashing = input.dash && sta > 0f, hit = playerHit, muzzle = input.fire,
            weaponType = activeWeapon, armorId = gearLook.armor?.id, oc = gearLook.hasOverclockThruster,
            moving = pMoving,
        )

        updateCamera(delta, px, py, fx, fy)
        if (shakeOn && gw.fx.shakeMag > 0f) { camera.position.add(gw.fx.shakeX(), gw.fx.shakeY(), 0f); camera.update() }
        // v2.85: the recoil kick — a directional punch that snaps back in a tenth of a second.
        if (shakeOn && (gw.fx.kickX != 0f || gw.fx.kickY != 0f)) { camera.position.add(gw.fx.kickX, gw.fx.kickY, 0f); camera.update() }

        ScreenUtils.clear(0.06f, 0.07f, 0.10f, 1f)

        // v2.87 儀式: display-only — the gate lights up while the keeper holds enough shards.
        gw.worldState.gateReady = gw.worldState.mode == WorldMode.SPACE &&
            with(gw.world) { gw.player[Materials].shards } >= gateNeed()

        // world (procedural sprites — ported from legacy renderer.js + enemy-sprites.js)
        worldViewport.apply()
        scene.draw(shapes, batch, font, camera, gw, animTime, pose, memoryTones)

        drawWeather(delta) // v2.74: the planet's climate, between the world and the HUD
        drawEventFx(delta) // v2.86: the wave event colors the whole sky, not just a line
        drawKillFlash()    // v2.88: the white-out that crowns a boss kill
        drawLowHpPulse(delta) // v2.112: the hull's red breathing under 30%
        drawHud(paused, sta, staMax, overheat)
        drawNavMarkers() // v2.108: the sky's landmarks, pinned to the screen edge
        updateBossBar(delta, px, py)
        drawBossBar()      // v2.88: the heavy's name and health, top-center
        drawComboChip()    // v2.92: the melee rhythm while its window is alive
        drawEventBanner() // v2.86: the opening band rides over the HUD
        drawEnding()       // v2.93: the final dialogue, over everything
    }













    /**
     * Step the fixed-timestep sim, or freeze it for game over / pause / the upgrade modal.
     * Returns false when a game-over restart rebuilt the run (the frame should not be drawn).
     */
    internal fun advanceSim(delta: Float, paused: Boolean): Boolean {
        if (gw.gameOver.isOver) {
            accumulator = 0f
            if (!prevOver) {
                // v2.53: a death inside the simulation is a simulated death — no REAL records.
                // v2.62: but the training hall keeps its own scoreboard.
                // v2.102: and the proving run keeps a third, wiped when the week's sky turns.
                newBest = when {
                    challengeMode -> Scores.recordChallenge(challengeWeek, gw.waveState.num, gw.gameOver.kills)
                    simMode -> Scores.recordSim(gw.waveState.num, gw.gameOver.kills)
                    else -> Scores.record(gw.waveState.num, gw.gameOver.kills)
                }
                foldBestiary() // v2.113 図鑑
                if (!simMode) io.github.panda17tk.arpg.save.Stats.addDeath() // v2.123
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
                    // v2.90 工房: a fraction of the carried fragments reaches the workshop ledger.
                    val carried = with(gw.world) { gw.player[Materials].dust }
                    io.github.panda17tk.arpg.save.Workshop.deposit(io.github.panda17tk.arpg.save.WorkshopCatalog.salvage(carried))
                }
                Sfx.play("dead"); Haptics.buzz(140)
            }
            val goHit = if (tapped) {
                Modals.hitModal(Modals.gameOverButtons(hudViewport.worldWidth, hudViewport.worldHeight), tapX, tapY)
            } else null
            // v2.102: a challenge retry rebuilds the SAME weekly sky; everything else starts over.
            if (Gdx.input.isKeyJustPressed(Input.Keys.R) || goHit == 0) {
                if (challengeMode) enterChallenge() else newRun()
                return false
            }
            if (goHit == 1) { (Gdx.app.applicationListener as? App)?.showTitle(); return false } // v2.59
        } else if (paused) {
            accumulator = 0f // freeze the sim while paused; skip stepping & the upgrade flow
        } else {
            if (!simMode) io.github.panda17tk.arpg.save.Stats.tick(delta) // v2.123 勤続時計
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
        // v2.89: a heavy on the boss bar keeps the combat pulse from cooling below a floor.
        val pulseFloor = if (bossBar.k > 0.5f) 0.7f else 0f
        Ambience.setLayers(maxOf(ambHeat.value, pulseFloor), shimmerLevel)
        gw.fx.update(delta)
        // v2.89: sim systems queue their sounds through Fx; the screen is the only speaker.
        for (req in gw.fx.drainSfx()) Sfx.play(req.name, req.pitch, Sfx.BASE * req.vol)
        // v2.89 オーディオダック: held time pulls the ambient bed down; release eases it back.
        duckLevel = io.github.panda17tk.arpg.audio.AudioDuck.step(
            duckLevel,
            io.github.panda17tk.arpg.audio.AudioDuck.target(gw.fx.simTimeScale() < 1f, gw.fx.flashAlpha()),
            delta,
        )
        Ambience.setDuck(duckLevel)
        eventBanner.update(delta) // v2.86
        animTime += delta
        if (!gw.gameOver.isOver && !choosing && !paused) runTime += delta
        // v2.69 観測依頼: surface time only ticks while actually playing the surface.
        if (!gw.gameOver.isOver && !choosing && !paused && gw.worldState.mode == WorldMode.SURFACE) {
            gw.worldState.questTime += delta
        }
        return true
    }







    internal fun trackPlayerHitShake() {
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
    internal fun updateUpgradeFlow(delta: Float) {
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

    internal fun applyUpgrade(u: Upgrade) {
        val cfg = configStore.config.upgrades
        with(gw.world) {
            Upgrades.apply(u.id, cfg, gw.player[Mods], gw.player[Health], gw.player[Ammo], gw.player[Materials], gw.player[Stamina]) // v2.107
        }
        Sfx.play("levelup")
    }




    /** v2.44: the player is close enough to the gate ring to enter it. */
    internal fun nearGate(): Boolean {
        val g = gw.worldState.gate ?: return false
        val (ppx, ppy) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        return hypot(ppx - g.first, ppy - g.second) < Tuning.TILE * 4f
    }

    /** v2.52 同期復旧度: derived progression — systems jumped + planets known + planets trusting. */
    internal fun syncPercent(): Int = SyncRestoration.percent(session.spaceSeed.toInt(), session.memory.memories.values)

    /** How many shards THIS jump costs (a 60%+ restored network recognizes the keeper). */
    internal fun gateNeed(): Int = SyncRestoration.gateShardsNeeded(syncPercent())

    /** v2.44: a jump is possible right now — in space, at the gate, holding enough gate shards. */
    internal fun canJumpNow(): Boolean = !simMode && gw.worldState.mode == WorldMode.SPACE && nearGate() &&
        with(gw.world) { gw.player[Materials].shards } >= gateNeed()

    /**
     * v2.44 ジャンプゲート: spend the shards and move the whole run to the NEXT star system.
     * A fresh spaceSeed regenerates planets/enemies/the gate; the player keeps everything they
     * carry (PlayerCarry hauls gear, materials and wave across, so difficulty keeps climbing).
     */
    internal fun performJump() {
        pendingJump = false
        with(gw.world) {
            val m = gw.player[Materials]
            m.shards = (m.shards - gateNeed()).coerceAtLeast(0)
        }
        session.spaceSeed += 1
        session.surfSeed = session.spaceSeed * 100
        session.landedPlanetId = null
        session.returnSpawn = null
        endingSeenThisWorld = false // v2.93: a new sky may ask the question again
        transitionWorld(WorldMode.SPACE, null, session.spaceSeed, null)
        with(gw.world) { val h = gw.player[Health]; h.hp = h.hpMax } // the jump mends the hull
        tryUnlock(Achievement.FIRST_JUMP) // v2.62
        if (syncPercent() >= 50) tryUnlock(Achievement.SYNC_50)
        if (syncPercent() >= 90) tryUnlock(Achievement.SYNC_90) // v2.68
        if (session.spaceSeed >= 3) tryUnlock(Achievement.SYSTEM_3) // v2.70
        rewardToast = "第${session.spaceSeed}星系に到達した"
        rewardToastT = TOAST_TIME
        // v2.91 星系の個性: the new sky introduces itself on the banner.
        gw.worldState.trait.takeIf { it != SystemTrait.NONE }?.let {
            eventBanner.start(it.line)
            tryUnlock(Achievement.TRAIT_ARRIVAL) // v2.92
        }
        Sfx.play("takeoff")
    }

    /** Whether the pause carries the 4th 「この星の記憶」 entry (surface only — LP v2.25). */
    internal fun pauseHasMemory(): Boolean = gw.worldState.mode == WorldMode.SURFACE




    /** The inventory overlay's view model → Hud.inventory (v2.33). */


    /** v2.62 実績: unlock + one-line toast the first time only. */
    /** v2.117 図鑑: fold the current world's tallies at a seam (real runs only) and mark
     *  the record achievements. Every world-discarding path calls this exactly once. */
    internal fun foldBestiary() {
        if (simMode) return
        io.github.panda17tk.arpg.save.Bestiary.record(gw.gameOver.killsByKind)
        io.github.panda17tk.arpg.save.Stats.fold(gw.gameOver.killsByKind) // v2.123 勤続記録: same seam
        gw.gameOver.killsByKind.clear() // a seam folds once — a second pass finds nothing
        val known = io.github.panda17tk.arpg.save.Bestiary.knownCount()
        if (known >= 50) tryUnlock(Achievement.BESTIARY_50)
        if (known >= configStore.config.enemies.size) tryUnlock(Achievement.BESTIARY_FULL)
    }

    internal fun tryUnlock(a: Achievement) {
        if (Achievements.unlock(a)) {
            rewardToast = "実績解除『${a.title}』 — ${a.desc}"
            rewardToastT = TOAST_TIME
            Sfx.play("levelup")
        }
    }

    /** v2.60: same ending either way — the reward never depends on finishing vs skipping. */
    internal fun finishTutorial() {
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












    /** v2.33 SAVE tab: snapshot the run (world identity + player state + gear) and persist it. */
    internal fun saveRun() {
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
                reloadMul = mods.reloadMul, blastMul2 = mods.blastMul, regenAdd = mods.regenAdd, // v2.107
                dashCostMul = mods.dashCostMul, bulletSpeedMul = mods.bulletSpeedMul,
                armorMul = mods.armorMul, pickupRange = mods.pickupRange,
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
    internal fun tryRestoreRun(): Boolean {
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
            boons = metaBoons, // v2.90
            trait = if (mode == WorldMode.SPACE) SystemTraits.traitFor(dto.worldSeed) else SystemTrait.NONE, // v2.91
            difficulty = runDifficulty, // v2.97
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
            mods.reloadMul = dto.reloadMul; mods.blastMul = dto.blastMul2; mods.regenAdd = dto.regenAdd // v2.107
            mods.dashCostMul = dto.dashCostMul; mods.bulletSpeedMul = dto.bulletSpeedMul
            mods.armorMul = dto.armorMul; mods.pickupRange = dto.pickupRange
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
        marketSold.clear(); traderSold.clear(); traderGreeted = false
        rebuildMemoryTones()
        return true
    }

    internal fun step(delta: Float) {
        // v2.85: hitstop freezes the accumulator feed, slow-mo starves it — the sim itself
        // still runs whole FIXED_DT steps, so determinism is untouched.
        val dt = minOf(Constants.MAX_DT, delta) * gw.fx.simTimeScale()
        if (dt <= 0f) return
        accumulator += dt
        var steps = 0
        while (accumulator >= Constants.FIXED_DT && steps < Constants.MAX_STEPS) {
            gw.world.update(Constants.FIXED_DT)
            accumulator -= Constants.FIXED_DT
            steps++
        }
        if (steps >= Constants.MAX_STEPS) accumulator = 0f
    }

    internal fun updateCamera(delta: Float, px: Float, py: Float, fx: Float, fy: Float) {
        val tgX = px + fx * Tuning.CAM_LOOK_AHEAD
        val tgY = py + fy * Tuning.CAM_LOOK_AHEAD
        if (!camInit) { camX = tgX; camY = tgY; camInit = true }
        val k = 1f - 0.0001f.pow(delta) // legacy smoothing
        camX += (tgX - camX) * k
        camY += (tgY - camY) * k
        camera.position.set(camX, camY, 0f)
        camera.zoom = 1f + fadeZoomDir * fade.alpha // v2.85: dive in on landing, pull back on takeoff
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
