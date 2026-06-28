package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.combat.Weapons
import io.github.panda17tk.arpg.combat.WeaponDef
import kotlinx.serialization.Serializable

/** Root editable config. Phases 5/6 add enemies/waves/drops/upgrades sections. */
@Serializable
data class GameConfig(
    val player: PlayerConfig = PlayerConfig(),
    val weapons: List<WeaponDef> = Weapons.ALL,
)
