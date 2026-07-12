package io.github.panda17tk.arpg.save

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * v2.179 細かい直し — two tripwires over the codebase itself:
 * 1) every preference store named anywhere in main must ride the Handover (a new store that
 *    isn't listed would silently NOT transfer between devices);
 * 2) the sim never reads the clock, preferences, or the input device (the determinism
 *    contract that replays, the proving run and every seed-tuned test stand on).
 */
class AccountContractsTest {
    private val mainRoot = File("src/main/kotlin/io/github/panda17tk/arpg")

    @Test fun `every store literal in main rides the handover`() {
        assertTrue(mainRoot.exists(), "test runs from the core module dir")
        val literal = Regex("\"((?:arpg|drift)-[a-z0-9-]+)\"")
        val found = mainRoot.walkTopDown().filter { it.extension == "kt" }
            .flatMap { f -> literal.findAll(f.readText()).map { it.groupValues[1] } }
            .toSortedSet()
        for (store in found) {
            assertTrue(
                store in Handover.STORES || (0..2).any { SaveSlots.keyFor(store, it) in Handover.STORES },
                "store \"${'$'}store\" is used in main but missing from Handover.STORES — it would not transfer",
            )
        }
        assertTrue(found.isNotEmpty(), "the sweep found the known stores (${'$'}found)")
    }

    @Test fun `the sim never reads the clock, preferences, or the input device`() {
        val forbidden = Regex("System\\.currentTimeMillis|System\\.nanoTime|getPreferences|Gdx\\.input|Gdx\\.files")
        val simDirs = listOf("sim", "ecs", "combat", "planet", "map", "config", "pathfinding", "math", "item")
        for (dir in simDirs) {
            File(mainRoot, dir).walkTopDown().filter { it.extension == "kt" }.forEach { f ->
                if (f.name == "ConfigStore.kt") return@forEach // the one sanctioned boundary object
                val hit = forbidden.find(f.readText())
                assertTrue(hit == null, "determinism contract: ${'$'}{f.name} reads ${'$'}{hit?.value}")
            }
        }
    }
}
