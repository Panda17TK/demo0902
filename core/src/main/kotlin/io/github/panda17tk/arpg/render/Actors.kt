package io.github.panda17tk.arpg.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.panda17tk.arpg.config.FamilyRole
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Procedural player + enemy drawing ported from legacy render/enemy-sprites.js + renderer.js.
 * All draws run inside a Filled ShapeRenderer pass (caller manages begin/end + blend). World is
 * y-down (matches the legacy canvas), so -y is "up" on screen.
 */
object Actors {
    private val TAU = (Math.PI * 2.0).toFloat()

    private const val TINY_FISH_W = 13f // v2.167: at or under this body width, a fish is crowd
    private val SHADOW = Color(0f, 0f, 0f, 0.30f)

    private val SHADOW_LEGS = Color(0.10f, 0.09f, 0.08f, 0.9f) // v2.129: creature legs read as dark strokes
    private val TUSK = Color(0.94f, 0.90f, 0.80f, 1f) // v2.129: boar ivory
    private val PLAYER = Color.valueOf("7ab0ff")
    private val PLAYER_HIT = Color.valueOf("ff9aa2")
    private val BELLY = Color(1f, 1f, 1f, 0.18f)
    private val GUN = Color.valueOf("2b3a50")
    private val GUN_TIP = Color.valueOf("1a2738")
    private val MUZZLE = Color.valueOf("fff1c0")
    private val THRUST_OUT = Color(1f, 0.40f, 0.10f, 0.45f)  // soft orange plume (the trailing tail)
    private val THRUST_MID = Color(1f, 0.62f, 0.18f, 0.80f)  // orange body
    private val THRUST_CORE = Color(1f, 0.93f, 0.70f, 0.95f) // hot near-white nozzle
    private val OC_OUT = Color(0.25f, 0.55f, 1f, 0.45f)      // v2.37: an OC thruster burns blue
    private val OC_MID = Color(0.45f, 0.75f, 1f, 0.80f)
    private val OC_CORE = Color(0.85f, 0.95f, 1f, 0.95f)
    private val BEAM_TIP = Color.valueOf("9fe8ff")           // v2.37: the beam gun's glowing emitter
    // v2.37 suit tints (lerped over PLAYER so the silhouette stays recognizable)
    private val SUIT_LIGHT = Color.valueOf("9fb4c8")
    private val SUIT_COMBAT = Color.valueOf("7d9a6f")
    private val SUIT_HEAVY = Color.valueOf("6a7480")
    private val SUIT_RELIC = Color.valueOf("9a86c8")
    private val SUIT_THERMAL = Color.valueOf("c88a6a")
    private val SUIT_INSULATED = Color.valueOf("8ad0e0")
    private val suitTmp = Color()
    private val EYE_DARK = Color.valueOf("16202e")
    private val RED_EYE = Color.valueOf("ff5a5a")
    private val BOSS_EYE = Color.valueOf("ffd166")
    private val SPITTER_EYE = Color.valueOf("0a0f14")
    private val STALKER_EYE = Color.valueOf("d6c2ff")
    private val RANK_GOLD = Color.valueOf("ffd166")   // v2.42: crowns + level pips
    private val RANK_IRON = Color.valueOf("4a5568")   // v2.42: pauldrons + shield slabs
    private val RANK_CLOTH = Color(1f, 1f, 1f, 0.75f) // v2.42: elder headband / shield boss
    private val BAR_BG = Color(0f, 0f, 0f, 0.6f)
    private val BAR_HI = Color.valueOf("7fe08a")
    private val BAR_MID = Color.valueOf("e0d27f")
    private val BAR_LO = Color.valueOf("e08a7f")

    private val tmp = Color()      // for shade()
    private val baseTmp = Color()  // for the per-mob base color (flash/dodge)
    private val dashTmp = Color()

    private fun shade(c: Color, amt: Int): Color { val d = amt / 255f; return tmp.set(c).add(d, d, d, 0f).clamp() }
    private fun ellipseC(s: ShapeRenderer, cx: Float, cy: Float, rx: Float, ry: Float) = s.ellipse(cx - rx, cy - ry, rx * 2f, ry * 2f)

