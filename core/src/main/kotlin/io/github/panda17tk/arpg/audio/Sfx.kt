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
    private const val VOLUME = 0.4f
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
    }

    /** v2.59 設定: master switch (persisted by the title screen's toggle). */
    var enabled = true

    fun play(name: String) {
        if (!enabled) return
        try { sounds[name]?.play(VOLUME) } catch (_: Throwable) {}
    }

    fun dispose() {
        sounds.values.forEach { try { it.dispose() } catch (_: Throwable) {} }
        sounds.clear()
    }

    private fun gen(name: String, freq: Float, ms: Int, noise: Float) {
        try {
            val n = RATE * ms / 1000
            val samples = ShortArray(n)
            val rng = Rng(name.hashCode().toLong())
            for (i in 0 until n) {
                val t = i.toFloat() / RATE
                val env = 1f - i.toFloat() / n // linear decay
                val tone = sin(2.0 * PI * freq * t).toFloat()
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
