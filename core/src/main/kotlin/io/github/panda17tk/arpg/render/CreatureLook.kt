package io.github.panda17tk.arpg.render

import io.github.panda17tk.arpg.config.WildRole

/**
 * v2.129 野生動物の意匠 — the pure classifier behind the wildlife painters. Given a roster id
 * and wild role it picks the body plan and its motif features (antlers, tusks, long ears,
 * wings, shells…) so every animal reads as an animal at a glance. Pure data: Actors paints
 * from it, tests pin it, and a new species only needs a keyword here to find its shape.
 */
object CreatureLook {
    enum class Form { QUADRUPED, PREDATOR, FLYER, FLOATER, SHELLED, SERPENT, RODENT, FISH }

    data class Look(
        val form: Form,
        val horns: Boolean = false,    // deer/elk antlers, ram/muskox curls
        val tusks: Boolean = false,    // boar tusks
        val longEars: Boolean = false, // hare/hopper ears
        val bushyTail: Boolean = false, // wolves, lynxes, otters carry a visible tail
    )

    fun of(id: String, role: WildRole): Look {
        fun has(vararg keys: String) = keys.any { it in id }
        return when {
            has("sardine", "koi", "angler", "fish") -> Look(Form.FISH) // v2.130 宙を泳ぐもの
            has("serpent", "eel", "worm", "parasite") -> Look(Form.SERPENT)
            has("moth", "crow", "owl", "ashwing", "ray", "sky_") -> Look(Form.FLYER)
            has("jelly", "plankton", "wisp") -> Look(Form.FLOATER)
            has("tortoise", "beetle", "mimic") -> Look(Form.SHELLED)
            has("rat", "hare", "hopper", "skipper", "lizard") ->
                Look(Form.RODENT, longEars = has("hare", "hopper"))
            has("wolf", "stalker", "lynx", "fang", "hound", "beast") ||
                role == WildRole.PREDATOR || role == WildRole.APEX ->
                Look(Form.PREDATOR, bushyTail = true)
            else -> Look(
                Form.QUADRUPED,
                horns = has("deer", "elk", "ram", "muskox", "horn"),
                tusks = has("boar"),
                bushyTail = has("otter"),
            )
        }
    }
}