    private fun eyes(s: ShapeRenderer, cx: Float, cy: Float, spread: Float, ry: Float, fx: Float, fy: Float, pupil: Color, r: Float, white: Boolean) {
        val l = hypot(fx, fy).let { if (it == 0f) 1f else it }
        val ex = fx / l * 3f; val ey = fy / l * 2f
        if (white) { s.color = Color.WHITE; s.circle(cx - spread, cy + ry, r + 1f, 8); s.circle(cx + spread, cy + ry, r + 1f, 8) }
        s.color = pupil; s.circle(cx - spread + ex, cy + ry + ey, r, 8); s.circle(cx + spread + ex, cy + ry + ey, r, 8)
    }

    /** v2.37: the gear shows on the body — [weaponType] shapes the gun, [armorId] tints the suit,
     *  and an OC thruster ([oc]) burns blue-white instead of orange. */
    @Suppress("LongParameterList")
    fun drawPlayer(
        s: ShapeRenderer, x: Float, y: Float, faceX: Float, faceY: Float,
        dashing: Boolean, hitFlash: Boolean, muzzle: Boolean, t: Float = 0f,
        weaponType: String? = null, armorId: String? = null, oc: Boolean = false,
        moving: Boolean = false,
    ) {
        val r = Tuning.PLAYER_RADIUS; val w = r * 2f; val h = r * 2f
        val suit = suitColor(armorId)
        // v2.85 squash & stretch: breath at rest, bob on the move, stretch through a dash,
        // flinch squash on a hit — the feet stay planted (y-down: bottom edge fixed).
        var wS = 1f; var hS = 1f; var yOff = 0f
        when {
            dashing -> if (abs(faceX) >= abs(faceY)) { wS = 1.16f; hS = 0.86f } else { wS = 0.86f; hS = 1.16f }
            moving -> yOff = -abs(sin(t * 9f)) * 1.6f
            else -> { hS = 1f + 0.035f * sin(t * 2.6f); wS = 2f - hS }
        }
        if (hitFlash) { wS *= 1.10f; hS *= 0.84f }
        val bw = w * wS; val bh = h * hS
        val by = y + (h - bh) / 2f + yOff
        s.color = SHADOW; ellipseC(s, x, y + h / 2f - 1f, w * 0.46f, h * 0.18f)
        if (dashing) {
            s.color = dashTmp.set(suit).also { it.a = 0.25f }; Draw.roundedRect(s, x - faceX * 8f - w / 2f, y - faceY * 8f - h / 2f, w, h, 5f)
            // Two thruster flames out the back (opposite facing), one off each shoulder, with a slight flicker tail.
            val bx = -faceX; val by = -faceY; val perpX = -faceY; val perpY = faceX
            val flick = 0.82f + 0.18f * sin(t * 42f)
            for (sgn in intArrayOf(-1, 1)) {
                val nx = x + bx * (r * 0.55f) + perpX * sgn * r * 0.42f
                val ny = y + by * (r * 0.55f) + perpY * sgn * r * 0.42f
                s.color = if (oc) OC_OUT else THRUST_OUT; s.circle(nx + bx * 9f * flick, ny + by * 9f * flick, r * 0.30f * flick, 8)
                s.color = if (oc) OC_MID else THRUST_MID; s.circle(nx + bx * 5f * flick, ny + by * 5f * flick, r * 0.40f * flick, 8)
                s.color = if (oc) OC_CORE else THRUST_CORE; s.circle(nx + bx * 1.5f, ny + by * 1.5f, r * 0.26f * flick, 8)
            }
        }
        drawGun(s, x, y, faceX, faceY, weaponType)
        if (muzzle) { s.color = MUZZLE; Draw.orientedRect(s, x, y, faceX, faceY, 18f, 6f, 3.5f) }
        s.color = if (hitFlash) PLAYER_HIT else suit
        Draw.roundedRect(s, x - bw / 2f, by - bh / 2f, bw, bh, 6f)
        s.color = BELLY; Draw.roundedRect(s, x - bw / 2f + 3f, by - bh / 2f + 3f, bw - 6f, bh * 0.45f, 4f)
        eyes(s, x, by, 4f, -2f, faceX, faceY, EYE_DARK, 1.5f, true)
    }

