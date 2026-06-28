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
    }

    fun play(name: String) {
        try { sounds[name]?.play(VOLUME) } catch (_: Throwable) {}
    }

    fun dispose() {
        sounds.values.forEach { try { it.dispose() } catch (_: Throwable) {} }
        sounds.clear()
    }

    private fun gen(name: String, freq: Float, ms: Int, noise: Float) {
        try {
            val n = RATE * ms / 1000
            val bytes = ByteArray(44 + n * 2)
            writeHeader(bytes, n)
            val rng = Rng(name.hashCode().toLong())
            for (i in 0 until n) {
                val t = i.toFloat() / RATE
                val env = 1f - i.toFloat() / n // linear decay
                val tone = sin(2.0 * PI * freq * t).toFloat()
                val ns = rng.nextFloat() * 2f - 1f
                val s = (tone * (1f - noise) + ns * noise) * env * 0.7f
                val v = (s * 32767f).toInt().coerceIn(-32768, 32767)
                bytes[44 + i * 2] = (v and 0xFF).toByte()
                bytes[45 + i * 2] = ((v shr 8) and 0xFF).toByte()
            }
            val fh = Gdx.files.local("sfx/$name.wav")
            fh.writeBytes(bytes, false)
            sounds[name] = Gdx.audio.newSound(fh)
        } catch (_: Throwable) { /* no audio on this platform → silent */ }
    }

    /** Minimal 16-bit mono PCM WAV header. */
    private fun writeHeader(b: ByteArray, samples: Int) {
        val dataLen = samples * 2
        fun str(off: Int, s: String) { for (j in s.indices) b[off + j] = s[j].code.toByte() }
        fun i32(off: Int, v: Int) {
            b[off] = (v and 0xFF).toByte(); b[off + 1] = ((v shr 8) and 0xFF).toByte()
            b[off + 2] = ((v shr 16) and 0xFF).toByte(); b[off + 3] = ((v shr 24) and 0xFF).toByte()
        }
        fun i16(off: Int, v: Int) { b[off] = (v and 0xFF).toByte(); b[off + 1] = ((v shr 8) and 0xFF).toByte() }
        str(0, "RIFF"); i32(4, 36 + dataLen); str(8, "WAVE"); str(12, "fmt ")
        i32(16, 16); i16(20, 1); i16(22, 1); i32(24, RATE); i32(28, RATE * 2); i16(32, 2); i16(34, 16)
        str(36, "data"); i32(40, dataLen)
    }
}
