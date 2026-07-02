package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome

/** The kinds of surface goals v1 can derive read-only from existing state (§9: no quest machine). */
enum class SurfaceGoalKind { DEFEAT_MASTERS, CLAIM_RELIC, PROTECT_CHILDREN, DEFEAT_APEX }

enum class GoalState { OPEN, DONE, FAILED }

/**
 * Per-planet surface goals (LP v2.26) — derived deterministically from the planet's biome + context
 * and judged read-only against [PlanetSocietyState] + the live elite count. No state machine: DONE /
 * FAILED fall out of what the society already remembers. On an APEX-sacred world the apex goal
 * inverts into a 「触れるな」 taboo (killing it FAILS). Chip strings live only here (§14.3). Pure.
 */
object SurfaceGoals {
    /** Biomes whose SurfaceEcology **always** places a WildRole.APEX beast (forest_apex / frost_worm).
     *  MAGMA has none and LONELY's last_beast is a coin flip, so the goal would be unfulfillable there. */
    val APEX_BIOMES = setOf(PlanetBiome.NATURE, PlanetBiome.ICE)

    /** The deterministic goal set for a planet — same planet, same goals, every visit (FR-6.2). */
    fun forPlanet(biome: PlanetBiome, ctx: PlanetContext): List<SurfaceGoalKind> = buildList {
        add(SurfaceGoalKind.DEFEAT_MASTERS)
        add(SurfaceGoalKind.CLAIM_RELIC)
        if (biome == PlanetBiome.NATURE || ctx.sacredThing == SacredThing.CHILDREN || ctx.storySeed == PlanetStorySeed.LOST_CHILD) {
            add(SurfaceGoalKind.PROTECT_CHILDREN)
        }
        if (biome in APEX_BIOMES) add(SurfaceGoalKind.DEFEAT_APEX)
    }

    fun state(kind: SurfaceGoalKind, s: PlanetSocietyState, ctx: PlanetContext, elitesAlive: Int): GoalState = when (kind) {
        SurfaceGoalKind.DEFEAT_MASTERS -> if (elitesAlive == 0 && s.leaderDefeated) GoalState.DONE else GoalState.OPEN
        SurfaceGoalKind.CLAIM_RELIC -> if (s.relicClaimed) GoalState.DONE else GoalState.OPEN
        // Deduction-only: there is no DONE for keeping children safe — only the wound of failing (FR-6.1).
        SurfaceGoalKind.PROTECT_CHILDREN -> if (s.childHarmed || s.childKilled) GoalState.FAILED else GoalState.OPEN
        SurfaceGoalKind.DEFEAT_APEX ->
            if (ctx.sacredThing == SacredThing.APEX) { // taboo type: the sacred beast must not fall
                if (s.apexKilled) GoalState.FAILED else GoalState.OPEN
            } else {
                if (s.apexKilled) GoalState.DONE else GoalState.OPEN
            }
    }

    /** The short HUD chip for one goal — command form while open, past form when settled. */
    fun chip(kind: SurfaceGoalKind, st: GoalState, ctx: PlanetContext): String = when (kind) {
        SurfaceGoalKind.DEFEAT_MASTERS -> when (st) {
            GoalState.DONE -> "◯ 主を倒した"
            else -> "− 主を倒せ"
        }
        SurfaceGoalKind.CLAIM_RELIC -> when (st) {
            GoalState.DONE -> "◯ 遺物を手にした"
            else -> "− 遺物"
        }
        SurfaceGoalKind.PROTECT_CHILDREN -> when (st) {
            GoalState.FAILED -> "× 子らを守れ"
            else -> "− 子らを守れ"
        }
        SurfaceGoalKind.DEFEAT_APEX ->
            if (ctx.sacredThing == SacredThing.APEX) when (st) {
                GoalState.FAILED -> "× 神獣に触れてしまった"
                else -> "！ 神獣に触れるな"
            } else when (st) {
                GoalState.DONE -> "◯ 星の主を倒した"
                else -> "− 星の主を倒せ"
            }
    }

    /**
     * The HUD's chip row: PROTECT / APEX / RELIC in priority order, at most [max] shown (FR-6.3).
     * DEFEAT_MASTERS is excluded — the main objective line already carries it.
     */
    fun chipsFor(biome: PlanetBiome, ctx: PlanetContext, s: PlanetSocietyState, elitesAlive: Int, max: Int = 2): List<String> =
        forPlanet(biome, ctx)
            .filter { it != SurfaceGoalKind.DEFEAT_MASTERS }
            .sortedBy { CHIP_PRIORITY.indexOf(it) }
            .take(max)
            .map { chip(it, state(it, s, ctx, elitesAlive), ctx) }

    /** Every goal as a chip line, uncapped — for the pause memory summary (FR-6.5). */
    fun allChips(biome: PlanetBiome, ctx: PlanetContext, s: PlanetSocietyState, elitesAlive: Int): List<String> =
        forPlanet(biome, ctx).map { chip(it, state(it, s, ctx, elitesAlive), ctx) }

    private val CHIP_PRIORITY = listOf(
        SurfaceGoalKind.PROTECT_CHILDREN, SurfaceGoalKind.DEFEAT_APEX, SurfaceGoalKind.CLAIM_RELIC,
    )
}
