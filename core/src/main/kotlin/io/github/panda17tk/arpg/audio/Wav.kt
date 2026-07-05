package io.github.panda17tk.arpg.audio

/** v2.63 生成オーディオ: minimal 16-bit mono PCM WAV encoder — the one header both the SFX
 *  blips and the ambient loops share. Pure bytes in, bytes out; no Gdx, fully testable. */
object Wav {
    const val HEADER = 44

    fun mono16(samples: ShortArray, rate: Int): ByteArray {
        val dataLen = samples.size * 2
        val b = ByteArray(HEADER + dataLen)
        fun str(off: Int, s: String) { for (j in s.indices) b[off + j] = s[j].code.toByte() }
        fun i32(off: Int, v: Int) {
            b[off] = (v and 0xFF).toByte(); b[off + 1] = ((v shr 8) and 0xFF).toByte()
            b[off + 2] = ((v shr 16) and 0xFF).toByte(); b[off + 3] = ((v shr 24) and 0xFF).toByte()
        }
        fun i16(off: Int, v: Int) { b[off] = (v and 0xFF).toByte(); b[off + 1] = ((v shr 8) and 0xFF).toByte() }
        str(0, "RIFF"); i32(4, 36 + dataLen); str(8, "WAVE"); str(12, "fmt ")
        i32(16, 16); i16(20, 1); i16(22, 1); i32(24, rate); i32(28, rate * 2); i16(32, 2); i16(34, 16)
        str(36, "data"); i32(40, dataLen)
        for (i in samples.indices) {
            val v = samples[i].toInt()
            b[HEADER + i * 2] = (v and 0xFF).toByte()
            b[HEADER + i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return b
    }
}
