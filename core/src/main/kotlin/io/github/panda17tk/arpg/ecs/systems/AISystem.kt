package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ai.AiMove
import io.github.panda17tk.arpg.combat.MobAttacks
import io.github.panda17tk.arpg.config.AiConfig
import io.github.panda17tk.arpg.config.FamilyRole
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.CreatureMind
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Speech
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.pathfinding.Los
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.CircleCollision
import io.github.panda17tk.arpg.sim.Collision
import io.github.panda17tk.arpg.sim.CrashModel
import io.github.panda17tk.arpg.sim.CreatureAI
import io.github.panda17tk.arpg.sim.CreatureState
import io.github.panda17tk.arpg.sim.Dash
import io.github.panda17tk.arpg.sim.Family
import io.github.panda17tk.arpg.sim.Leveling
import io.github.panda17tk.arpg.sim.PlanetField
import io.github.panda17tk.arpg.sim.AiPressure
import io.github.panda17tk.arpg.sim.SocietySpeechLines
import io.github.panda17tk.arpg.sim.SocietyTuning
import io.github.panda17tk.arpg.sim.SpeechLines
import io.github.panda17tk.arpg.sim.Tribes
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldState
import io.github.panda17tk.arpg.sim.toPressure
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Enemy AI: flow-field chase + separation + contact + data-driven attack dispatch (legacy ai.js +
 * runAttacks). Each tick runs the same pipeline: decay timers → read the player → scan neighbours
 * (one grid pass) → flocking → behavioural mind + speech → steer → integrate & collide → contact
 * damage → attack dispatch. Wild animals are driven by WildlifeSystem instead.
 */
class AISystem(private val mobGrid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(Mob, Transform, Velocity, Body, Health, Facing, MobAction, CreatureMind, Speech) }) {

    private val map: TileMap = world.inject()
    private val flow: FlowField = world.inject()
    private val config: GameConfig = world.inject()
    private val rng: Rng = world.inject()
    private val tribes: Tribes = world.inject()
    private val planetField: PlanetField = world.inject()
    private val worldState: WorldState = world.inject()

    private val players by lazy { world.family { all(PlayerTag, Transform, Health, Velocity) } }

    /** The player's pose/components as seen this tick (components null when no player exists). */
    private class PlayerRef {
        var x = 0f; var y = 0f
        var t: Transform? = null; var h: Health? = null; var v: Velocity? = null
    }

    /** One neighbour-grid pass: separation + same-tribe cohesion + nearest hostile + ward/king context. */
    private class NeighbourScan {
        var sepX = 0f; var sepY = 0f
        var cohX = 0f; var cohY = 0f; var cohN = 0
        var hostX = 0f; var hostY = 0f; var hostD = Float.MAX_VALUE; var hostFound = false
        var hostHp: Health? = null; var hostV: Velocity? = null
        var kingNear = false; var wardThreatened = false; var wardHurt = false
        var wardNear = false; var wildPredatorNear = false // a wild predator stalking a ward also rouses guardians

        fun reset() {
            sepX = 0f; sepY = 0f
            cohX = 0f; cohY = 0f; cohN = 0
            hostX = 0f; hostY = 0f; hostD = Float.MAX_VALUE; hostFound = false
            hostHp = null; hostV = null
            kingNear = false; wardThreatened = false; wardHurt = false
            wardNear = false; wildPredatorNear = false
        }
    }

    // Reusable per-tick scratch (systems tick entities sequentially — never shared across entities).
    private val playerRef = PlayerRef()
    private val scan = NeighbourScan()
    // This tick's AI-chosen step, handed from steer() to integrateAndCollide().
    private var moveX = 0f
    private var moveY = 0f

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        val v = entity[Velocity]
        val b = entity[Body]
        val m = entity[Mob]
        if (m.def.lifeKind == LifeKind.WILDLIFE) return // wild animals are driven by WildlifeSystem, not the hostile AI
        val f = entity[Facing]
        val h = entity[Health]
        val action = entity[MobAction]
        val speech = entity[Speech]
        val dt = deltaTime
        val ai = config.ai
        // Living Planets: how this planet's society currently feels (null in space / when no context) → small nudges.
        val aiP = worldState.context?.let { SocietyTuning.ai(worldState.society.toPressure(it)) }

