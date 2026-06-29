package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

class PlanetsTest {
    @Test fun `places the requested number of planets`() {
        val ps = Planets.place(4000f, 4000f, 200f, 200f, count = 3, rng = Rng(1))
        assertEquals(3, ps.size)
    }

    @Test fun `planets keep clear of the player spawn`() {
        val ps = Planets.place(4000f, 4000f, 2000f, 2000f, count = 4, rng = Rng(2))
        for (p in ps) assertTrue(hypot(p.cx - 2000f, p.cy - 2000f) > p.radius, "planet at ${p.cx},${p.cy}")
    }

    @Test fun `planets do not overlap each other`() {
        val ps = Planets.place(4000f, 4000f, 200f, 200f, 4, Rng(3))
        for (i in ps.indices) for (j in i + 1 until ps.size) {
            val d = hypot(ps[i].cx - ps[j].cx, ps[i].cy - ps[j].cy)
            assertTrue(d > ps[i].radius + ps[j].radius, "planets $i,$j overlap (d=$d)")
        }
    }

    @Test fun `same seed yields identical placement`() {
        val a = Planets.place(4000f, 4000f, 200f, 200f, 4, Rng(7))
        val b = Planets.place(4000f, 4000f, 200f, 200f, 4, Rng(7))
        assertEquals(a, b)
    }
}
