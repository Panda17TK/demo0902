package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.47 オンボーディング: four timed steps, touch/desktop wording, silence after END. */
class OnboardingTest {
    @Test fun `walks move, aim, dash, land in order`() {
        assertTrue(Onboarding.lineFor(1f, touch = true)!!.contains("移動"))
        assertTrue(Onboarding.lineFor(9f, touch = true)!!.contains("照準"))
        assertTrue(Onboarding.lineFor(17f, touch = true)!!.contains("ダッシュ"))
        assertTrue(Onboarding.lineFor(25f, touch = true)!!.contains("着陸"))
    }

    @Test fun `wording follows the input device`() {
        assertTrue(Onboarding.lineFor(1f, touch = false)!!.contains("WASD"))
        assertTrue(Onboarding.lineFor(1f, touch = true)!!.contains("スティック"))
    }

    @Test fun `falls silent after the walkthrough ends`() {
        assertNull(Onboarding.lineFor(Onboarding.END, touch = true))
        assertNull(Onboarding.lineFor(999f, touch = false))
    }
}
