package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/**
 * Living Planets — wildlife layer. The watchword: 知性生物は社会を作る。野生動物は生態系を作る。
 * (Sapients build societies; wildlife build ecosystems.)
 *
 * [LifeKind] separates the three populations that share the `Mob` machinery:
 *  - [HOSTILE]  legacy always-aggressive enemies (the space waves) — the back-compat default.
 *  - [SAPIENT]  intelligent society creatures that speak, protect kin and surrender (driven by AISystem).
 *  - [WILDLIFE] mute wild animals that graze, herd, flee and hunt each other (driven by WildlifeSystem).
 */
@Serializable
enum class LifeKind { HOSTILE, SAPIENT, WILDLIFE }

/** An animal's niche in the food web. NONE = not wildlife. */
@Serializable
enum class WildRole {
    NONE,
    PREY,        // small, skittish, bolts from anything
    HERD,        // grazes in groups, regroups when split, flees predators
    PREDATOR,    // hunts prey when hungry, wary of the player
    APEX,        // territorial top of the chain — threatens intruders
    SCAVENGER,   // feeds on carrion/leftovers, flees the living
    SWARM,       // tiny clustering creatures
    NEST_GUARD,  // defends a nest of hatchlings
    HATCHLING,   // defenceless young — always flees
    SCHOOL,      // v2.131: boid fish — moved by SchoolFishSystem, not WildlifeSystem
}

/** What an animal eats (drives who it hunts / ignores). */
@Serializable
enum class Diet { NONE, HERBIVORE, CARNIVORE, OMNIVORE, SCAVENGER, ENERGY, MINERAL }

/**
 * Runtime behavioural state for a wild animal (set each tick by [io.github.panda17tk.arpg.sim.WildAI]).
 * Not an EnemyDef field — it lives on the Mob component, so it stays out of the serialized config.
 */
enum class WildState {
    Graze,      // standing, nibbling — barely moves
    Wander,     // ambling along a slow heading
    Herd,       // moving back toward the herd's centre
    Flee,       // sprinting directly away from a threat
    Threaten,   // facing an intruder, holding ground (no charge)
    Hunt,       // prowling outward, looking for prey
    Stalk,      // creeping toward spotted prey
    Chase,      // running down prey (or an intruder)
    Feed,       // stopped at food/carrion
    GuardNest,  // circling the nest to defend it
    ReturnNest, // heading back toward the nest
    Sleep,      // resting, immobile
}
