package io.github.panda17tk.arpg.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.panda17tk.arpg.map.DecorKind
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Boomerang
import io.github.panda17tk.arpg.ecs.components.EBullet
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Meteor
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Smoke
import io.github.panda17tk.arpg.ecs.components.Speech
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.ecs.world.GameWorld
import io.github.panda17tk.arpg.item.ItemCatalog
import io.github.panda17tk.arpg.item.ItemKind
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.FacilityKind
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** The player's interpolated pose + per-frame visual flags, computed by GameScreen for the camera too.
 *  v2.37: the gear look rides along — the active weapon type shapes the drawn gun, the armor tints
 *  the suit, and an OC thruster burns blue-white. */
class PlayerPose(
    val x: Float, val y: Float, val faceX: Float, val faceY: Float,
    val dashing: Boolean, val hit: Boolean, val muzzle: Boolean,
    val weaponType: String? = null, val armorId: String? = null, val oc: Boolean = false,
    val moving: Boolean = false, // v2.85: drives the walk bob / idle breath
)

/**
 * World-space scene drawing — terrain, drift field, facilities, actors, projectiles, FX, planets,
 * bases, telegraph rings, melee slashes and speech bubbles (spec §5.1: GameScreen keeps state +
 * wiring; painting lives here). Three passes in draw() mirror the GPU state changes: one Filled
 * shape batch, one Line shape batch, then the text batch for speech. All coordinates are world
 * units (y-down camera).
 */
class SceneRenderer {
    private val glyphLayout = GlyphLayout()
    private val tmpC = Color()

    // Cached projectile/actor colors (avoid per-frame alloc), ported from the legacy renderer.
    private val cTrail = Color(0.63f, 0.82f, 1f, 0.5f)
    private val cBulletCore = Color.valueOf("eaf4ff")
    private val cEbGlow = Color(1f, 0.55f, 0.55f, 0.5f)
    private val cEbCore = Color.valueOf("ffd0d0")
    private val cBladeGlow = Color(0.6f, 0.95f, 0.75f, 0.28f) // v2.101 帰還刃
    private val cBladeCore = Color.valueOf("cfe8d8")
    private val cEbMine = Color.valueOf("ff6b6b")
    private val cGren = Color.valueOf("5b6b3a")
    private val cFuseOn = Color.valueOf("ff5a3a")
    private val cFuseOff = Color.valueOf("7a2a1a")
    private val cTelegraph = Color(1f, 0.3f, 0.2f, 0.9f)
    private val cBlink = Color(0.72f, 0.52f, 1f, 0.9f)
    private val cAmmo9 = Color.valueOf("ffe066")
    private val cAmmo12 = Color.valueOf("ff9a4d")
    private val cAmmoBeam = Color.valueOf("66e0ff")
    private val cAmmoNade = Color.valueOf("ff6b6b")
    private val cBlocks = Color.valueOf("b48a5a")
    private val cMed = Color.valueOf("7fe08a")
    private val cItem = Color.valueOf("c08bff") // v2.33: equipment spoils glow purple
    private val cPickupCase = Color.valueOf("e8ecf2")  // v2.38: med case / blade / page white
    private val cPickupBrass = Color.valueOf("c8a35a") // v2.38: cartridge brass
    private val cThrustDot = Color.valueOf("ffb060")   // v2.38: thruster drop's exhaust dot
    private val cDust = Color.valueOf("ffd980")        // v2.43: 星屑 (currency) gold
    private val cShard = Color.valueOf("7de8ff")       // v2.44: gate-key shard cyan
    private val cGateRing = Color(0.35f, 0.8f, 1f, 0.35f)
    private val cGateCore = Color(0.8f, 0.97f, 1f, 0.9f)
    private val cWreckHull = Color(0.38f, 0.40f, 0.46f, 1f)   // v2.46: dead hull grey-blue
    private val cWreckDark = Color(0.22f, 0.23f, 0.28f, 1f)   // v2.46: hull shadow / torn plating
    private val cWreckLight = Color(1f, 0.55f, 0.3f, 0.8f)    // v2.46: a dying emergency lamp
    private val tribeColors = arrayOf(
        Color.valueOf("ff6b6b"), Color.valueOf("66e0ff"), Color.valueOf("7fe08a"), Color.valueOf("ffd166"), Color.valueOf("c08bff"),
    )

    /**
     * Draw the whole world-space scene for this frame. The camera must already be positioned.
     * [memoryTones] (LP v2.30/10c): per-planet-id memory tint — 1 hostile (reddish halo), 2 grateful
     * (greenish) — so a remembered star reads from across the void. Empty map = no tinting.
     */
    fun draw(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont,
        camera: OrthographicCamera, gw: GameWorld, animTime: Float, pose: PlayerPose,
        memoryTones: Map<Long, Int> = emptyMap(),
    ) {
        this.memoryTones = memoryTones
        shapes.projectionMatrix = camera.combined
        drawFilledPass(shapes, camera, gw, animTime, pose)
        drawLinePass(shapes, gw, animTime)
        drawSpeechBubbles(shapes, batch, font, camera, gw)
    }

    private var memoryTones: Map<Long, Int> = emptyMap()

