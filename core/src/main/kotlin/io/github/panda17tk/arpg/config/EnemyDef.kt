package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.planet.PlanetBiome
import kotlinx.serialization.Serializable

/** One enemy attack spec (data-driven; AISystem interprets `type`). */
@Serializable
data class AttackSpec(
    val type: String,
    var cd: Float = 1f,  // v2.99: tunable live (roster knob)
    var dmg: Float = 0f, // v2.99: tunable live (roster knob)
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
    var id: String = "", // v2.113 図鑑: the roster key, stamped by defaultEnemies() — a stable save id

    val tier: String = "normal",
    val color: String = "#b24a4a",
    var hp: Float,    // v2.99: tunable live (roster knob)
    var speed: Float, // v2.99: tunable live (roster knob)
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
    val canSpeak: Boolean = false, // intelligent creatures emit short speech bubbles
    val speechStyle: String = "", // v2.82: the voice's register ("mechanical"/"savage"/"archaic"/"polite"; "" = plain)
    val familyRole: FamilyRole = FamilyRole.NONE, // child/elder/guardian/king drive protection + morale
    val biome: PlanetBiome? = null, // null = generic (space waves); set = lives on that planet type (surface only)
    // Living Planets wildlife (mute animals, driven by WildlifeSystem). Legacy-safe defaults keep a creature HOSTILE.
    val lifeKind: LifeKind = LifeKind.HOSTILE, // HOSTILE legacy / SAPIENT society / WILDLIFE wild animal
    val wildRole: WildRole = WildRole.NONE,    // its niche in the food web (prey/herd/predator/apex/…)
    val diet: Diet = Diet.NONE,                // what it eats (drives who it hunts)
    val herdAffinity: Float = 0f,              // 0..1: how strongly it sticks with its herd
    val fear: Float = 0f,                      // 0..1: how readily it flees the player / predators
    val territoryRadius: Float = 0f,           // px an apex / nest-guard reacts within (0 = none)
    val attacks: List<AttackSpec> = emptyList(),
    val dodge: DodgeSpec? = null,
)
