package io.github.panda17tk.arpg.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import io.github.panda17tk.arpg.math.Rng
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tiny procedural sound service (Phase 8). Synthesizes short PCM blips at startup and writes them
 * to local WAV files, so no binary audio assets need bundling. Everything is wrapped defensively:
 * on a platform with no audio (or no write access) it silently no-ops and never affects gameplay.
 */
object Sfx {
    private val sounds = HashMap<String, Sound>()
    const val BASE = 0.4f // v2.106: the one-shot base gain (public so the sim's queue can scale it)
    private const val VOLUME = BASE
    private const val RATE = 22050

    fun init() {
        sounds.clear()
        gen("shot", freq = 660f, ms = 90, noise = 0f)
        gen("hit", freq = 200f, ms = 120, noise = 0.6f)
        gen("kill", freq = 320f, ms = 140, noise = 0.4f)
        gen("levelup", freq = 880f, ms = 240, noise = 0f)
        gen("dead", freq = 110f, ms = 420, noise = 0.3f)
        // LP v2.30 (10a): landing / takeoff / scan cues — low rumble down, brighter rise, a clean ping.
        gen("land", freq = 180f, ms = 300, noise = 0.25f)
        gen("takeoff", freq = 460f, ms = 320, noise = 0.15f)
        gen("scan", freq = 990f, ms = 110, noise = 0f)
        // v2.89 音の追い込み: the feel pass gets its own voices (all still synthesized).
        gen("melee_hit", freq = 240f, ms = 90, noise = 0.5f)              // pitched up per combo step
        gen("banner", freq = 520f, ms = 260, noise = 0f, freq2 = 780f)    // the event stinger rises
        gen("meteor", freq = 900f, ms = 900, noise = 0.35f, freq2 = 160f) // the falling whistle
        gen("boss_down", freq = 150f, ms = 620, noise = 0.4f, freq2 = 55f) // the heavy's last breath
        gen("phase", freq = 170f, ms = 320, noise = 0.5f, freq2 = 330f)   // the rage rising
        // v2.106 音の第3弾: the guns find their voices — "shot" gets per-weapon pitch, and the
        // newcomers, the pickups and the settled rail aim each get a small synthesized cue.
        gen("beam", freq = 320f, ms = 220, noise = 0.12f, freq2 = 1400f)       // the emitter's rising lance
        gen("rail", freq = 90f, ms = 260, noise = 0.30f, freq2 = 30f)          // a deep magnetic thunk
        gen("rail_ready", freq = 1240f, ms = 90, noise = 0f)                   // 置き撃ちが据わった合図
        gen("blade_throw", freq = 340f, ms = 160, noise = 0.55f, freq2 = 900f) // the spinning whoosh
        gen("blade_catch", freq = 760f, ms = 70, noise = 0.2f)                 // back in the hand
        gen("pickup", freq = 1050f, ms = 60, noise = 0f)                       // a small, dry blip
    }

    /** v2.59 設定: master switch (persisted by the title screen's toggle). */
    var enabled = true

    /** v2.96 音量: master gain 0..1 (persisted; scales every one-shot). */
    var volume = 1f

    fun play(name: String) {
        if (!enabled) return
        try { sounds[name]?.play(VOLUME * volume) } catch (_: Throwable) {}
    }

    /** v2.89: pitched playback — a combo climbing in semitone-ish steps, a soft distant boom. */
    fun play(name: String, pitch: Float, vol: Float = VOLUME) {
        if (!enabled) return
        try { sounds[name]?.play(vol * volume, pitch.coerceIn(0.5f, 2f), 0f) } catch (_: Throwable) {}
    }

    fun dispose() {
        sounds.values.forEach { try { it.dispose() } catch (_: Throwable) {} }
        sounds.clear()
    }

    private fun gen(name: String, freq: Float, ms: Int, noise: Float, freq2: Float = freq) {
        try {
            val n = RATE * ms / 1000
            val samples = ShortArray(n)
            val rng = Rng(name.hashCode().toLong())
            var phase = 0.0 // v2.89: integrate the (possibly gliding) frequency for a clean sweep
            for (i in 0 until n) {
                val k = i.toFloat() / n
                val env = 1f - k // linear decay
                phase += 2.0 * PI * (freq + (freq2 - freq) * k) / RATE
                val tone = sin(phase).toFloat()
                val ns = rng.nextFloat() * 2f - 1f
                val s = (tone * (1f - noise) + ns * noise) * env * 0.7f
                samples[i] = (s * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            }
            val fh = Gdx.files.local("sfx/$name.wav")
            fh.writeBytes(Wav.mono16(samples, RATE), false)
            sounds[name] = Gdx.audio.newSound(fh)
        } catch (_: Throwable) { /* no audio on this platform → silent */ }
    }
}
