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
 * v2.105 分割: オーバーレイの章 — pause/inventory/market/trader/tuning/ending tap+draw.
 * Extracted from GameScreen (v2.105) with zero behavior change — every function is an
 * internal extension so the screen reads as chapters instead of one 2300-line scroll.
 */

/** v2.98 調整モード: dim + the page's knob rows ([−] value [＋]) + [前へ][次へ][閉じる]. */
internal fun GameScreen.drawTuning() {
        hudViewport.apply()
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        val rows = io.github.panda17tk.arpg.ui.TuningPanel.rows(w, h)
        val footer = io.github.panda17tk.arpg.ui.TuningPanel.footer(w, h)
        val pageStart = tunePage * io.github.panda17tk.arpg.ui.TuningPanel.ROWS
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND)
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        cEventTmp.set(0f, 0f, 0f, 0.78f); shapes.color = cEventTmp
        shapes.rect(0f, 0f, w, h)
        rows.forEachIndexed { i, row ->
            if (pageStart + i >= tuneParams.size) return@forEachIndexed
            cEventTmp.set(0.55f, 0.75f, 1f, 0.22f); shapes.color = cEventTmp
            shapes.rect(row.x - 1.5f, row.y - 1.5f, row.w + 3f, row.h + 3f)
            cEventTmp.set(0.09f, 0.12f, 0.18f, 0.95f); shapes.color = cEventTmp
            shapes.rect(row.x, row.y, row.w, row.h)
            for (btn in listOf(
                io.github.panda17tk.arpg.ui.TuningPanel.minusBig(row), io.github.panda17tk.arpg.ui.TuningPanel.minus(row),
                io.github.panda17tk.arpg.ui.TuningPanel.plus(row), io.github.panda17tk.arpg.ui.TuningPanel.plusBig(row),
            )) {
                cEventTmp.set(0.16f, 0.22f, 0.32f, 0.95f); shapes.color = cEventTmp
                shapes.rect(btn.x, btn.y, btn.w, btn.h)
            }
        }
        footer.forEach { b ->
            cEventTmp.set(0.55f, 0.75f, 1f, 0.22f); shapes.color = cEventTmp
            shapes.rect(b.x - 1.5f, b.y - 1.5f, b.w + 3f, b.h + 3f)
            cEventTmp.set(0.09f, 0.12f, 0.18f, 0.95f); shapes.color = cEventTmp
            shapes.rect(b.x, b.y, b.w, b.h)
        }
        shapes.end()
        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        cEventTmp.set(0.62f, 0.68f, 0.80f, 1f); font.color = cEventTmp
        bannerGlyph.setText(font, "調整モード　${tunePage + 1}/${io.github.panda17tk.arpg.ui.TuningPanel.pageCount(tuneParams.size)}")
        font.draw(batch, bannerGlyph, (w - bannerGlyph.width) / 2f, h * 0.90f)
        font.color = Color.WHITE
        rows.forEachIndexed { i, row ->
            val param = tuneParams.getOrNull(pageStart + i) ?: return@forEachIndexed
            val bx = font.data.scaleX; val by = font.data.scaleY
        // v2.99: the shipped 基準 rides beside the live value; drifted rows glow warm.
            val text = "${param.name}　${param.display()}／基準${param.displayDef()}"
            if (param.changed()) { cEventTmp.set(1f, 0.85f, 0.55f, 1f); font.color = cEventTmp }
            bannerGlyph.setText(font, text)
            val maxW = row.w - 150f
            if (bannerGlyph.width > maxW) {
                val k = maxW / bannerGlyph.width
                font.data.setScale(bx * k, by * k)
                bannerGlyph.setText(font, text)
            }
            font.draw(batch, bannerGlyph, row.centerX - bannerGlyph.width / 2f, row.centerY + bannerGlyph.height / 2f)
            font.data.setScale(bx, by)
            font.color = Color.WHITE
            for (btn in listOf(
                io.github.panda17tk.arpg.ui.TuningPanel.minusBig(row), io.github.panda17tk.arpg.ui.TuningPanel.minus(row),
                io.github.panda17tk.arpg.ui.TuningPanel.plus(row), io.github.panda17tk.arpg.ui.TuningPanel.plusBig(row),
            )) {
                bannerGlyph.setText(font, btn.label)
                font.draw(batch, bannerGlyph, btn.centerX - bannerGlyph.width / 2f, btn.centerY + bannerGlyph.height / 2f)
            }
        }
        footer.forEach { b ->
            bannerGlyph.setText(font, io.github.panda17tk.arpg.i18n.Lang.tr(b.label)) // v2.115
            font.draw(batch, bannerGlyph, b.centerX - bannerGlyph.width / 2f, b.centerY + bannerGlyph.height / 2f)
        }
        batch.end()
    }

