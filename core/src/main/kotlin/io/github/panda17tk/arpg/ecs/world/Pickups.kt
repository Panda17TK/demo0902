package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.World
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.math.Rng
import kotlin.math.cos
import kotlin.math.sin

/** Spawns floor drops (loot) for enemy kills and wall/asteroid breaks. */
object Pickups {
    private val AMMO = arrayOf("ammo9" to 12, "ammo12" to 4, "ammoBeam" to 2, "ammoNade" to 1)
    private const val TAU = 6.2831855f

    fun spawn(world: World, kind: String, amount: Int, x: Float, y: Float) {
        world.entity {
            it += Transform(x = x, y = y, prevX = x, prevY = y)
            it += Pickup(kind, amount)
        }
    }

    /** Enemy death loot: many ammo pickups of mixed kinds (more for bosses) + a chance of med/blocks.
     *  [bonusBlocksChance] (LP v2.27): a grateful planet yields materials more readily. */
    fun dropOnKill(world: World, rng: Rng, x: Float, y: Float, boss: Boolean, bonusBlocksChance: Float = 0f) {
        val n = if (boss) 6 + rng.nextInt(5) else 2 + rng.nextInt(3)
        repeat(n) {
            val (kind, amt) = AMMO[rng.nextInt(AMMO.size)]
            val a = rng.nextFloat() * TAU; val r = 6f + rng.nextFloat() * 16f
            spawn(world, kind, amt, x + cos(a) * r, y + sin(a) * r)
        }
        if (boss || rng.nextFloat() < 0.20f) spawn(world, "med", 25, x, y)
        if (boss || rng.nextFloat() < 0.15f + bonusBlocksChance) spawn(world, "blocks", if (boss) 4 else 1, x, y)
    }

    /** Wall/asteroid break loot: many varied drops (materials/ammo/med + rare consumables) via [Loot]. */
    fun dropOnWall(world: World, rng: Rng, x: Float, y: Float) {
        for ((kind, amt) in Loot.wallDrops(rng)) {
            val a = rng.nextFloat() * TAU; val r = rng.nextFloat() * 12f
            spawn(world, kind, amt, x + cos(a) * r, y + sin(a) * r)
        }
    }
}
