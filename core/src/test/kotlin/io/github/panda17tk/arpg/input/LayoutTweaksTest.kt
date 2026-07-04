package io.github.panda17tk.arpg.input

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.56 ボタン配置エディタ: tweaks round-trip, clamp, and apply to the layout geometry. */
class LayoutTweaksTest {
    @Test fun `tweaks round-trip through JSON`() {
        val m = mapOf(
            TouchButton.DASH to ButtonTweak(0.8f, 0.2f, 1.3f),
            TouchButton.INV to ButtonTweak(0.9f, 0.9f, 0.7f),
        )
        assertEquals(m, LayoutTweaks.fromJson(LayoutTweaks.toJson(m)))
    }

    @Test fun `broken JSON and unknown buttons parse to nothing`() {
        assertTrue(LayoutTweaks.fromJson("{").isEmpty())
        assertTrue(LayoutTweaks.fromJson("not json").isEmpty())
        val kept = LayoutTweaks.fromJson(
            """{"items":[{"name":"NO_SUCH_BUTTON","fx":0.5,"fy":0.5},{"name":"DASH","fx":0.7,"fy":0.3}]}""",
        )
        assertEquals(setOf(TouchButton.DASH), kept.keys)
    }

    @Test fun `sanitize clamps fractions and scale`() {
        val t = LayoutTweaks.sanitize(ButtonTweak(-0.5f, 2f, 9f))
        assertEquals(0f, t.fx); assertEquals(1f, t.fy)
        assertEquals(LayoutTweaks.SCALE_MAX, t.scale)
        assertEquals(LayoutTweaks.SCALE_MIN, LayoutTweaks.sanitize(ButtonTweak(0.5f, 0.5f, 0f)).scale)
    }

    @Test fun `a tweak moves and resizes its button`() {
        val l = TouchLayout(1000f, 600f)
        val before = l.centerX(TouchButton.WALL) to l.radiusOf(TouchButton.WALL)
        l.tweaks = mapOf(TouchButton.WALL to ButtonTweak(0.7f, 0.5f, 1.4f))
        assertEquals(700f, l.centerX(TouchButton.WALL), 0.5f)
        assertEquals(300f, l.centerY(TouchButton.WALL), 0.5f)
        assertEquals(before.second * 1.4f, l.radiusOf(TouchButton.WALL), 0.01f)
    }

    @Test fun `a tweaked button can never leave the screen or the tappable zone`() {
        val l = TouchLayout(360f, 800f)
        // Dragged hard into the stick zone and off the bottom: it clamps back.
        l.tweaks = mapOf(TouchButton.DASH to ButtonTweak(0.01f, -1f, 1f))
        val r = l.radiusOf(TouchButton.DASH)
        assertTrue(l.centerX(TouchButton.DASH) >= 360f * 0.46f + r, "stays in the tappable right zone")
        assertEquals(r, l.centerY(TouchButton.DASH), 0.5f) // clamped onto the screen bottom
        // And its own hit-test still finds it.
        assertEquals(TouchButton.DASH, l.button(l.centerX(TouchButton.DASH), l.centerY(TouchButton.DASH)))
    }
}