/** v2.98: taps drive the knobs — [−]/[＋] nudge (clamped), the footer pages and closes. */
internal fun GameScreen.handleTuningTaps() {
        if (!tapped) return
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        val footer = io.github.panda17tk.arpg.ui.TuningPanel.footer(w, h)
        val pages = io.github.panda17tk.arpg.ui.TuningPanel.pageCount(tuneParams.size)
        when (Modals.hitModal(footer, tapX, tapY)) {
            0 -> { tunePage = (tunePage - 1 + pages) % pages; Sfx.play("scan"); return }
            1 -> { tunePage = (tunePage + 1) % pages; Sfx.play("scan"); return }
            2 -> { overlay = Overlay.NONE; Sfx.play("scan"); return }
            3 -> { exportTuning(); return } // v2.99 書き出し
            4 -> { tuneParams.forEach { it.reset() }; Sfx.play("levelup"); return } // v2.99 全て既定へ
        }
        val rows = io.github.panda17tk.arpg.ui.TuningPanel.rows(w, h)
        val pageStart = tunePage * io.github.panda17tk.arpg.ui.TuningPanel.ROWS
        rows.forEachIndexed { i, row ->
            val param = tuneParams.getOrNull(pageStart + i) ?: return@forEachIndexed
            when {
                io.github.panda17tk.arpg.ui.TuningPanel.minusBig(row).contains(tapX, tapY) -> { param.nudge(-1, big = true); Sfx.play("shot") }
                io.github.panda17tk.arpg.ui.TuningPanel.minus(row).contains(tapX, tapY) -> { param.nudge(-1); Sfx.play("shot") }
                io.github.panda17tk.arpg.ui.TuningPanel.plus(row).contains(tapX, tapY) -> { param.nudge(+1); Sfx.play("shot") }
                io.github.panda17tk.arpg.ui.TuningPanel.plusBig(row).contains(tapX, tapY) -> { param.nudge(+1, big = true); Sfx.play("shot") }
            }
        }
    }

/** v2.99 書き出し: the knob table as plain text — external storage first, app-local fallback. */
internal fun GameScreen.exportTuning() {
        val text = io.github.panda17tk.arpg.config.TuningExport.render("drift 調整パラメータ", tuneParams)
        val written = try {
            val fh = Gdx.files.external("drift-tuning.txt")
            fh.writeString(text, false)
            fh.file().absolutePath
        } catch (_: Throwable) {
            try {
                val fh = Gdx.files.local("drift-tuning.txt")
                fh.writeString(text, false)
                fh.file().absolutePath
            } catch (_: Throwable) { null }
        }
        eventBanner.start(if (written != null) "書き出した → $written" else "書き出しに失敗した")
        Sfx.play(if (written != null) "levelup" else "hit")
    }

