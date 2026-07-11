package io.github.panda17tk.arpg

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // v2.168 安全な帰還: the black box — record what killed the process, so the title can
        // show it next boot (an on-device 「内部エラー」 report gives nothing to work with).
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val top = e.stackTrace.firstOrNull { it.className.contains("panda17tk") } ?: e.stackTrace.firstOrNull()
                val where = top?.let { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" } ?: "?"
                getSharedPreferences("drift-settings", MODE_PRIVATE).edit()
                    .putString("lastCrash", "${e.javaClass.simpleName}: ${e.message ?: ""} @ $where".take(220))
                    .commit()
            } catch (_: Throwable) { /* the box must never make things worse */ }
            prev?.uncaughtException(t, e)
        }
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
        }
        initialize(App(), config)
    }
}
