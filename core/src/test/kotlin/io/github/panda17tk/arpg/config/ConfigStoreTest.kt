package io.github.panda17tk.arpg.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfigStoreTest {
    @Test fun `starts at defaults`() {
        val store = ConfigStore()
        assertEquals(GameConfig(), store.config)
    }
    @Test fun `import replaces the active config`() {
        val store = ConfigStore()
        store.import("""{"player":{"baseSpeed":250.0}}""")
        assertEquals(250f, store.config.player.baseSpeed, 1e-3f)
    }
    @Test fun `export then import is stable`() {
        val store = ConfigStore()
        store.import("""{"player":{"meleeDmg":50.0}}""")
        val exported = store.export()
        val store2 = ConfigStore()
        store2.import(exported)
        assertEquals(store.config, store2.config)
    }
    @Test fun `reset returns to defaults`() {
        val store = ConfigStore()
        store.import("""{"player":{"baseSpeed":1f}}""")
        store.reset()
        assertEquals(GameConfig(), store.config)
    }
}
