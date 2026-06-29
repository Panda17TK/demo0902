package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome

/**
 * Pure HUD text for the surface exploration goal: while the planet's masters (kings/elites) live, the
 * objective is to defeat them; once they are gone the planet is subdued and the player is invited to take off.
 */
object SurfaceObjective {
    fun hudLine(biome: PlanetBiome, elitesAlive: Int): String =
        if (elitesAlive > 0) "${biome.displayName}：この星の主を倒せ（残り $elitesAlive）"
        else "${biome.displayName}を制圧した　脱出パッドへ戻れ"
}
