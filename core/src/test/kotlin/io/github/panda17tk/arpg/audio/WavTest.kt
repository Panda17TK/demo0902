package io.github.panda17tk.arpg.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** v2.63 生成オーディオ: the shared WAV encoder — header fields and little-endian samples. */
class WavTest {
    private fun le32(b: ByteArray, off: Int): Int =
        (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8) or
            ((b[off + 2].toInt() and 0xFF) shl 16) or ((b[off + 3].toInt() and 0xFF) shl 24)

    private fun le16(b: ByteArray, off: Int): Int =
        (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8)

    @Test fun `the header describes 16-bit mono PCM at the given rate`() {
        val rate = 22050
        val samples = ShortArray(100) { (it * 100).toShort() }
        val b = Wav.mono16(samples, rate)
        assertEquals(Wav.HEADER + 200, b.size)
        assertEquals("RIFF", String(b, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(b, 8, 4, Charsets.US_ASCII))
        assertEquals("fmt ", String(b, 12, 4, Charsets.US_ASCII))
        assertEquals("data", String(b, 36, 4, Charsets.US_ASCII))
        assertEquals(36 + 200, le32(b, 4))   // RIFF chunk size
        assertEquals(1, le16(b, 20))         // PCM
        assertEquals(1, le16(b, 22))         // mono
        assertEquals(rate, le32(b, 24))      // sample rate
        assertEquals(rate * 2, le32(b, 28))  // byte rate
        assertEquals(2, le16(b, 32))         // block align
        assertEquals(16, le16(b, 34))        // bits per sample
        assertEquals(200, le32(b, 40))       // data length
    }

    @Test fun `samples round-trip through the little-endian bytes`() {
        val samples = shortArrayOf(0, 1, -1, 32767, -32768, 12345, -12345)
        val b = Wav.mono16(samples, 8000)
        for (i in samples.indices) {
            val v = le16(b, Wav.HEADER + i * 2).toShort()
            assertEquals(samples[i], v)
        }
    }
}
