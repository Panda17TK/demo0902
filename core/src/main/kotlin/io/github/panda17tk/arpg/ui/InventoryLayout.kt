package io.github.panda17tk.arpg.ui

import io.github.panda17tk.arpg.item.EquipSlot
import kotlin.math.min

/** The inventory overlay's four tabs (v2.33): 装備 / アイテム / マップ / セーブ. */
enum class InvTab { EQUIP, ITEMS, MAP, SAVE }

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

    val TAB_LABELS = listOf("装備", "アイテム", "マップ", "セーブ")
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

    /** Four equal tabs across the panel's top edge, in [TAB_LABELS] order. */
    fun tabs(hudW: Float, hudH: Float): List<UiButton> {
        val p = panel(hudW, hudH)
        val tabW = (p.w - PAD * 2f - ROW_GAP * 3f) / 4f
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

    /** SAVE tab: one big save button centered in the body. */
    fun saveButton(hudW: Float, hudH: Float): UiButton {
        val b = body(hudW, hudH)
        val w = min(280f, b.w * 0.7f)
        return UiButton(b.x + (b.w - w) / 2f, b.y + b.h / 2f - 32f, w, 64f, "セーブする")
    }
}
