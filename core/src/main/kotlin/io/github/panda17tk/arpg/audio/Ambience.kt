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
    private const val WEATHER_VOL = 0.20f // v2.76: the sky sits just under the pad

    /** Gated by the same サウンド toggle as Sfx (v2.59 設定). */
    var enabled = true
        private set

    private var duck = 1f // v2.89: slow-mo / the white-out pull the whole bed down with them
    private var master = 1f // v2.96 音量: the settings' master gain
    private var current: AmbientTrack? = null
    private var music: Music? = null
    private var pulse: Music? = null   // v2.67 reactive layers ride at volume 0 until asked
    private var shimmer: Music? = null
    private var weather: Music? = null // v2.76: the sky's own loop (rain hiss / wind)
    private var weatherKind: io.github.panda17tk.arpg.sim.WeatherKind = io.github.panda17tk.arpg.sim.WeatherKind.CLEAR

    /** Switch to [track] — no-op if it is already humming. Remembered even while disabled. */
    fun play(track: AmbientTrack) {
        if (current == track && (music != null || !enabled)) return
        current = track
        kill()
        if (enabled) start(track)
    }

    /** v2.67 状況反応: fade the reactive layers (0..1 each) — called every frame, cheap. */
    fun setLayers(pulseLevel: Float, shimmerLevel: Float) {
        try { pulse?.volume = pulseLevel.coerceIn(0f, 1f) * PULSE_MAX * duck * master } catch (_: Throwable) {}
        try { shimmer?.volume = shimmerLevel.coerceIn(0f, 1f) * SHIMMER_MAX * duck * master } catch (_: Throwable) {}
    }

    /** v2.89 オーディオダック: 1 = full bed, ~0.35 while time is held. Applied every frame. */
    fun setDuck(f: Float) {
        duck = f.coerceIn(0.2f, 1f)
        applyBed()
    }

    /** v2.96 音量: the settings' master gain (0..1) over every loop. */
    fun setMaster(f: Float) {
        master = f.coerceIn(0f, 1f)
        applyBed()
    }

    private fun applyBed() {
        try { music?.volume = VOLUME * duck * master } catch (_: Throwable) {}
        try { weather?.volume = WEATHER_VOL * duck * master } catch (_: Throwable) {}
    }

    /** v2.76 天候: swap the sky's loop — CLEAR (or leaving the surface) fades it away. */
    fun playWeather(kind: io.github.panda17tk.arpg.sim.WeatherKind) {
        if (kind == weatherKind && (weather != null || !enabled || kind == io.github.panda17tk.arpg.sim.WeatherKind.CLEAR)) return
        weatherKind = kind
        try { weather?.stop() } catch (_: Throwable) {}
        try { weather?.dispose() } catch (_: Throwable) {}
        weather = null
        if (!enabled) return
        val samples = AmbienceScore.renderWeather(kind) ?: return
        weather = loop("bgm/weather-${kind.name.lowercase()}.wav", samples, WEATHER_VOL)
    }

    /** The sound toggle: off stops the loop, on resumes whatever the scene last asked for. */
    fun setEnabled(on: Boolean) {
        enabled = on
        if (!on) kill() else {
            if (music == null) current?.let { start(it) }
            val k = weatherKind
            weatherKind = io.github.panda17tk.arpg.sim.WeatherKind.CLEAR
            playWeather(k) // restart the sky too
        }
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
        for (m in listOf(music, pulse, shimmer, weather)) {
            try { m?.stop() } catch (_: Throwable) {}
            try { m?.dispose() } catch (_: Throwable) {}
        }
        music = null; pulse = null; shimmer = null; weather = null
    }
}
