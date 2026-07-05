package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.GameConfig
import com.badlogic.gdx.graphics.Color
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Buff
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.FullThrottle
import io.github.panda17tk.arpg.item.ItemTrait
import io.github.panda17tk.arpg.map.Biome
import io.github.panda17tk.arpg.map.Biomes
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.CircleCollision
import io.github.panda17tk.arpg.sim.Collision
import io.github.panda17tk.arpg.sim.CrashModel
import io.github.panda17tk.arpg.sim.Locomotion
import io.github.panda17tk.arpg.sim.PlanetField
import io.github.panda17tk.arpg.sim.SpaceDrive
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.sim.WorldState
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow

class MovementSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina, Body, Velocity, Mods, Health, Buff) }) {
    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val config: GameConfig = world.inject()
    private val planetField: PlanetField = world.inject()
    private val worldState: WorldState = world.inject()
    private val fx: Fx = world.inject()
    private var prevDash = false // 縮地 press-edge tracker (single player; system-local scratch)

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        val f = entity[Facing]
        val s = entity[Stamina]
        val b = entity[Body]
        val v = entity[Velocity]
        val mods = entity[Mods]
        val h = entity[Health]
        val tag = entity[PlayerTag]
        val bf = entity[Buff]
        val dt = deltaTime
        if (h.iTime > 0f) h.iTime -= dt // tick down invuln (fixes stuck pink/invincible)
        if (h.hitFlash > 0f) h.hitFlash -= dt
        if (bf.staminaInfT > 0f) bf.staminaInfT -= dt
        if (bf.dashUpT > 0f) bf.dashUpT -= dt
        if (bf.heatProofT > 0f) bf.heatProofT -= dt
        if (bf.coldProofT > 0f) bf.coldProofT -= dt
        if (bf.magnetT > 0f) bf.magnetT -= dt
        if (bf.regenT > 0f) bf.regenT -= dt
        val staInf = bf.staminaInfT > 0f // pickup buff: dash freely without draining stamina

        val mv = Locomotion.keyboardDirection(input.left, input.right, input.up, input.down)
        // Resolve facing before thrust: the right stick aims, else face the way you move. A button dash
        // launches along this facing, so it must be current.
        if (input.aiming) { f.x = input.aimX; f.y = input.aimY }
        else if (mv.isMoving) { f.x = mv.dirX; f.y = mv.dirY }

        // Block biomes around the player drive terrain effects (magma burns, snow slows, grass restores).
        // On a planet surface the block material leans to the planet's biome (lava on magma worlds, etc.).
        val ptx = floor(t.x / Tuning.TILE).toInt(); val pty = floor(t.y / Tuning.TILE).toInt()
        val surf = if (worldState.mode == WorldMode.SURFACE) worldState.biome else null
        var magma = false; var snow = false; var grass = false
        for (oy in -1..1) for (ox in -1..1) {
            if (!map.solidAt(ptx + ox, pty + oy)) continue
            val mat = if (surf != null) Biomes.surface(surf, ptx + ox, pty + oy) else Biomes.of(ptx + ox, pty + oy)
            when (mat) {
                Biome.MAGMA -> magma = true; Biome.SNOW -> snow = true; Biome.GRASS -> grass = true; else -> {}
            }
        }

        // Decide this frame's thrust. Stick dash / button dash need stamina (unless the buff is up);
        // when overheated, a big push or held button just falls back to a capped walk.
        val canDash = staInf || (!s.overheat && s.value > 0f)
        var mode = SpaceDrive.mode(mv.isMoving, input.moveMag, input.dash, canDash, STICK_DASH_MIN)

