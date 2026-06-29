package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WallGravityTest {
    @Test fun `detect finds a big block as one cluster with radius at least 2`() {
        val wall = { tx: Int, ty: Int -> tx in 10..14 && ty in 10..14 } // 5x5 block
        val cs = WallGravity.detect(0, 30, 0, 30, wall)
        assertEquals(1, cs.size)
        assertEquals(12.5f, cs[0].cx, 0.6f)
        assertEquals(12.5f, cs[0].cy, 0.6f)
        assertTrue(cs[0].radius >= 2f, "radius ${cs[0].radius}")
    }

    @Test fun `detect ignores a block smaller than radius 2`() {
        val wall = { tx: Int, ty: Int -> tx in 5..6 && ty in 5..6 } // 2x2, radius ~0.7
        assertTrue(WallGravity.detect(0, 20, 0, 20, wall).isEmpty())
    }

    @Test fun `detect separates two disjoint blocks`() {
        val wall = { tx: Int, ty: Int -> (tx in 2..6 && ty in 2..6) || (tx in 20..24 && ty in 20..24) }
        assertEquals(2, WallGravity.detect(0, 30, 0, 30, wall).size)
    }

    @Test fun `gravity pulls toward a cluster`() {
        val cs = listOf(Cluster(0f, 0f, 3f, 25))
        val (ax, ay) = WallGravity.gravityAt(cs, 10f, 0f, range = 40f, strength = 50f)
        assertTrue(ax < 0f, "ax $ax should pull left toward the cluster")
        assertEquals(0f, ay, 0.001f)
    }

    @Test fun `gravity is zero beyond range`() {
        val cs = listOf(Cluster(0f, 0f, 3f, 25))
        val (ax, ay) = WallGravity.gravityAt(cs, 100f, 0f, range = 40f, strength = 50f)
        assertEquals(0f, ax, 0.001f)
        assertEquals(0f, ay, 0.001f)
    }

    @Test fun `a bigger cluster pulls harder at the same distance`() {
        val small = WallGravity.gravityAt(listOf(Cluster(0f, 0f, 3f, 4)), 10f, 0f, 40f, 50f)
        val big = WallGravity.gravityAt(listOf(Cluster(0f, 0f, 3f, 100)), 10f, 0f, 40f, 50f)
        assertTrue(kotlin.math.abs(big.first) > kotlin.math.abs(small.first))
    }
}
