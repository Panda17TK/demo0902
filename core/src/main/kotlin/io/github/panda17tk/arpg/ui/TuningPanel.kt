package io.github.panda17tk.arpg.ui

import kotlin.math.min

/**
 * v2.98 調整モード — the in-game tuning popup's pure geometry: seven knob rows a page
 * (name + value centered, [−] and [＋] squares at the rims), then [前へ][次へ][閉じる]
 * at the foot. GameScreen draws and hit-tests against these same rects.
 */
object TuningPanel {
    const val ROWS = 7
    const val PREV = "前へ"
    const val NEXT = "次へ"
    const val CLOSE = "閉じる"

    fun pageCount(total: Int): Int = if (total <= 0) 1 else (total + ROWS - 1) / ROWS

    /** The seven row plates, top-down (draw only the ones your page fills). */
    fun rows(w: Float, h: Float): List<UiButton> {
        val bw = min(360f, w * 0.90f)
        val x = (w - bw) / 2f
        val top = h * 0.84f
        return (0 until ROWS).map { i -> UiButton(x, top - 46f - i * 56f, bw, 46f, "") }
    }

    /** The [−] square hugging a row's left rim. */
    fun minus(row: UiButton): UiButton = UiButton(row.x + 3f, row.y + 3f, 40f, row.h - 6f, "−")

    /** The [＋] square hugging a row's right rim. */
    fun plus(row: UiButton): UiButton = UiButton(row.x + row.w - 43f, row.y + 3f, 40f, row.h - 6f, "＋")

    /** [前へ][次へ][閉じる] side by side at the panel's foot. */
    fun footer(w: Float, h: Float): List<UiButton> {
        val bw = min(360f, w * 0.90f)
        val x = (w - bw) / 2f
        val each = (bw - 16f) / 3f
        val y = h * 0.10f
        return listOf(
            UiButton(x, y, each, 48f, PREV),
            UiButton(x + each + 8f, y, each, 48f, NEXT),
            UiButton(x + 2 * (each + 8f), y, each, 48f, CLOSE),
        )
    }
}

/**
 * v2.98 調整モード — the title screen's passcode pad: digits 1-9, then [消][0][決].
 * Pure geometry; the screen owns the typed code and asks TuneMode to unlock.
 */
object TunePad {
    val KEYS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "消", "0", "決")
    const val ERASE = "消"
    const val ENTER = "決"

    fun buttons(w: Float, h: Float): List<UiButton> {
        val bw = 76f
        val gap = 10f
        val gridW = 3 * bw + 2 * gap
        val x0 = (w - gridW) / 2f
        val top = h * 0.60f
        return KEYS.mapIndexed { i, k ->
            val col = i % 3
            val row = i / 3
            UiButton(x0 + col * (bw + gap), top - row * (58f + gap), bw, 58f, k)
        }
    }
}
