package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BasesTest {
    private fun c(count: Int) = Cluster(0f, 0f, 3f, count)

    @Test fun `picks the largest clusters first`() {
        val picked = Bases.pickLargest(listOf(c(5), c(50), c(20)), 2, 1)
        assertEquals(listOf(50, 20), picked.map { it.count })
    }

    @Test fun `ignores clusters below the minimum size`() {
        val picked = Bases.pickLargest(listOf(c(3), c(40)), 5, 10)
        assertEquals(listOf(40), picked.map { it.count })
    }

    @Test fun `takes at most k`() {
        assertEquals(2, Bases.pickLargest(listOf(c(10), c(20), c(30)), 2, 1).size)
    }
}
