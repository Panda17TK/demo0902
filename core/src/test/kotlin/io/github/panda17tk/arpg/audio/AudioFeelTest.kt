package io.github.panda17tk.arpg.audio

import io.github.panda17tk.arpg.ecs.components.Fx
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.89 音の追い込み: the duck curve and the sim→screen sound bridge — both pure. */
class AudioFeelTest {
    @Test fun `held time pulls the bed to the floor and release eases it back`() {
        assertEquals(AudioDuck.FLOOR, AudioDuck.target(timeHeld = true, flashAlpha = 0f))
        assertEquals(AudioDuck.FLOOR, AudioDuck.target(timeHeld = false, flashAlpha = 0.6f))
        assertEquals(1f, AudioDuck.target(timeHeld = false, flashAlpha = 0.1f))
        // easing: fast on the way down, slower on the way up
        var v = 1f
        v = AudioDuck.step(v, AudioDuck.FLOOR, 0.1f)
        assertTrue(v < 0.6f, "drops fast (got $v)")
        val down = v
        var up = AudioDuck.FLOOR
        up = AudioDuck.step(up, 1f, 0.1f)
        assertTrue(1f - up > down - AudioDuck.FLOOR - 0.2f, "recovery is the gentler slope")
        repeat(60) { up = AudioDuck.step(up, 1f, 1f / 30f) }
        assertEquals(1f, up, 0.02f, "settles back to full")
    }

    @Test fun `sim systems queue sounds, the screen drains them once`() {
        val fx = Fx()
        fx.requestSfx("melee_hit", 1.2f)
        fx.requestSfx("boss_down")
        val drained = fx.drainSfx()
        assertEquals(listOf("melee_hit", "boss_down"), drained.map { it.name })
        assertEquals(1.2f, drained[0].pitch, 1e-4f)
        assertTrue(fx.drainSfx().isEmpty(), "a drain empties the queue")
    }

    @Test fun `the queue is capped — a storm of asks cannot backlog`() {
        val fx = Fx()
        repeat(100) { fx.requestSfx("hit") }
        assertTrue(fx.drainSfx().size <= 16, "capped at one frame's worth")
    }
}
