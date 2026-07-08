package io.github.panda17tk.arpg.render

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.129 野生動物の意匠: the classifier is pure — every animal gets a plan that reads as its motif. */
class CreatureLookTest {
    @Test fun `signature species land on their motif`() {
        assertEquals(CreatureLook.Form.PREDATOR, CreatureLook.of("fang_wolf", WildRole.PREDATOR).form)
        assertTrue(CreatureLook.of("fang_wolf", WildRole.PREDATOR).bushyTail, "a wolf carries its tail")
        val deer = CreatureLook.of("horn_deer", WildRole.HERD)
        assertEquals(CreatureLook.Form.QUADRUPED, deer.form)
        assertTrue(deer.horns, "a deer wears antlers")
        assertTrue(CreatureLook.of("root_boar", WildRole.HERD).tusks, "a boar wears tusks")
        val hare = CreatureLook.of("frost_hare", WildRole.PREY)
        assertEquals(CreatureLook.Form.RODENT, hare.form)
        assertTrue(hare.longEars, "a hare's ears stand tall")
        assertEquals(CreatureLook.Form.FLYER, CreatureLook.of("snow_owl", WildRole.PREY).form)
        assertEquals(CreatureLook.Form.FLOATER, CreatureLook.of("cloud_plankton", WildRole.PREY).form)
        assertEquals(CreatureLook.Form.SHELLED, CreatureLook.of("thorn_tortoise", WildRole.HERD).form)
        assertEquals(CreatureLook.Form.SERPENT, CreatureLook.of("lava_serpent", WildRole.PREDATOR).form)
        assertEquals(CreatureLook.Form.FISH, CreatureLook.of("star_sardine", WildRole.HERD).form) // v2.130
        assertEquals(CreatureLook.Form.FISH, CreatureLook.of("void_koi", WildRole.HERD).form)
        assertEquals(CreatureLook.Form.FISH, CreatureLook.of("lantern_angler", WildRole.PREY).form)
    }

    @Test fun `the live roster's wildlife spreads across many distinct body plans`() {
        val wild = GameConfig().enemies.filterValues { it.lifeKind == LifeKind.WILDLIFE }
        assertTrue(wild.size >= 30, "the roster keeps its wildlife (got ${wild.size})")
        val forms = wild.map { (id, def) -> CreatureLook.of(id, def.wildRole).form }.toSet()
        assertTrue(forms.size >= 6, "at least six distinct body plans in the wild (got $forms)")
    }
}