    /** Each weapon type gets its own silhouette: stub pistol, wide shotgun, long mg, glowing beam, grenade tube. */
    private fun drawGun(s: ShapeRenderer, x: Float, y: Float, fx: Float, fy: Float, weaponType: String?) {
        when (weaponType) {
            "shotgun" -> {
                s.color = GUN; Draw.orientedRect(s, x, y, fx, fy, 5f, 10f, 3.5f)
                s.color = GUN_TIP; Draw.orientedRect(s, x, y, fx, fy, 12f, 4f, 2.6f)
            }
            "mg" -> {
                s.color = GUN; Draw.orientedRect(s, x, y, fx, fy, 6f, 15f, 2.5f)
                s.color = GUN_TIP; Draw.orientedRect(s, x, y, fx, fy, 18f, 4f, 1.4f)
            }
            "beam" -> {
                s.color = GUN; Draw.orientedRect(s, x, y, fx, fy, 6f, 11f, 2f)
                s.color = BEAM_TIP; s.circle(x + fx * 18f, y + fy * 18f, 2.8f, 8)
            }
            "grenade" -> {
                s.color = GUN; Draw.orientedRect(s, x, y, fx, fy, 4f, 8f, 3.5f)
                s.color = GUN_TIP; Draw.orientedRect(s, x, y, fx, fy, 10f, 2.5f, 3f)
            }
            "smg" -> { // v2.38: compact machine pistol — short and thin
                s.color = GUN; Draw.orientedRect(s, x, y, fx, fy, 5f, 9f, 2f)
                s.color = GUN_TIP; Draw.orientedRect(s, x, y, fx, fy, 12f, 3f, 1.2f)
            }
            "rifle" -> { // v2.38: marksman rifle — the longest barrel on the field
                s.color = GUN; Draw.orientedRect(s, x, y, fx, fy, 6f, 18f, 1.8f)
                s.color = GUN_TIP; Draw.orientedRect(s, x, y, fx, fy, 22f, 4f, 1.2f)
            }
            else -> { // pistol / unknown: the classic sidearm
                s.color = GUN; Draw.orientedRect(s, x, y, fx, fy, 6f, 12f, 2.5f)
                s.color = GUN_TIP; Draw.orientedRect(s, x, y, fx, fy, 15f, 3f, 1.5f)
            }
        }
    }

    /** The suit's base color: the armor tints the classic blue — subtle, but you can tell at a glance. */
    private fun suitColor(armorId: String?): Color = when (armorId) {
        "armor_light" -> suitTmp.set(PLAYER).lerp(SUIT_LIGHT, 0.35f)
        "armor_combat" -> suitTmp.set(PLAYER).lerp(SUIT_COMBAT, 0.45f)
        "armor_heavy" -> suitTmp.set(PLAYER).lerp(SUIT_HEAVY, 0.55f)
        "armor_relic" -> suitTmp.set(PLAYER).lerp(SUIT_RELIC, 0.45f)
        "armor_thermal" -> suitTmp.set(PLAYER).lerp(SUIT_THERMAL, 0.5f)
        "armor_insulated" -> suitTmp.set(PLAYER).lerp(SUIT_INSULATED, 0.5f)
        else -> PLAYER // pilot suit / none: the classic blue
    }