/** v2.93 エンディング: dim + the dialogue pages / choice / epilogue, glass style. */
internal fun GameScreen.drawEnding() {
        if (endingStage <= 0) return
        hudViewport.apply()
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        val pages = io.github.panda17tk.arpg.sim.Endgame.PAGES
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND)
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        cEventTmp.set(0f, 0f, 0f, 0.78f); shapes.color = cEventTmp
        shapes.rect(0f, 0f, w, h)
        if (endingStage == pages.size + 1) { // the choice carries its two glass buttons
            Modals.endingButtons(w, h).forEach { b ->
                cEventTmp.set(0.55f, 0.75f, 1f, 0.22f); shapes.color = cEventTmp
                shapes.rect(b.x - 1.5f, b.y - 1.5f, b.w + 3f, b.h + 3f)
                cEventTmp.set(0.09f, 0.12f, 0.18f, 0.95f); shapes.color = cEventTmp
                shapes.rect(b.x, b.y, b.w, b.h)
            }
        }
        shapes.end()
        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        val lines = when {
            endingStage <= pages.size -> pages[endingStage - 1]
            endingStage == pages.size + 1 -> pages.last() // the question stays on screen behind the choice
            else -> io.github.panda17tk.arpg.sim.Endgame.EPILOGUE
        }
        var y = h * (if (endingStage == pages.size + 1) 0.80f else 0.62f)
        cEventTmp.set(0.90f, 0.93f, 1f, 1f); font.color = cEventTmp
        for (line in lines) {
            bannerGlyph.setText(font, io.github.panda17tk.arpg.i18n.Lang.tr(line)) // v2.162 英語化第4弾
            font.draw(batch, bannerGlyph, (w - bannerGlyph.width) / 2f, y)
            y -= 30f
        }
        cEventTmp.set(0.62f, 0.68f, 0.80f, 1f); font.color = cEventTmp
        val hint = when {
            endingStage <= pages.size -> "タップで続ける"
            endingStage == pages.size + 1 -> ""
            else -> "タップで記録を閉じる"
        }
        if (hint.isNotEmpty()) {
            bannerGlyph.setText(font, io.github.panda17tk.arpg.i18n.Lang.tr(hint)) // v2.162
            font.draw(batch, bannerGlyph, (w - bannerGlyph.width) / 2f, h * 0.16f)
        }
        if (endingStage == pages.size + 1) {
            font.color = Color.WHITE
            Modals.endingButtons(w, h).forEach { b ->
                bannerGlyph.setText(font, io.github.panda17tk.arpg.i18n.Lang.tr(b.label)) // v2.115
                font.draw(batch, bannerGlyph, b.centerX - bannerGlyph.width / 2f, b.centerY + bannerGlyph.height / 2f)
            }
        }
        font.color = Color.WHITE
        batch.end()
    }

/** v2.93: taps drive the final dialogue — pages, the choice, then the closing of the record. */
internal fun GameScreen.handleEndingTaps() {
        if (!tapped) return
        val pages = io.github.panda17tk.arpg.sim.Endgame.PAGES
        when {
            endingStage in 1..pages.size -> { endingStage++; Sfx.play("scan") }
            endingStage == pages.size + 1 -> {
                val hit = Modals.hitModal(Modals.endingButtons(hudViewport.worldWidth, hudViewport.worldHeight), tapX, tapY)
                if (hit == 0) { // 眠りにつく — the record closes
                    tryUnlock(Achievement.FINAL_SYNC)
                    io.github.panda17tk.arpg.save.Endings.recordClear()
                    endingStage = pages.size + 2
                    Sfx.play("levelup")
                } else if (hit == 1) { // 漂流を続ける — the sky lets go
                    tryUnlock(Achievement.DRIFT_ON)
                    endingStage = 0
                    endingSeenThisWorld = true
                    gw.worldState.controlCore = null
                    eventBanner.start(io.github.panda17tk.arpg.sim.Endgame.DRIFT_LINE)
                    Sfx.play("scan")
                }
            }
            else -> { // the epilogue closes to the title; the finished run is consumed
                foldBestiary() // v2.117: the final world's tallies close the book too
                runStore.clear()
                (Gdx.app.applicationListener as? App)?.showTitle()
            }
        }
    }