    /** Pass 1 (Filled): terrain → drift → facilities → pad → actors → projectiles → FX → pickups → smoke → planets → bases. */
    private fun drawFilledPass(
        shapes: ShapeRenderer, camera: OrthographicCamera, gw: GameWorld,
        animTime: Float, pose: PlayerPose,
    ) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawTerrain(shapes, camera, gw)
        drawDrift(shapes, gw)
        drawWater(shapes, gw, animTime) // v2.79: lakes and rivers, beneath everything that stands
        drawDecor(shapes, gw, animTime) // v2.78: the ground's furniture, under everything built
        drawFacilities(shapes, gw, animTime)
        drawEscapePad(shapes, gw)
        drawMobs(shapes, gw, animTime)
        // v2.128 捕食: mid-snap the keeper lunges toward the swallowed pickup and back.
        val chompP = (gw.fx.chompT / Fx.CHOMP_TIME).coerceIn(0f, 1f) // 1 fresh -> 0 done
        val lunge = if (chompP > 0f) sin((1f - chompP) * 3.1415927f) * 7f else 0f
        val plX = pose.x + gw.fx.chompDx * lunge
        val plY = pose.y + gw.fx.chompDy * lunge
        Actors.drawPlayer(
            shapes, plX, plY, pose.faceX, pose.faceY, pose.dashing, pose.hit, pose.muzzle, animTime,
            pose.weaponType, pose.armorId, pose.oc, pose.moving,
        )
        if (chompP > 0f) drawChomp(shapes, plX, plY, gw.fx.chompDx, gw.fx.chompDy, 1f - chompP)
        drawProjectiles(shapes, gw, animTime)
        drawMeteors(shapes, gw)  // v2.87 流星群
        drawPillars(shapes, gw)  // v2.87 依頼の答え
        drawFx(shapes, gw)
        drawPickups(shapes, gw, animTime)
        drawSmoke(shapes, gw)
        drawPlanets(shapes, gw)
        drawGate(shapes, gw, animTime)
        drawControlCore(shapes, gw, animTime) // v2.93: the ending, waiting off the gate's shoulder
        drawWrecks(shapes, gw, animTime)
        drawTrader(shapes, gw, animTime) // v2.100: the friendly vessel, if this sky hosts one
        drawComet(shapes, gw, animTime)  // v2.110: the comet and its dust-strung tail
        drawMemoryCore(shapes, gw, animTime)
        drawBases(shapes, gw, animTime)
        shapes.end()
    }

    /** Terrain tiles, culled to the visible camera region (big maps). */
    private fun drawTerrain(shapes: ShapeRenderer, camera: OrthographicCamera, gw: GameWorld) {
        val vt = Tuning.TILE
        val vhw = camera.viewportWidth / 2f + vt; val vhh = camera.viewportHeight / 2f + vt
        val minTx = maxOf(0, ((camera.position.x - vhw) / vt).toInt())
        val maxTx = minOf(gw.map.width - 1, ((camera.position.x + vhw) / vt).toInt())
        val minTy = maxOf(0, ((camera.position.y - vhh) / vt).toInt())
        val maxTy = minOf(gw.map.height - 1, ((camera.position.y + vhh) / vt).toInt())
        // On a surface, render the terrain as that planet's biome (biome ground + biome-material walls).
        WorldView.draw(
            shapes, gw.map, minTx, maxTx, minTy, maxTy,
            if (gw.worldState.mode == WorldMode.SURFACE) gw.worldState.biome else null,
        )
    }

    /**
     * Drifting debris + asteroids flowing through the void (cosmetic; behind planets). Big rocks tumble
     * (craters ride the spin), small bits are faint dust — together they make the vast space feel alive.
     */
    private fun drawDrift(shapes: ShapeRenderer, gw: GameWorld) {
        val df = gw.worldState.drift ?: return
        for (d in df.items) {
            if (d.asteroid) {
                tmpC.set(0.40f, 0.39f, 0.42f, 0.9f); shapes.color = tmpC
                shapes.circle(d.x, d.y, d.size, 9)
                tmpC.set(0.27f, 0.26f, 0.29f, 0.9f); shapes.color = tmpC
                shapes.circle(d.x + cos(d.rot) * d.size * 0.42f, d.y + sin(d.rot) * d.size * 0.42f, d.size * 0.26f, 7)
                shapes.circle(d.x - cos(d.rot) * d.size * 0.5f, d.y - sin(d.rot) * d.size * 0.5f, d.size * 0.18f, 6)
            } else {
                tmpC.set(0.52f, 0.52f, 0.58f, 0.5f); shapes.color = tmpC
                shapes.circle(d.x, d.y, d.size, 6)
            }
        }
    }

    /** v2.79 水域: layered still water — a dark rim, the body, and a slow travelling glint.
     *  Frozen ponds render pale and bright; the dead world's pools stay black and still. */
    private fun drawWater(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        val w = gw.worldState.water
        if (w.isEmpty) return
        val frozen = w.frozen
        for (l in w.lakes) {
            if (frozen) tmpC.set(0.62f, 0.76f, 0.88f, 0.95f) else tmpC.set(0.10f, 0.20f, 0.32f, 0.92f)
            shapes.color = tmpC
            shapes.ellipse(l.cx - l.rx, l.cy - l.ry, l.rx * 2f, l.ry * 2f, 40)
            if (frozen) tmpC.set(0.78f, 0.88f, 0.96f, 0.95f) else tmpC.set(0.14f, 0.30f, 0.46f, 0.95f)
            shapes.color = tmpC
            shapes.ellipse(l.cx - l.rx * 0.82f, l.cy - l.ry * 0.82f, l.rx * 1.64f, l.ry * 1.64f, 36)
            // a slow glint sliding along the surface (or a static sheen on ice)
            val gx = if (frozen) l.cx - l.rx * 0.3f else l.cx + cos(animTime * 0.4f) * l.rx * 0.4f
            val gy = l.cy + (if (frozen) l.ry * 0.25f else sin(animTime * 0.3f) * l.ry * 0.3f)
            tmpC.set(0.85f, 0.93f, 1f, if (frozen) 0.35f else 0.18f); shapes.color = tmpC
            shapes.ellipse(gx - l.rx * 0.22f, gy - l.ry * 0.10f, l.rx * 0.44f, l.ry * 0.20f, 16)
        }
        for (rv in w.rivers) {
            for (i in 0 until rv.points.size - 1) {
                val (ax, ay) = rv.points[i]; val (bx, by) = rv.points[i + 1]
                tmpC.set(0.10f, 0.20f, 0.32f, 0.92f); shapes.color = tmpC
                shapes.rectLine(ax, ay, bx, by, rv.width)
                tmpC.set(0.16f, 0.33f, 0.50f, 0.95f); shapes.color = tmpC
                shapes.rectLine(ax, ay, bx, by, rv.width * 0.6f)
            }
            // drifting flecks of current, spaced along the run
            tmpC.set(0.80f, 0.90f, 1f, 0.30f); shapes.color = tmpC
            val n = rv.points.size
            for (k in 0 until 6) {
                val f = ((animTime * 0.05f + k / 6f) % 1f) * (n - 1)
                val i0 = f.toInt().coerceIn(0, n - 2)
                val frac = f - i0
                val (ax, ay) = rv.points[i0]; val (bx, by) = rv.points[i0 + 1]
                shapes.circle(ax + (bx - ax) * frac, ay + (by - ay) * frac, rv.width * 0.12f, 6)
            }
        }
    }

    /** v2.78 装飾: trees, grass, flowers, rocks and each biome's own furniture — pure shapes,
     *  no collision, drawn beneath facilities and actors. A little life on every ground. */
    private fun drawDecor(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        val s = Tuning.TILE / 32f // decor is authored at 32px-tile scale
        for (d in gw.worldState.decor) {
            val k = d.scale * s
            when (d.kind) {
                DecorKind.TREE -> {
                    val sway = sin(animTime * 0.8f + d.x * 0.05f) * 1.2f * k
                    tmpC.set(0.30f, 0.20f, 0.12f, 1f); shapes.color = tmpC
                    shapes.rect(d.x - 1.6f * k, d.y - 2f * k, 3.2f * k, 10f * k)
                    val g = 0.32f + d.hue * 0.18f // each tree its own green
                    tmpC.set(0.10f, g, 0.14f, 1f); shapes.color = tmpC
                    shapes.circle(d.x + sway, d.y + 12f * k, 7.5f * k, 12)
                    shapes.circle(d.x - 4f * k + sway, d.y + 8f * k, 5f * k, 10)
                    shapes.circle(d.x + 4f * k + sway, d.y + 8f * k, 5f * k, 10)
                }
                DecorKind.GRASS -> {
                    tmpC.set(0.24f, 0.42f + d.hue * 0.12f, 0.22f, 0.9f); shapes.color = tmpC
                    for (i in -1..1) shapes.rectLine(d.x + i * 2f * k, d.y, d.x + i * 3f * k, d.y + (5f + i.toFloat()) * k, 1.1f * k)
                }
                DecorKind.FLOWER -> {
                    tmpC.set(0.30f, 0.45f, 0.26f, 0.9f); shapes.color = tmpC
                    shapes.rectLine(d.x, d.y, d.x, d.y + 4f * k, 1f * k)
                    when { // three petal hues across the worlds
                        d.hue < 0.34f -> tmpC.set(0.95f, 0.75f, 0.85f, 1f)
                        d.hue < 0.67f -> tmpC.set(0.95f, 0.90f, 0.55f, 1f)
                        else -> tmpC.set(0.75f, 0.80f, 0.98f, 1f)
                    }
                    shapes.color = tmpC
                    for (i in 0 until 4) { val a = i / 4f * TAU + 0.6f; shapes.circle(d.x + cos(a) * 1.8f * k, d.y + 4f * k + sin(a) * 1.8f * k, 1.3f * k, 6) }
                    tmpC.set(0.98f, 0.85f, 0.35f, 1f); shapes.color = tmpC
                    shapes.circle(d.x, d.y + 4f * k, 1f * k, 6)
                }
                DecorKind.ROCK -> {
                    tmpC.set(0.42f, 0.41f, 0.44f, 1f); shapes.color = tmpC
                    shapes.circle(d.x, d.y, 4.5f * k, 9)
                    tmpC.set(0.33f, 0.32f, 0.35f, 1f); shapes.color = tmpC
                    shapes.circle(d.x + 3f * k, d.y - 1f * k, 2.8f * k, 8)
                }
                DecorKind.DEAD_TREE -> {
                    tmpC.set(0.24f, 0.20f, 0.17f, 1f); shapes.color = tmpC
                    shapes.rect(d.x - 1.2f * k, d.y, 2.4f * k, 11f * k)
                    shapes.rectLine(d.x, d.y + 8f * k, d.x + 5f * k, d.y + 12f * k, 1.4f * k)
                    shapes.rectLine(d.x, d.y + 6f * k, d.x - 4f * k, d.y + 10f * k, 1.2f * k)
                }
                DecorKind.ICE_SPIKE -> {
                    tmpC.set(0.75f, 0.88f, 0.98f, 0.95f); shapes.color = tmpC
                    shapes.triangle(d.x - 3f * k, d.y, d.x + 3f * k, d.y, d.x, d.y + 12f * k)
                    tmpC.set(0.92f, 0.97f, 1f, 0.8f); shapes.color = tmpC
                    shapes.triangle(d.x - 1f * k, d.y, d.x + 1.5f * k, d.y, d.x + 0.4f * k, d.y + 8f * k)
                }
                DecorKind.CRYSTAL_SHARD -> {
                    val glint = 0.65f + 0.3f * sin(animTime * 1.6f + d.hue * 6f)
                    tmpC.set(0.45f, 0.75f, 0.95f, glint); shapes.color = tmpC
                    shapes.triangle(d.x - 2f * k, d.y, d.x + 2f * k, d.y, d.x + 1f * k, d.y + 7f * k)
                    tmpC.set(0.70f, 0.55f, 0.95f, glint * 0.8f); shapes.color = tmpC
                    shapes.triangle(d.x, d.y, d.x + 3.4f * k, d.y, d.x + 3f * k, d.y + 5f * k)
                }
                DecorKind.VENT -> {
                    tmpC.set(0.16f, 0.10f, 0.09f, 1f); shapes.color = tmpC
                    shapes.circle(d.x, d.y, 4f * k, 10)
                    tmpC.set(0.55f, 0.25f, 0.12f, 0.9f); shapes.color = tmpC
                    shapes.circle(d.x, d.y, 2f * k, 8)
                    val puff = (animTime * 0.5f + d.hue) % 1f // a slow breath of smoke, forever rising
                    tmpC.set(0.5f, 0.45f, 0.42f, 0.30f * (1f - puff)); shapes.color = tmpC
                    shapes.circle(d.x, d.y + (4f + puff * 14f) * k, (2f + puff * 4f) * k, 8)
                }
                DecorKind.SPORE -> {
                    val bob = sin(animTime * 1.1f + d.hue * 6.28f) * 3f * k
                    tmpC.set(0.75f, 0.82f, 0.60f, 0.35f); shapes.color = tmpC
                    shapes.circle(d.x, d.y + 8f * k + bob, 4.5f * k, 10)
                    tmpC.set(0.88f, 0.92f, 0.70f, 0.55f); shapes.color = tmpC
                    shapes.circle(d.x, d.y + 8f * k + bob, 2f * k, 8)
                }
                DecorKind.CAIRN -> {
                    tmpC.set(0.40f, 0.39f, 0.37f, 1f); shapes.color = tmpC
                    shapes.circle(d.x, d.y, 3.6f * k, 8)
                    shapes.circle(d.x + 0.5f * k, d.y + 3.4f * k, 2.6f * k, 8)
                    tmpC.set(0.50f, 0.49f, 0.46f, 1f); shapes.color = tmpC
                    shapes.circle(d.x - 0.3f * k, d.y + 6f * k, 1.8f * k, 8)
                }
            }
        }
    }

    /** Biome facilities — the society's built landmarks (camp/crater/dais/eye/shrine/ruins) on the ground. */
    private fun drawFacilities(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        for (f in gw.worldState.facilities) {
            val fr = f.radius
            when (f.kind) {
                FacilityKind.CAMP -> {
                    tmpC.set(0.34f, 0.26f, 0.16f, 0.6f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr, 26)
                    tmpC.set(0.50f, 0.40f, 0.24f, 1f); shapes.color = tmpC
                    for (i in 0 until 8) { val a = i / 8f * TAU; shapes.circle(f.x + cos(a) * fr, f.y + sin(a) * fr, fr * 0.12f, 8) }
                }
                FacilityKind.CRATER -> {
                    tmpC.set(0.12f, 0.06f, 0.05f, 1f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr, 28)
                    tmpC.set(0.92f, 0.40f, 0.12f, 0.85f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr * 0.55f, 22)
                    tmpC.set(1f, 0.78f, 0.28f, 0.9f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr * 0.25f, 16)
                }
                FacilityKind.DAIS -> {
                    tmpC.set(0.80f, 0.88f, 0.96f, 0.9f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr, 26)
                    tmpC.set(0.60f, 0.72f, 0.86f, 1f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr * 0.6f, 22)
                }
                FacilityKind.EYE -> {
                    tmpC.set(0.62f, 0.56f, 0.36f, 0.30f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr, 30)
                    tmpC.set(0.86f, 0.78f, 0.50f, 0.40f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr * 0.6f, 24)
                    tmpC.set(0.18f, 0.16f, 0.24f, 0.7f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr * 0.25f, 16)
                }
                FacilityKind.SHRINE -> {
                    tmpC.set(0.30f, 0.30f, 0.36f, 0.9f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr, 22)
                    tmpC.set(0.55f, 0.55f, 0.64f, 1f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr * 0.5f, 18)
                    tmpC.set(0.80f, 0.85f, 1f, 0.9f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr * 0.18f, 12)
                }
                FacilityKind.RUIN -> {
                    tmpC.set(0.45f, 0.43f, 0.40f, 1f); shapes.color = tmpC
                    shapes.circle(f.x - fr * 0.4f, f.y - fr * 0.2f, fr * 0.45f, 10)
                    shapes.circle(f.x + fr * 0.3f, f.y + fr * 0.25f, fr * 0.38f, 10)
                    shapes.circle(f.x + fr * 0.1f, f.y - fr * 0.4f, fr * 0.30f, 8)
                }
                FacilityKind.NEST -> { // a woven bowl cradling pale eggs
                    tmpC.set(0.36f, 0.30f, 0.18f, 0.9f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr, 20)
                    tmpC.set(0.22f, 0.18f, 0.10f, 1f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr * 0.62f, 18)
                    tmpC.set(0.90f, 0.88f, 0.78f, 1f); shapes.color = tmpC
                    for (i in 0 until 3) { val a = i / 3f * TAU; shapes.circle(f.x + cos(a) * fr * 0.32f, f.y + sin(a) * fr * 0.32f, fr * 0.16f, 10) }
                }
                FacilityKind.GRAVE -> { // turned earth + a leaning headstone
                    tmpC.set(0.18f, 0.17f, 0.16f, 1f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr, 18)
                    tmpC.set(0.40f, 0.39f, 0.37f, 1f); shapes.color = tmpC; shapes.circle(f.x, f.y - fr * 0.2f, fr * 0.32f, 8)
                }
                FacilityKind.RELIC_ALTAR -> { // a stone pedestal with a pulsing relic
                    tmpC.set(0.28f, 0.26f, 0.20f, 1f); shapes.color = tmpC; shapes.circle(f.x, f.y, fr, 18)
                    tmpC.set(0.95f, 0.85f, 0.45f, 0.5f + 0.25f * sin(animTime * 2.4f)); shapes.color = tmpC
                    shapes.circle(f.x, f.y, fr * 0.4f, 16)
                }
            }
        }
    }

    /** Escape pad — a glowing return ring at the surface landing point (drawn on the ground, under the actors). */
    private fun drawEscapePad(shapes: ShapeRenderer, gw: GameWorld) {
        val pad = gw.worldState.escapePad ?: return
        tmpC.set(0.45f, 0.85f, 1f, 0.30f); shapes.color = tmpC
        shapes.circle(pad.first, pad.second, Tuning.TILE * 1.3f, 28)
        tmpC.set(0.70f, 0.95f, 1f, 0.85f); shapes.color = tmpC
        shapes.circle(pad.first, pad.second, Tuning.TILE * 0.55f, 20)
    }

    private fun drawMobs(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        with(gw.world) {
            gw.world.family { all(Mob, Transform, Health, Facing, Velocity, MobAction) }.forEach { e ->
                val mt = e[Transform]; val mm = e[Mob]; val mh = e[Health]; val mf = e[Facing]; val mv = e[Velocity]; val ma = e[MobAction]
                val moving = (mt.x - mt.prevX) * (mt.x - mt.prevX) + (mt.y - mt.prevY) * (mt.y - mt.prevY) > 0.0025f || (mv.vx * mv.vx + mv.vy * mv.vy) > 4f
                val chargeProg = if (ma.chargeT > 0f) {
                    val windup = mm.def.attacks.firstOrNull { it.type == "charge_melee" }?.windup ?: 0.7f
                    (1f - ma.chargeT.coerceAtLeast(0f) / windup).coerceIn(0f, 1f)
                } else -1f
                val hpFrac = if (mh.hpMax > 0f) mh.hp / mh.hpMax else 1f
                // v2.42: rank rides into the drawing — family role ornaments + level pips.
                val role = e.getOrNull(io.github.panda17tk.arpg.ecs.components.CreatureMind)?.familyRole
                    ?: io.github.panda17tk.arpg.config.FamilyRole.NONE
                val look = if (mm.def.lifeKind == io.github.panda17tk.arpg.config.LifeKind.WILDLIFE) {
                    CreatureLook.of(mm.def.id, mm.def.wildRole) // v2.129 野生動物の意匠
                } else null
                Actors.drawMob(shapes, mm.kind, mm.tier, mt.x, mt.y, mm.def.w, mm.def.h, Color.valueOf(mm.def.color), mf.x, mf.y, moving, mh.hitFlash > 0f, ma.dodgeT > 0f, chargeProg, ma.enrageT > 0f, hpFrac, e.id * 1.3f, animTime, role, mm.level, look)
                if (mm.bountyDust > 0) { // v2.86: the head wears its price — a gold pulse + a diamond overhead
                    val pulse = 0.6f + 0.4f * sin(animTime * 5f)
                    tmpC.set(1f, 0.82f, 0.35f, 0.16f * pulse); shapes.color = tmpC
                    shapes.circle(mt.x, mt.y, (mm.def.w + mm.def.h) * 0.5f + 10f + 3f * pulse, 24)
                    tmpC.set(1f, 0.82f, 0.35f, 0.95f); shapes.color = tmpC
                    val dy = mt.y - mm.def.h / 2f - 22f + 2f * sin(animTime * 3f)
                    shapes.triangle(mt.x, dy - 5f, mt.x - 4f, dy, mt.x, dy + 5f)
                    shapes.triangle(mt.x, dy - 5f, mt.x + 4f, dy, mt.x, dy + 5f)
                }
            }
        }
    }

    /** v2.128 捕食: two pale jaws gape in front of the keeper and clack shut on the morsel. */
    private fun drawChomp(s: ShapeRenderer, x: Float, y: Float, dx: Float, dy: Float, p: Float) {
        val gape = (if (p < 0.5f) p * 2f else (1f - p) * 2f) * 9f + 1.5f // open out, snap shut
        val nx = -dy; val ny = dx
        val mx = x + dx * 13f; val my = y + dy * 13f
        tmpC.set(1f, 0.95f, 0.78f, 0.85f); s.color = tmpC
        s.triangle(mx + nx * gape, my + ny * gape, mx + dx * 9f, my + dy * 9f, mx - dx * 3f + nx * 2f, my - dy * 3f + ny * 2f)
        s.triangle(mx - nx * gape, my - ny * gape, mx + dx * 9f, my + dy * 9f, mx - dx * 3f - nx * 2f, my - dy * 3f - ny * 2f)
    }

    private fun drawProjectiles(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
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
            // v2.101 帰還刃: a spinning cross of blades with a faint jade halo.
            gw.world.family { all(Boomerang, Transform) }.forEach { e ->
                val kt = e[Transform]; val k = e[Boomerang]
                shapes.color = cBladeGlow; shapes.circle(kt.x, kt.y, 9f, 12)
                val a1 = k.spin; val a2 = k.spin + (Math.PI / 2.0).toFloat()
                shapes.color = cBladeCore
                Draw.orientedRect(shapes, kt.x, kt.y, cos(a1), sin(a1), -11f, 22f, 2.2f)
                Draw.orientedRect(shapes, kt.x, kt.y, cos(a2), sin(a2), -11f, 22f, 2.2f)
            }
        }
    }

    /** v2.87 流星群: the telegraph shadow grows as the rock drops in from "above" (-y). */
    private fun drawMeteors(shapes: ShapeRenderer, gw: GameWorld) {
        with(gw.world) {
            gw.world.family { all(Meteor, Transform) }.forEach { e ->
                val mt = e[Transform]; val m = e[Meteor]
                val k = (1f - m.fall / io.github.panda17tk.arpg.sim.WaveEvents.METEOR_FALL).coerceIn(0f, 1f)
                tmpC.set(0f, 0f, 0f, 0.15f + 0.30f * k); shapes.color = tmpC
                shapes.ellipse(mt.x - 16f * k - 4f, mt.y - 8f * k - 2f, (16f * k + 4f) * 2f, (8f * k + 2f) * 2f)
                tmpC.set(0.69f, 0.60f, 0.48f, 1f); shapes.color = tmpC
                shapes.circle(mt.x, mt.y - (1f - k) * 150f, 5.5f, 10) // the rock, dropping in
                tmpC.set(1f, 0.69f, 0.38f, 0.5f); shapes.color = tmpC
                shapes.circle(mt.x, mt.y - (1f - k) * 150f - 5f, 2.5f, 8) // its burning tail
            }
        }
    }

    /** v2.87 quest celebration: the star's answer — a column of light fading over the keeper. */
    private fun drawPillars(shapes: ShapeRenderer, gw: GameWorld) {
        gw.fx.pillars.forEach { pl ->
            val k = (1f - pl.t / pl.life).coerceIn(0f, 1f)
            tmpC.set(1f, 0.90f, 0.60f, 0.28f * k); shapes.color = tmpC
            shapes.rect(pl.x - 9f, pl.y - 190f, 18f, 190f)
            tmpC.set(1f, 0.97f, 0.85f, 0.55f * k); shapes.color = tmpC
            shapes.rect(pl.x - 3.5f, pl.y - 190f, 7f, 190f)
        }
    }

    /** Death-burst particles (gibs shrink as they expire), dash afterimages and beam flashes. */
    private fun drawFx(shapes: ShapeRenderer, gw: GameWorld) {
        gw.fx.corpses.forEach { c -> // v2.85: the body squashes flat and pales before it bursts
            val k = (1f - c.t / c.life).coerceIn(0f, 1f)
            val ch = c.h * (0.15f + 0.85f * k)
            val cw = c.w * (1f + 0.35f * (1f - k))
            tmpC.set(c.color).lerp(Color.WHITE, 0.5f * (1f - k)); tmpC.a = 0.35f + 0.55f * k
            shapes.color = tmpC
            Draw.roundedRect(shapes, c.x - cw / 2f, c.y + c.h / 2f - ch, cw, ch, 4f)
        }
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
            // v2.39: charged beams are FAT — the glow and core scale with the shot's charge width.
            shapes.color = tmpC.set(0.5f, 0.85f, 1f, 0.32f * k); Draw.orientedRect(shapes, bm.sx, bm.sy, ux, uy, 0f, len, bm.width * 3.3f)
            shapes.color = tmpC.set(0.92f, 0.98f, 1f, 0.9f * k); Draw.orientedRect(shapes, bm.sx, bm.sy, ux, uy, 0f, len, bm.width)
        }
    }

    private fun drawPickups(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        val core = gw.worldState.memoryCore // v2.87 共鳴: the passing ring lights what it touches
        with(gw.world) {
            gw.world.family { all(Pickup, Transform) }.forEach { e ->
                val pt = e[Transform]; val pk = e[Pickup]
                val bob = sin(animTime * 4f + pt.x * 0.05f) * 2f
                if (core != null) {
                    val d = hypot(pt.x - core.first, pt.y - core.second)
                    if (io.github.panda17tk.arpg.sim.Resonance.lit(animTime, d)) {
                        tmpC.set(0.65f, 0.9f, 1f, 0.35f); shapes.color = tmpC
                        shapes.circle(pt.x, pt.y + bob, 10f, 14)
                    }
                }
                drawPickupGlyph(shapes, pk.kind, pt.x, pt.y + bob)
            }
        }
    }

    /** v2.38: pickups get a real silhouette per kind instead of a bare circle. World px, y-down. */
    private fun drawPickupGlyph(shapes: ShapeRenderer, kind: String, x: Float, y: Float) {
        val c = pickupColor(kind)
        when {
            kind == "med" -> { // medkit: a white case with a green cross
                shapes.color = cPickupCase; shapes.rect(x - 5f, y - 4f, 10f, 8f)
                shapes.color = c
                shapes.rect(x - 1.2f, y - 3f, 2.4f, 6f); shapes.rect(x - 3f, y - 1.2f, 6f, 2.4f)
            }
            kind == "blocks" -> { // building material: a small crate with a lid line
                shapes.color = c; shapes.rect(x - 4.5f, y - 4.5f, 9f, 9f)
                shapes.color = cPickupCase; shapes.rect(x - 4.5f, y - 1f, 9f, 1.6f)
            }
            kind == "staminaInf" -> { // stim: a lightning bolt
                shapes.color = c
                shapes.triangle(x - 2.5f, y - 5f, x + 2f, y - 5f, x - 0.5f, y - 0.5f)
                shapes.triangle(x + 2.5f, y + 5f, x - 2f, y + 5f, x + 0.5f, y + 0.5f)
            }
            kind == "dashUp" -> { // dash boost: double chevrons
                shapes.color = c
                shapes.triangle(x - 4f, y - 4f, x - 4f, y + 4f, x + 0f, y)
                shapes.triangle(x + 0f, y - 4f, x + 0f, y + 4f, x + 4f, y)
            }
            kind == "smoke" -> { // smoke charge: two grey puffs
                shapes.color = c; shapes.circle(x - 2f, y, 3.2f, 8); shapes.circle(x + 2.4f, y - 1.4f, 2.6f, 8)
            }
            kind.startsWith("mat_") -> { // planet core: a cut gem (two triangles)
                shapes.color = c
                shapes.triangle(x - 4.5f, y - 1f, x + 4.5f, y - 1f, x, y + 5f)
                shapes.triangle(x - 3f, y - 4f, x + 3f, y - 4f, x + 4.5f, y - 1f)
                shapes.triangle(x - 3f, y - 4f, x - 4.5f, y - 1f, x + 4.5f, y - 1f)
            }
            kind == "shard" -> { // ゲート鍵の断片: a tall cyan crystal shard
                shapes.color = cShard
                shapes.triangle(x - 3f, y + 3f, x + 3f, y + 3f, x, y - 6f)
                shapes.triangle(x - 3f, y + 3f, x + 3f, y + 3f, x, y + 6f)
            }
            kind == "dust" -> { // 星屑: a small gold four-point star
                shapes.color = cDust
                shapes.triangle(x - 3.5f, y, x + 3.5f, y, x, y - 5f)
                shapes.triangle(x - 3.5f, y, x + 3.5f, y, x, y + 5f)
            }
            kind.startsWith("item:") -> drawItemGlyph(shapes, kind.removePrefix("item:"), x, y)
            else -> { // ammo: a cartridge — brass case + pool-colored tip
                shapes.color = cPickupBrass; shapes.rect(x - 1.8f, y - 1.5f, 3.6f, 5f)
                shapes.color = c
                shapes.triangle(x - 1.8f, y - 1.5f, x + 1.8f, y - 1.5f, x, y - 4.5f)
            }
        }
    }

    /** Equipment/consumable/lore drops: a silhouette by catalog kind, purple-accented so spoils pop. */
    private fun drawItemGlyph(shapes: ShapeRenderer, id: String, x: Float, y: Float) {
        when (ItemCatalog.byId(id)?.kind) {
            ItemKind.RANGED_WEAPON -> { // a small gun: body + barrel
                shapes.color = cItem; shapes.rect(x - 4f, y - 1f, 8f, 3f)
                shapes.rect(x + 1f, y - 3f, 4f, 2.2f)
                shapes.rect(x - 3f, y + 2f, 2f, 2.5f)
            }
            ItemKind.MELEE_WEAPON -> { // a blade + hilt
                shapes.color = cPickupCase; shapes.triangle(x - 1.2f, y + 1f, x + 1.2f, y + 1f, x, y - 6f)
                shapes.color = cItem; shapes.rect(x - 2.6f, y + 1f, 5.2f, 1.6f); shapes.rect(x - 0.8f, y + 2.6f, 1.6f, 3f)
            }
            ItemKind.THRUSTER -> { // a nozzle cone + exhaust dot
                shapes.color = cItem; shapes.triangle(x - 3.5f, y - 4f, x + 3.5f, y - 4f, x, y + 2f)
                shapes.color = cThrustDot; shapes.circle(x, y + 4f, 1.8f, 8)
            }
            ItemKind.ARMOR -> { // a shield: slab + tapered bottom
                shapes.color = cItem; shapes.rect(x - 4f, y - 4.5f, 8f, 6f)
                shapes.triangle(x - 4f, y + 1.5f, x + 4f, y + 1.5f, x, y + 5.5f)
            }
            ItemKind.ACCESSORY -> { // a charm: small diamond + stud
                shapes.color = cItem
                shapes.triangle(x - 3.5f, y, x, y - 3.5f, x + 3.5f, y)
                shapes.triangle(x - 3.5f, y, x, y + 3.5f, x + 3.5f, y)
                shapes.color = cPickupCase; shapes.circle(x, y - 4.5f, 1.2f, 6)
            }
            ItemKind.CONSUMABLE -> { // a capsule: two-tone pill
                shapes.color = cPickupCase; shapes.circle(x - 2.2f, y, 2.6f, 8); shapes.rect(x - 2.2f, y - 2.6f, 2.2f, 5.2f)
                shapes.color = cItem; shapes.circle(x + 2.2f, y, 2.6f, 8); shapes.rect(x, y - 2.6f, 2.2f, 5.2f)
            }
            ItemKind.LORE -> { // a readable: a small book with a pale page edge
                shapes.color = cItem; shapes.rect(x - 4f, y - 5f, 8f, 10f)
                shapes.color = cPickupCase; shapes.rect(x - 2.6f, y - 3.8f, 5.6f, 7.6f)
                shapes.color = cItem; shapes.rect(x - 4f, y - 5f, 1.6f, 10f) // spine
            }
            null -> { shapes.color = cItem; shapes.circle(x, y, 4f, 10) } // unknown id: the old dot
        }
    }

    /** v2.44: the system's jump gate — a slow ring of orbiting lights around a bright throat. */
    private fun drawGate(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        val g = gw.worldState.gate ?: return
        if (gw.worldState.gateReady) { // v2.87 儀式: enough shards — the gate lights and beckons
            val pulse = 0.5f + 0.5f * sin(animTime * 2.2f)
            tmpC.set(1f, 0.82f, 0.35f, 0.20f + 0.18f * pulse); shapes.color = tmpC
            shapes.circle(g.first, g.second, 58f + 6f * pulse, 40)
            tmpC.set(1f, 0.90f, 0.55f, 0.9f); shapes.color = tmpC
            for (i in 0 until 3) {
                val a = animTime * 1.6f + i * (6.2831855f / 3f)
                shapes.circle(g.first + cos(a) * 52f, g.second + sin(a) * 52f, 2.6f, 8)
            }
        }
        shapes.color = cGateRing
        shapes.circle(g.first, g.second, 46f, 32)
        shapes.color = cGateCore
        shapes.circle(g.first, g.second, 7f, 12)
        for (i in 0 until 8) {
            val a = animTime * 0.7f + i * (6.2831855f / 8f)
            shapes.circle(g.first + cos(a) * 40f, g.second + sin(a) * 40f, 3.2f, 8)
        }
    }

    /** v2.48 惑星サーバー: the surface's memory core — a dark monolith with a slow-breathing light. */
    /** v2.93 管制核: a pale gold presence — two breathing halos and a slow orbit of motes. */
    private fun drawControlCore(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        val (cx, cy) = gw.worldState.controlCore ?: return
        val breath = 0.5f + 0.5f * sin(animTime * 1.4f)
        tmpC.set(1f, 0.92f, 0.72f, 0.10f + 0.06f * breath); shapes.color = tmpC
        shapes.circle(cx, cy, 40f + 5f * breath, 36)
        tmpC.set(1f, 0.95f, 0.82f, 0.22f + 0.10f * breath); shapes.color = tmpC
        shapes.circle(cx, cy, 22f + 3f * breath, 28)
        tmpC.set(1f, 0.99f, 0.94f, 0.95f); shapes.color = tmpC
        shapes.circle(cx, cy, 6f, 16)
        for (k in 0 until 3) {
            val a = animTime * 0.7f + k * (TAU / 3f)
            tmpC.set(1f, 0.9f, 0.6f, 0.8f); shapes.color = tmpC
            shapes.circle(cx + cos(a) * 30f, cy + sin(a) * 30f, 2f, 8)
        }
    }

    private fun drawMemoryCore(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        val m = gw.worldState.memoryCore ?: return
        val (mx, my) = m
        shapes.color = cWreckDark
        shapes.rect(mx - 10f, my - 30f, 20f, 34f) // the slab (y-down: extends upward on screen)
        shapes.color = cWreckHull
        shapes.rect(mx - 14f, my + 2f, 28f, 5f) // its plinth
        // The archive light breathes — slower than any living thing.
        val breath = 0.4f + 0.6f * ((kotlin.math.sin(animTime * 0.9f) + 1f) / 2f)
        shapes.color = cGateCore
        shapes.circle(mx, my - 20f, 2.2f + 1.8f * breath, 10)
        shapes.color = cGateRing
        shapes.circle(mx, my - 20f, 7f + 3f * breath, 16)
    }

    /** v2.46 難破船: a broken hull in two pieces, torn plating, and a slowly blinking distress lamp. */
    private fun drawWrecks(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        for ((i, w) in gw.worldState.wrecks.withIndex()) {
            val (wx, wy) = w
            val tilt = 18f + i * 31f // each wreck lies at its own angle
            shapes.color = cWreckHull
            shapes.rect(wx - 34f, wy - 8f, 34f, 8f, 46f, 15f, 1f, 1f, tilt)
            shapes.color = cWreckDark
            shapes.rect(wx + 6f, wy + 2f, 0f, 0f, 26f, 11f, 1f, 1f, tilt - 24f) // sheared-off stern
            shapes.rect(wx - 14f, wy - 14f, 0f, 0f, 10f, 4f, 1f, 1f, tilt + 40f) // drifting plate
            // The distress lamp still breathes, slowly — visible from far off.
            val blink = 0.35f + 0.65f * ((kotlin.math.sin(animTime * 2.1f + i) + 1f) / 2f)
            shapes.color = cWreckLight
            shapes.circle(wx + 10f, wy - 4f, 2.6f + 1.4f * blink, 10)
        }
    }

    /** v2.100 行商船: an intact hull with warm cabin lamps — unmistakably NOT a wreck. */
    private fun drawTrader(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        val (tx, ty) = gw.worldState.trader ?: return
        val bob = sin(animTime * 0.8f) * 2.5f // a gentle ride on the void's swell
        // hull: one whole body and a raised cabin, sitting level (wrecks lie tilted and torn)
        shapes.color = cWreckHull
        shapes.rect(tx - 30f, ty - 8f + bob, 60f, 16f)
        shapes.color = cWreckDark
        shapes.rect(tx - 12f, ty + 8f + bob, 26f, 10f)
        // the awning stripe over the stall side — the merchant's sign, warm against the grey
        tmpC.set(0.92f, 0.62f, 0.28f, 0.9f); shapes.color = tmpC
        shapes.rect(tx - 30f, ty + 8f + bob, 16f, 4f)
        // cabin lamps breathe warm and slow — a lit window, not a distress blink
        val glow = 0.55f + 0.45f * ((sin(animTime * 1.1f) + 1f) / 2f)
        tmpC.set(1f, 0.82f, 0.45f, 0.16f + 0.10f * glow); shapes.color = tmpC
        shapes.circle(tx, ty + bob, 34f + 4f * glow, 24)
        tmpC.set(1f, 0.87f, 0.55f, 0.9f); shapes.color = tmpC
        shapes.circle(tx + 18f, ty + 2f + bob, 2.4f, 10)
        shapes.circle(tx - 2f, ty + 12f + bob, 2.0f, 10)
    }

    /** v2.110 彗星: a bright head, a breathing halo, and a tail fading down its own dust. */
    private fun drawComet(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        val (hx, hy) = gw.worldState.comet ?: return
        val (dx, dy) = gw.worldState.cometDir ?: return
        val breath = 0.5f + 0.5f * sin(animTime * 1.6f)
        for (i in 0 until 7) { // the tail — fading beads along the drift
            val dist = 40f + i * 52f
            tmpC.set(0.75f, 0.88f, 1f, 0.18f * (1f - i / 7f)); shapes.color = tmpC
            shapes.circle(hx + dx * dist, hy + dy * dist, 9f - i, 14)
        }
        tmpC.set(0.85f, 0.94f, 1f, 0.20f + 0.10f * breath); shapes.color = tmpC
        shapes.circle(hx, hy, 22f + 3f * breath, 24)
        tmpC.set(1f, 1f, 1f, 0.95f); shapes.color = tmpC
        shapes.circle(hx, hy, 5f, 12)
    }

    /** Smoke clouds — overlapping puffs of varying white/grey + opacity, fading over life. */
    private fun drawSmoke(shapes: ShapeRenderer, gw: GameWorld) {
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
    }

    /**
     * Planets — biome-flavoured bodies: a halo/ring, the body, then surface features (spots/caps/bands/craters),
     * all built from circles kept inside the silhouette so each planet type reads at a glance.
     */
    private fun drawPlanets(shapes: ShapeRenderer, gw: GameWorld) {
        for (p in gw.planets) {
            val r = p.radius
            // Memory tint (LP v2.30/10c): a remembered star wears its reputation as an outermost glow.
            when (memoryTones[p.id]) {
                1 -> { tmpC.set(0.95f, 0.25f, 0.18f, 0.14f); shapes.color = tmpC; shapes.circle(p.cx, p.cy, r * 1.5f, 40) }
                2 -> { tmpC.set(0.35f, 0.9f, 0.5f, 0.12f); shapes.color = tmpC; shapes.circle(p.cx, p.cy, r * 1.5f, 40) }
            }
            // halo / ring behind the body (translucent; none for the dead/lonely rocks)
            when (p.biome) {
                PlanetBiome.MAGMA -> { tmpC.set(0.95f, 0.32f, 0.12f, 0.22f); shapes.color = tmpC; shapes.circle(p.cx, p.cy, r * 1.30f, 40) }
                PlanetBiome.ICE -> { tmpC.set(0.60f, 0.92f, 0.96f, 0.18f); shapes.color = tmpC; shapes.circle(p.cx, p.cy, r * 1.30f, 40) }
                PlanetBiome.GAS -> { tmpC.set(0.82f, 0.70f, 0.45f, 0.18f); shapes.color = tmpC; shapes.circle(p.cx, p.cy, r * 1.34f, 40) }
                PlanetBiome.NATURE -> { tmpC.set(0.42f, 0.72f, 0.96f, 0.14f); shapes.color = tmpC; shapes.circle(p.cx, p.cy, r * 1.22f, 40) }
                else -> {}
            }
            // body
            when (p.biome) {
                PlanetBiome.NATURE -> tmpC.set(0.20f, 0.44f, 0.62f, 1f)   // blue-green ocean world
                PlanetBiome.MAGMA -> tmpC.set(0.24f, 0.10f, 0.09f, 1f)    // near-black, molten crust
                PlanetBiome.ICE -> tmpC.set(0.82f, 0.90f, 0.96f, 1f)
                PlanetBiome.GAS -> tmpC.set(0.74f, 0.62f, 0.40f, 1f)
                PlanetBiome.DEAD -> tmpC.set(0.42f, 0.40f, 0.38f, 1f)
                PlanetBiome.LONELY -> tmpC.set(0.26f, 0.26f, 0.30f, 1f)
            }
            shapes.color = tmpC
            shapes.circle(p.cx, p.cy, r, 44)
            // surface features
            when (p.biome) {
                PlanetBiome.NATURE -> {
                    feature(shapes, p.cx, p.cy, r, -0.25f, 0.18f, 0.28f, 0.24f, 0.52f, 0.26f, 1f)   // continents
                    feature(shapes, p.cx, p.cy, r, 0.30f, -0.20f, 0.20f, 0.24f, 0.52f, 0.26f, 1f)
                    feature(shapes, p.cx, p.cy, r, 0.06f, 0.42f, 0.16f, 0.86f, 0.92f, 0.96f, 0.7f)   // cloud band
                }
                PlanetBiome.MAGMA -> {
                    feature(shapes, p.cx, p.cy, r, -0.20f, -0.10f, 0.16f, 0.96f, 0.55f, 0.12f, 1f)   // lava pools / eruptions
                    feature(shapes, p.cx, p.cy, r, 0.28f, 0.22f, 0.12f, 1f, 0.72f, 0.22f, 1f)
                    feature(shapes, p.cx, p.cy, r, 0.05f, -0.34f, 0.09f, 1f, 0.45f, 0.10f, 1f)
                }
                PlanetBiome.ICE -> {
                    feature(shapes, p.cx, p.cy, r, 0f, -0.46f, 0.40f, 0.96f, 0.98f, 1f, 1f)          // polar cap
                    feature(shapes, p.cx, p.cy, r, 0.26f, 0.30f, 0.16f, 0.70f, 0.82f, 0.92f, 1f)
                }
                PlanetBiome.GAS -> {
                    feature(shapes, p.cx, p.cy, r, 0f, 0.30f, 0.42f, 0.66f, 0.54f, 0.34f, 1f)        // darker band
                    feature(shapes, p.cx, p.cy, r, 0f, -0.28f, 0.40f, 0.86f, 0.74f, 0.50f, 1f)       // lighter band
                    feature(shapes, p.cx, p.cy, r, -0.22f, 0.06f, 0.16f, 0.88f, 0.42f, 0.20f, 1f)    // the great storm
                }
                PlanetBiome.DEAD -> {
                    feature(shapes, p.cx, p.cy, r, -0.20f, 0.20f, 0.14f, 0.30f, 0.29f, 0.27f, 1f)    // craters
                    feature(shapes, p.cx, p.cy, r, 0.25f, -0.15f, 0.10f, 0.30f, 0.29f, 0.27f, 1f)
                    feature(shapes, p.cx, p.cy, r, 0.02f, -0.30f, 0.08f, 0.30f, 0.29f, 0.27f, 1f)
                }
                PlanetBiome.LONELY -> {
                    feature(shapes, p.cx, p.cy, r, 0.04f, 0f, 0.62f, 0.34f, 0.34f, 0.40f, 1f)        // a small rocky body
                    feature(shapes, p.cx, p.cy, r, 0.18f, -0.12f, 0.08f, 0.98f, 0.90f, 0.55f, 1f)    // a lone artificial light
                }
            }
        }
    }

    /** A planet surface feature: one circle at a fractional offset/size inside the body's silhouette. */
    private fun feature(shapes: ShapeRenderer, cx: Float, cy: Float, r: Float, fx: Float, fy: Float, fr: Float, cr: Float, cg: Float, cb: Float, ca: Float) {
        tmpC.set(cr, cg, cb, ca); shapes.color = tmpC
        shapes.circle(cx + fx * r, cy + fy * r, fr * r, 22)
    }

    /** Tribe strongholds ("stars") — a tribe-coloured aura + pulsing core. */
    private fun drawBases(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        for (base in gw.bases) {
            val col = tribeColors[base.tribe % tribeColors.size]
            tmpC.set(col.r, col.g, col.b, 0.16f); shapes.color = tmpC
            shapes.circle(base.x, base.y, base.radius + 8f, 28)
            tmpC.set(col.r, col.g, col.b, 0.5f + 0.25f * sin(animTime * 2.2f)); shapes.color = tmpC
            shapes.circle(base.x, base.y, 7f, 12)
        }
    }

    /** Pass 2 (Line): gravity-well rings, attack telegraphs and melee slash arcs. */
    private fun drawLinePass(shapes: ShapeRenderer, gw: GameWorld, animTime: Float) {
        shapes.begin(ShapeRenderer.ShapeType.Line)
        // v2.87 記憶核の共鳴: a slow pulse ring sweeps out from the core every few breaths
        gw.worldState.memoryCore?.let { core ->
            io.github.panda17tk.arpg.sim.Resonance.radius(animTime)?.let { rr ->
                tmpC.set(0.55f, 0.85f, 1f, 0.35f * (1f - rr / io.github.panda17tk.arpg.sim.Resonance.RANGE))
                shapes.color = tmpC
                shapes.circle(core.first, core.second, rr, 48)
            }
        }
        // v2.86: heavy-arrival warning rings — two expanding circles chasing each other
        gw.fx.warnRings.forEach { r ->
            val k = (r.t / r.life).coerceIn(0f, 1f)
            tmpC.set(1f, 0.35f, 0.25f, (1f - k) * 0.9f); shapes.color = tmpC
            shapes.circle(r.x, r.y, r.maxR * k + 4f, 36)
            tmpC.set(1f, 0.55f, 0.35f, (1f - k) * 0.5f); shapes.color = tmpC
            shapes.circle(r.x, r.y, (r.maxR * ((k + 0.4f) % 1f)) + 4f, 30)
        }
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
        // charge-melee telegraph rings
        with(gw.world) {
            gw.world.family { all(Mob, Transform, MobAction) }.forEach { e ->
                val mt = e[Transform]; val ma = e[MobAction]
                if (ma.charging) { shapes.color = cTelegraph; shapes.circle(mt.x, mt.y, 14f + ma.chargeT * 30f, 16) }
                if (ma.blinkChargeT > 0f) { shapes.color = cBlink; shapes.circle(mt.x, mt.y, 10f + ma.blinkChargeT * 50f, 14) }
            }
        }
        // Melee swing: a thick cyan-white crescent that swooshes outward as it fades, with a bright core arc and a
        // leading-edge spark — reads as a fast, strong slash rather than a faint line.
        gw.fx.slashes.forEach { sl ->
            val p = (sl.t / sl.life).coerceIn(0f, 1f); val k = 1f - p
            val a0 = sl.ang - 1.15f; val a1 = sl.ang + 1.15f
            // v2.81: the crescent grows with the combo swing that made it
            val rBase = (30f + p * 16f) * sl.scale; val steps = 14 // expands outward over its life (the swoosh)
            for (layer in -1..1) { // 3 offset radii → a thick glowing band
                val rr = rBase + layer * 3.5f * sl.scale
                shapes.color = tmpC.set(0.62f, 0.9f, 1f, 0.5f * k)
                for (i in 0 until steps) {
                    val b0 = a0 + (a1 - a0) * i / steps; val b1 = a0 + (a1 - a0) * (i + 1) / steps
                    shapes.line(sl.x + cos(b0) * rr, sl.y + sin(b0) * rr, sl.x + cos(b1) * rr, sl.y + sin(b1) * rr)
                }
            }
            shapes.color = tmpC.set(1f, 1f, 1f, 0.95f * k) // bright white core arc
            for (i in 0 until steps) {
                val b0 = a0 + (a1 - a0) * i / steps; val b1 = a0 + (a1 - a0) * (i + 1) / steps
                shapes.line(sl.x + cos(b0) * rBase, sl.y + sin(b0) * rBase, sl.x + cos(b1) * rBase, sl.y + sin(b1) * rBase)
            }
            shapes.color = tmpC.set(1f, 0.95f, 0.7f, 0.9f * k) // hot spark off the leading edge
            shapes.line(sl.x + cos(a1) * (rBase - 6f), sl.y + sin(a1) * (rBase - 6f), sl.x + cos(a1) * (rBase + 9f), sl.y + sin(a1) * (rBase + 9f))
        }
        shapes.end()
    }

    /** Speech bubbles — translucent plate + short line above creatures (y-down → negative scaleY flips glyphs upright). */
    private fun drawSpeechBubbles(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont,
        camera: OrthographicCamera, gw: GameWorld,
    ) {
        val bubScaleX = font.data.scaleX; val bubScaleY = font.data.scaleY
        font.data.setScale(0.2f, -0.2f) // small world-unit text; tune on device
        val speakers = gw.world.family { all(Mob, Transform, Speech) }
        // backing plates first (shapes) so short lines stay legible against the starfield / terrain
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        with(gw.world) {
            speakers.forEach { e ->
                val sp = e[Speech]
                if (sp.remaining > 0f && sp.text.isNotEmpty()) {
                    val mt = e[Transform]
                    glyphLayout.setText(font, sp.text)
                    val lw = glyphLayout.width; val lh = abs(glyphLayout.height)
                    tmpC.set(0f, 0f, 0f, 0.5f); shapes.color = tmpC
                    shapes.rect(mt.x - lw / 2f - 4f, mt.y - 20f - lh - 2f, lw + 8f, lh * 2f + 6f)
                }
            }
        }
        shapes.end()
        // then the lines themselves (batch)
        batch.projectionMatrix = camera.combined
        batch.begin()
        with(gw.world) {
            speakers.forEach { e ->
                val sp = e[Speech]
                if (sp.remaining > 0f && sp.text.isNotEmpty()) {
                    val mt = e[Transform]
                    glyphLayout.setText(font, sp.text)
                    font.draw(batch, glyphLayout, mt.x - glyphLayout.width / 2f, mt.y - 20f)
                }
            }
        }
        // v2.85 damage pops: small numbers floating off the wound, fading over their last third.
        val popC = tmpC
        gw.fx.pops.forEach { pop ->
            val k = pop.t / pop.life
            popC.set(pop.color); popC.a = if (k > 0.66f) (1f - k) * 3f else 1f
            font.color = popC
            font.data.setScale(0.16f * pop.scale, -0.16f * pop.scale)
            glyphLayout.setText(font, pop.text)
            font.draw(batch, glyphLayout, pop.x - glyphLayout.width / 2f, pop.y)
        }
        font.color = Color.WHITE
        font.data.setScale(bubScaleX, bubScaleY)
        batch.end()
    }

    private fun pickupColor(kind: String): Color = when (kind) {
        "ammo9" -> cAmmo9
        "ammo12" -> cAmmo12
        "ammoBeam" -> cAmmoBeam
        "ammoNade" -> cAmmoNade
        "blocks" -> cBlocks
        "med" -> cMed
        else -> if (kind.startsWith("item:")) cItem else Color.WHITE
    }

    private companion object {
        const val TAU = 6.2831855f
    }
}
