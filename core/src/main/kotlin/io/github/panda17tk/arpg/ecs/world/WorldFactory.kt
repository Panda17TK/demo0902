package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.configureWorld
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Buff
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.GameOver
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.ecs.components.WaveState
import io.github.panda17tk.arpg.ecs.components.WeaponRuntime
import io.github.panda17tk.arpg.ecs.systems.AISystem
import io.github.panda17tk.arpg.ecs.systems.BaseSystem
import io.github.panda17tk.arpg.ecs.systems.BuildSystem
import io.github.panda17tk.arpg.ecs.systems.EBulletSystem
import io.github.panda17tk.arpg.ecs.systems.FireSystem
import io.github.panda17tk.arpg.ecs.systems.FlowRebuildSystem
import io.github.panda17tk.arpg.ecs.systems.GameOverSystem
import io.github.panda17tk.arpg.ecs.systems.GravitySystem
import io.github.panda17tk.arpg.ecs.systems.LandingSystem
import io.github.panda17tk.arpg.ecs.systems.MeleeSystem
import io.github.panda17tk.arpg.ecs.systems.MobActionSystem
import io.github.panda17tk.arpg.ecs.systems.MobDamageSystem
import io.github.panda17tk.arpg.ecs.systems.MovementSystem
import io.github.panda17tk.arpg.ecs.systems.PickupSystem
import io.github.panda17tk.arpg.ecs.systems.ProjectileSystem
import io.github.panda17tk.arpg.ecs.systems.ReloadSystem
import io.github.panda17tk.arpg.ecs.systems.SmokeSystem
import io.github.panda17tk.arpg.ecs.systems.SnapshotSystem
import io.github.panda17tk.arpg.ecs.systems.SpawnerSystem
import io.github.panda17tk.arpg.ecs.systems.WeaponSwitchSystem
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.MapLoader
import io.github.panda17tk.arpg.map.Stages
import io.github.panda17tk.arpg.map.SurfaceStages
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.Base
import io.github.panda17tk.arpg.sim.BaseField
import io.github.panda17tk.arpg.sim.Bases
import io.github.panda17tk.arpg.sim.GravityField
import io.github.panda17tk.arpg.sim.PlanetField
import io.github.panda17tk.arpg.sim.Planets
import io.github.panda17tk.arpg.sim.Tribes
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WallGravity
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.sim.WorldState
import kotlin.math.floor