    @Suppress("LongParameterList")
    fun drawMob(
        s: ShapeRenderer, kind: String, tier: String, x: Float, yIn: Float, wIn: Float, hIn: Float, color: Color,
        faceX: Float, faceY: Float, moving: Boolean, hitFlash: Boolean, dodge: Boolean,
        chargeProg: Float, enrage: Boolean, hpFrac: Float, animSeed: Float, time: Float,
        role: FamilyRole = FamilyRole.NONE, level: Int = 1,
        look: CreatureLook.Look? = null, // v2.129 野生動物の意匠: non-null routes to the animal painters
    ) {
        // v2.85 squash & stretch: breath at rest, bob on the move, flinch squash on a hit —
        // every painter below sees the scaled body; the shadow stays grounded on the raw box.
        var wS = 1f; var hS = 1f; var yOff = 0f
        when {
            hitFlash -> { wS = 1.10f; hS = 0.84f }
            moving -> yOff = -abs(sin(time * 8f + animSeed)) * hIn * 0.05f
            else -> { hS = 1f + 0.04f * sin(time * 2.2f + animSeed); wS = 2f - hS }
        }
        val w = wIn * wS; val h = hIn * hS
        val y = yIn + (hIn - h) / 2f + yOff
        val elite = tier == "midboss" || tier == "boss"
        // v2.167 性能計: the sardine crowd is the bulk of every space frame — a tiny fish keeps
        // body + tail and skips shadow, fins, belly and eye (half the primitives of the school).
        val tinyFish = look?.form == CreatureLook.Form.FISH && wIn <= TINY_FISH_W
        if (!tinyFish) { s.color = SHADOW; ellipseC(s, x, yIn + hIn / 2f - 1f, wIn * 0.46f, hIn * 0.18f) }
        val base = when {
            dodge -> baseTmp.set(Color.WHITE).also { it.a = 0.55f }
            hitFlash -> baseTmp.set(Color.WHITE)
            else -> baseTmp.set(color)
        }
        when {
            elite -> elite(s, x, y, w, h, base, color, faceX, faceY, enrage, tier == "boss", time, animSeed)
            look != null -> creature(s, x, y, w, h, base, color, faceX, faceY, moving, time, animSeed, look, tinyFish) // v2.129 / v2.167
            kind == "zombie" -> zombie(s, x, y, w, h, base, color, faceX, faceY, moving, chargeProg, time, animSeed)
            kind == "spitter" -> spitter(s, x, y, w, h, base, color, faceX, faceY, time, animSeed)
            kind == "stalker" -> stalker(s, x, y, w, h, base, color, faceX, faceY)
            else -> { s.color = base; Draw.roundedRect(s, x - w / 2f, y - h / 2f, w, h, 4f); eyes(s, x, y, 4f, -3f, faceX, faceY, EYE_DARK, 2.2f, true) }
        }
        // v2.42: rank worn on the body — a king's crown, an elder's headband, a guardian's shield
        // slab, an elite's pauldrons, and level pips for anyone who has climbed past their birth rank.
        when (role) {
            FamilyRole.KING -> { // a gold three-point crown above the head
                s.color = RANK_GOLD
                val cy = y - h / 2f - 4f
                s.rect(x - 6f, cy - 2.5f, 12f, 2.5f)
                s.triangle(x - 6f, cy - 2.5f, x - 3f, cy - 2.5f, x - 4.5f, cy - 7f)
                s.triangle(x - 1.5f, cy - 2.5f, x + 1.5f, cy - 2.5f, x, cy - 8f)
                s.triangle(x + 3f, cy - 2.5f, x + 6f, cy - 2.5f, x + 4.5f, cy - 7f)
            }
            FamilyRole.ELDER -> { s.color = RANK_CLOTH; s.rect(x - w / 2f, y - h / 2f + 1.5f, w, 2.2f) } // a pale headband
            FamilyRole.GUARDIAN -> { // a shield slab carried on the off-side
                s.color = RANK_IRON; s.rect(x - w / 2f - 4.5f, y - h * 0.30f, 4f, h * 0.60f)
                s.color = RANK_CLOTH; s.rect(x - w / 2f - 3.6f, y - 1f, 2.2f, 2f)
            }
            FamilyRole.CHILD, FamilyRole.NONE -> {}
        }
        if (elite) { // pauldrons: rank plates on both shoulders
            s.color = RANK_IRON
            s.rect(x - w / 2f - 2f, y - h / 2f - 2f, w * 0.30f, 5f)
            s.rect(x + w / 2f + 2f - w * 0.30f, y - h / 2f - 2f, w * 0.30f, 5f)
        }
        if (level > 1) { // level pips: one dot per rank climbed (capped at 4)
            s.color = RANK_GOLD
            val pips = minOf(4, level - 1)
            for (p in 0 until pips) s.circle(x - (pips - 1) * 2.5f + p * 5f, y - h / 2f - (if (elite) 16f else 11f), 1.4f, 6)
        }
        if (elite || hpFrac < 1f) {
            val bw = if (elite) maxOf(w, 40f) else w
            val by = y - h / 2f - (if (elite) 12f else 8f)
            val bh = if (elite) 4f else 3f
            s.color = BAR_BG; s.rect(x - bw / 2f, by, bw, bh)
            s.color = if (hpFrac > 0.5f) BAR_HI else if (hpFrac > 0.25f) BAR_MID else BAR_LO
            s.rect(x - bw / 2f, by, bw * hpFrac.coerceIn(0f, 1f), bh)
        }
    }

