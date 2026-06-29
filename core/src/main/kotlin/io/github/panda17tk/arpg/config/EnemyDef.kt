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
    val power: Float = 0f,
    val windup: Float = 0.7f,
    val reach: Float = 0f,
    val maxTiles: Int = 5,
    val dur: Float = 0.1f,
    val minDist: Float = 60f,
    val standoff: Float = 28f,
    val kb: Float = 240f,
    val life: Float = 1.6f,
    // Boss attack fields (legacy attacks.js): fans/radials, summon, heal, enrage/guard, homing.
    val count: Int = 0,
    val spread: Float = 0f,
    val amount: Float = 0f,
    val mul: Float = 1f,
    val minion: String = "",
    val turn: Float = 0f,
    val duration: Float = 0f,
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
    val gravityResponse: Float = 1f, // 0 = ignores gravity, 1 = normal, >1 = heavy (easily flung into planets)
    // Living Planets temperament (legacy-safe defaults → the creature stays always hostile):
    val intelligence: Float = 0f,
    val bravery: Float = 1f,
    val protectiveness: Float = 0f,
    val mercyThreshold: Float = 0f,
    val canBeg: Boolean = false,
    val canHideAndRest: Boolean = false,
    val attacks: List<AttackSpec> = emptyList(),
    val dodge: DodgeSpec? = null,
)
