package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** One enemy attack spec (data-driven; AISystem interprets `type`). */
@Serializable
data class AttackSpec(
    val type: String,
    val cd: Float = 1f,
    val dmg: Float = 0f,
    val range: Float = 0f,
    val arc: Float = 360f,
    val speed: Float = 0f,
)

/** Enemy archetype (legacy CONFIG.enemies entry). tier: normal/midboss/boss. */
@Serializable
data class EnemyDef(
    val name: String,
    val tier: String = "normal",
    val color: String = "#b24a4a",
    val hp: Float,
    val speed: Float,
    val w: Float = 22f,
    val h: Float = 22f,
    val seeRange: Float = 240f,
    val contactKB: Float = 220f,
    val attacks: List<AttackSpec> = emptyList(),
)
