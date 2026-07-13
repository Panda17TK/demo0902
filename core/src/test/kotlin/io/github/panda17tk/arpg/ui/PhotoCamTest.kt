package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.187 写真モード: the pure framing math + the corner control chips. */
class PhotoCamTest {
    @Test fun `zoom clamps between close and wide`() {
        assertEquals(PhotoCam.ZOOM_MAX, PhotoCam.zoomBy(PhotoCam.ZOOM_MAX, PhotoCam.ZOOM_STEP), 0.001f)
        assertEquals(PhotoCam.ZOOM_MIN, PhotoCam.zoomBy(PhotoCam.ZOOM_MIN, -PhotoCam.ZOOM_STEP), 0.001f)
        assertTrue(PhotoCam.zoomBy(1f, PhotoCam.ZOOM_STEP) > 1f)
        assertTrue(PhotoCam.zoomBy(1f, -PhotoCam.ZOOM_STEP) < 1f)
    }

    @Test fun `clamp keeps the frame inside its bounds`() {
        assertEquals(0f, PhotoCam.clamp(-50f, 0f, 100f), 0.001f)
        assertEquals(100f, PhotoCam.clamp(200f, 0f, 100f), 0.001f)
        assertEquals(30f, PhotoCam.clamp(30f, 0f, 100f), 0.001f)
        assertEquals(7f, PhotoCam.clamp(7f, 0f, 0f), 0.001f) // degenerate bounds pass through
    }

    @Test fun `the three chips fit on screen and never overlap`() {
        for ((w, h) in listOf(320f to 640f, 360f to 800f, 800f to 480f)) {
            val b = PhotoCam.buttons(w, h)
            assertEquals(listOf("×", "遠", "近"), b.map { it.label })
            for (r in b) assertTrue(r.x >= 0f && r.y >= 0f && r.x + r.w <= w && r.y + r.h <= h, "off screen: $r at ${w}x$h")
            for (i in b.indices) for (j in i + 1 until b.size) {
                val a = b[i]; val c = b[j]
                assertTrue(a.x + a.w <= c.x || c.x + c.w <= a.x || a.y + a.h <= c.y || c.y + c.h <= a.y, "overlap $a / $c")
            }
        }
    }
}