    /** v2.129 野生動物の意匠: motif-based animal painters — each form reads as its animal.
     *  All plans face horizontally (dir = sign of faceX) like the creatures on old field guides. */
    @Suppress("LongParameterList", "LongMethod")
    private fun creature(
        s: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, base: Color, color: Color,
        fx: Float, fy: Float, moving: Boolean, time: Float, seed: Float, look: CreatureLook.Look,
        tiny: Boolean = false, // v2.167 性能計: the sardine crowd draws body + tail only
    ) {
        val dir = if (fx < 0f) -1f else 1f
        val trot = if (moving) sin(time * 10f + seed) else 0f
        val dark = shade(color, -30)
        val light = shade(color, 22)
        when (look.form) {
            CreatureLook.Form.FISH -> { // v2.130 宙を泳ぐもの: a fish gliding through the void
                val swim = sin(time * 8f + seed)
                s.color = base
                ellipseC(s, x, y, w * 0.38f, h * 0.30f) // the body, nose toward dir
                s.color = shade(color, -18)
                s.triangle(x - dir * w * 0.30f, y, x - dir * w * 0.58f, y - h * 0.26f + swim * 2f, x - dir * w * 0.58f, y + h * 0.26f + swim * 2f) // tail fin, sculling
                if (!tiny) { // v2.167: the details belong to fish big enough to read them
                    s.triangle(x - dir * w * 0.02f, y - h * 0.26f, x + dir * w * 0.16f, y - h * 0.26f, x + dir * w * 0.02f, y - h * 0.48f) // dorsal fin
                    s.color = light; ellipseC(s, x + dir * w * 0.06f, y + h * 0.06f, w * 0.22f, h * 0.14f) // pale belly
                    s.color = EYE_DARK; s.circle(x + dir * w * 0.28f, y - h * 0.06f, 1.3f, 4)
                }
            }
            CreatureLook.Form.FLYER -> {
                val flap = sin(time * 11f + seed) * h * 0.45f
                s.color = base
                s.triangle(x, y, x - w * 0.65f, y - h * 0.25f - flap, x - w * 0.15f, y + h * 0.10f)
                s.triangle(x, y, x + w * 0.65f, y - h * 0.25f + flap, x + w * 0.15f, y + h * 0.10f)
                s.color = light; ellipseC(s, x, y, w * 0.22f, h * 0.30f) // the slight body between the wings
                s.color = dark; s.circle(x + dir * w * 0.14f, y - h * 0.18f, 2.2f, 6) // head
                s.color = EYE_DARK; s.circle(x + dir * w * 0.20f, y - h * 0.20f, 1.1f, 4)
            }
            CreatureLook.Form.FLOATER -> {
                s.color = base; ellipseC(s, x, y - h * 0.12f, w * 0.42f, h * 0.34f) // the bell
                s.color = light; ellipseC(s, x, y - h * 0.20f, w * 0.26f, h * 0.16f)
                s.color = dark
                for (i in -1..1) { // tendrils sway under the bell
                    val sway = sin(time * 3f + seed + i) * 2.5f
                    s.rectLine(x + i * w * 0.18f, y + h * 0.05f, x + i * w * 0.18f + sway, y + h * 0.46f, 1.6f)
                }
            }
            CreatureLook.Form.SERPENT -> {
                s.color = base
                for (i in 0..3) { // body beads trailing behind the head with a travelling wave
                    val bx = x - dir * i * w * 0.22f
                    val by = y + sin(time * 6f + seed + i * 1.1f) * h * 0.14f
                    s.circle(bx, by, (w * 0.16f) * (1f - i * 0.12f), 10)
                }
                s.color = dark; s.circle(x + dir * w * 0.16f, y, w * 0.15f, 10) // head
                s.color = EYE_DARK; s.circle(x + dir * w * 0.24f, y - 1.5f, 1.2f, 4)
            }
            CreatureLook.Form.SHELLED -> {
                s.color = SHADOW_LEGS; for (lx in intArrayOf(-1, 1)) s.rect(x + lx * w * 0.28f - 1.5f, y + h * 0.18f, 3f, h * 0.22f + trot) // stub legs
                s.color = base; arcDome(s, x, y + h * 0.10f, w * 0.44f, h * 0.42f) // the shell dome
                s.color = light; arcDome(s, x, y + h * 0.06f, w * 0.30f, h * 0.26f)
                s.color = dark; s.rect(x - w * 0.44f, y + h * 0.08f, w * 0.88f, 2.2f) // the shell rim
                s.color = dark; s.circle(x + dir * w * 0.50f, y + h * 0.12f, 2.6f, 8) // head poking out
                s.color = EYE_DARK; s.circle(x + dir * w * 0.55f, y + h * 0.10f, 1f, 4)
            }
            CreatureLook.Form.RODENT -> {
                s.color = base; ellipseC(s, x, y + h * 0.08f, w * 0.34f, h * 0.28f) // a small crouched body
                s.color = light; s.circle(x + dir * w * 0.26f, y - h * 0.06f, w * 0.16f, 10) // head
                s.color = dark
                val earH = if (look.longEars) h * 0.42f else h * 0.16f // the hare's ears stand tall
                s.rectLine(x + dir * w * 0.20f, y - h * 0.14f, x + dir * w * 0.16f, y - h * 0.14f - earH, 2f)
                s.rectLine(x + dir * w * 0.30f, y - h * 0.14f, x + dir * w * 0.28f, y - h * 0.14f - earH, 2f)
                s.rectLine(x - dir * w * 0.30f, y + h * 0.08f, x - dir * w * 0.44f, y - h * 0.02f + trot, 1.6f) // tail flick
                s.color = EYE_DARK; s.circle(x + dir * w * 0.32f, y - h * 0.08f, 1.1f, 4)
            }
            CreatureLook.Form.PREDATOR -> {
                s.color = base; ellipseC(s, x, y + h * 0.06f, w * 0.42f, h * 0.24f) // a long, low body
                s.color = SHADOW_LEGS
                for (i in intArrayOf(-1, 1)) { // trotting legs, front pair opposite the back pair
                    s.rect(x + i * w * 0.26f - 1.5f, y + h * 0.22f, 3f, h * 0.24f + trot * i)
                    s.rect(x + i * w * 0.10f - 1.5f, y + h * 0.22f, 3f, h * 0.24f - trot * i)
                }
                s.color = base; s.circle(x + dir * w * 0.40f, y - h * 0.06f, w * 0.16f, 10) // head
                s.color = dark // the pointed snout, pricked ears, and a swaying tail
                s.triangle(x + dir * w * 0.48f, y - h * 0.12f, x + dir * w * 0.66f, y - h * 0.02f, x + dir * w * 0.46f, y + h * 0.02f)
                s.triangle(x + dir * w * 0.34f, y - h * 0.16f, x + dir * w * 0.30f, y - h * 0.34f, x + dir * w * 0.42f, y - h * 0.18f)
                s.triangle(x + dir * w * 0.46f, y - h * 0.14f, x + dir * w * 0.46f, y - h * 0.30f, x + dir * w * 0.54f, y - h * 0.12f)
                if (look.bushyTail) {
                    val wag = sin(time * 5f + seed) * h * 0.10f
                    s.rectLine(x - dir * w * 0.40f, y + h * 0.02f, x - dir * w * 0.62f, y - h * 0.16f + wag, 3.2f)
                }
                s.color = RED_EYE; s.circle(x + dir * w * 0.44f, y - h * 0.10f, 1.3f, 4)
            }
            CreatureLook.Form.QUADRUPED -> {
                s.color = SHADOW_LEGS
                for (i in intArrayOf(-1, 1)) { // grazing legs
                    s.rect(x + i * w * 0.24f - 1.5f, y + h * 0.16f, 3f, h * 0.30f + trot * i)
                    s.rect(x + i * w * 0.08f - 1.5f, y + h * 0.16f, 3f, h * 0.30f - trot * i)
                }
                s.color = base; ellipseC(s, x, y, w * 0.38f, h * 0.30f) // a round grazer's body
                s.color = light; ellipseC(s, x - dir * w * 0.06f, y - h * 0.06f, w * 0.24f, h * 0.16f)
                s.color = base; s.circle(x + dir * w * 0.38f, y - h * 0.22f, w * 0.14f, 10) // head held high
                s.color = dark
                s.triangle(x + dir * w * 0.34f, y - h * 0.32f, x + dir * w * 0.30f, y - h * 0.44f, x + dir * w * 0.40f, y - h * 0.32f) // ear
                if (look.horns) { // antlers / curls above the brow
                    s.rectLine(x + dir * w * 0.36f, y - h * 0.34f, x + dir * w * 0.28f, y - h * 0.58f, 1.8f)
                    s.rectLine(x + dir * w * 0.32f, y - h * 0.48f, x + dir * w * 0.44f, y - h * 0.56f, 1.6f)
                    s.rectLine(x + dir * w * 0.44f, y - h * 0.34f, x + dir * w * 0.52f, y - h * 0.52f, 1.8f)
                }
                if (look.tusks) { // pale tusks curling up from the jaw
                    s.color = TUSK
                    s.triangle(x + dir * w * 0.46f, y - h * 0.16f, x + dir * w * 0.56f, y - h * 0.28f, x + dir * w * 0.50f, y - h * 0.14f)
                    s.color = dark
                }
                s.rectLine(x - dir * w * 0.38f, y - h * 0.04f, x - dir * w * 0.48f, y + h * 0.06f + trot, 2f) // tail
                s.color = EYE_DARK; s.circle(x + dir * w * 0.42f, y - h * 0.24f, 1.2f, 4)
            }
        }
    }

