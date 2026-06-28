package io.github.panda17tk.arpg.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.MeleeResolve
import io.github.panda17tk.arpg.combat.MobDamage
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.ecs.world.Pickups
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.map.Tiles
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot

class MeleeSystem(private val mobGrid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina, Cooldowns, Mods) }) {

    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val config: GameConfig = world.inject()
    private val rng: Rng = world.inject()
    private val fx: Fx = world.inject()
    private val chipColor = Color.valueOf("8a8076")

    override fun onTickEntity(entity: Entity) {
        val cd = entity[Cooldowns]
        if (cd.melee > 0f) cd.melee -= deltaTime
        if (!input.melee || cd.melee > 0f) return

        val t = entity[Transform]; val f = entity[Facing]; val s = entity[Stamina]; val mods = entity[Mods]
        cd.melee = config.player.meleeCd
        fx.spawnSlash(t.x, t.y, atan2(f.y, f.x))
        val outcome = MeleeResolve.resolve(if (s.max > 0f) s.value / s.max else 1f, config.player)
        s.value = maxOf(0f, s.value - config.player.meleeStaCost) // melee drains stamina

        // Break destructible walls in the front 3x3 (legacy melee.js)
        val ftx = floor((t.x + f.x * Tuning.MELEE_WALL_OFFSET) / Tuning.TILE).toInt()
        val fty = floor((t.y + f.y * Tuning.MELEE_WALL_OFFSET) / Tuning.TILE).toInt()
        for (oy in -1..1) for (ox in -1..1) {
            val tx = ftx + ox; val ty = fty + oy
            if (map.tileAt(tx, ty) == Tile.WALL) {
                val broke = Tiles.damageTile(map, tx, ty, outcome.dmg).broke
                fx.spawnChips((tx + 0.5f) * Tuning.TILE, (ty + 0.5f) * Tuning.TILE, 2, chipColor)
                if (broke) Pickups.dropOnWall(world, rng, (tx + 0.5f) * Tuning.TILE, (ty + 0.5f) * Tuning.TILE)
            }
        }

        // --- Melee vs mob: 180° arc at meleeReach (legacy melee.js meleeHit) ---
        val reach = config.player.meleeReach
        val arc = PI.toFloat()          // 180°
        val faceAng = atan2(f.y, f.x)
        mobGrid.forNearby(t.x, t.y, reach + 24f) { mobEntity ->
            val mobT = with(world) { mobEntity[Transform] }
            val mobB = with(world) { mobEntity[Body] }
            val ddx = mobT.x - t.x; val ddy = mobT.y - t.y
            val dist = hypot(ddx, ddy)
            val mobHalf = (mobB.halfW + mobB.halfH) * 0.5f
            if (dist < reach + mobHalf) {
                val ang = atan2(ddy, ddx) - faceAng
                val a = abs(((ang + 3f * PI.toFloat()) % (2f * PI.toFloat())) - PI.toFloat())
                if (a <= arc / 2f) {
                    val mobH = with(world) { mobEntity[Health] }
                    val mobV = with(world) { mobEntity[Velocity] }
                    val mobA = with(world) { mobEntity[MobAction] }
                    val mobDodge = with(world) { mobEntity[Mob].def.dodge }
                    val nx = if (dist > 0f) ddx / dist else 1f
                    val ny = if (dist > 0f) ddy / dist else 0f
                    MobDamage.hurt(mobH, mobV, mobA, mobDodge, outcome.dmg * mods.meleeMul, nx, ny, 240f, rng.nextFloat())
                }
            }
        }
    }
}