        decayMotionAndTimers(m, v, h, speech, dt)

        // While charging/blinking, MobActionSystem drives the mob; skip normal AI.
        if (action.charging || action.blinking) return

        val player = readPlayer(t)
        val px = player.x; val py = player.y

        val slow = if (m.tier == "normal" && h.hp <= h.hpMax * 0.5f) ai.hpSlowMul else 1f
        val enrage = if (action.enrageT > 0f) action.enrageMul else 1f
        val eff = m.speed * slow * enrage

        val dx = px - t.x; val dy = py - t.y
        val dist = hypot(dx, dy)
        val see = dist < m.def.seeRange && Los.hasLineOfSight(map, t.x, t.y, px, py)
        val toPx = if (dist > 0f) dx / dist else 1f
        val toPy = if (dist > 0f) dy / dist else 0f
        val mind = entity[CreatureMind]

        scanNeighbours(entity, t, m, px, py, ai.sepRadius)
        // A wild predator prowling beside a ward threatens it just as the player would → guardians defend.
        if (scan.wardNear && scan.wildPredatorNear) scan.wardThreatened = true
        // Gratitude (LP): a tribe that saw the player drive a predator off a child won't bristle at mere proximity.
        if (aiP?.gratitude == true && !(scan.wardNear && scan.wildPredatorNear)) scan.wardThreatened = false
        applyFlocking(t, v, eff, ai.sepRadius)

        val aggressive = advanceMind(mind, m, h, speech, dist, see, dt, aiP)

        // Tactics scale with smarts (tribe intelligence + level): smart tribes take cover + kite.
        val smarts = Leveling.smarts(tribes.intelligenceOf(m.tribe), m.level)
        val brawl = aggressive && scan.hostFound && scan.hostD <= HOSTILE_RANGE
        steer(t, v, f, m, mind, eff, dt, aggressive, brawl, smarts, see, dist, dx, dy, toPx, toPy, px, py)
        integrateAndCollide(t, v, b)