    /** The upper half of an ellipse — a shell dome (Filled). */
    private fun arcDome(s: ShapeRenderer, cx: Float, cy: Float, rx: Float, ry: Float) {
        var px = cx - rx
        var py = cy
        val steps = 14
        for (i in 1..steps) {
            val a = Math.PI.toFloat() * i / steps
            val nx = cx - cos(a) * rx
            val ny = cy - sin(a) * ry
            s.triangle(cx, cy, px, py, nx, ny)
            px = nx; py = ny
        }
    }

    private fun zombie(s: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, base: Color, color: Color, fx: Float, fy: Float, moving: Boolean, chargeProg: Float, time: Float, seed: Float) {
        val by = sin(time * 9f + seed) * (if (moving) 1.6f else 0.6f)
        val cy = y + by
        s.color = base; Draw.roundedRect(s, x - w / 2f, cy - h / 2f, w, h, 5f)
        s.color = shade(color, 18); Draw.roundedRect(s, x - w / 2f + 3f, cy - h / 2f + 3f, w - 6f, h * 0.5f, 4f)
        s.color = shade(color, -22)
        if (chargeProg >= 0f) {
            val raise = -6f - chargeProg * 6f
            s.rect(x - w / 2f - 2f, cy + raise, 4f, 10f); s.rect(x + w / 2f - 2f, cy + raise, 4f, 10f)
        } else {
            val sway = sin(time * 9f + seed) * 2f
            s.rect(x - w / 2f - 2f, cy - 2f + sway, 4f, 9f); s.rect(x + w / 2f - 2f, cy - 2f - sway, 4f, 9f)
        }
        eyes(s, x, cy, 4f, -2f, fx, fy, RED_EYE, 2.2f, true)
    }

