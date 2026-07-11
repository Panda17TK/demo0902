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
 * v2.105 分割: 演出の章 — weather, boss bar, combo chip, kill flash, event fx and banner.
 * Extracted from GameScreen (v2.105) with zero behavior change — every function is an
 * internal extension so the screen reads as chapters instead of one 2300-line scroll.
 */

/** v2.74 天候: purely cosmetic surface weather — deterministic per planet, pure in time. */
internal fun GameScreen.drawWeather(delta: Float) {
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
            WeatherKind.THUNDER -> shapes.setColor(0.06f, 0.09f, 0.22f, 0.09f) // v2.77: a darker storm
            WeatherKind.FOG -> shapes.setColor(0.60f, 0.62f, 0.66f, 0.10f)
            WeatherKind.AURORA -> shapes.setColor(0.05f, 0.12f, 0.16f, 0.05f)
            WeatherKind.CLEAR -> {}
        }
        shapes.rect(0f, 0f, w, h)
        when (weatherKind) {
            WeatherKind.RAIN -> shapes.setColor(0.60f, 0.75f, 0.95f, 0.35f)
            WeatherKind.SNOW -> shapes.setColor(0.95f, 0.97f, 1f, 0.50f)
            WeatherKind.ASH -> shapes.setColor(0.55f, 0.50f, 0.48f, 0.40f)
            WeatherKind.DUSTWIND -> shapes.setColor(0.75f, 0.68f, 0.50f, 0.30f)
            WeatherKind.THUNDER -> shapes.setColor(0.65f, 0.78f, 0.98f, 0.40f)
            else -> {}
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
        when (weatherKind) { // v2.77: the skies that are shapes, not particles
            WeatherKind.THUNDER -> { // the strike lights the whole field for a blink
                val flash = Weather.lightningAt(weatherT)
                if (flash > 0f) {
                    shapes.setColor(0.85f, 0.90f, 1f, flash * 0.18f)
                    shapes.rect(0f, 0f, w, h)
                }
            }
            WeatherKind.FOG -> { // slow grey banks sliding across, layered for depth
                shapes.setColor(0.70f, 0.72f, 0.76f, 0.06f)
                for (i in 0 until 4) {
                    val fx = (i * 0.618034f + weatherT * 0.008f * (1f + i * 0.3f)) % 1.3f - 0.15f
                    val fy = 0.15f + (i * 0.7548777f) % 0.7f
                    val r = w * (0.30f + 0.10f * (i % 3))
                    shapes.circle(fx * w, fy * h, r, 40)
                    shapes.circle(fx * w + r * 0.6f, fy * h - r * 0.2f, r * 0.75f, 36)
                }
            }
            WeatherKind.AURORA -> { // three slow ribbons high above, breathing in colour
                for (band in 0 until 3) {
                    when (band) {
                        0 -> shapes.setColor(0.25f, 0.85f, 0.55f, 0.055f)
                        1 -> shapes.setColor(0.30f, 0.70f, 0.85f, 0.045f)
                        else -> shapes.setColor(0.60f, 0.45f, 0.85f, 0.035f)
                    }
                    val baseY = h * (0.86f - band * 0.06f)
                    var x = 0f
                    while (x < w) {
                        val yy = baseY + kotlin.math.sin(x / w * 6.28f + weatherT * (0.25f + band * 0.1f) + band * 2f) * h * 0.025f
                        shapes.circle(x, yy, h * 0.035f, 10)
                        x += h * 0.03f
                    }
                }
            }
            else -> {}
        }
        shapes.end()
    }

/** v2.88: scan for the nearest-priority heavy (boss > bounty > midboss) within earshot. */
internal fun GameScreen.updateBossBar(delta: Float, ppx: Float, ppy: Float) {
        // v2.149: the heavy scan walks the whole mob family — every 0.1s is plenty for a HP bar
        bossScanT += delta
        if (bossScanT < 0.1f) {
            bossBar.update(bossScanName != null, bossScanName, bossScanFrac, delta)
            return
        }
        bossScanT = 0f
        var bestName: String? = null; var bestFrac = 1f; var bestRank = -1
        with(gw.world) {
            gw.world.family { all(Mob, Health, Transform) }.forEach { e ->
                val m = e[Mob]
                if (m.def.lifeKind == io.github.panda17tk.arpg.config.LifeKind.WILDLIFE) return@forEach
                val rank = when {
                    m.tier == "boss" -> 3
                    m.bountyDust > 0 -> 2
                    m.tier == "midboss" -> 1
                    else -> -1
                }
                if (rank <= bestRank) return@forEach
                val mt = e[Transform]
                if (hypot(mt.x - ppx, mt.y - ppy) > BOSSBAR_RANGE) return@forEach
                val mh = e[Health]
                bestRank = rank
                bestName = m.bountyName.ifEmpty { m.def.name }
                bestFrac = if (mh.hpMax > 0f) mh.hp / mh.hpMax else 1f
            }
        }
        bossScanName = bestName; bossScanFrac = bestFrac
        bossBar.update(bestName != null, bestName, bestFrac, delta)
    }

