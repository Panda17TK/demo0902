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
import io.github.panda17tk.arpg.sim.Inertia
import io.github.panda17tk.arpg.sim.Locomotion
import io.github.panda17tk.arpg.sim.PlanetField
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.sim.WorldState
import kotlin.math.abs
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
        val dashing = (staInf || !s.overheat) && Locomotion.isDashing(input.dash, mv.isMoving, if (staInf) 1f else s.value)
        tag.dashing = dashing
        // Block biomes around the player drive terrain effects (magma burns, snow slows, grass restores).
        val ptx = floor(t.x / Tuning.TILE).toInt(); val pty = floor(t.y / Tuning.TILE).toInt()
        var magma = false; var snow = false; var grass = false
        for (oy in -1..1) for (ox in -1..1) {
            if (!map.solidAt(ptx + ox, pty + oy)) continue
            when (Biomes.of(ptx + ox, pty + oy)) {
                Biome.MAGMA -> magma = true; Biome.SNOW -> snow = true; Biome.GRASS -> grass = true; else -> {}
            }
        }

        val maxV = Locomotion.speed(dashing, config.player) * mods.moveMul *
            (if (bf.dashUpT > 0f) DASH_UP_MUL else 1f) * (if (snow) SNOW_SLOW else 1f)
        val accel = if (dashing) DASH_ACCEL else MOVE_ACCEL

        // Knockback decays fast and stays separate from movement (keeps hits punchy).
        v.vx *= 0.0001f.pow(dt)
        v.vy *= 0.0001f.pow(dt)
        // Coast drag depends on where we are: space glides; planet ground stops fast; an ice planet slips.
        val coast = when {
            worldState.mode != WorldMode.SURFACE -> COAST_DECAY
            worldState.biome == PlanetBiome.ICE -> ICE_COAST
            else -> SURFACE_COAST
        }
        // Newtonian-ish momentum: input/dash accelerate the drift; the coast drag set above lets it glide or grip.
        val thrustX = if (mv.isMoving) mv.dirX * accel else 0f
        val thrustY = if (mv.isMoving) mv.dirY * accel else 0f
        val decay = if (mv.isMoving) MOVE_DECAY else coast
        val (nvx, nvy) = Inertia.step(v.driftX, v.driftY, thrustX, thrustY, decay, maxV, dt)
        v.driftX = nvx; v.driftY = nvy

        // Axis-separated collision so the player slides/crawls along walls instead of catching on edges.
        val r1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, (v.vx + v.driftX) * dt, 0f)
        val r2 = Collision.moveAndCollide(map, r1.x, r1.y, b.halfW, b.halfH, 0f, (v.vy + v.driftY) * dt)
        t.x = r2.x; t.y = r2.y
        if (r1.hitX) { applyCrash(h, abs(v.vx + v.driftX)); v.vx = 0f; v.driftX = 0f }
        if (r2.hitY) { applyCrash(h, abs(v.vy + v.driftY)); v.vy = 0f; v.driftY = 0f }
        // Solid planets: push out + crash damage scaled by inward speed (slams hurt; gentle for the player).
        val pc = CircleCollision.resolve(t.x, t.y, b.halfW, v.vx + v.driftX, v.vy + v.driftY, planetField.planets)
        if (pc.hit) {
            t.x = pc.x; t.y = pc.y
            applyCrash(h, pc.inwardSpeed)
            val (rdx, rdy) = CrashModel.rebound(v.driftX, v.driftY, pc.normalX, pc.normalY, CRASH_RESTITUTION)
            v.driftX = rdx; v.driftY = rdy
        }
        if (input.aiming) { f.x = input.aimX; f.y = input.aimY } // right stick aims independent of movement
        else if (mv.isMoving) { f.x = mv.dirX; f.y = mv.dirY }
        if (staInf) {
            s.value = s.max; s.overheat = false
        } else {
            s.value = Locomotion.nextStamina(s.value, dashing, dt, config.player)
            if (s.value <= 0.05f) s.overheat = true            // fully drained → overheat (no stamina actions)
            if (s.value >= s.max - 0.05f) s.overheat = false   // fully recovered → clear overheat
        }
        // Terrain effects: magma block burns HP; grass restores stamina; a magma planet surface burns everywhere.
        val onMagmaSurface = worldState.mode == WorldMode.SURFACE && worldState.biome == PlanetBiome.MAGMA
        if (magma || onMagmaSurface) h.hp = (h.hp - MAGMA_DPS * dt).coerceAtLeast(0f)
        if (grass) s.value = (s.value + GRASS_STA * dt).coerceAtMost(s.max)
    }

    /** Crash damage on a high-speed impact (wall or planet), respecting the player's i-frames. */
    private fun applyCrash(h: Health, inwardSpeed: Float) {
        val dmg = CrashModel.damage(inwardSpeed, CRASH_THRESHOLD, CRASH_K)
        if (dmg > 0f && h.iTime <= 0f) { h.hp = (h.hp - dmg).coerceAtLeast(0f); h.iTime = CRASH_IFRAME }
    }

    companion object {
        private const val DASH_UP_MUL = 1.5f // dash-speed pickup buff
        private const val CRASH_THRESHOLD = 200f // impact speed below this → no crash damage (forgiving)
        private const val CRASH_K = 0.25f // player crash damage per unit speed over threshold
        private const val CRASH_IFRAME = 0.4f // invuln after a crash so one slam isn't multi-hit
        private const val CRASH_RESTITUTION = 0.35f // slight outward rebound off a planet
        private const val MOVE_ACCEL = 480f // walk acceleration — heavier: ramps up over ~0.2s
        private const val DASH_ACCEL = 1600f // dash acceleration — beefier dash
        private const val MOVE_DECAY = 0.6f // light drag while thrusting (still reaches the cap)
        private const val COAST_DECAY = 0.5f // space glide on release — momentum lingers (~halves in 1s); main feel tunable
        private const val SURFACE_COAST = 0.02f // planet ground: friction stops you fast (no space inertia)
        private const val ICE_COAST = 0.7f // ice/snow surface: slippery, long glide
        private const val SNOW_SLOW = 0.62f // snow block underfoot → slower
        private const val MAGMA_DPS = 8f // magma block burns HP/sec
        private const val GRASS_STA = 16f // grass block restores stamina/sec
    }
}