    private fun spitter(s: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, base: Color, color: Color, fx: Float, fy: Float, time: Float, seed: Float) {
        s.color = base; ellipseC(s, x, y, w / 2f, h / 2f)
        s.color = shade(color, 30)
        for (i in 0 until 4) { val a = i / 4f * TAU + time + seed; s.circle(x + cos(a) * 4f, y + sin(a) * 3f, 1.4f, 6) }
        val l = hypot(fx, fy).let { if (it == 0f) 1f else it }
        s.color = shade(color, -34); s.circle(x + fx / l * 7.2f, y + fy / l * 4.8f + 1f, 3f, 8)
        eyes(s, x, y, 0f, -3f, fx, fy, SPITTER_EYE, 3f, true)
    }

    private fun stalker(s: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, base: Color, color: Color, fx: Float, fy: Float) {
        val ang = atan2(fy, fx); val c = cos(ang); val sn = sin(ang)
        fun rx(lx: Float, ly: Float) = x + lx * c - ly * sn
        fun ry(lx: Float, ly: Float) = y + lx * sn + ly * c
        s.color = base
        s.triangle(rx(w * 0.6f, 0f), ry(w * 0.6f, 0f), rx(0f, h * 0.42f), ry(0f, h * 0.42f), rx(-w * 0.5f, 0f), ry(-w * 0.5f, 0f))
        s.triangle(rx(w * 0.6f, 0f), ry(w * 0.6f, 0f), rx(-w * 0.5f, 0f), ry(-w * 0.5f, 0f), rx(0f, -h * 0.42f), ry(0f, -h * 0.42f))
        s.color = STALKER_EYE
        s.circle(rx(3f, -2f), ry(3f, -2f), 1.3f, 6); s.circle(rx(3f, 2f), ry(3f, 2f), 1.3f, 6)
    }

