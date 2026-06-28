package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.MeleeResolve
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.map.Tiles
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

class MeleeSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina, Cooldowns) }) {
    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val config: GameConfig = world.inject()

    override fun onTickEntity(entity: Entity) {
        val cd = entity[Cooldowns]
        if (cd.melee > 0f) cd.melee -= deltaTime
        if (!input.melee || cd.melee > 0f) return

        val t = entity[Transform]; val f = entity[Facing]; val s = entity[Stamina]
        cd.melee = config.player.meleeCd
        val outcome = MeleeResolve.resolve(if (s.max > 0f) s.value / s.max else 1f, config.player) // mob hits: Phase 5

        // break destructible walls in the front 3x3 (legacy melee.js)
        val ftx = floor((t.x + f.x * Tuning.MELEE_WALL_OFFSET) / Tuning.TILE).toInt()
        val fty = floor((t.y + f.y * Tuning.MELEE_WALL_OFFSET) / Tuning.TILE).toInt()
        for (oy in -1..1) for (ox in -1..1) {
            val tx = ftx + ox; val ty = fty + oy
            if (map.tileAt(tx, ty) == Tile.WALL) Tiles.damageTile(map, tx, ty, outcome.dmg)
        }
    }
}
