package io.github.panda17tk.arpg.ui

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure live-HUD geometry (P2). The bottom corners are owned by the touch controls (move stick
 * bottom-left, action cluster bottom-right, floating aim stick on the right), so the HUD is
 * distributed across the **top band**: a centered wave badge, an HP/stamina stack on the left,
 * and a weapon/ammo panel on the right — all clear of the P1 ⏸ button (`Modals.pauseButton`).
 * libGDX-free → unit-testable; drawing lives in render/Hud.
 */
data class HudLayout(
    val wave: UiButton,
    val hp: UiButton,
    val stamina: UiButton,
    val ammo: UiButton,
    val stats: UiButton,
) {
    companion object {
        const val PAD = 12f
        const val WAVE_H = 40f
        const val BAR_H = 16f
        const val GAP = 8f
        const val ICON_W = 22f

        fun of(hudW: Float, hudH: Float): HudLayout {
            // Top row: centered wave badge.
            val waveW = min(160f, hudW * 0.5f)
            val waveY = hudH - PAD - WAVE_H
            val wave = UiButton((hudW - waveW) / 2f, waveY, waveW, WAVE_H, "WAVE")

            // Second band (below the wave row): HP + stamina stacked on the left.
            val hpY = waveY - GAP - BAR_H
            val staY = hpY - GAP - BAR_H
            val barW = min(200f, hudW * 0.42f)
            val hp = UiButton(PAD + ICON_W, hpY, barW, BAR_H, "HP")
            val stamina = UiButton(PAD + ICON_W, staY, barW, BAR_H, "ST")

            // Weapon/ammo panel on the right, spanning the two bar rows, below the ⏸ button.
            val ammoW = min(150f, hudW * 0.34f)
            val ammoH = 2f * BAR_H + GAP
            val ammo = UiButton(hudW - PAD - ammoW, staY, ammoW, ammoH, "")

            // Secondary stats (time/kills/materials) — lowest priority, below the stamina row.
            val statsW = min(hudW - 2f * PAD, hudW * 0.6f)
            val stats = UiButton(PAD, staY - GAP - 16f, statsW, 16f, "")

            return HudLayout(wave, hp, stamina, ammo, stats)
        }
    }
}

/** Number of lit segments for a segmented bar, clamped to 0..count (shape-based, colorblind-safe). */
fun filledSegments(value: Float, max: Float, count: Int): Int {
    if (max <= 0f || count <= 0) return 0
    return ((value / max).coerceIn(0f, 1f) * count).roundToInt()
}
