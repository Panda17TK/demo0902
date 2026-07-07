package io.github.panda17tk.arpg.save

/**
 * v2.103 セーブスロット: three independent journeys. Each slot owns its run save, its universe
 * memory and its death relic; the workshop, achievements, endings and settings stay account-wide.
 * Slot 0 keeps the pre-slot keys, so every existing save simply becomes slot 1 on screen.
 */
object SaveSlots {
    const val COUNT = 3

    /** Preferences key for slot [slot] on top of the legacy [base] key (slot 0 = the legacy key). */
    fun keyFor(base: String, slot: Int): String = if (slot <= 0) base else "$base.s$slot"

    /** The title picker's one-line summary of a slot, or null when the slot is empty. */
    fun summary(slot: Int): String? = try {
        PreferencesRunSaveStore(slot).load()?.let { dto ->
            val place = if (dto.mode == "SURFACE") "地表" else "宇宙"
            "第${dto.spaceSeed}星系　$place　同期汚染 ${dto.wave}"
        }
    } catch (_: Throwable) { null }

    /** Whether any slot holds a run (gates つづきから on the title). */
    fun hasAny(): Boolean = (0 until COUNT).any { summary(it) != null }

    /** The first occupied slot — the keyboard shortcut's continue target. */
    fun firstUsed(): Int? = (0 until COUNT).firstOrNull { summary(it) != null }
}