/** v2.88 ボスHPバー: name over a slim bar, top-center under the status band. */
internal fun GameScreen.drawBossBar() {
        if (!bossBar.visible) return
        hudViewport.apply()
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        val a = bossBar.k
        val bw = minOf(280f, w * 0.7f); val bh = 7f
        val x = (w - bw) / 2f
        val y = h - 132f - (1f - a) * 10f
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND)
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        cEventTmp.set(0f, 0f, 0f, 0.55f * a); shapes.color = cEventTmp
        shapes.rect(x - 4f, y - 4f, bw + 8f, bh + 8f)
        cEventTmp.set(0.28f, 0.06f, 0.08f, 0.9f * a); shapes.color = cEventTmp
        shapes.rect(x, y, bw, bh)
        cEventTmp.set(0.95f, 0.30f, 0.28f, 0.95f * a); shapes.color = cEventTmp
        shapes.rect(x, y, bw * bossBar.frac.coerceIn(0f, 1f), bh)
        shapes.end()
        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        cEventTmp.set(1f, 0.92f, 0.9f, a); font.color = cEventTmp
        bannerGlyph.setText(font, bossBar.name)
        font.draw(batch, bannerGlyph, (w - bannerGlyph.width) / 2f, y + bh + 6f + bannerGlyph.height)
        font.color = Color.WHITE
        batch.end()
    }

/** v2.92 連撃チップ: 「連撃 ×N」 riding mid-low screen while the chain window lives. */
internal fun GameScreen.drawComboChip() {
        val fxc = gw.fx
        if (fxc.comboT <= 0f || fxc.comboStep < 2) return
        hudViewport.apply()
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        val a = (fxc.comboT / 0.3f).coerceIn(0f, 1f)
        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        val bx = font.data.scaleX; val by = font.data.scaleY
        val grow = 1f + 0.06f * (fxc.comboStep - 1)
        font.data.setScale(bx * grow, by * grow)
        when { // alloc-free tinting, tier by rhythm
            fxc.comboStep >= io.github.panda17tk.arpg.combat.MeleeCombo.MAX_STEP -> cEventTmp.set(1f, 0.91f, 0.29f, a)
            fxc.comboStep >= 3 -> cEventTmp.set(1f, 0.82f, 0.48f, a)
            else -> cEventTmp.set(0.81f, 0.89f, 1f, a)
        }
        font.color = cEventTmp
        bannerGlyph.setText(font, "連撃 ×${fxc.comboStep}")
        font.draw(batch, bannerGlyph, (w - bannerGlyph.width) / 2f, h * 0.40f)
        font.color = Color.WHITE
        font.data.setScale(bx, by)
        batch.end()
    }

/** v2.88 撃破の儀式: a whole-screen white-out easing away after the killing blow. */
internal fun GameScreen.drawKillFlash() {
        val a = gw.fx.flashAlpha() * (if (softFlash) 0.35f else 1f) // v2.96 光過敏に配慮
        if (a <= 0f) return
        hudViewport.apply()
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND)
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        cEventTmp.set(1f, 0.98f, 0.92f, 0.8f * a * a); shapes.color = cEventTmp
        shapes.rect(0f, 0f, hudViewport.worldWidth, hudViewport.worldHeight)
        shapes.end()
    }

/** v2.86 イベントの見える化: each space wave event owns a screen-space look —
 *  大群 pulses the rim dark red, 磁気嵐 skates noise streaks, 清掃 sweeps a scanline. */