    private fun elite(s: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, base: Color, color: Color, fx: Float, fy: Float, enrage: Boolean, boss: Boolean, time: Float, seed: Float) {
        val cy = y + sin(time * 3f + seed) * 1.2f * 0.3f
        s.color = base; Draw.roundedRect(s, x - w / 2f, cy - h / 2f, w, h, 8f)
        s.color = shade(color, -26); Draw.roundedRect(s, x - w / 2f, cy - h / 2f, w, h * 0.32f, 8f)
        s.color = shade(color, 20); Draw.roundedRect(s, x - w / 2f + 4f, cy + h * 0.06f, w - 8f, h * 0.34f, 5f)
        s.color = shade(color, -40)
        for (sx in intArrayOf(-1, 1)) {
            val sf = sx.toFloat()
            s.triangle(x + sf * (w / 2f - 2f), cy - h / 2f + 4f, x + sf * (w / 2f + 6f), cy - h / 2f - 2f, x + sf * (w / 2f - 2f), cy - h / 2f + 12f)
        }
        val l = hypot(fx, fy).let { if (it == 0f) 1f else it }
        s.color = if (enrage) RED_EYE else BOSS_EYE
        for (ex in intArrayOf(-5, 5)) for (ey in intArrayOf(-3, 1)) s.circle(x + ex + fx / l * 1.8f, cy + ey + fy / l * 1.2f, 1.7f, 6)
        if (boss) {
            s.color = BOSS_EYE
            val ccy = cy - h / 2f - 4f
            s.triangle(x - 8f, ccy + 6f, x - 8f, ccy, x - 4f, ccy + 4f)
            s.triangle(x - 4f, ccy + 4f, x, ccy - 2f, x + 4f, ccy + 4f)
            s.triangle(x + 4f, ccy + 4f, x + 8f, ccy, x + 8f, ccy + 6f)
        }
    }
}
