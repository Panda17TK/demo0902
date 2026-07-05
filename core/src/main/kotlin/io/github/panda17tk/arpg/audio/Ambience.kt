package io.github.panda17tk.arpg.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music

/**
 * v2.63 生成オーディオ — the playback side. Renders each ambient loop to a local WAV on first
 * request (no binary assets) and loops it quietly under the scene. Lives across screens (the
 * title fades into space without a gap); App.dispose() is the only thing that tears it down.
 * Defensive like Sfx: on a platform with no audio it silently no-ops.
 */
object Ambience {
    private const val VOLUME = 0.22f
    private const val PULSE_MAX = 0.30f   // v2.67: combat heartbeat, at full heat
    private const val SHIMMER_MAX = 0.26f // v2.67: the memory core, standing beside it

    /** Gated by the same サウンド toggle as Sfx (v2.59 設定). */
    var enabled = true
        private set

    private var current: AmbientTrack? = null
    private var music: Music? = null
    private var pulse: Music? = null   // v2.67 reactive layers ride at volume 0 until asked
    private var shimmer: Music? = null

    /** Switch to [track] — no-op if it is already humming. Remembered even while disabled. */
    fun play(track: AmbientTrack) {
        if (current == track && (music != null || !enabled)) return
        current = track
        kill()
        if (enabled) start(track)
    }

    /** v2.67 状況反応: fade the reactive layers (0..1 each) — called every frame, cheap. */
    fun setLayers(pulseLevel: Float, shimmerLevel: Float) {
        try { pulse?.volume = pulseLevel.coerceIn(0f, 1f) * PULSE_MAX } catch (_: Throwable) {}
        try { shimmer?.volume = shimmerLevel.coerceIn(0f, 1f) * SHIMMER_MAX } catch (_: Throwable) {}
    }

    /** The sound toggle: off stops the loop, on resumes whatever the scene last asked for. */
    fun setEnabled(on: Boolean) {
        enabled = on
        if (!on) kill() else if (music == null) current?.let { start(it) }
    }

    fun stop() { kill(); current = null }
    fun dispose() = stop()

    private fun start(track: AmbientTrack) {
        music = loop("bgm/${track.name.lowercase()}.wav", AmbienceScore.render(track), VOLUME)
        // v2.67: the reactive layers loop alongside at volume 0; setLayers() breathes them in.
        pulse = loop("bgm/pulse-${track.name.lowercase()}.wav", AmbienceScore.renderLayer(AmbientLayer.PULSE, track), 0f)
        shimmer = loop("bgm/shimmer-${track.name.lowercase()}.wav", AmbienceScore.renderLayer(AmbientLayer.SHIMMER, track), 0f)
    }

    private fun loop(path: String, samples: ShortArray, vol: Float): Music? = try {
        val fh = Gdx.files.local(path)
        fh.writeBytes(Wav.mono16(samples, AmbienceScore.RATE), false)
        Gdx.audio.newMusic(fh).apply {
            isLooping = true
            volume = vol
            play()
        }
    } catch (_: Throwable) { null /* no audio on this platform → silent */ }

    private fun kill() {
        for (m in listOf(music, pulse, shimmer)) {
            try { m?.stop() } catch (_: Throwable) {}
            try { m?.dispose() } catch (_: Throwable) {}
        }
        music = null; pulse = null; shimmer = null
    }
}
