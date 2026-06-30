package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.hypot

class DriftTest {

    // ---- field() ----

    @Test fun `field produces exactly the requested count`() {
        assertEquals(200, Drift.field(Rng(1L), 200, 0f, 0f, 1000f).items.size)
    }

    @Test fun `field scatters every piece inside the box around the centre`() {
        val cx = 100f; val cy = 200f; val range = 800f
        val f = Drift.field(Rng(3L), 300, cx, cy, range)
        assertTrue(f.items.all { abs(it.x - cx) <= range && abs(it.y - cy) <= range }, "all pieces inside the seed box")
    }

    @Test fun `field mixes big asteroids with small debris`() {
        val f = Drift.field(Rng(5L), 300, 0f, 0f, 1000f)
        assertTrue(f.items.any { it.asteroid }, "should seed some asteroids")
        assertTrue(f.items.any { !it.asteroid }, "should seed some debris")
        // asteroids are visibly larger than debris
        assertTrue(f.items.filter { it.asteroid }.all { it.size >= 12f })
        assertTrue(f.items.filter { !it.asteroid }.all { it.size <= 6f })
    }

    @Test fun `every piece drifts at a slow-but-nonzero speed`() {
        val f = Drift.field(Rng(9L), 300, 0f, 0f, 1000f)
        assertTrue(f.items.all { val s = hypot(it.vx, it.vy); s in 7.99f..34.01f }, "speeds within [MIN,MAX]")
    }

    @Test fun `field is deterministic for a given seed`() {
        val a = Drift.field(Rng(7L), 64, 0f, 0f, 500f).items
        val b = Drift.field(Rng(7L), 64, 0f, 0f, 500f).items
        assertEquals(a.size, b.size)
        for (i in a.indices) {
            assertEquals(a[i].x, b[i].x, 1e-4f)
            assertEquals(a[i].y, b[i].y, 1e-4f)
            assertEquals(a[i].vx, b[i].vx, 1e-4f)
        }
    }

    // ---- advance() ----

    @Test fun `advance moves a piece by its velocity and spins it`() {
        val d = Drifter(x = 0f, y = 0f, vx = 60f, vy = -30f, size = 4f, asteroid = false, rot = 1f, rotSpeed = 2f)
        Drift.advance(DriftField(listOf(d)), px = 0f, py = 0f, range = 1400f, dt = 0.5f)
        assertEquals(30f, d.x, 1e-3f)
        assertEquals(-15f, d.y, 1e-3f)
        assertEquals(2f, d.rot, 1e-3f)
    }

    @Test fun `a piece that drifts past the far edge wraps to the near edge`() {
        // 1450 is 50 past +range of the player at the origin → wraps by one span (2·range) to the other side.
        val d = Drifter(x = 1450f, y = 0f, vx = 0f, vy = 0f, size = 4f, asteroid = false, rot = 0f, rotSpeed = 0f)
        Drift.advance(DriftField(listOf(d)), px = 0f, py = 0f, range = 1400f, dt = 1f / 60f)
        assertEquals(-1350f, d.x, 1e-3f)
    }

    @Test fun `wrapping keeps the whole field within range of the player after a long flight`() {
        val range = 1200f
        val f = Drift.field(Rng(11L), 200, 0f, 0f, range)
        // Player flies steadily; the field must keep wrapping to stay around them, never streaming off to infinity.
        var px = 0f; var py = 0f
        repeat(400) {
            px += 9f; py += 6f // ~fast cruise per step
            Drift.advance(f, px, py, range, 1f / 60f)
        }
        assertTrue(
            f.items.all { abs(it.x - px) <= range + 1f && abs(it.y - py) <= range + 1f },
            "every piece stays inside the toroidal box around the moving player",
        )
    }
}
