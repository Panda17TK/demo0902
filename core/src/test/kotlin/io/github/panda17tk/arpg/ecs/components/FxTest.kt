package io.github.panda17tk.arpg.ecs.components

import com.badlogic.gdx.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FxTest {
    @Test fun `shake decays to zero after its duration`() {
        val fx = Fx()
        fx.addShake(0.1f, 6f)
        assertTrue(fx.shakeMag > 0f)
        fx.update(0.2f)
        assertEquals(0f, fx.shakeMag, 1e-6f)
    }

    @Test fun `particles expire after their life`() {
        val fx = Fx()
        fx.spawnSparks(0f, 0f, 5, Color.WHITE)
        assertEquals(5, fx.particles.size)
        repeat(20) { fx.update(0.05f) } // 1.0s elapsed > 0.25 spark life
        assertTrue(fx.particles.isEmpty())
    }
}
