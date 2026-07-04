package io.github.panda17tk.arpg.ecs.components

import io.github.panda17tk.arpg.sim.WaveEvent

/** Run-time wave state injected into the world (legacy state.wave). num doubles as the score. */
class WaveState(
    var num: Int = 1,
    var phase: String = "active", // "active" | "intermission"
    var toSpawn: Int = 0,
    var spawnCd: Float = 0.4f,
    var interT: Float = 0f,
    var elapsed: Float = 0f,
    // v2.45: this wave's flavor (horde/storm/bounty) + a one-shot announcement line the
    // sim leaves for the screen to pick up as a toast (consumed by GameScreen, set to null).
    var event: WaveEvent = WaveEvent.NONE,
    var announce: String? = null,
)
