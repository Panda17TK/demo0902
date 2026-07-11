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
        loop("bgm/weather-${kind.name.lowercase()}.wav", { AmbienceScore.renderWeather(kind) }, WEATHER_VOL) {
            // a late arrival for a sky we already left is put down, not attached
            if (kind == weatherKind && enabled) { weather = swap(weather, it); applyBed() } else discard(it)
        }
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
        val g = gen // a render that lands after kill() switched scenes must not attach
        // v2.161 細かい残り: applyBed() on arrival — the settings' master gain holds from the
        // first breath (start() used to play at raw VOLUME until the next setMaster/setDuck).
        loop("bgm/${track.name.lowercase()}.wav", { AmbienceScore.render(track) }, VOLUME) {
            if (g == gen && enabled) { music = swap(music, it); applyBed() } else discard(it)
        }
        // v2.67: the reactive layers loop alongside at volume 0; setLayers() breathes them in.
        loop("bgm/pulse-${track.name.lowercase()}.wav", { AmbienceScore.renderLayer(AmbientLayer.PULSE, track) }, 0f) {
            if (g == gen && enabled) pulse = swap(pulse, it) else discard(it)
        }
        loop("bgm/shimmer-${track.name.lowercase()}.wav", { AmbienceScore.renderLayer(AmbientLayer.SHIMMER, track) }, 0f) {
            if (g == gen && enabled) shimmer = swap(shimmer, it) else discard(it)
        }
    }

    // v2.153 音と手の修理: tracks are deterministic per name — render + write each ONCE per
    // session. Re-synthesizing ~1MB of WAV on the main thread at every land/takeoff was the
    // transition hitch.
    // v2.161 細かい残り: the FIRST render moves off the main thread too — a fresh session no
    // longer hitches at the title / first landing while the WAV synthesizes; the loop breathes
    // in a beat later instead. Each assign closure decides whether its arrival still belongs.
    private val rendered = HashSet<String>()
    private var gen = 0 // bumped by kill(); start()'s closures compare against it

    private fun loop(path: String, samples: () -> ShortArray?, vol: Float, assign: (Music?) -> Unit) {
        try {
            val fh = Gdx.files.local(path)
            if (path in rendered && fh.exists()) {
                assign(attach(fh, vol))
                return
            }
            Thread {
                try {
                    val s = samples() ?: return@Thread
                    val bytes = Wav.mono16(s, AmbienceScore.RATE)
                    Gdx.app.postRunnable {
                        try {
                            fh.writeBytes(bytes, false)
                            rendered.add(path)
                            assign(attach(fh, vol))
                        } catch (_: Throwable) { /* no files here → silent */ }
                    }
                } catch (_: Throwable) { /* render failed → stay silent */ }
            }.apply { isDaemon = true; name = "ambience-render" }.start()
        } catch (_: Throwable) { /* no files on this platform → silent */ }
    }

    private fun attach(fh: com.badlogic.gdx.files.FileHandle, vol: Float): Music? = try {
        Gdx.audio.newMusic(fh).apply {
            isLooping = true
            volume = vol
            play()
        }
    } catch (_: Throwable) { null /* no audio on this platform → silent */ }

    private fun discard(m: Music?) {
        try { m?.stop() } catch (_: Throwable) {}
        try { m?.dispose() } catch (_: Throwable) {}
    }

    /** Swap in a late-arriving loop: any older sibling still humming is put down first. */
    private fun swap(old: Music?, new: Music?): Music? {
        if (old !== new) discard(old)
        return new
    }

    private fun kill() {
        gen++ // v2.161: a render still in flight for the old scene must not attach to the new
        for (m in listOf(music, pulse, shimmer, weather)) {
            try { m?.stop() } catch (_: Throwable) {}
            try { m?.dispose() } catch (_: Throwable) {}
        }
        music = null; pulse = null; shimmer = null; weather = null
    }
}
