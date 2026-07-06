package io.github.panda17tk.arpg.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.GameOver
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.WaveState
import io.github.panda17tk.arpg.ecs.world.Pickups
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.sim.WaveEvent
import io.github.panda17tk.arpg.sim.WaveEvents
import io.github.panda17tk.arpg.sim.WorldState

/** Rebuilds the mob spatial grid each tick and reaps dead mobs (kills++, lifesteal heal). */
class MobDamageSystem(private val grid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(Mob, Transform, Health) }) {

    private val gameOver: GameOver = world.inject()
    private val fx: Fx = world.inject()
    private val rng: Rng = world.inject()
    private val worldState: WorldState = world.inject()
    private val waveState: WaveState = world.inject()
    private val players by lazy { world.family { all(PlayerTag, Health, Mods) } }

    override fun onTick() {
        grid.clear()
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        if (entity[Health].hp <= 0f) {
            val mob = entity[Mob]
            // A wild animal's death is part of the ecosystem, not the player's score — no kill count,
            // no lifesteal, no loot farm (a wolf eating a deer must not tick the player's tally).
            val wild = mob.def.lifeKind == LifeKind.WILDLIFE
            val big = mob.tier != "normal"
            if (!wild) {
                gameOver.kills++
                healOnKill()
                // v2.45 星の依頼: the visit's tallies quests are paid from at takeoff.
                worldState.questKills++
                if (big) worldState.questElites++
            } else if (mob.def.wildRole == WildRole.PREDATOR) {
                // v2.69 護衛: one less predator pressing on the children (whoever felled it —
                // the ecosystem's own kills count too; the star only sees the pressure lift).
                worldState.questPredators++
            }
            // v2.85 段階的な死 / v2.88 撃破の儀式: bodies squash out, then burst. A true boss or a
            // bounty head goes grander — five chained blasts, a white-out, a longer slow-mo and a
            // gold reward shower (the player's kills only, never the wild's own hunts).
            val bodyColor = Color.valueOf(mob.def.color.removePrefix("#"))
            val grand = !wild && (mob.tier == "boss" || mob.bountyDust > 0)
            if (grand) {
                fx.spawnDeathGrand(t.x, t.y, mob.def.w, mob.def.h, bodyColor)
                fx.spawnRewardShower(t.x, t.y)
                fx.addShake(0.45f, 12f)
                fx.slowmo(0.5f)
                fx.flash()
                fx.requestSfx("boss_down") // v2.89
            } else {
                fx.spawnDeathStaged(t.x, t.y, mob.def.w, mob.def.h, bodyColor, big)
                fx.addShake(if (big) 0.25f else 0.08f, if (big) 9f else 3.5f)
                if (big && !wild) fx.slowmo(0.30f)
            }
            // v2.45: a magnetic-storm wave shakes double dust from every kill.
            val dustMul = if (waveState.event == WaveEvent.STORM) WaveEvents.STORM_DUST_MUL else 1
            if (!wild) Pickups.dropOnKill(world, rng, t.x, t.y, big, worldState.spawnTweaks.bonusMaterialChance, dustMul)
            // v2.45 賞金首: a bounty head bursts into its dust pile, and the HUD says so.
            if (mob.bountyDust > 0) {
                Pickups.spawn(world, "dust", mob.bountyDust, t.x, t.y - 8f)
                waveState.announce = "賞金首を討ち取った（+${mob.bountyDust}屑）"
            }
            // A planet's king/elite drops a biome material (a core/relic) that grants the player a small boon.
            val biome = mob.def.biome
            if (biome != null && big) Pickups.spawn(world, "mat_" + biome.name.lowercase(), 1, t.x, t.y)
            world -= entity
            return
        }
        // v2.88 フェーズ変化: a heavy that falls past half health rages once — a one-way latch
        // riding the existing enrage machinery (faster swings, quicker attacks, the red aura).
        val mobAlive = entity[Mob]
        if (!mobAlive.phase2 && mobAlive.def.lifeKind != LifeKind.WILDLIFE &&
            (mobAlive.tier == "midboss" || mobAlive.tier == "boss" || mobAlive.bountyDust > 0)
        ) {
            val h = entity[Health]
            if (h.hp <= h.hpMax * 0.5f) {
                mobAlive.phase2 = true
                val a = entity.getOrNull(MobAction)
                if (a != null) {
                    a.enrageMul = maxOf(a.enrageMul, 1.35f)
                    a.enrageT = maxOf(a.enrageT, 9999f) // the rage does not cool
                }
                fx.spawnWarnRing(t.x, t.y)
                fx.addShake(0.2f, 5f)
                fx.requestSfx("phase") // v2.89: the rage has a voice
            }
        }
        grid.insert(entity, t.x, t.y)
    }

    /** Lifesteal: each kill heals the player by Mods.healOnKill, capped at hpMax (legacy). */
    private fun healOnKill() = with(world) {
        players.forEach { p ->
            val mods = p[Mods]
            if (mods.healOnKill > 0f) {
                val ph = p[Health]
                ph.hp = minOf(ph.hpMax, ph.hp + mods.healOnKill)
            }
        }
    }
}