        resolveContacts(t, v, b, m, h, player, ai, brawl, dist, aggressive)
        dispatchAttacks(m, t, f, v, action, h, player, ai.iFrameContact, aggressive, dist, toPx, toPy, see)
    }

    /** Decay knockback/drift velocity and the per-mob timers (always, even while charging). */
    private fun decayMotionAndTimers(m: Mob, v: Velocity, h: Health, speech: Speech, dt: Float) {
        val decay = 0.02f.pow(dt)
        v.vx *= decay; v.vy *= decay
        // Decay gravity/crash momentum (drift) lightly so flung mobs coast, then AI reasserts control.
        val driftDecay = MOB_DRIFT_DECAY.pow(dt)
        v.driftX *= driftDecay; v.driftY *= driftDecay
        if (speech.cooldown > 0f) speech.cooldown -= dt
        if (speech.remaining > 0f) speech.remaining -= dt
        if (m.bumpCd > 0f) m.bumpCd -= dt
        if (m.dashCd > 0f) m.dashCd -= dt
        if (h.hitFlash > 0f) h.hitFlash -= dt
        for (i in m.attackCd.indices) if (m.attackCd[i] > 0f) m.attackCd[i] -= dt
    }

    /** Read the player's transform/health/velocity into the reusable [playerRef] (defaults to our spot). */
    private fun readPlayer(t: Transform): PlayerRef {
        playerRef.x = t.x; playerRef.y = t.y
        playerRef.t = null; playerRef.h = null; playerRef.v = null
        players.forEach { e ->
            playerRef.t = e[Transform]; playerRef.h = e[Health]; playerRef.v = e[Velocity]
            playerRef.x = e[Transform].x; playerRef.y = e[Transform].y
        }
        return playerRef
    }

    /** Neighbour scan (one grid pass) into the reusable [scan]: separation, cohesion, hostiles, wards. */
    private fun scanNeighbours(entity: Entity, t: Transform, m: Mob, px: Float, py: Float, sepRadius: Float) {
        scan.reset()
        val myTribe = m.tribe
        mobGrid.forNearby(t.x, t.y, COH_RADIUS) { other ->
            if (other == entity) return@forNearby
            with(world) {
                val ot = other[Transform]; val om = other[Mob]
                val ddx = t.x - ot.x; val ddy = t.y - ot.y
                val d = hypot(ddx, ddy)
                if (d < 0.0001f) return@forNearby
                if (d <= sepRadius) { val w = (sepRadius - d) / sepRadius; scan.sepX += ddx / d * w; scan.sepY += ddy / d * w }
                if (d < WARD_THREAT_RANGE && om.def.lifeKind == LifeKind.WILDLIFE &&
                    (om.def.wildRole == WildRole.PREDATOR || om.def.wildRole == WildRole.APEX)) scan.wildPredatorNear = true
                if (tribes.areHostile(myTribe, om.tribe)) {
                    if (d < scan.hostD) {
                        scan.hostD = d; scan.hostX = ot.x; scan.hostY = ot.y; scan.hostFound = true
                        scan.hostHp = other[Health]; scan.hostV = other[Velocity]
                    }
                } else if (om.tribe == myTribe) {
                    scan.cohX += ot.x; scan.cohY += ot.y; scan.cohN++
                    val ocm = other[CreatureMind]
                    if (ocm.familyRole == FamilyRole.KING) scan.kingNear = true
                    if (Family.isWard(ocm.familyRole)) {
                        scan.wardNear = true
                        if (hypot(ot.x - px, ot.y - py) < WARD_THREAT_RANGE) scan.wardThreatened = true
                        val oh = other[Health]
                        if (oh.hp <= oh.hpMax * WARD_HURT_FRAC) scan.wardHurt = true // a wounded ward calls defenders to a fury
                    }
                }
            }
        }
    }

    /** Separation pushes away from crowding neighbours; cohesion drifts toward the same-tribe centroid (herding). */
    private fun applyFlocking(t: Transform, v: Velocity, eff: Float, sepRadius: Float) {
        if (scan.sepX != 0f || scan.sepY != 0f) {
            val l = sqrt(scan.sepX * scan.sepX + scan.sepY * scan.sepY)
            v.vx += scan.sepX / l * eff * 0.5f; v.vy += scan.sepY / l * eff * 0.5f
        }
        // Cohesion: drift toward the same-tribe centroid when it isn't already crowding us (herding).
        if (scan.cohN > 0) {
            val ccx = scan.cohX / scan.cohN - t.x; val ccy = scan.cohY / scan.cohN - t.y
            val cl = hypot(ccx, ccy)
            if (cl > sepRadius) { v.vx += ccx / cl * eff * COH_W; v.vy += ccy / cl * eff * COH_W }
        }
    }

    /**
     * Living Planets society: advance the behavioural mind (guardians defend threatened wards, a nearby
     * king steels morale, the society's mood nudges warnings/mercy) and voice any state change.
     * Returns whether the creature is now aggressive (Hostile / Protect / Rally).
     */
    private fun advanceMind(
        mind: CreatureMind, m: Mob, h: Health, speech: Speech,
        dist: Float, see: Boolean, dt: Float, aiP: AiPressure?,
    ): Boolean {
        val guardian = mind.protectiveness >= GUARDIAN_MIN || mind.familyRole == FamilyRole.GUARDIAN
        // Once struck or crowded, a creature commits to the fight and stops issuing warnings.
        if (h.hitFlash > 0f || dist < WARN_NEAR) mind.provoked = true
        val hasStanding = mind.familyRole == FamilyRole.KING || mind.familyRole == FamilyRole.ELDER ||
            mind.familyRole == FamilyRole.GUARDIAN || mind.intelligence >= WARN_INTEL
        // A very hostile world skips the courtesy of a warning (a PROUD world always warns first, so never skips).
        val canWarn = !mind.provoked && hasStanding && aiP?.skipWarn != true
        val playerNear = see && dist < WARN_RANGE
        val prevState = mind.state
        // A child's death (or harm, on a vengeful world) makes guardians treat the very-near player as a threat;
        // a forgiving/indebted world eases creatures toward begging a touch sooner.
        val rally = guardian && (scan.wardThreatened || (aiP?.rallyEager == true && playerNear))
        val effMercy = (mind.mercyThreshold + (aiP?.mercyBoost ?: 0f)).coerceIn(0f, 1f)
        updateMind(mind, h, dist, dt, rally, scan.kingNear, scan.wardHurt, playerNear, canWarn, effMercy)
        val aggressive = mind.state == CreatureState.Hostile || mind.state == CreatureState.Protect ||
            mind.state == CreatureState.Rally
        if (speech.canSpeak && mind.state != prevState && speech.cooldown <= 0f) {
            speak(speech, mind, m, aggressive)
        }
        return aggressive
    }

    /** Voice a mind-state change: the society's memory first, then the state's flavour line. */
    private fun speak(speech: Speech, mind: CreatureMind, m: Mob, aggressive: Boolean) {
        // A reacting creature voices the society's memory first — a child-killer is named as one (inert if blank).
        val socLine = if (aggressive) SocietySpeechLines.triggerFor(worldState.society)?.let { SocietySpeechLines.pick(it, rng.nextInt(1000)) } else null
        if (socLine != null) {
            speech.text = socLine; speech.remaining = BUBBLE_TIME; speech.cooldown = SPEECH_CD
        } else {
            var trig = SpeechLines.forState(mind.state)
            // Flavour the first encounter: kings decree, lone survivors muse, guards rally to the throne.
            if (trig == SpeechLines.Trigger.Warn) {
                if (mind.familyRole == FamilyRole.KING) trig = SpeechLines.Trigger.KingEncounter
                else if (m.def.biome == PlanetBiome.LONELY) trig = SpeechLines.Trigger.LonelyEncounter
            } else if (trig == SpeechLines.Trigger.ProtectChild && scan.kingNear) {
                trig = SpeechLines.Trigger.ProtectKing
            }
            if (trig != null) {
                val line = SpeechLines.pick(trig, rng.nextInt(1000))
                if (line != null) { speech.text = line; speech.remaining = BUBBLE_TIME; speech.cooldown = SPEECH_CD }
            }
        }
    }

    /** Pick this tick's movement (flee/hide/brawl/cover/kite/chase), set facing, and queue a dash burst. */
    private fun steer(
        t: Transform, v: Velocity, f: Facing, m: Mob, mind: CreatureMind, eff: Float, dt: Float,
        aggressive: Boolean, brawl: Boolean, smarts: Float, see: Boolean,
        dist: Float, dx: Float, dy: Float, toPx: Float, toPy: Float, px: Float, py: Float,
    ) {
        val hasRanged = m.def.attacks.any { it.type == "shot" }
        val cover = if (aggressive && !brawl && smarts > COVER_SMARTS && see) coverDir(t.x, t.y, px, py) else null
        // Rallying defenders surge; everyone else moves at their normal effective speed.
        val moveEff = eff * if (mind.state == CreatureState.Rally) RALLY_SPEED else 1f
        var mvx = 0f; var mvy = 0f
        if (!aggressive) {
            // The creature has broken off: run, hide, freeze or simply mind its own business.
            when (mind.state) {
                CreatureState.Flee -> if (dist > 0f) { mvx = -toPx * moveEff * dt; mvy = -toPy * moveEff * dt; f.x = -toPx; f.y = -toPy }
                CreatureState.Hide -> {
                    val c = coverDir(t.x, t.y, px, py)
                    if (c != null) { mvx = c[0] * moveEff * dt; mvy = c[1] * moveEff * dt }
                    else if (dist > 0f) { mvx = -toPx * moveEff * dt; mvy = -toPy * moveEff * dt }
                    if (dist > 0f) { f.x = toPx; f.y = toPy }
                }
                CreatureState.Ignore -> {} // pacifist: holds its ground, indifferent to the player
                else -> if (dist > 0f) { f.x = toPx; f.y = toPy } // Warn / Beg / Rest / Surrender: stand, face the player
            }
        } else if (brawl) {
            // Frontline: charge the rival-tribe mob.
            val bdx = scan.hostX - t.x; val bdy = scan.hostY - t.y; val bd = hypot(bdx, bdy)
            if (bd > 0f) { mvx = bdx / bd * moveEff * dt; mvy = bdy / bd * moveEff * dt; f.x = bdx / bd; f.y = bdy / bd }
        } else if (cover != null) {
            // Smart: slip toward a wall to shield from the player, still facing them to fire.
            mvx = cover[0] * moveEff * dt; mvy = cover[1] * moveEff * dt; f.x = toPx; f.y = toPy
        } else if (smarts > KITE_SMARTS && hasRanged && see && dist in 1f..KITE_DIST) {
            // Ranged support kites: back off and support from behind the frontline.
            mvx = -toPx * moveEff * dt; mvy = -toPy * moveEff * dt; f.x = toPx; f.y = toPy
        } else {
            val (fx, fy) = AiMove.followDir(map, flow, t.x, t.y)
            if (fx != 0f || fy != 0f) {
                mvx = fx * moveEff * dt; mvy = fy * moveEff * dt; f.x = fx; f.y = fy
            } else if (see && dist > 0f) {
                mvx = dx / dist * moveEff * dt; mvy = dy / dist * moveEff * dt; f.x = dx / dist; f.y = dy / dist
            }
        }
        // A dasher thrusts in its facing direction — the burst becomes its inertial drift, then coasts.
        if (Dash.ready(m.dashes, aggressive, see, dist, m.dashCd)) {
            val (ddx, ddy) = Dash.velocity(f.x, f.y)
            v.driftX = ddx; v.driftY = ddy
            m.dashCd = Dash.COOLDOWN
        }
        moveX = mvx; moveY = mvy
    }

    private fun integrateAndCollide(t: Transform, v: Velocity, b: Body) {
        // Drift (gravity / dash / knockback momentum) rides on top of AI movement; mobs stop at walls, no damage.
        val r1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, moveX + (v.vx + v.driftX) * deltaTime, 0f)
        if (r1.hitX) v.driftX = 0f
        val r2 = Collision.moveAndCollide(map, r1.x, r1.y, b.halfW, b.halfH, 0f, moveY + (v.vy + v.driftY) * deltaTime)
        if (r2.hitY) v.driftY = 0f
        t.x = r2.x; t.y = r2.y
        // Solid planets: push out + a slight rebound. No crash/fall damage.
        val pc = CircleCollision.resolve(t.x, t.y, b.halfW, v.vx + v.driftX, v.vy + v.driftY, planetField.planets)
        if (pc.hit) {
            t.x = pc.x; t.y = pc.y
            val (rdx, rdy) = CrashModel.rebound(v.driftX, v.driftY, pc.normalX, pc.normalY, MOB_CRASH_RESTITUTION)
            v.driftX = rdx; v.driftY = rdy
        }
    }

    /** Contact: brawl a hostile-tribe mob if we're locked onto one, else bounce off (or dash-ram) the player. */
    private fun resolveContacts(
        t: Transform, v: Velocity, b: Body, m: Mob, h: Health, player: PlayerRef,
        ai: AiConfig, brawl: Boolean, dist: Float, aggressive: Boolean,
    ) {
        val pt = player.t; val ph = player.h; val pv = player.v
        if (brawl && m.bumpCd <= 0f) {
            val hh = scan.hostHp; val hv = scan.hostV
            if (hh != null && hv != null && scan.hostD < (b.halfW + 14f)) {
                val nl = scan.hostD.coerceAtLeast(0.0001f)
                hh.hp -= MOB_VS_MOB_DMG
                hv.vx += (scan.hostX - t.x) / nl * m.def.contactKB; hv.vy += (scan.hostY - t.y) / nl * m.def.contactKB
                v.vx -= (scan.hostX - t.x) / nl * m.def.contactKB * 0.5f; v.vy -= (scan.hostY - t.y) / nl * m.def.contactKB * 0.5f
                m.bumpCd = 0.4f
                if (hh.hp <= 0f) { // killing blow on a rival tribe → gain XP, level up past the threshold
                    m.xp += Leveling.xpForKill(m.level)
                    while (m.xp >= Leveling.threshold(m.level)) { m.xp -= Leveling.threshold(m.level); m.level++ }
                }
            }
        } else if (aggressive && pt != null && ph != null && pv != null && m.bumpCd <= 0f) {
            if (abs(t.x - pt.x) < (b.halfW + 11f) && abs(t.y - pt.y) < (b.halfH + 11f)) {
                // Mere contact never hurts (or shoves) the player — only explicit attacks (melee/shot/…)
                // deal damage, so melee-range mobs are free to press in and swing. But a mob slamming in
                // with dash/fling momentum is a ram: the rammed side (the player) takes a knockback along
                // the mob's motion and the rammer rebounds. (The player's dash-ram lives in MobActionSystem.)
                val driftSp = hypot(v.driftX, v.driftY)
                if (driftSp > RAM_MIN_SPEED) {
                    pv.vx += v.driftX / driftSp * ai.playerKnockback
                    pv.vy += v.driftY / driftSp * ai.playerKnockback
                    val nx = if (dist > 0f) (pt.x - t.x) / dist else 1f
                    val ny = if (dist > 0f) (pt.y - t.y) / dist else 0f
                    v.vx -= nx * m.def.contactKB; v.vy -= ny * m.def.contactKB // the rammer rebounds
                    m.bumpCd = 0.28f
                }
            }
        }
    }

    /** Data-driven attack dispatch (legacy runAttacks). Non-hostile creatures (fleeing/begging/hiding) hold fire. */
    private fun dispatchAttacks(
        m: Mob, t: Transform, f: Facing, v: Velocity, action: MobAction, h: Health,
        player: PlayerRef, iFrameContact: Float, aggressive: Boolean,
        dist: Float, toPx: Float, toPy: Float, see: Boolean,
    ) {
        val pt = player.t; val ph = player.h; val pv = player.v
        if (!aggressive || pt == null || ph == null || pv == null) return
        val usable = Leveling.attacksForLevel(m.level, m.def.attacks.size) // level unlocks more skills
        m.def.attacks.forEachIndexed { i, atk ->
            if (i < usable && m.attackCd[i] <= 0f) {
                val executed = MobAttacks.tryAttack(
                    world, atk, m.def, t, f, v, action, h, ph, pv,
                    dist, toPx, toPy, see, iFrameContact,
                    config, m.waveNum, rng,
                )
                // Enraged bosses attack faster too (legacy cdScale = 1/enrageMul).
                if (executed) m.attackCd[i] = atk.cd * if (action.enrageT > 0f) 1f / action.enrageMul else 1f
            }
        }
    }

    /** A step direction (away-ish from the player) that breaks the player's line of sight → wall cover. */
    private fun coverDir(x: Float, y: Float, px: Float, py: Float): FloatArray? {
        val away = atan2(y - py, x - px)
        val step = Tuning.TILE * 1.5f
        for (off in COVER_OFFSETS) {
            val a = away + off
            val nx = x + cos(a) * step; val ny = y + sin(a) * step
            if (!Los.hasLineOfSight(map, nx, ny, px, py)) return floatArrayOf(cos(a), sin(a))
        }
        return null
    }

    /** Advance behavioural state: hide→rest heals, the player's approach interrupts, beg→surrender, pacifists ignore. */
    private fun updateMind(
        mind: CreatureMind, h: Health, dist: Float, dt: Float,
        protectedThreatened: Boolean, kingNear: Boolean,
        wardHurt: Boolean, playerNear: Boolean, canWarn: Boolean, mercyThreshold: Float,
    ) {
        val hpFrac = if (h.hpMax > 0f) h.hp / h.hpMax else 0f
        when (mind.state) {
            CreatureState.Rest -> {
                if (dist < REST_INTERRUPT) { mind.state = CreatureState.Hostile; return }
                h.hp = minOf(h.hpMax, h.hp + REST_HEAL * dt)
                if (hpFrac >= REST_RECOVER) mind.state = CreatureState.Hostile
            }
            CreatureState.Hide -> {
                if (dist < REST_INTERRUPT) { mind.state = CreatureState.Flee; return }
                mind.stateTimer -= dt
                if (mind.stateTimer <= 0f) mind.state = CreatureState.Rest
            }
            CreatureState.Surrender -> { /* yielded: it stays down and harmless */ }
            CreatureState.Ignore -> {
                if (mind.provoked) mind.state = CreatureState.Flee // a pacifist that gets hit just runs
            }
            else -> {
                if (mind.familyRole == FamilyRole.CHILD) { mind.state = CreatureState.Flee; return } // children never fight
                val effBravery = Family.effectiveBravery(mind.bravery, kingNear) // a nearby king steels nerves
                val next = CreatureAI.nextState(
                    hpFrac, effBravery, mind.intelligence, mind.canBeg, mind.canHideAndRest,
                    mercyThreshold, protectedThreatened, mind.protectiveness,
                    wardHurt, playerNear, canWarn,
                )
                when {
                    // A creature left to beg long enough — and not struck again — lays down its arms for good.
                    next == CreatureState.Beg -> {
                        if (mind.state == CreatureState.Beg && h.hitFlash <= 0f) {
                            mind.stateTimer -= dt
                            if (mind.stateTimer <= 0f) { mind.state = CreatureState.Surrender; return }
                        } else {
                            mind.stateTimer = SURRENDER_TIME
                        }
                    }
                    next == CreatureState.Hide && mind.state != CreatureState.Hide -> mind.stateTimer = HIDE_TIME
                }
                mind.state = next
            }
        }
    }

    companion object {
        private val COH_RADIUS = Tuning.TILE * 5f // same-tribe cohesion + hostile-scan radius
        private val HOSTILE_RANGE = Tuning.TILE * 7f // lock onto a hostile-tribe mob within this range
        private const val COH_W = 0.22f // herd cohesion weight (< separation's 0.5 so herds don't collapse)
        private const val MOB_VS_MOB_DMG = 14f // contact damage between hostile tribes
        private const val RAM_MIN_SPEED = 150f // drift speed above this = a dash/fling ram → knock the player back
        private const val COVER_SMARTS = 0.55f // smarts above this → seek wall cover
        private const val KITE_SMARTS = 0.40f // smarts above this → ranged mobs kite
        private val KITE_DIST = Tuning.TILE * 5f // back off when the player is closer than this
        private const val MOB_DRIFT_DECAY = 0.8f // gravity/crash momentum bleeds ~20%/s so AI movement dominates
        private const val MOB_CRASH_RESTITUTION = 0.35f // slight outward rebound off a planet (no crash damage)
        private const val HIDE_TIME = 2.5f // seconds a smart creature stays hidden before resting
        private const val REST_HEAL = 14f // HP/sec regained while resting
        private const val REST_RECOVER = 0.85f // rested back to this HP fraction → rejoin the fight
        private val REST_INTERRUPT = Tuning.TILE * 4f // player this close interrupts hide/rest
        private const val BUBBLE_TIME = 2.2f // seconds a speech bubble stays up
        private const val SPEECH_CD = 4f // minimum seconds between a creature's lines
        private const val GUARDIAN_MIN = 0.5f // protectiveness at/above this makes a creature a guardian
        private val WARD_THREAT_RANGE = Tuning.TILE * 5f // player this close to a ward → guardians defend it
        private const val WARD_HURT_FRAC = 0.5f // a ward/king below this HP fraction makes defenders rally
        private val WARN_RANGE = Tuning.TILE * 6f // territorial creatures warn the player within this range
        private val WARN_NEAR = Tuning.TILE * 2.5f // player this close (or any hit) provokes → no more warnings
        private const val WARN_INTEL = 0.4f // intelligence at/above this lets a creature warn before fighting
        private const val RALLY_SPEED = 1.35f // rallying defenders surge faster
        private const val SURRENDER_TIME = 3f // seconds a begging creature is left unharmed before it yields
        private val COVER_OFFSETS = floatArrayOf(0f, 0.6f, -0.6f, 1.2f, -1.2f) // away ± up to ~70°
    }
}
