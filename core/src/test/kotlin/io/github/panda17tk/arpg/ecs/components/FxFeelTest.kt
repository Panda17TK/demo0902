package io.github.panda17tk.arpg.ecs.components

import com.badlogic.gdx.graphics.Color
import io.github.panda17tk.arpg.combat.MeleeCombo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.85 ゲームフィール土台: hitstop/slow-mo, staged deaths, damage pops — all cosmetic, all pure. */
class FxFeelTest {
    @Test fun `hitstop freezes the sim, then slow-mo breathes, then time runs true`() {
        val fx = Fx()
        fx.hitstop(0.05f)
        fx.slowmo(0.3f)
        assertEquals(0f, fx.simTimeScale(), "hitstop outranks slow-mo")
        fx.update(0.06f) // the hold expires
        assertEquals(Fx.SLOWMO_SCALE, fx.simTimeScale(), "the exhale follows the hold")
        fx.update(0.4f) // the exhale expires
        assertEquals(1f, fx.simTimeScale(), "time runs true again")
    }

    @Test fun `feel requests max-combine instead of stacking`() {
        val fx = Fx()
        fx.hitstop(0.08f)
        fx.hitstop(0.03f) // a weaker request must not shorten the hold
        assertEquals(0.08f, fx.hitstopT, 1e-4f)
        fx.slowmo(0.2f); fx.slowmo(0.5f); fx.slowmo(0.1f)
        assertEquals(0.5f, fx.slowmoT, 1e-4f)
    }

    @Test fun `a big death stages a corpse and three chained bursts`() {
        val fx = Fx()
        fx.spawnDeathStaged(100f, 100f, 20f, 20f, Color.WHITE, big = true)
        assertEquals(1, fx.corpses.size)
        assertEquals(3, fx.bursts.size, "big deaths chain three blasts")
        assertTrue(fx.particles.isEmpty(), "no gibs before the first stage fires")
        fx.update(0.01f) // the first burst (delay 0) fires
        assertTrue(fx.particles.isNotEmpty(), "the first stage bursts")
        assertEquals(2, fx.bursts.size, "two stages still pending")
        fx.update(0.40f) // the rest fire, the corpse expires
        assertEquals(0, fx.bursts.size)
        assertTrue(fx.corpses.isEmpty(), "the corpse squashes out and is gone")
    }

    @Test fun `a small death is one corpse and one delayed burst`() {
        val fx = Fx()
        fx.spawnDeathStaged(0f, 0f, 12f, 12f, Color.WHITE, big = false)
        assertEquals(1, fx.corpses.size)
        assertEquals(1, fx.bursts.size)
        fx.update(0.05f)
        assertTrue(fx.particles.isEmpty(), "the burst waits a tenth of a second")
        fx.update(0.06f)
        assertTrue(fx.particles.isNotEmpty())
    }

    @Test fun `damage pops float up and expire`() {
        val fx = Fx()
        fx.spawnPop(50f, 200f, 24, Color.WHITE)
        val startY = fx.pops.first().y
        fx.update(0.3f)
        assertTrue(fx.pops.first().y < startY, "world is y-down — floating up means y falls")
        fx.update(0.5f)
        assertTrue(fx.pops.isEmpty(), "pops die after their life")
        fx.spawnPop(0f, 0f, 0, Color.WHITE)
        assertTrue(fx.pops.isEmpty(), "a zero never prints")
    }

    @Test fun `the camera kick decays to nothing on its own`() {
        val fx = Fx()
        fx.addKick(-6f, 2f)
        fx.update(0.2f)
        assertTrue(kotlin.math.abs(fx.kickX) < 0.6f && kotlin.math.abs(fx.kickY) < 0.6f, "the punch snaps back fast")
    }

    @Test fun `the melee hold grows with the rhythm and caps at the top step`() {
        assertEquals(0.03f, MeleeCombo.hitstop(1), 1e-4f)
        assertTrue(MeleeCombo.hitstop(3) > MeleeCombo.hitstop(1))
        assertEquals(MeleeCombo.hitstop(MeleeCombo.MAX_STEP), MeleeCombo.hitstop(99), 1e-4f, "clamped past the top")
    }
}
