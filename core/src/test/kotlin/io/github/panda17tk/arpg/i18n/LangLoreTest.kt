package io.github.panda17tk.arpg.i18n

import io.github.panda17tk.arpg.item.ItemCatalog
import io.github.panda17tk.arpg.item.ItemKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.163 英語化第4弾(後半・その2): every readable carries a whole English text. */
class LangLoreTest {
    @Test fun `every readable in the catalog has a complete English text`() {
        val lore = ItemCatalog.ALL.filter { it.kind == ItemKind.LORE }
        assertTrue(lore.isNotEmpty(), "the shelf is stocked")
        for (d in lore) {
            val en = LoreEn.textOf(d.id)
            assertTrue(en != null && en.isNotBlank(), "${d.id} has no English text")
            assertTrue(en!!.none { it.code >= 0x2E80 }, "${d.id}: CJK left in the translation")
        }
    }

    @Test fun `the english texts keep the original page shape`() {
        // the reading pane draws line by line with a hard bottom cut — a translation that
        // grows extra lines would push the closing sentences off the page unseen
        for (d in ItemCatalog.ALL.filter { it.kind == ItemKind.LORE }) {
            val ja = d.lore.split("\n").size
            val en = LoreEn.textOf(d.id)!!.split("\n").size
            assertEquals(ja, en, "${d.id}: JA $ja lines vs EN $en lines")
        }
    }

    @Test fun `the readable descriptions and shelf markers speak English`() {
        Lang.en = true
        try {
            for (d in ItemCatalog.ALL.filter { it.kind == ItemKind.LORE }) {
                val row = "【読】『${d.name}』　─　${d.desc}"
                val t = Lang.tr(row)
                assertTrue(t.none { it.code >= 0x2E80 && it != '　' }, "still CJK: 「$row」 -> 「$t」")
            }
        } finally { Lang.en = false }
    }
}
