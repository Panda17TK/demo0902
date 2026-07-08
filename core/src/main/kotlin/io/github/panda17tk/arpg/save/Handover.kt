package io.github.panda17tk.arpg.save

import com.badlogic.gdx.Gdx

/**
 * v2.122 引き継ぎ — the whole account as one block of text on the clipboard, for moving
 * between devices. Export dumps every preference store with its value types intact (so
 * Android's typed SharedPreferences round-trip exactly); import validates the header,
 * writes everything back, and reloads the account singletons. The codec half is pure
 * (tab-separated, escaped) and tested headless; only export()/import() touch Gdx.
 */
object Handover {
    const val HEADER = "DRIFT-HANDOVER v1"

    /** Every store the account lives in — settings, records, and all three journey slots. */
    val STORES: List<String> = buildList {
        add("drift-settings")
        addAll(listOf("arpg-scores", "arpg-achievements", "drift-bestiary", "drift-workshop", "drift-endings"))
        for (base in listOf("arpg-run", "arpg-universe", "arpg-relic")) {
            for (s in 0..2) add(SaveSlots.keyFor(base, s))
        }
    }

    /** Pure: typed key/values → the handover text. Type tags: s/i/b/f/l. */
    fun encode(data: Map<String, Map<String, Any>>): String = buildString {
        append(HEADER).append('\n')
        for ((store, kv) in data) {
            for ((k, v) in kv) {
                val (tag, str) = when (v) {
                    is Boolean -> "b" to v.toString()
                    is Int -> "i" to v.toString()
                    is Long -> "l" to v.toString()
                    is Float -> "f" to v.toString()
                    else -> "s" to v.toString()
                }
                append(esc(store)).append('\t').append(esc(k)).append('\t')
                append(tag).append('\t').append(esc(str)).append('\n')
            }
        }
    }

    /** Pure: handover text → typed key/values, or null if the text isn't one of ours. */
    fun decode(text: String): Map<String, MutableMap<String, Any>>? {
        val lines = text.trim().lines()
        if (lines.firstOrNull()?.trim() != HEADER) return null
        val out = LinkedHashMap<String, MutableMap<String, Any>>()
        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val p = line.split('\t')
            if (p.size != 4) return null
            val v: Any = when (p[2]) {
                "b" -> unesc(p[3]).toBooleanStrictOrNull() ?: return null
                "i" -> unesc(p[3]).toIntOrNull() ?: return null
                "l" -> unesc(p[3]).toLongOrNull() ?: return null
                "f" -> unesc(p[3]).toFloatOrNull() ?: return null
                "s" -> unesc(p[3])
                else -> return null
            }
            out.getOrPut(unesc(p[0])) { LinkedHashMap() }[unesc(p[1])] = v
        }
        return out
    }

    /** Gather every store into the handover text (best-effort; null when storage is absent). */
    fun export(): String? = try {
        val data = LinkedHashMap<String, Map<String, Any>>()
        for (name in STORES) {
            val kv = Gdx.app.getPreferences(name).get()
            if (kv.isNotEmpty()) {
                data[name] = kv.entries.associate { (k, v) -> k.toString() to (v as Any) }
            }
        }
        encode(data)
    } catch (_: Throwable) { null }

    /** Overwrite the stores from the text and reload the account singletons. */
    fun import(text: String): Boolean {
        val data = decode(text) ?: return false
        return try {
            for ((store, kv) in data) {
                if (store !in STORES) continue // only our stores — a foreign line never lands
                val p = Gdx.app.getPreferences(store)
                p.clear()
                for ((k, v) in kv) when (v) {
                    is Boolean -> p.putBoolean(k, v)
                    is Int -> p.putInteger(k, v)
                    is Long -> p.putLong(k, v)
                    is Float -> p.putFloat(k, v)
                    else -> p.putString(k, v.toString())
                }
                p.flush()
            }
            Scores.load(); Achievements.load(); Bestiary.load(); Workshop.load(); Endings.load()
            true
        } catch (_: Throwable) { false }
    }

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")

    private fun unesc(s: String): String {
        val b = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    't' -> b.append('\t'); 'n' -> b.append('\n'); else -> b.append(s[i + 1])
                }
                i += 2
            } else { b.append(c); i++ }
        }
        return b.toString()
    }
}
