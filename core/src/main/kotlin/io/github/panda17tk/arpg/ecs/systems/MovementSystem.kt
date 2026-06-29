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
import io.github.panda17tk.arpg.sim.Collision
import io.github.panda17tk.arpg.sim.Inertia
import io.github.panda17tk.arpg.sim.Locomotion
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor
import kotlin.math.pow

class MovementSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina, Body, Velocity, Mods, Health, Buff) }) {
    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val config: GameConfig = world.inject()

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
        // Newtonian-ish momentum: input/dash accelerate the drift; light space-drag lets it coast (space inertia).
        val thrustX = if (mv.isMoving) mv.dirX * accel else 0f
        val thrustY = if (mv.isMoving) mv.dirY * accel else 0f
        val decay = if (mv.isMoving) MOVE_DECAY else COAST_DECAY
        val (nvx, nvy) = Inertia.step(v.driftX, v.driftY, thrustX, thrustY, decay, maxV, dt)
        v.driftX = nvx; v.driftY = nvy

        // Axis-separated collision so the player slides/crawls along walls instead of catching on edges.
        val r1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, (v.vx + v.driftX) * dt, 0f)
        val r2 = Collision.moveAndCollide(map, r1.x, r1.y, b.halfW, b.halfH, 0f, (v.vy + v.driftY) * dt)
        t.x = r2.x; t.y = r2.y
        if (r1.hitX) { v.vx = 0f; v.driftX = 0f }
        if (r2.hitY) { v.vy = 0f; v.driftY = 0f }
        if (input.aiming) { f.x = input.aimX; f.y = input.aimY } // right stick aims independent of movement
        else if (mv.isMoving) { f.x = mv.dirX; f.y = mv.dirY }
        if (staInf) {
            s.value = s.max; s.overheat = false
        } else {
            s.value = Locomotion.nextStamina(s.value, dashing, dt, config.player)
            if (s.value <= 0.05f) s.overheat = true            // fully drained → overheat (no stamina actions)
            if (s.value >= s.max - 0.05f) s.overheat = false   // fully recovered → clear overheat
        }
        // Terrain effects: magma block burns HP; grass block restores stamina.
        if (magma) h.hp = (h.hp - MAGMA_DPS * dt).coerceAtLeast(0f)
        if (grass) s.value = (s.value + GRASS_STA * dt).coerceAtMost(s.max)
    }

    companion object {
        private const val DASH_UP_MUL = 1.5f // dash-speed pickup buff
        private const val MOVE_ACCEL = 480f // walk acceleration — heavier: ramps up over ~0.2s
        private const val DASH_ACCEL = 1600f // dash acceleration — beefier dash
        private const val MOVE_DECAY = 0.6f // light drag while thrusting (still reaches the cap)
        private const val COAST_DECAY = 0.5f // space glide on release — momentum lingers (~halves in 1s); main feel tunable
        private const val SNOW_SLOW = 0.62f // snow block underfoot → slower
        private const val MAGMA_DPS = 8f // magma block burns HP/sec
        private const val GRASS_STA = 16f // grass block restores stamina/sec
    }
}
