package io.github.panda17tk.arpg.save

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.122 引き継ぎ: the codec is pure — typed values survive the round trip, and anything
 *  that is not a handover text is refused instead of half-applied. */
class HandoverTest {
    @Test fun `typed values survive the round trip, including awkward strings`() {
        val data = mapOf(
            "arpg-scores" to mapOf<String, Any>("bestWave" to 12, "chWeek" to 2947L, "ratio" to 0.5f, "seen" to true),
            "drift-settings" to mapOf<String, Any>("note" to "tab\there\nand a \\ slash", "langEn" to false),
        )
        val decoded = Handover.decode(Handover.encode(data))!!
        assertEquals(12, decoded["arpg-scores"]!!["bestWave"])
        assertEquals(2947L, decoded["arpg-scores"]!!["chWeek"])
        assertEquals(0.5f, decoded["arpg-scores"]!!["ratio"])
        assertEquals(true, decoded["arpg-scores"]!!["seen"])
        assertEquals("tab\there\nand a \\ slash", decoded["drift-settings"]!!["note"])
        assertEquals(false, decoded["drift-settings"]!!["langEn"])
    }

    @Test fun `texts that are not a handover are refused whole`() {
        assertNull(Handover.decode(""), "empty")
        assertNull(Handover.decode("hello world"), "no header")
        assertNull(Handover.decode(Handover.HEADER + "\nbroken line without tabs"), "bad row")
        assertNull(Handover.decode(Handover.HEADER + "\na\tb\tx\tvalue"), "unknown type tag")
        assertNull(Handover.decode(Handover.HEADER + "\na\tb\ti\tnot-a-number"), "bad int")
    }

    @Test fun `the store list carries the account and all three journey slots`() {
        for (name in listOf("drift-settings", "arpg-scores", "arpg-achievements", "drift-bestiary", "drift-workshop", "drift-endings")) {
            assertTrue(name in Handover.STORES, "missing $name")
        }
        for (base in listOf("arpg-run", "arpg-universe", "arpg-relic")) {
            assertTrue(base in Handover.STORES && "$base.s1" in Handover.STORES && "$base.s2" in Handover.STORES, "missing slots of $base")
        }
    }

    @Test fun `the transfer page's plates fit on screen without overlap`() {
        for ((w, h) in listOf(320f to 640f, 360f to 780f, 420f to 900f)) {
            val bs = io.github.panda17tk.arpg.ui.RecordsPanel.handoverButtons(w, h)
            assertEquals(
                listOf(
                    io.github.panda17tk.arpg.ui.RecordsPanel.EXPORT_LABEL, io.github.panda17tk.arpg.ui.RecordsPanel.IMPORT_LABEL,
                    io.github.panda17tk.arpg.ui.RecordsPanel.BACK_LABEL, io.github.panda17tk.arpg.ui.RecordsPanel.CLOSE_LABEL,
                ),
                bs.map { it.label },
            )
            for (b in bs) assertTrue(b.x >= 0f && b.y >= 0f && b.x + b.w <= w && b.y + b.h <= h, "off screen: $b")
            for (i in bs.indices) for (j in i + 1 until bs.size) {
                val a = bs[i]; val b = bs[j]
                assertTrue(a.y + a.h <= b.y || b.y + b.h <= a.y, "overlap $a / $b")
            }
        }
        assertTrue(io.github.panda17tk.arpg.ui.RecordsPanel.handoverLines().first() == "引き継ぎ")
        assertTrue(io.github.panda17tk.arpg.ui.RecordsPanel.isHeader("引き継ぎ"))
    }
}
