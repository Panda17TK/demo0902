package io.github.panda17tk.arpg.ui

/**
 * v2.187 写真モード: pure camera-framing math + the corner control chips (no libGDX — the screen
 * wires Gdx.input → these → the camera). Zoom is the libGDX camera.zoom (larger = wider view).
 */
object PhotoCam {
    const val ZOOM_MIN = 0.5f // the closest the frame pushes in
    const val ZOOM_MAX = 2.5f // the widest it pulls back
    const val ZOOM_STEP = 0.2f

    fun zoomBy(zoom: Float, delta: Float): Float = (zoom + delta).coerceIn(ZOOM_MIN, ZOOM_MAX)

    /** Keep a panned camera centre inside [lo]..[hi] (degenerate bounds pass through untouched). */
    fun clamp(v: Float, lo: Float, hi: Float): Float = if (hi <= lo) v else v.coerceIn(lo, hi)

    /** The three chips — exit (×), wider (遠), closer (近) — a top row clear of the framed subject.
     *  Kanji glyphs (font-safe) instead of ＋/－ so no coverage gaps show tofu. */
    fun buttons(hudW: Float, hudH: Float): List<UiButton> {
        val s = (minOf(hudW, hudH) * 0.12f).coerceIn(52f, 76f)
        val m = 16f
        val y = hudH - m - s
        return listOf(
            UiButton(m, y, s, s, "×"), // exit, top-left
            UiButton(hudW - m - s * 2f - 10f, y, s, s, "遠"), // wider
            UiButton(hudW - m - s, y, s, s, "近"), // closer
        )
    }
}