/** Route a tap to the pause/help/memory overlays, or open pause from the in-play ⏸ button (spec §5.2). */
internal fun GameScreen.handlePauseTaps() {
        if (!tapped) return
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        when (overlay) {
            Overlay.PAUSE -> {
                val hasMemory = pauseHasMemory()
                when (PauseFlow.action(Modals.hitModal(Modals.pauseButtons(w, h, hasMemory, simMode, challengeMode), tapX, tapY) ?: -1, hasMemory)) {
                    PauseAction.RESUME -> overlay = Overlay.NONE
                    PauseAction.RESTART -> { if (challengeMode) enterChallenge() else newRun(); overlay = Overlay.NONE }
                    PauseAction.HELP -> overlay = Overlay.HELP
                    PauseAction.MEMORY -> overlay = Overlay.MEMORY
                    PauseAction.SIM -> { toggleTraining(); overlay = Overlay.NONE } // v2.53
                    PauseAction.TITLE -> { // v2.58: auto-save the real run, then the front door
                        foldBestiary() // v2.113
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
            Overlay.TRADER -> handleTraderTap(w, h) // v2.100 行商船
            Overlay.TUNING -> handleTuningTaps() // v2.111: routed like every other overlay
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
internal fun GameScreen.handleInventoryTap(w: Float, h: Float) {
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
internal fun GameScreen.handleItemsTap(w: Float, h: Float) {
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

/** v2.43: buy the tapped stall slot if it's still there and the dust covers it. */
internal fun GameScreen.handleMarketTap(w: Float, h: Float) {
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

/** v2.100 行商船: dim + the vessel's shelves (name left, price right) + 所持屑 + [離れる]. */
/** v2.114 買い取り: the backpack's sellable items, with their original indices kept for removal. */
internal fun GameScreen.sellables(): List<Pair<Int, io.github.panda17tk.arpg.item.ItemDef>> =
    with(gw.world) { gw.player[Gear].backpack.withIndex().filter { Trader.sellable(it.value) }.map { it.index to it.value } }

internal fun GameScreen.drawTraderShop() {
        hudViewport.apply()
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        val stock = Trader.stockFor(worldSeed)
        val selling = traderSelling // v2.114: which face of the stall
        val goods = sellables()
        val pageItems = if (selling) goods.drop(sellPage * TraderPanel.SELL_ROWS).take(TraderPanel.SELL_ROWS) else emptyList()
        val rows = if (selling) TraderPanel.sellRows(w, h, pageItems.size) else TraderPanel.rows(w, h, stock.size)
        val footer = if (selling) TraderPanel.sellFooter(w, h) else TraderPanel.shelfFooter(w, h)
        if (traderNoteT > 0f) { traderNoteT -= Gdx.graphics.deltaTime; if (traderNoteT <= 0f) traderNote = null }
        if (sellUndoT > 0f) { sellUndoT -= Gdx.graphics.deltaTime; if (sellUndoT <= 0f) sellUndoItem = null } // v2.118
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND)
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        cEventTmp.set(0f, 0f, 0f, 0.78f); shapes.color = cEventTmp
        shapes.rect(0f, 0f, w, h)
        val undo = if (selling && sellUndoT > 0f && sellUndoItem != null) TraderPanel.undoButton(w, h) else null // v2.118
        (rows + footer + listOfNotNull(undo)).forEach { b ->
            cEventTmp.set(1f, 0.82f, 0.45f, 0.20f); shapes.color = cEventTmp // the warm lamp, not HUD blue
            shapes.rect(b.x - 1.5f, b.y - 1.5f, b.w + 3f, b.h + 3f)
            cEventTmp.set(0.12f, 0.10f, 0.08f, 0.95f); shapes.color = cEventTmp
            shapes.rect(b.x, b.y, b.w, b.h)
        }
        shapes.end()
        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        cEventTmp.set(1f, 0.87f, 0.55f, 1f); font.color = cEventTmp
        bannerGlyph.setText(font, if (selling) "行商船 — 買い取り ${sellPage + 1}/${TraderPanel.sellPages(goods.size)}" else "行商船")
        font.draw(batch, bannerGlyph, (w - bannerGlyph.width) / 2f, h * 0.80f)
        font.color = Color.WHITE
        if (selling) { // v2.114: what the keeper carries, priced for buyback
            rows.forEachIndexed { i, row ->
                val (_, item) = pageItems[i]
                bannerGlyph.setText(font, "${item.name}　【+${Trader.sellPrice(item)}屑】")
                font.draw(batch, bannerGlyph, row.centerX - bannerGlyph.width / 2f, row.centerY + bannerGlyph.height / 2f)
            }
            if (pageItems.isEmpty()) {
                cEventTmp.set(0.62f, 0.68f, 0.80f, 1f); font.color = cEventTmp
                bannerGlyph.setText(font, "売れる持物がない")
                font.draw(batch, bannerGlyph, (w - bannerGlyph.width) / 2f, h * 0.55f)
                font.color = Color.WHITE
            }
        } else {
            rows.forEachIndexed { i, row ->
                val good = stock[i]
                val shown = Trader.discounted(good.price, gw.worldState.traderRescued) // v2.110 襲撃の礼
                val text = if (i in traderSold) "─ 売約済 ─" else "${good.label}　【${shown}屑】"
                bannerGlyph.setText(font, text)
                font.draw(batch, bannerGlyph, row.centerX - bannerGlyph.width / 2f, row.centerY + bannerGlyph.height / 2f)
            }
        }
        val dust = with(gw.world) { gw.player[Materials].dust }
        cEventTmp.set(0.62f, 0.68f, 0.80f, 1f); font.color = cEventTmp
        bannerGlyph.setText(font, traderNote ?: (if (selling) "所持 星屑 $dust　行をタップで売却" else "所持 星屑 $dust　行をタップで購入"))
        font.draw(batch, bannerGlyph, (w - bannerGlyph.width) / 2f, footer.first().y + footer.first().h + 30f)
        font.color = Color.WHITE
        footer.forEach { b ->
            bannerGlyph.setText(font, io.github.panda17tk.arpg.i18n.Lang.tr(b.label)) // v2.115
            font.draw(batch, bannerGlyph, b.centerX - bannerGlyph.width / 2f, b.centerY + bannerGlyph.height / 2f)
        }
        undo?.let { b -> // v2.118: the take-it-back chip names its price
            bannerGlyph.setText(font, "${io.github.panda17tk.arpg.i18n.Lang.tr(TraderPanel.UNDO)}（-${sellUndoPay}屑）")
            font.draw(batch, bannerGlyph, b.centerX - bannerGlyph.width / 2f, b.centerY + bannerGlyph.height / 2f)
        }
        batch.end()
    }

/** v2.100 行商船: buy the tapped shelf slot if the dust covers it — or flip to buyback, or step away. */
internal fun GameScreen.handleTraderTap(w: Float, h: Float) {
        if (traderSelling) { // v2.114 買い取り
            val u = sellUndoItem // v2.118 戻す: same price, same slot, while the chip is lit
            if (u != null && sellUndoT > 0f && Modals.hitModal(listOf(TraderPanel.undoButton(w, h)), tapX, tapY) == 0) {
                with(gw.world) {
                    val mats = gw.player[Materials]
                    if (mats.dust >= sellUndoPay) {
                        mats.dust -= sellUndoPay
                        val pack = gw.player[Gear].backpack
                        pack.add(sellUndoIdx.coerceAtMost(pack.size), u)
                        traderNote = "${u.name} を棚から戻した（-${sellUndoPay}屑）"
                        sellUndoItem = null; sellUndoT = 0f
                        Sfx.play("pickup", 1.1f)
                    } else {
                        traderNote = "屑が足りない — 戻せない"
                    }
                }
                traderNoteT = SAVED_NOTE_TIME
                return
            }
            val goods = sellables()
            when (Modals.hitModal(TraderPanel.sellFooter(w, h), tapX, tapY)) {
                0 -> { val p = TraderPanel.sellPages(goods.size); sellPage = (sellPage - 1 + p) % p; Sfx.play("scan"); return }
                1 -> { val p = TraderPanel.sellPages(goods.size); sellPage = (sellPage + 1) % p; Sfx.play("scan"); return }
                2 -> { traderSelling = false; Sfx.play("scan"); return }
            }
            val pageItems = goods.drop(sellPage * TraderPanel.SELL_ROWS).take(TraderPanel.SELL_ROWS)
            val ri = Modals.hitModal(TraderPanel.sellRows(w, h, pageItems.size), tapX, tapY) ?: return
            val (backpackIdx, item) = pageItems[ri]
            val pay = Trader.sellPrice(item)
            with(gw.world) {
                val pack = gw.player[Gear].backpack
                if (backpackIdx < pack.size && pack[backpackIdx] === item) pack.removeAt(backpackIdx) else pack.remove(item)
                gw.player[Materials].dust += pay
            }
            sellPage = sellPage.coerceAtMost(TraderPanel.sellPages(sellables().size) - 1).coerceAtLeast(0)
            traderNote = "${item.name} を売った（+${pay}屑）"; traderNoteT = SAVED_NOTE_TIME
            sellUndoItem = item; sellUndoPay = pay; sellUndoIdx = backpackIdx; sellUndoT = SELL_UNDO_TIME // v2.118
            tryUnlock(Achievement.TRADE_LEDGER) // v2.117
            Sfx.play("pickup", 0.9f)
            return
        }
        when (Modals.hitModal(TraderPanel.shelfFooter(w, h), tapX, tapY)) {
            0 -> { traderSelling = true; sellPage = 0; Sfx.play("scan"); return } // v2.114 売る
            1 -> { overlay = Overlay.NONE; Sfx.play("scan"); return }
        }
        val stock = Trader.stockFor(worldSeed)
        val idx = Modals.hitModal(TraderPanel.rows(w, h, stock.size), tapX, tapY) ?: return
        if (idx in traderSold) return
        val good = stock[idx]
        val price = Trader.discounted(good.price, gw.worldState.traderRescued) // v2.110 襲撃の礼
        with(gw.world) {
            val mats = gw.player[Materials]
            if (mats.dust < price) {
                traderNote = "星屑が足りない（${price} 必要）"; traderNoteT = SAVED_NOTE_TIME
                Sfx.play("hit")
                return
            }
            mats.dust -= price
            when (good.kind) {
                TraderGoodKind.MED -> { val hlt = gw.player[Health]; hlt.hp = minOf(hlt.hpMax, hlt.hp + Trader.MED_HEAL) }
                TraderGoodKind.AMMO -> {
                    val am = gw.player[Ammo]
                    am.ammo9 += Trader.AMMO9; am.ammo12 += Trader.AMMO12
                    am.ammoBeam += Trader.AMMO_BEAM; am.ammoNade += Trader.AMMO_NADE
                }
                TraderGoodKind.GEAR -> good.item?.let { gw.player[Gear].backpack.add(it) }
                TraderGoodKind.SHARD -> mats.shards += 1
            }
            traderSold.add(idx)
            traderNote = "${good.label} を購入した（-${price}屑）"; traderNoteT = SAVED_NOTE_TIME
            Sfx.play("levelup")
            if (!simMode) tryUnlock(Achievement.TRADER_CLIENT)
        }
    }
