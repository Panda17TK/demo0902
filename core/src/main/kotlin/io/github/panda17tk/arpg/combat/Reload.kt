package io.github.panda17tk.arpg.combat

data class ReloadResult(val newMag: Int, val newReserve: Int, val taken: Int)

/** Pure reload math (legacy combat.js reload). Pulls min(need, reserve) from the reserve. */
object Reload {
    fun reload(magSize: Int, mag: Int, reserve: Int): ReloadResult {
        val need = magSize - mag
        if (need <= 0) return ReloadResult(mag, reserve, 0)
        val take = minOf(need, reserve)
        return ReloadResult(mag + take, reserve - take, take)
    }
}
