package io.github.panda17tk.arpg.ui

/**
 * v2.47 オンボーディング: the very first run walks the basics in four timed steps, then never
 * speaks again (completion is persisted by the screen). Pure — time in, line out.
 */
object Onboarding {
    const val END = 32f // seconds of guidance in total

    fun lineFor(t: Float, touch: Boolean): String? = when {
        t < 8f -> if (touch) "左スティックで移動" else "WASD で移動"
        t < 16f -> if (touch) "右スティックで照準・離して発射" else "マウスで照準・クリックで発射"
        t < 24f -> if (touch) "DASH でダッシュ（地表では縮地になる）" else "Shift でダッシュ（地表では縮地になる）"
        t < END -> "惑星に近づくとカードが出る — 着陸してみよう"
        else -> null
    }
}
