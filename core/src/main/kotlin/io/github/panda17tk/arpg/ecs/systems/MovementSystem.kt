package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Buff
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState
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
import kotlin.math.pow

class MovementSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina, Body, Velocity, Mods, Health, Buff) }) {
    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val config: GameConfig = world.inject()
    private val planetField: PlanetField = world.inject()
    private val worldState: WorldState = world.inject()

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
        val mode = SpaceDrive.mode(mv.isMoving, input.moveMag, input.dash, canDash, STICK_DASH_MIN)
        val dashing = mode == SpaceDrive.Mode.BUTTON_DASH || mode == SpaceDrive.Mode.STICK_DASH
        tag.dashing = dashing

        // Normal-move speed cap scales with buffs/snow; dashes are bounded only by the hard ceiling.
        val cruise = Locomotion.speed(false, config.player) * mods.moveMul *
            (if (bf.dashUpT > 0f) DASH_UP_MUL else 1f) * (if (snow) SNOW_SLOW else 1f)
        val hardCap = V_HARD * mods.moveMul * (if (bf.dashUpT > 0f) DASH_UP_MUL else 1f)
        // Zero friction in open space; a planet surface (ice especially) restores ground drag so you stop.
        val decay = when {
            worldState.mode != WorldMode.SURFACE -> SPACE_DECAY
            worldState.biome == PlanetBiome.ICE -> ICE_COAST
            else -> SURFACE_COAST
        }
        // Thrust direction: the facing for a button dash, the move stick otherwise.
        val tx = if (mode == SpaceDrive.Mode.BUTTON_DASH) f.x else mv.dirX
        val ty = if (mode == SpaceDrive.Mode.BUTTON_DASH) f.y else mv.dirY

        // Knockback decays fast and stays separate from movement (keeps hits punchy).
        v.vx *= 0.0001f.pow(dt)
        v.vy *= 0.0001f.pow(dt)
        val (nvx, nvy) = SpaceDrive.step(
            v.driftX, v.driftY, tx, ty, mode,
            MOVE_ACCEL, STICK_DASH_ACCEL, BUTTON_DASH_ACCEL, cruise, decay, hardCap, dt,
        )
        v.driftX = nvx; v.driftY = nvy

        // Axis-separated collision so the player slides/crawls along walls instead of catching on edges.
        val r1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, (v.vx + v.driftX) * dt, 0f)
        val r2 = Collision.moveAndCollide(map, r1.x, r1.y, b.halfW, b.halfH, 0f, (v.vy + v.driftY) * dt)
        t.x = r2.x; t.y = r2.y
        if (r1.hitX) { v.vx = 0f; v.driftX = 0f } // stop at the wall — no crash/fall damage
        if (r2.hitY) { v.vy = 0f; v.driftY = 0f }
        // Solid planets: push out + a slight rebound. No crash/fall damage.
        val pc = CircleCollision.resolve(t.x, t.y, b.halfW, v.vx + v.driftX, v.vy + v.driftY, planetField.planets)
        if (pc.hit) {
            t.x = pc.x; t.y = pc.y
            val (rdx, rdy) = CrashModel.rebound(v.driftX, v.driftY, pc.normalX, pc.normalY, CRASH_RESTITUTION)
            v.driftX = rdx; v.driftY = rdy
        }
        // Stamina: a button dash drains hard, a stick dash barely sips, otherwise it regenerates.
        if (staInf) {
            s.value = s.max; s.overheat = false
        } else {
            val drain = when (mode) {
                SpaceDrive.Mode.BUTTON_DASH -> BUTTON_DASH_DRAIN
                SpaceDrive.Mode.STICK_DASH -> STICK_DASH_DRAIN
                else -> 0f
            }
            s.value = if (drain > 0f) (s.value - drain * dt).coerceAtLeast(0f)
            else (s.value + config.player.staRegen * dt).coerceAtMost(s.max)
            if (s.value <= 0.05f) s.overheat = true            // fully drained → overheat (no stamina actions)
            if (s.value >= s.max - 0.05f) s.overheat = false   // fully recovered → clear overheat
        }
        // Terrain effects: magma block burns HP; grass restores stamina; a magma planet surface burns everywhere.
        val onMagmaSurface = worldState.mode == WorldMode.SURFACE && worldState.biome == PlanetBiome.MAGMA
        if (magma || onMagmaSurface) h.hp = (h.hp - MAGMA_DPS * dt).coerceAtLeast(0f)
        if (grass) s.value = (s.value + GRASS_STA * dt).coerceAtMost(s.max)
    }

    companion object {
        private const val DASH_UP_MUL = 1.5f // dash-speed pickup buff
        private const val CRASH_RESTITUTION = 0.35f // slight outward rebound off a planet (no crash damage)
        private const val MOVE_ACCEL = 640f // walk ramp — reaches the cruise cap in ~0.16s
        private const val STICK_DASH_ACCEL = 96f // big-stick boost: a gentle accel toward the 2× cruise cap
        private const val BUTTON_DASH_ACCEL = 660f // dash button: a firm thrust along the facing toward the 3× cap
        private const val STICK_DASH_MIN = 0.85f // move-stick deflection (0..1) that trips a stick dash
        private const val V_HARD = 1000f // absolute speed ceiling (zero-friction safety; not tied to dash state)
        private const val BUTTON_DASH_DRAIN = 70f // stamina/sec while button-dashing (expensive)
        private const val STICK_DASH_DRAIN = 7f // stamina/sec while stick-dashing (very cheap)
        private const val SPACE_DECAY = 1f // open space: zero friction — momentum is conserved
        private const val SURFACE_COAST = 0.02f // planet ground: friction stops you fast (no space inertia)
        private const val ICE_COAST = 0.7f // ice/snow surface: slippery, long glide
        private const val SNOW_SLOW = 0.62f // snow block underfoot → slower
        private const val MAGMA_DPS = 8f // magma block burns HP/sec
        private const val GRASS_STA = 16f // grass block restores stamina/sec
    }
}
