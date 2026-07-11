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
 * v2.105 分割: HUDの章 — the HUD, hints, quest chips, the tutorial and the inventory view.
 * Extracted from GameScreen (v2.105) with zero behavior change — every function is an
 * internal extension so the screen reads as chapters instead of one 2300-line scroll.
 */

/** HUD (screen space) — P2 live HUD delegated to render/Hud (geometry from ui/HudLayout). */
internal fun GameScreen.drawHud(paused: Boolean, sta: Float, staMax: Float, overheat: Boolean) {
        hudViewport.apply()
        val hudW = hudViewport.worldWidth; val hudH = hudViewport.worldHeight
        val blocks = with(gw.world) { gw.player[Materials].blocks }
        val dust = with(gw.world) { gw.player[Materials].dust }
        val ammo = with(gw.world) { gw.player[Ammo] }
        val hp = with(gw.world) { gw.player[Health].hp }
        val hpMax = with(gw.world) { gw.player[Health].hpMax }
        // v2.130: 残プロセス counts hostiles — the fish are not processes.
        // v2.149: rescanned every 15 frames (~0.25s), not every frame — the family is ~5000 strong now.
        if (foesCache < 0 || --foesRescan <= 0) {
            foesCache = with(gw.world) {
                var n = 0
                gw.world.family { all(Mob) }.forEach { if (it[Mob].def.lifeKind == io.github.panda17tk.arpg.config.LifeKind.HOSTILE) n++ }
                n
            }
            foesRescan = 15
        }
        val foes = foesCache
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
                listOf("ボタン配置", "ドラッグで移動 — 完了で保存"),
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
                choices.map { it.name },
                choices.map { u -> // v2.107: the card shows YOUR numbers, before and after
                    with(gw.world) {
                        Upgrades.desc(u, cfg) + "　" + Upgrades.preview(u, cfg, gw.player[Mods], gw.player[Health], gw.player[Stamina])
                    }
                },
            )
        }
        if (gw.gameOver.isOver) {
            val bestText = when {
                challengeMode && newBest -> "検証記録更新！  ${Challenge.codeFor(challengeWeek)} ウェーブ ${Scores.chBestWave}" // v2.102
                challengeMode -> "検証記録  ${Challenge.codeFor(challengeWeek)} ウェーブ ${Scores.chBestWave}  撃破 ${Scores.chBestKills}"
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
        if (overlay == Overlay.TRADER) drawTraderShop() // v2.100 行商船
        if (overlay == Overlay.TUNING) drawTuning() // v2.111: the knob popup rides the register too
        if (overlay == Overlay.PAUSE) Hud.pause(shapes, batch, font, Fonts.title, hudViewport, Modals.pauseButtons(hudW, hudH, pauseHasMemory(), simMode, challengeMode))
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
internal fun GameScreen.drawOnboarding(paused: Boolean, hudW: Float) {
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
internal fun GameScreen.drawObjectiveHint(paused: Boolean, hudW: Float, hudH: Float) {
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
                        listOfNotNull(toast, "訓練環境 — 模擬戦闘のみ"), hudH - HINT_TOP,
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
                    else if (touchEnabled) "惑星をタップで着陸" else "惑星に近づいて [L] で着陸"
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
                            "ゲート鍵 $shards/${gateNeed()}"
                        } else {
                            val dx = g.first - ppx; val dy = g.second - ppy
                            val dist = hypot(dx, dy).toInt()
                            val arrow = if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                                if (dx >= 0f) "→" else "←"
                            } else {
                                if (dy >= 0f) "↓" else "↑"
                            }
                            "ジャンプゲート $arrow $dist"
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
                    touchEnabled -> "タップで着陸"
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
            onPad -> "[L] 離陸"
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
internal fun GameScreen.questProgress(kind: QuestKind, ws: WorldState): Int = when (kind) {
    // v2.72 連鎖: each stage counts from the snapshot taken when the previous one settled.
        QuestKind.ELITES -> ws.questElites - ws.questBaseElites
        QuestKind.KILLS -> ws.questKills - ws.questBaseKills
        QuestKind.DUST -> ws.questDust - ws.questBaseDust
        QuestKind.CORE -> if (ws.coreVisited) 1 else 0
        QuestKind.PROTECT -> ws.questPredators - ws.questBasePredators // v2.69
        QuestKind.OBSERVE -> (ws.questTime - ws.questBaseTime).toInt() // v2.69: whole seconds
    }

/** v2.45/72 星の依頼: the chip shows the CURRENT link of the chain, or the satisfied star. */
internal fun GameScreen.questChip(ws: WorldState): String? {
        val pid = session.landedPlanetId ?: return null
        val b = ws.biome ?: return null
        if (ws.questStage >= PlanetQuest.CHAIN) return "依頼 完了 — この星は満ちている"
        val q = PlanetQuest.questFor(pid, b, ws.questStage)
        val prog = questProgress(q.kind, ws)
        val key = prog.coerceAtMost(q.target) + ws.questStage * 1000
        if (key != questChipKey) {
            val pay = io.github.panda17tk.arpg.planet.WeatherQuest.rewardFor(q, ws.weather) // v2.109
            val bonus = if (pay > q.rewardDust) "　※この空なら${pay}屑" else ""
            questChip = "依頼${ws.questStage + 1}/${PlanetQuest.CHAIN}　${q.line}$bonus　$prog/${q.target}"
            questChipKey = key
        }
        return questChip
    }

/** The cached surface goal chips (max 2); rebuilt only when the deciding inputs change (§14.2). */
internal fun GameScreen.surfaceChips(biome: PlanetBiome, ws: WorldState, elites: Int): List<String> {
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

/** v2.60 起動診断: the current prompt panel (+ boot choice / the always-there skip). */
internal fun GameScreen.drawTutorial(hudW: Float, hudH: Float) {
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
internal fun GameScreen.handleTutorialTaps(): Boolean {
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

internal fun GameScreen.drawInventory() {
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
            layoutEditLabel = "ボタン配置を編集", // v2.56
            pois = navPois().map { (pos, c, _) -> // v2.108: the same landmarks the edge markers point at
                Triple(floor(pos.first / Tuning.TILE).toInt(), floor(pos.second / Tuning.TILE).toInt(), c)
            },
        )
    }

/** v2.108 ナビ: this sky's landmarks — position, colour, and a one-glyph label. */
internal fun GameScreen.navPois(): List<Triple<Pair<Float, Float>, com.badlogic.gdx.graphics.Color, String>> {
    val ws = gw.worldState
    return buildList {
        if (ws.mode == WorldMode.SPACE) {
            ws.gate?.let { add(Triple(it, cNavGate, "門")) }
            ws.controlCore?.let { add(Triple(it, cNavCore, "核")) }
            ws.trader?.let { add(Triple(it, cNavTrader, "商")) }
            ws.wrecks.forEach { add(Triple(it, cNavWreck, "船")) }
        } else {
            ws.escapePad?.let { add(Triple(it, cNavPad, "発")) }
            ws.memoryCore?.let { add(Triple(it, cNavMemory, "核")) }
            if (!ws.vaultEntered) ws.vault?.let { add(Triple(it, cNavVault, "遺")) }
        }
    }
}

/** v2.108 ナビ: off-screen landmarks sit as small labeled dots on the screen edge. */
internal fun GameScreen.drawNavMarkers() {
    if (overlay != Overlay.NONE || choosing || gw.gameOver.isOver || layoutEditing ||
        endingStage > 0
    ) return
    val pois = navPois()
    if (pois.isEmpty()) return
    hudViewport.apply()
    val hw = hudViewport.worldWidth; val hh = hudViewport.worldHeight
    val vw = worldViewport.worldWidth; val vh = worldViewport.worldHeight
    val marks = pois.mapNotNull { (pos, c, label) ->
        io.github.panda17tk.arpg.ui.EdgeMarkers.place(pos.first - camX, pos.second - camY, vw, vh, hw, hh, NAV_MARGIN)
            ?.let { Triple(it, c, label) }
    }
    if (marks.isEmpty()) return
    Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND)
    shapes.projectionMatrix = hudViewport.camera.combined
    shapes.begin(ShapeRenderer.ShapeType.Filled)
    for ((m, c, _) in marks) {
        cEventTmp.set(0.05f, 0.07f, 0.11f, 0.72f); shapes.color = cEventTmp
        shapes.rect(m.first - 11f, m.second - 11f, 11f, 11f, 22f, 22f, 1f, 1f, 45f) // the plate
        shapes.color = c
        shapes.rect(m.first - 8f, m.second - 8f, 8f, 8f, 16f, 16f, 1f, 1f, 45f) // the diamond
    }
    shapes.end()
    batch.projectionMatrix = hudViewport.camera.combined
    batch.begin()
    val bx = font.data.scaleX; val by = font.data.scaleY
    font.data.setScale(bx * 0.8f, by * 0.8f)
    font.color = com.badlogic.gdx.graphics.Color.BLACK
    for ((m, _, label) in marks) {
        bannerGlyph.setText(font, label)
        font.draw(batch, bannerGlyph, m.first - bannerGlyph.width / 2f, m.second + bannerGlyph.height / 2f)
    }
    font.color = com.badlogic.gdx.graphics.Color.WHITE
    font.data.setScale(bx, by)
    batch.end()
}

private const val NAV_MARGIN = 30f // the markers' inset from the screen edge
private val cNavGate = com.badlogic.gdx.graphics.Color.valueOf("59ccff")   // the gate's cyan
private val cNavCore = com.badlogic.gdx.graphics.Color.valueOf("ffe9b8")   // 管制核 — pale gold
private val cNavTrader = com.badlogic.gdx.graphics.Color.valueOf("ffb347") // the merchant's lamp
private val cNavWreck = com.badlogic.gdx.graphics.Color.valueOf("b0a89e")  // hull grey
private val cNavPad = com.badlogic.gdx.graphics.Color.valueOf("7fe08f")    // the way home
private val cNavMemory = com.badlogic.gdx.graphics.Color.valueOf("8fd0ff") // the archive's light
private val cNavVault = com.badlogic.gdx.graphics.Color.valueOf("c9a0ff")  // the sealed deep

/** v2.46 航海日誌: the run's present tense + all-time bests + what each visited star remembers. */
internal fun GameScreen.logbookLines(): List<String> {
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
internal fun GameScreen.marketView(): Pair<List<String>, String> {
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
