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

    /** Gated by the same サウンド toggle as Sfx (v2.59 設定). */
    var enabled = true
        private set

    private var current: AmbientTrack? = null
    private var music: Music? = null

    /** Switch to [track] — no-op if it is already humming. Remembered even while disabled. */
    fun play(track: AmbientTrack) {
        if (current == track && (music != null || !enabled)) return
        current = track
        kill()
        if (enabled) start(track)
    }

    /** The sound toggle: off stops the loop, on resumes whatever the scene last asked for. */
    fun setEnabled(on: Boolean) {
        enabled = on
        if (!on) kill() else if (music == null) current?.let { start(it) }
    }

    fun stop() { kill(); current = null }
    fun dispose() = stop()

    private fun start(track: AmbientTrack) {
        try {
            val fh = Gdx.files.local("bgm/${track.name.lowercase()}.wav")
            fh.writeBytes(Wav.mono16(AmbienceScore.render(track), AmbienceScore.RATE), false)
            music = Gdx.audio.newMusic(fh).apply {
                isLooping = true
                volume = VOLUME
                play()
            }
        } catch (_: Throwable) { music = null /* no audio on this platform → silent */ }
    }

    private fun kill() {
        try { music?.stop() } catch (_: Throwable) {}
        try { music?.dispose() } catch (_: Throwable) {}
        music = null
    }
}
