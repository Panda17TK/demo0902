package io.github.panda17tk.arpg.save

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** v2.46 遺品回収: half the dust, every shard; nothing worth nothing; defensive JSON. */
class DeathRelicTest {
    @Test fun `a relic keeps half the dust and every shard`() {
        val r = DeathRelic.of(10f, 20f, dust = 99, shards = 2, space = true)!!
        assertEquals(49, r.dust) // 99 / 2 rounded down
        assertEquals(2, r.shards)
        assertEquals(true, r.space)
    }

    @Test fun `an empty-handed death leaves no relic`() {
        assertNull(DeathRelic.of(0f, 0f, dust = 1, shards = 0, space = true)) // 1/2 == 0
        assertNull(DeathRelic.of(0f, 0f, dust = 0, shards = 0, space = false))
    }

    @Test fun `shards alone still warrant a relic`() {
        val r = DeathRelic.of(5f, 5f, dust = 0, shards = 1, space = false)!!
        assertEquals(0, r.dust)
        assertEquals(1, r.shards)
    }

    @Test fun `round-trips through JSON and survives garbage`() {
        val r = RelicDto(x = 640f, y = 320f, dust = 33, shards = 3, space = false)
        assertEquals(r, DeathRelic.fromJson(DeathRelic.toJson(r)))
        assertNull(DeathRelic.fromJson("{"))
    }

    @Test fun `the in-memory store exercises the same codec path`() {
        val store = InMemoryRelicStore()
        assertNull(store.load())
        store.save(RelicDto(1f, 2f, 3, 1))
        assertEquals(RelicDto(1f, 2f, 3, 1), store.load())
        store.clear()
        assertNull(store.load())
    }
}