        // v2.42 縮地: under gravity (on a planet surface) the dash button is a SHUKUCHI — an instant
        // afterimage step — instead of a thruster burn. Space keeps the classic thruster dash.
        val onSurface = worldState.mode == WorldMode.SURFACE
        val cds = entity[Cooldowns]
        if (cds.blink > 0f) cds.blink -= dt
        var blinked = false
        if (onSurface && mode == SpaceDrive.Mode.BUTTON_DASH) {
            if (input.dash && !prevDash && cds.blink <= 0f && canDash) {
                // Step direction: the move stick if held, else the facing.
                val bdx = if (mv.isMoving) mv.dirX else f.x
                val bdy = if (mv.isMoving) mv.dirY else f.y
                val distMul = if (bf.dashUpT > 0f) DASH_UP_MUL else 1f
                val blinkDist = BLINK_DIST * distMul
                // Afterimages along the path, then the wall-clipped step itself.
                for (k in 1..BLINK_IMAGES) {
                    val fr = k.toFloat() / (BLINK_IMAGES + 1)
                    fx.spawnAfterimage(t.x + bdx * blinkDist * fr, t.y + bdy * blinkDist * fr, b.halfW * 2f, b.halfH * 2f, BLINK_TINT)
                }
                val b1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, bdx * blinkDist, 0f)
                val b2 = Collision.moveAndCollide(map, b1.x, b1.y, b.halfW, b.halfH, 0f, bdy * blinkDist)
                t.x = b2.x; t.y = b2.y
                if (!staInf) s.value = (s.value - BLINK_COST).coerceAtLeast(0f)
                cds.blink = BLINK_CD
                blinked = true
            }
            mode = SpaceDrive.Mode.WALK // holding the button never thrusts on the ground — 縮地 only
        }
        prevDash = input.dash
        val dashing = blinked || mode == SpaceDrive.Mode.BUTTON_DASH || mode == SpaceDrive.Mode.STICK_DASH
        tag.dashing = dashing

        // v2.33: the equipped thruster shapes thrust; an OC thruster's FULL THROTTLE (held) trades
        // stamina for 3× thrust and a 2× cruise cap. Armor/accessories fold into the move multiplier.
        val loadout = entity[Gear].loadout
        val ft = input.fullThrottle && loadout.hasOverclockThruster && !s.overheat && (s.value > 0f || staInf)
        val gearAccel = loadout.thrustAccelMul * (if (ft) FullThrottle.ACCEL_MUL else 1f)
        val gearCruise = loadout.thrustCruiseMul * (if (ft) FullThrottle.CRUISE_MUL else 1f)
        // Special effects (v2.35): worn traits or their timed consumable coatings negate a hazard outright.
        val heatProof = loadout.has(ItemTrait.HEAT_PROOF) || bf.heatProofT > 0f
        val coldProof = loadout.has(ItemTrait.COLD_PROOF) || bf.coldProofT > 0f
        // Normal-move speed cap scales with buffs/snow; dashes are bounded only by the hard ceiling.
        // v2.79 水域: wading through open water drags the walk (frozen ponds do not).
        val wading = worldState.mode == WorldMode.SURFACE &&
            io.github.panda17tk.arpg.map.SurfaceWater.wadingAt(worldState.water, t.x, t.y)
        val cruise = Locomotion.speed(false, config.player) * mods.moveMul * loadout.moveMul * gearCruise *
            (if (bf.dashUpT > 0f) DASH_UP_MUL else 1f) * (if (snow && !coldProof) SNOW_SLOW else 1f) *
            (if (wading) io.github.panda17tk.arpg.map.SurfaceWater.WADE_SLOW else 1f)
        val hardCap = V_HARD * mods.moveMul * (if (bf.dashUpT > 0f) DASH_UP_MUL else 1f)
        // Zero friction in open space; a planet surface (ice especially) restores ground drag so you stop.
        // Cold-proof soles grip the ice: it behaves like ordinary ground.
        val decay = when {
            worldState.mode != WorldMode.SURFACE -> SPACE_DECAY
            worldState.biome == PlanetBiome.ICE && !coldProof -> ICE_COAST
            else -> SURFACE_COAST
        }
        // Thrust direction: the facing for a button dash, the move stick otherwise.
        val tx = if (mode == SpaceDrive.Mode.BUTTON_DASH) f.x else mv.dirX
        val ty = if (mode == SpaceDrive.Mode.BUTTON_DASH) f.y else mv.dirY

        // Normal-move (WALK) caps: half cruise in open space (calm drifting), 3× that on the
        // ground (v2.72 — boots grip; the surface reads as brisk against the vacuum's coast).
        val surface = worldState.mode == WorldMode.SURFACE
        val walkCapMul = if (surface) SURFACE_WALK_MUL else SPACE_WALK_MUL
        // Knockback decays fast and stays separate from movement (keeps hits punchy).
        v.vx *= 0.0001f.pow(dt)
        v.vy *= 0.0001f.pow(dt)
        val (nvx, nvy) = SpaceDrive.step(
            v.driftX, v.driftY, tx, ty, mode,
            MOVE_ACCEL * gearAccel * (if (surface) SURFACE_ACCEL_MUL else 1f),
            STICK_DASH_ACCEL * gearAccel, BUTTON_DASH_ACCEL * gearAccel,
            cruise, decay, hardCap, dt, walkCapMul,
        )
        v.driftX = nvx; v.driftY = nvy

        // Axis-separated collision so the player slides/crawls along walls instead of catching on edges.
        val impactSpeed = hypot(v.vx + v.driftX, v.vy + v.driftY) // speed entering the collision step
        val r1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, (v.vx + v.driftX) * dt, 0f)
        val r2 = Collision.moveAndCollide(map, r1.x, r1.y, b.halfW, b.halfH, 0f, (v.vy + v.driftY) * dt)
        t.x = r2.x; t.y = r2.y
        if (r1.hitX) { v.vx = 0f; v.driftX = 0f } // stop at the wall
        if (r2.hitY) { v.vy = 0f; v.driftY = 0f }
        // Solid planets: push out + a slight rebound.
        val pc = CircleCollision.resolve(t.x, t.y, b.halfW, v.vx + v.driftX, v.vy + v.driftY, planetField.planets)
        if (pc.hit) {
            t.x = pc.x; t.y = pc.y
            val (rdx, rdy) = CrashModel.rebound(v.driftX, v.driftY, pc.normalX, pc.normalY, CRASH_RESTITUTION)
            v.driftX = rdx; v.driftY = rdy
        }
        // A fast smack into a wall or planet jolts the screen, scaled by speed — and past the damage
        // threshold it hurts (v2.36): 1..5 HP scaling with impact speed. Gentle bumps stay free.
        if ((r1.hitX || r2.hitY || pc.hit) && impactSpeed > IMPACT_MIN_SPEED) {
            fx.addShake(IMPACT_SHAKE_T, (impactSpeed * IMPACT_SHAKE_K).coerceAtMost(IMPACT_SHAKE_MAX))
            val crash = CrashModel.damage(impactSpeed, CRASH_DMG_MIN_SPEED, CRASH_DMG_K)
            // v2.38: crash-proof gear (対衝撃フレーム等) eats the slam — shake only, no HP.
            if (crash > 0f && !loadout.has(ItemTrait.CRASH_PROOF)) {
                h.hp = (h.hp - crash.coerceIn(CRASH_DMG_MIN, CRASH_DMG_MAX)).coerceAtLeast(0f)
            }
        }
        // Stamina: a button dash drains hard, a stick dash barely sips, otherwise it regenerates.
        if (staInf) {
            s.value = s.max; s.overheat = false
        } else {
            var drain = when (mode) {
                SpaceDrive.Mode.BUTTON_DASH -> BUTTON_DASH_DRAIN
                SpaceDrive.Mode.STICK_DASH -> STICK_DASH_DRAIN
                else -> 0f
            }
            // Full throttle: its own burn (twice the dash rate — スタミナ消費2倍) + doubled dash costs.
            if (ft) drain = drain * FullThrottle.DRAIN_MUL + BUTTON_DASH_DRAIN * FullThrottle.DRAIN_MUL
            s.value = if (drain > 0f) (s.value - drain * dt).coerceAtLeast(0f)
            else (s.value + config.player.staRegen * loadout.staRegenMul * dt).coerceAtMost(s.max)
            if (s.value <= 0.05f) s.overheat = true            // fully drained → overheat (no stamina actions)
            if (s.value >= s.max - 0.05f) s.overheat = false   // fully recovered → clear overheat
        }
        // Terrain effects: magma heat smolders — ~1 HP per 20 seconds (v2.40; was a fierce 8/s), and
        // it's a single flag: standing beside ten lava blocks burns no faster than beside one.
        // A magma planet's surface radiates the same everywhere. Heat-proof gear (v2.35) cuts it to zero.
        val onMagmaSurface = worldState.mode == WorldMode.SURFACE && worldState.biome == PlanetBiome.MAGMA
        if ((magma || onMagmaSurface) && !heatProof) h.hp = (h.hp - MAGMA_DPS * dt).coerceAtLeast(0f)
        if (grass) s.value = (s.value + GRASS_STA * dt).coerceAtMost(s.max)
        // Passive regeneration (v2.35): worn repair patches + the timed regen pack, additive; never revives.
        val regen = loadout.hpRegen + (if (bf.regenT > 0f) PATCH_REGEN else 0f)
        if (regen > 0f && h.hp > 0f) h.hp = (h.hp + regen * dt).coerceAtMost(h.hpMax)
    }

    companion object {
        private const val DASH_UP_MUL = 1.5f // dash-speed pickup buff
        private const val CRASH_RESTITUTION = 0.35f // slight outward rebound off a planet (no crash damage)
        private const val MOVE_ACCEL = 213.3f // walk ramp (v2.31: 1/3 of the old 640) — a weighty, from-zero start;
        // in space the cruise cap is reached in ~0.5s; on a surface, ground friction (0.02) now balances the
        // lighter thrust at ~55 u/s, so surface walking tops out below the cruise cap — deliberate heaviness.
        private const val STICK_DASH_ACCEL = 32f // v2.32: 1/3 of 96 — the stick boost builds gently
        private const val BUTTON_DASH_ACCEL = 220f // v2.32: 1/3 of 660 — the dash winds up instead of kicking
        private const val STICK_DASH_MIN = 0.85f // move-stick deflection (0..1) that trips a stick dash
        private const val V_HARD = 1000f // absolute speed ceiling (zero-friction safety; not tied to dash state)
        private const val BUTTON_DASH_DRAIN = 35f // v2.32: half of 70 — dashing costs less
        private const val STICK_DASH_DRAIN = 3.5f // v2.32: half of 7
        private const val SPACE_WALK_MUL = 0.5f // open space: normal-move cap is half (calmer cruising; dashes unaffected)
        private const val SURFACE_WALK_MUL = SPACE_WALK_MUL * 3f // v2.72: ground walking runs 3× the space cruise cap
        private const val SURFACE_ACCEL_MUL = 3f // v2.72: traction — enough ramp to actually reach the higher cap against ground drag
        private const val SPACE_DECAY = 1f // open space: zero friction — momentum is conserved
        private const val SURFACE_COAST = 0.02f // planet ground: friction stops you fast (no space inertia)
        private const val ICE_COAST = 0.7f // ice/snow surface: slippery, long glide
        private const val SNOW_SLOW = 0.62f // snow block underfoot → slower
        private const val MAGMA_DPS = 0.05f // magma heat: ~1 HP per 20s (v2.40 — a pressure, not a shredder)
        private const val GRASS_STA = 16f // grass block restores stamina/sec
        private const val PATCH_REGEN = 2.5f // HP/sec while the timed regen pack (v2.35) runs
        private val BLINK_DIST = Tuning.TILE * 3.5f // v2.42 縮地: instant step length under gravity
        private const val BLINK_COST = 18f          // stamina per step (a chunk, not a drain)
        private const val BLINK_CD = 0.55f          // seconds between steps
        private const val BLINK_IMAGES = 3          // afterimages left along the path
        private val BLINK_TINT = Color(0.48f, 0.69f, 1f, 0.85f) // the player's blue, ghosted
        private const val IMPACT_MIN_SPEED = 220f // above this, a wall/planet smack jolts the screen
        private const val CRASH_DMG_MIN_SPEED = 320f // above this, the smack also costs HP (v2.36)
        private const val CRASH_DMG_K = 0.008f // damage per unit of speed past the threshold (~1 HP at 445)
        private const val CRASH_DMG_MIN = 1f // a damaging crash costs at least 1 HP...
        private const val CRASH_DMG_MAX = 5f // ...and at most 5, however hard the slam
        private const val IMPACT_SHAKE_K = 0.022f // shake magnitude per unit of impact speed
        private const val IMPACT_SHAKE_MAX = 16f // …capped so a hard hit doesn't nauseate
        private const val IMPACT_SHAKE_T = 0.2f // shake duration (s)
    }
}
