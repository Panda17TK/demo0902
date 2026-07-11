package io.github.panda17tk.arpg.save

/**
 * v2.167 性能計 — the on-device profiler line. Three trains of blind cuts (v2.164 LOD, v2.165
 * density, v2.166 the ninefold sky) and the phone still stutters, so the next cut is chosen by
 * MEASUREMENT: a settings toggle paints fps / sim ms / draw ms / drawn-vs-live entities / heap
 * in the corner, and the report from the real device names the culprit. Presentation-only —
 * the timers wrap the screen's frame, never the sim's clock; determinism is untouched.
 */
object PerfHud {
    var enabled = false

    /** Exponential moving averages, in milliseconds (smoothed so the line is readable). */
    var simMs = 0f
        private set
    var drawMs = 0f
        private set

    /** Mobs the scene actually painted last frame (set by SceneRenderer's culling pass). */
    var mobsDrawn = 0

    fun tickSim(nanos: Long) { simMs = simMs * 0.9f + (nanos / 1_000_000f) * 0.1f }
    fun tickDraw(nanos: Long) { drawMs = drawMs * 0.9f + (nanos / 1_000_000f) * 0.1f }
}
