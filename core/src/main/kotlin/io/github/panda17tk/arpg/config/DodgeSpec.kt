package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** Mob evasion passive (legacy enemy def `dodge`). */
@Serializable
data class DodgeSpec(val chance: Float = 0.18f, val duration: Float = 0.15f, val cd: Float = 2.0f)
