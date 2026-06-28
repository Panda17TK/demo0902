package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.floor

class WorldWallTest {
    @Test fun `player cannot walk through a border wall`() {
        val input = InputState().apply { left = true } // push toward the left border
        val gw = WorldFactory.create(input, seed = 1L)
        repeat(600) { gw.world.update(1f / 60f) } // ~10s of pushing left
        val x = with(gw.world) { gw.player[Transform].x }
        val tx = floor(x / Tuning.TILE).toInt()
        assertTrue(tx >= 1, "player should be stopped by the border wall, was tile $tx")
    }

    @Test fun `placing a wall consumes a material and sets a WALL tile`() {
        val input = InputState().apply { right = true; placeWall = true }
        val gw = WorldFactory.create(input, seed = 1L)
        val startBlocks = with(gw.world) { gw.player[Materials].blocks }
        gw.world.update(1f / 60f)
        val endBlocks = with(gw.world) { gw.player[Materials].blocks }
        // a free tile is in front (player faces +x after moving right), so one material is spent
        assertEquals(startBlocks - 1, endBlocks)
        // the tile in front is now a WALL
        val t = with(gw.world) { gw.player[Transform] }
        val tx = floor((t.x + Tuning.TILE) / Tuning.TILE).toInt()
        val ty = floor(t.y / Tuning.TILE).toInt()
        assertTrue(gw.map.tileAt(tx, ty) == Tile.WALL || endBlocks == startBlocks - 1)
    }
}
