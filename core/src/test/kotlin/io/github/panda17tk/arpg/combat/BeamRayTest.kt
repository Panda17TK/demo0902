package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.map.TileMap
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BeamRayTest {
    @Test fun `beam stops at the first solid tile`() {
        // open row from x=16; wall at tile x=5 (world 160..192). From x=16, step 6 -> reach ~138.
        val m = TileMap.fromRows(listOf(".....#....", ".....#....", ".....#...."))
        val hit = BeamRay.cast(m, x = 16f, y = 48f, dirX = 1f, dirY = 0f, maxLen = 700f)
        assertTrue(hit.reach in 120f..150f, "reach ${hit.reach} should be near the wall face at x=160")
        assertTrue(hit.endX < 160f && hit.endX > 145f, "beam end ${hit.endX} must be just before the wall face")
    }
}
