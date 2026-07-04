package io.github.panda17tk.arpg.ui

import io.github.panda17tk.arpg.item.EquipSlot
import kotlin.math.min

/** The inventory overlay's tabs (v2.33; v2.43 market; v2.46 logbook): 装備/アイテム/マップ/市/セーブ/記録. */
enum class InvTab { EQUIP, ITEMS, MAP, MARKET, SAVE, LOG }

/**
 * Pure geometry for the inventory overlay (v2.33) — same policy as Modals/HudLayout: drawing
 * (render/Hud) and hit-testing (GameScreen) read these same rects; no libGDX → unit-testable.
 * A centered panel with a tab strip on top, per-tab body content, and a close button below.
 */
object InventoryLayout {
    const val TAB_H = 44f
    const val CLOSE_H = 48f
    const val PAD = 12f
    const val ROW_GAP = 8f

    val TAB_LABELS = listOf("装備", "アイテム", "マップ", "市", "セーブ", "記録")
    val SLOT_ORDER = listOf(
        EquipSlot.THRUSTER, EquipSlot.ARMOR, EquipSlot.RANGED, EquipSlot.MELEE,
        EquipSlot.ACC1, EquipSlot.ACC2, EquipSlot.ACC3,
    )
    val SLOT_LABELS = listOf("推進器", "防具", "遠距離", "近距離", "装飾1", "装飾2", "装飾3")

    /** The overlay panel, centered, leaving a margin all round. */
    fun panel(hudW: Float, hudH: Float): UiButton {
        val w = min(560f, hudW * 0.92f)
        val h = hudH * 0.88f
        return UiButton((hudW - w) / 2f, (hudH - h) / 2f, w, h)
    }

    /** Equal-width tabs across the panel's top edge, in [TAB_LABELS] order. */
    fun tabs(hudW: Float, hudH: Float): List<UiButton> {
        val p = panel(hudW, hudH)
        val n = TAB_LABELS.size
        val tabW = (p.w - PAD * 2f - ROW_GAP * (n - 1)) / n
        val y = p.y + p.h - PAD - TAB_H
        return TAB_LABELS.mapIndexed { i, lab -> UiButton(p.x + PAD + i * (tabW + ROW_GAP), y, tabW, TAB_H, lab) }
    }

    /** The close button, bottom-center of the panel. */
    fun closeButton(hudW: Float, hudH: Float): UiButton {
        val p = panel(hudW, hudH)
        val w = min(240f, p.w * 0.5f)
        return UiButton(p.x + (p.w - w) / 2f, p.y + PAD, w, CLOSE_H, "閉じる")
    }

    /** The body area between the tab strip and the close button. */
    fun body(hudW: Float, hudH: Float): UiButton {
        val p = panel(hudW, hudH)
        val top = p.y + p.h - PAD - TAB_H - ROW_GAP
        val bottom = p.y + PAD + CLOSE_H + ROW_GAP
        return UiButton(p.x + PAD, bottom, p.w - PAD * 2f, (top - bottom).coerceAtLeast(0f))
    }

    /** EQUIP tab: seven slot rows ([SLOT_ORDER]) stacked top-down, sized to fill the body. */
    fun slotRows(hudW: Float, hudH: Float): List<UiButton> {
        val b = body(hudW, hudH)
        val n = SLOT_ORDER.size
        val rowH = ((b.h - (n - 1) * ROW_GAP) / n).coerceIn(24f, 52f)
        val top = b.y + b.h - rowH
        return SLOT_ORDER.mapIndexed { i, _ -> UiButton(b.x, top - i * (rowH + ROW_GAP), b.w, rowH, SLOT_LABELS[i]) }
    }

    const val ITEM_ROW_H = 24f

    /**
     * ITEMS tab (v2.34): one tappable row per backpack group, top-down. Rows past what fits in
     * the body (minus a hint line at the bottom) are dropped — the drawn list clips identically,
     * so drawing and hit-testing stay in step.
     */
    fun itemRows(hudW: Float, hudH: Float, count: Int): List<UiButton> {
        val b = body(hudW, hudH)
        val fits = ((b.h - 28f) / ITEM_ROW_H).toInt().coerceAtLeast(0)
        return (0 until minOf(count, fits)).map { i ->
            UiButton(b.x, b.y + b.h - (i + 1) * ITEM_ROW_H, b.w, ITEM_ROW_H)
        }
    }

    /** EQUIP tab (v2.39): the control-swap toggle strip along the body's bottom edge. */
    fun controlToggle(hudW: Float, hudH: Float): UiButton {
        val b = body(hudW, hudH)
        return UiButton(b.x, b.y, b.w, 30f, "")
    }

    /** EQUIP tab (v2.56): the button-layout editor entry strip, just above the control toggle. */
    fun layoutEditToggle(hudW: Float, hudH: Float): UiButton {
        val b = body(hudW, hudH)
        return UiButton(b.x, b.y + 34f, b.w, 30f, "")
    }

    /** MARKET tab (v2.43): one tappable row per stall slot, top-down, leaving a footer line. */
    fun marketRows(hudW: Float, hudH: Float, count: Int): List<UiButton> {
        val b = body(hudW, hudH)
        val rowH = 34f
        val fits = ((b.h - 30f) / rowH).toInt().coerceAtLeast(0)
        return (0 until minOf(count, fits)).map { i ->
            UiButton(b.x, b.y + b.h - (i + 1) * rowH, b.w, rowH - 4f)
        }
    }

    /** SAVE tab: one big save button centered in the body. */
    fun saveButton(hudW: Float, hudH: Float): UiButton {
        val b = body(hudW, hudH)
        val w = min(280f, b.w * 0.7f)
        return UiButton(b.x + (b.w - w) / 2f, b.y + b.h / 2f - 32f, w, 64f, "セーブする")
    }
}