object WorldFactory {
    /** [seed] keeps stage selection deterministic for tests. */
    fun create(
        input: InputState, config: GameConfig = GameConfig(), seed: Long = 1L,
        mode: WorldMode = WorldMode.SPACE, biome: PlanetBiome? = null, carry: PlayerCarry? = null,
    ): GameWorld {
        val loaded = MapLoader.load(
            if (mode == WorldMode.SURFACE) SurfaceStages.forBiome(biome, seed) else Stages.random(Rng(seed)),
        )
        val map = loaded.tileMap
        val flow = FlowField(map.width, map.height)
        flow.rebuild(map, floor(loaded.playerSpawnX / Tuning.TILE).toInt(), floor(loaded.playerSpawnY / Tuning.TILE).toInt(), FlowRebuildSystem.MAX_DIST)
        val combatRng = Rng(seed xor 0x9E3779B9L)

        val mobGrid = SpatialGrid<Entity>(Tuning.TILE)
        val gameOver = GameOver()
        val fx = Fx(Rng(seed xor 0x123456789L))
        // Per-run enemy tribes: 5 spatial tribes, ~35% of pairs mutually hostile (they brawl on sight).
        val tribes = Tribes.build(5, map.width * Tuning.TILE, map.height * Tuning.TILE, 0.35f, Rng(seed xor 0x7A3B1C9DL))
        // Strongholds ("stars"): the biggest block clusters near spawn become tribe bases (reinforce from them).
        val baseField = run {
            val ctx = floor(loaded.playerSpawnX / Tuning.TILE).toInt(); val cty = floor(loaded.playerSpawnY / Tuning.TILE).toInt()
            val win = 120
            val clusters = WallGravity.detect(maxOf(0, ctx - win), minOf(map.width - 1, ctx + win), maxOf(0, cty - win), minOf(map.height - 1, cty + win), map::destructibleAt)
            BaseField(Bases.pickLargest(clusters, 6, 18).map { Base(it.cx * Tuning.TILE, it.cy * Tuning.TILE, tribes.tribeOf(it.cx * Tuning.TILE, it.cy * Tuning.TILE), it.radius * Tuning.TILE) })
        }
        val gravityField = GravityField()
        // Discrete planets: 2..4 per stage, deterministic from the seed, clear of the player spawn.
        val planetCount = 2 + Rng(seed xor 0x50A4E70BL).nextInt(3)
        val planetField = PlanetField(
            if (mode == WorldMode.SURFACE) emptyList()
            else Planets.place(
                map.width * Tuning.TILE, map.height * Tuning.TILE,
                loaded.playerSpawnX, loaded.playerSpawnY, planetCount, Rng(seed xor 0x91A2B3C4L),
            ),
        )
        val worldState = WorldState(mode = mode, biome = biome)
        val waveState = WaveState(
            num = 1,
            phase = "active",
            toSpawn = minOf(config.waves.maxQuota, config.waves.baseQuota),
            spawnCd = 0.4f,
        )

        val world = configureWorld {
            injectables {
                add(input)
                add(map)
                add(flow)
                add(combatRng)
                add(config)
                add(mobGrid)
                add(gameOver)
                add(waveState)
                add(fx)
                add(tribes)
                add(baseField)
                add(gravityField)
                add(planetField)
                add(worldState)
            }
            systems {
                add(SnapshotSystem())
                add(MobDamageSystem(mobGrid))
                add(GameOverSystem())
                add(MovementSystem())
                add(GravitySystem())
                add(LandingSystem())
                add(BuildSystem())
                add(WeaponSwitchSystem())
                add(MeleeSystem(mobGrid))
                add(FireSystem(mobGrid))
                add(ReloadSystem())
                add(ProjectileSystem(mobGrid))
                add(EBulletSystem())
                add(SmokeSystem())
                add(FlowRebuildSystem())
                add(AISystem(mobGrid))
                add(MobActionSystem())
                add(SpawnerSystem())
                add(BaseSystem())
                add(PickupSystem())
            }
        }

        val player = world.entity {
            it += Transform(x = loaded.playerSpawnX, y = loaded.playerSpawnY)
            it += PlayerTag()
            it += Facing()
            it += Stamina(config.player.staMax, config.player.staMax)
            it += Body(Tuning.PLAYER_HALF, Tuning.PLAYER_HALF)
            it += Materials()
            it += Mods()
            it += Buff()
            it += Arsenal(config.weapons.map { d -> WeaponRuntime(d, d.magSize ?: 0) })
            it += Ammo()
            it += Cooldowns()
            it += Health(config.player.hpMax, config.player.hpMax)
            it += Velocity()
        }
        carry?.applyTo(world, player) // carry HP/ammo/upgrades across a SPACE⇄SURFACE landing

        // Initial enemies placed at the stage's markers (the wave spawner adds more over time).
        for (marker in loaded.spawns) {
            if (marker.kind != "enemy") continue
            val def = config.enemies[marker.name] ?: continue
            MobFactory.spawn(world, def, marker.worldX, marker.worldY, tribe = tribes.tribeOf(marker.worldX, marker.worldY))
        }

        return GameWorld(world, player).also {
            it.map = map
            it.flow = flow
            it.waveState = waveState
            it.gameOver = gameOver
            it.fx = fx
            it.bases = baseField.bases
            it.gravityField = gravityField
            it.planets = planetField.planets
            it.worldState = worldState
        }
    }
}
