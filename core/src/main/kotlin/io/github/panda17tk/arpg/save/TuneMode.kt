package io.github.panda17tk.arpg.save

/**
 * v2.98 調整モード — a developer door behind a passcode on the title screen. While active,
 * the game screen grows a 調整 button (left of 持物) whose popup turns the live balance
 * knobs. Session-scoped on purpose: an app restart closes the door again.
 */
object TuneMode {
    const val PASSWORD = "2938"

    var active = false

    /** Try a code; the right one opens the mode (and stays open until toggled off). */
    fun tryUnlock(code: String): Boolean {
        if (code != PASSWORD) return false
        active = true
        return true
    }
}
