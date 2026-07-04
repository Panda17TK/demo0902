package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.combat.Weapons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfigCodecTest {
    @Test fun `default config round-trips through JSON unchanged`() {
        val cfg = GameConfig()
        val restored = ConfigCodec.fromJson(ConfigCodec.toJson(cfg))
        assertEquals(cfg, restored)
    }
    @Test fun `edited values survive a round-trip`() {
        val cfg = GameConfig(player = PlayerConfig(baseSpeed = 999f))
        val restored = ConfigCodec.fromJson(ConfigCodec.toJson(cfg))
        assertEquals(999f, restored.player.baseSpeed, 1e-3f)
        assertEquals(Weapons.ALL.size, restored.weapons.size)
    }
    @Test fun `partial JSON fills missing fields from defaults`() {
        // only baseSpeed provided; everything else should default
        val restored = ConfigCodec.fromJson("""{"player":{"baseSpeed":200.0}}""")
        assertEquals(200f, restored.player.baseSpeed, 1e-3f)
        assertEquals(1.2f, restored.player.speedMul, 1e-3f) // default
        assertEquals(Weapons.ALL.size, restored.weapons.size) // default weapons
    }
}
