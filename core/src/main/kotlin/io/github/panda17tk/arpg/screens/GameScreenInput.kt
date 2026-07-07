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

/*
 * v2.105 分割: 入力の章 — gameplay touch polling, tap routing, and the layout editor.
 * Extracted from GameScreen (v2.105) with zero behavior change — every function is an
 * internal extension so the screen reads as chapters instead of one 2300-line scroll.
 */

/** Gameplay touch (twin-stick / fire) runs only when no modal blocks it (spec §5.2). */
internal fun GameScreen.pollGameplayTouch(paused: Boolean) {
    // Pass the player context so the action buttons can show/hide by relevance (P3). The LAND button
    // appears only when landing is possible: near a planet in space, or standing on the escape pad.
        if (touchEnabled && !paused && !choosing && !gw.gameOver.isOver) {
            val tw = with(gw.world) { gw.player[Arsenal] }.current
            val tBlocks = with(gw.world) { gw.player[Materials].blocks }
            val ws = gw.worldState
            val canLand = (ws.mode == WorldMode.SPACE && ws.landingCandidate != null) ||
                (ws.mode == WorldMode.SURFACE && playerOnEscapePad())
            val hasOverclock = with(gw.world) { gw.player[Gear].loadout.hasOverclockThruster }
            touch.poll(input, hudViewport, tBlocks, tw.mag, tw.def.magSize, canLand, hasOverclock, controlSwap, io.github.panda17tk.arpg.save.TuneMode.active && !challengeMode) // v2.106 公正化
        }
    }

/** Capture this frame's HUD tap (a desktop mouse click counts too), unprojected into dp space. */
internal fun GameScreen.pollTap() {
        tapped = Gdx.input.justTouched()
        if (tapped) {
            rawTapX = Gdx.input.x.toFloat(); rawTapY = Gdx.input.y.toFloat() // kept for world-space hits (v2.38)
            tmpTap.set(rawTapX, rawTapY, 0f)
            hudViewport.unproject(tmpTap)
            tapX = tmpTap.x; tapY = tmpTap.y
        }
    }

/** v2.38: did this frame's tap land on the landing candidate planet itself (world space)? */
internal fun GameScreen.tapHitsCandidatePlanet(): Boolean {
        val cand = gw.worldState.landingCandidate ?: return false
        tmpTap.set(rawTapX, rawTapY, 0f)
        worldViewport.unproject(tmpTap)
        return hypot(tmpTap.x - cand.cx, tmpTap.y - cand.cy) < cand.radius + PLANET_TAP_PAD
    }

/** v2.44: did this frame's tap land on the jump gate itself (world space)? */
internal fun GameScreen.tapHitsGate(): Boolean {
        val g = gw.worldState.gate ?: return false
        tmpTap.set(rawTapX, rawTapY, 0f)
        worldViewport.unproject(tmpTap)
        return hypot(tmpTap.x - g.first, tmpTap.y - g.second) < GATE_TAP_R
    }

/** v2.56 ボタン配置エディタ: drag any button to move it; the toolbar resizes/resets/saves. */
internal fun GameScreen.handleLayoutEdit() {
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

internal fun GameScreen.nudgeScale(b: TouchButton, d: Float) {
        val cur = touch.layout.tweaks[b]
        val fx = cur?.fx ?: (touch.layout.centerX(b) / hudViewport.worldWidth)
        val fy = cur?.fy ?: (touch.layout.centerY(b) / hudViewport.worldHeight)
        setTweak(b, fx, fy, (cur?.scale ?: 1f) + d)
    }

internal fun GameScreen.setTweak(b: TouchButton, fx: Float, fy: Float, scale: Float) {
        touch.layout.tweaks = touch.layout.tweaks + (b to LayoutTweaks.sanitize(ButtonTweak(fx, fy, scale)))
    }

internal fun GameScreen.saveLayoutTweaks() {
        try {
            val prefs = Gdx.app.getPreferences(SETTINGS_PREFS)
            prefs.putString(SETTINGS_LAYOUT, LayoutTweaks.toJson(touch.layout.tweaks))
            prefs.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
    }

/** v2.39: flip the touch roles of the ML button and the right stick, and persist the choice. */
internal fun GameScreen.toggleControlSwap() {
        controlSwap = !controlSwap
        try {
            val p = Gdx.app.getPreferences(SETTINGS_PREFS)
            p.putBoolean(SETTINGS_SWAP, controlSwap)
            p.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
        invNote = if (controlSwap) "入替: 銃=ボタン / 近接=右スティック" else "入替: 近接=ボタン / 銃=右スティック"
        invNoteT = SAVED_NOTE_TIME
    }