internal fun GameScreen.drawEventFx(delta: Float) {
        if (gw.worldState.mode != WorldMode.SPACE) return
        val ev = gw.waveState.event
        if (ev == WaveEvent.NONE || ev == WaveEvent.BOUNTY) return
        eventFxT += delta
        hudViewport.apply()
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND)
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        when (ev) {
            WaveEvent.HORDE -> { // the rim of the sky breathes dark red
                val a = 0.10f + 0.06f * sin(eventFxT * 2.6f)
                cEventTmp.set(0.55f, 0.08f, 0.06f, a); shapes.color = cEventTmp
                val band = h * 0.10f
                shapes.rect(0f, 0f, w, band); shapes.rect(0f, h - band, w, band)
                shapes.rect(0f, 0f, w * 0.06f, h); shapes.rect(w - w * 0.06f, 0f, w * 0.06f, h)
            }
            WaveEvent.STORM -> { // magnetic noise: brief streaks skating across the view
                for (i in 0 until 12) {
                    val ph = (eventFxT * (0.9f + (i % 5) * 0.13f) + i * 0.37f) % 1f
                    val sx = ((i * 0.083f + ph * 0.61f) % 1f) * w
                    val sy = ((i * 0.29f + ph * 0.83f) % 1f) * h
                    cEventTmp.set(0.6f, 0.8f, 1f, 0.16f * (1f - ph)); shapes.color = cEventTmp
                    shapes.rect(sx, sy, 46f * (1f - ph * 0.5f), 1.5f)
                }
            }
            WaveEvent.PURGE -> { // the custodians' scanline sweeps the hall, top to bottom
                val ph = (eventFxT % 2.8f) / 2.8f
                val sy = h * (1f - ph)
                cEventTmp.set(0.45f, 0.95f, 1f, 0.10f); shapes.color = cEventTmp
                shapes.rect(0f, sy - 14f, w, 14f)
                cEventTmp.set(0.65f, 1f, 1f, 0.32f); shapes.color = cEventTmp
                shapes.rect(0f, sy, w, 2f)
            }
            else -> {}
        }
        shapes.end()
    }

/** v2.86 開幕バナー: a scrim band across the upper-middle carrying the moment line. */
internal fun GameScreen.drawEventBanner() {
        if (!eventBanner.active) return
        val a = eventBanner.alpha()
        if (a <= 0f) return
        hudViewport.apply()
        val w = hudViewport.worldWidth; val h = hudViewport.worldHeight
        val bandH = 52f
        val y = h * 0.64f
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND)
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        cEventTmp.set(0f, 0f, 0f, 0.55f * a); shapes.color = cEventTmp
        shapes.rect(0f, y, w, bandH)
        cEventTmp.set(1f, 0.62f, 0.30f, 0.85f * a); shapes.color = cEventTmp
        shapes.rect(0f, y + bandH, w, 1.5f); shapes.rect(0f, y - 1.5f, w, 1.5f)
        shapes.end()
        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        val bx = font.data.scaleX; val by = font.data.scaleY
        font.data.setScale(bx * 1.25f, by * 1.25f)
        val bannerLine = io.github.panda17tk.arpg.i18n.Lang.tr(eventBanner.text) // v2.162 英語化第4弾
        bannerGlyph.setText(font, bannerLine)
        if (bannerGlyph.width > w - 32f) {
            val k = (w - 32f) / bannerGlyph.width
            font.data.setScale(bx * 1.25f * k, by * 1.25f * k)
            bannerGlyph.setText(font, bannerLine)
        }
        cEventTmp.set(1f, 0.94f, 0.85f, a); font.color = cEventTmp
        font.draw(batch, bannerGlyph, (w - bannerGlyph.width) / 2f + eventBanner.slide(), y + bandH / 2f + bannerGlyph.height / 2f)
        font.color = Color.WHITE
        font.data.setScale(bx, by)
        batch.end()
    }


/** v2.112 低HP警告: under 30% hull the screen's rim breathes red — quiet, and softer still
 *  with 閃光をやわらげる on. Purely cosmetic; hidden behind overlays and the ending. */
internal fun GameScreen.drawLowHpPulse(delta: Float) {
    if (overlay != Overlay.NONE || choosing || gw.gameOver.isOver || endingStage > 0) return
    val (hp, hpMax) = with(gw.world) { val h = gw.player[Health]; h.hp to h.hpMax }
    if (hpMax <= 0f || hp / hpMax >= 0.30f) { lowHpT = 0f; return }
    lowHpT += delta
    val breath = 0.5f + 0.5f * kotlin.math.sin(lowHpT * 3.2f)
    val a = (0.10f + 0.10f * breath) * (1f - (hp / hpMax) / 0.30f + 0.4f).coerceAtMost(1f) *
        (if (softFlash) 0.5f else 1f)
    hudViewport.apply()
    val w = hudViewport.worldWidth; val h2 = hudViewport.worldHeight
    val edge = 26f
    Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND)
    shapes.projectionMatrix = hudViewport.camera.combined
    shapes.begin(ShapeRenderer.ShapeType.Filled)
    cEventTmp.set(0.85f, 0.12f, 0.10f, a); shapes.color = cEventTmp
    shapes.rect(0f, 0f, w, edge)
    shapes.rect(0f, h2 - edge, w, edge)
    shapes.rect(0f, edge, edge, h2 - edge * 2f)
    shapes.rect(w - edge, edge, edge, h2 - edge * 2f)
    shapes.end()
}
