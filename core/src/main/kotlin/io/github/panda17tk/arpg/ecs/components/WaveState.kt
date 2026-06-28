package io.github.panda17tk.arpg.ecs.components

/** Run-time wave state injected into the world (legacy state.wave). num doubles as the score. */
class WaveState(
    var num: Int = 1,
    var phase: String = "active", // "active" | "intermission"
    var toSpawn: Int = 0,
    var spawnCd: Float = 0.4f,
    var interT: Float = 0f,
    var elapsed: Float = 0f,
)
