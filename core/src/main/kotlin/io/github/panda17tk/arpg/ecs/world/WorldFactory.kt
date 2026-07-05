package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.configureWorld
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Buff
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.CreatureMind
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.Gear
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
import io.github.panda17tk.arpg.ecs.systems.EcologyEventSystem
import io.github.panda17tk.arpg.ecs.systems.EventFeedSystem
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
import io.github.panda17tk.arpg.ecs.systems.DesyncSurgeSystem
import io.github.panda17tk.arpg.ecs.systems.WeaponSwitchSystem
import io.github.panda17tk.arpg.ecs.systems.WildPredationSystem
import io.github.panda17tk.arpg.ecs.systems.WildlifeSystem
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.ItemCatalog
import io.github.panda17tk.arpg.map.MapLoader
import io.github.panda17tk.arpg.map.Stages
import io.github.panda17tk.arpg.map.SurfaceDecor
import io.github.panda17tk.arpg.map.SurfaceStages
import io.github.panda17tk.arpg.map.SurfaceWater
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.Base
import io.github.panda17tk.arpg.sim.BaseField
import io.github.panda17tk.arpg.sim.Bases
import io.github.panda17tk.arpg.sim.CreatureState
import io.github.panda17tk.arpg.sim.GravityField
import io.github.panda17tk.arpg.sim.PlanetField
import io.github.panda17tk.arpg.sim.Drift
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.sim.PlanetContext
import io.github.panda17tk.arpg.sim.PlanetSocietyState
import io.github.panda17tk.arpg.sim.ReturnVisitEffects
import io.github.panda17tk.arpg.sim.Planets
import io.github.panda17tk.arpg.sim.SurfaceEcology
import io.github.panda17tk.arpg.sim.Tribes
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.VisitedMap
import io.github.panda17tk.arpg.sim.toPressure
import io.github.panda17tk.arpg.sim.WallGravity
import io.github.panda17tk.arpg.sim.WeatherKind
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.sim.WorldState
import kotlin.math.floor

object WorldFactory {
    /**
     * [seed] keeps stage selection deterministic for tests. [society] seeds the surface's remembered
     * state BEFORE the world is built (R2) — so spawn-time consumers (SurfaceEcology, v2.27's return-visit
     * tweaks) can read the planet's memory; null builds the usual fresh state.
     */
    fun create(
        input: InputState, config: GameConfig = GameConfig(), seed: Long = 1L,
        mode: WorldMode = WorldMode.SPACE, biome: PlanetBiome? = null, carry: PlayerCarry? = null,
        playerSpawn: Pair<Float, Float>? = null, context: PlanetContext? = null,
        society: PlanetSocietyState? = null,
        weather: WeatherKind = WeatherKind.CLEAR, // v2.75: the landing's sky (SURFACE only)
    ): GameWorld {
        val loaded = MapLoader.load(
            if (mode == WorldMode.SURFACE) SurfaceStages.forBiome(biome, seed) else Stages.random(Rng(seed)),
        )
        val map = loaded.tileMap
        // The player normally starts at the stage's spawn; a return-to-space override re-emerges them beside a planet.
        val spawnX = playerSpawn?.first ?: loaded.playerSpawnX
        val spawnY = playerSpawn?.second ?: loaded.playerSpawnY
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
        // Discrete planets: 6..8 per stage (v2.39 — more worlds to land on), deterministic from the
        // seed, clear of the player spawn, with a wider size range so systems read as varied.
        val planetCount = 6 + Rng(seed xor 0x50A4E70BL).nextInt(3)
        val planetField = PlanetField(
            if (mode == WorldMode.SURFACE) emptyList()
            else Planets.place(
                map.width * Tuning.TILE, map.height * Tuning.TILE,
                loaded.playerSpawnX, loaded.playerSpawnY, planetCount, Rng(seed xor 0x91A2B3C4L),
                minRadius = 56f, maxRadius = 200f, // dwarf moons up to near-giants (v2.39)
                margin = 520f, // slightly denser than the old 768 so a planet is never far to find
                seed = seed, // stable planet ids per star system → society memory persists across landings
            ),
        )
        val worldState = WorldState(mode = mode, biome = biome, context = context, society = society ?: PlanetSocietyState())
        // The escape pad sits at the surface landing point; standing on it lets the player take off again.
        if (mode == WorldMode.SURFACE) worldState.escapePad = loaded.playerSpawnX to loaded.playerSpawnY
        // v2.48 惑星サーバー: every surface carries its memory core — the star's Layer-1 archive,
        // placed deterministically a walk away from the pad. Stand before it and it speaks once.
        if (mode == WorldMode.SURFACE) {
            val mRng = Rng(seed xor 0x3E3C0DEL)
            val ma = mRng.nextFloat() * TAU
            val md = 700f + mRng.nextFloat() * 900f
            val mx = (loaded.playerSpawnX + kotlin.math.cos(ma) * md).coerceIn(Tuning.TILE * 4f, map.width * Tuning.TILE - Tuning.TILE * 4f)
            val my = (loaded.playerSpawnY + kotlin.math.sin(ma) * md).coerceIn(Tuning.TILE * 4f, map.height * Tuning.TILE - Tuning.TILE * 4f)
            worldState.memoryCore = snapToFloor(map, mx, my)
        }
        // In space, scatter a flowing field of debris + asteroids around the player (cosmetic; fills the void).
        if (mode != WorldMode.SURFACE) worldState.drift = Drift.field(Rng(seed xor 0x0DEB712L), 120, loaded.playerSpawnX, loaded.playerSpawnY, 1400f)
        // v2.44: the system's jump gate — one per system, deterministic, out past the near planets.
        if (mode != WorldMode.SURFACE) {
            val gRng = Rng(seed xor 0x6A7E9A7EL)
            val ga = gRng.nextFloat() * TAU
            val gd = 1800f + gRng.nextFloat() * 1200f
            val gx = (loaded.playerSpawnX + kotlin.math.cos(ga) * gd).coerceIn(Tuning.TILE * 4f, map.width * Tuning.TILE - Tuning.TILE * 4f)
            val gy = (loaded.playerSpawnY + kotlin.math.sin(ga) * gd).coerceIn(Tuning.TILE * 4f, map.height * Tuning.TILE - Tuning.TILE * 4f)
            worldState.gate = snapToFloor(map, gx, gy)
        }
        // v2.46 難破船: 2..3 wrecked hulls drift in each system, nearer than the gate. Their loot
        // and guards are placed after the ECS world exists (below); the sites live here for drawing.
        if (mode != WorldMode.SURFACE) {
            val wRng = Rng(seed xor 0x37EC57A1L)
            worldState.wrecks = List(2 + wRng.nextInt(2)) {
                val wa = wRng.nextFloat() * TAU
                val wd = 900f + wRng.nextFloat() * 1500f
                val wx = (loaded.playerSpawnX + kotlin.math.cos(wa) * wd).coerceIn(Tuning.TILE * 4f, map.width * Tuning.TILE - Tuning.TILE * 4f)
                val wy = (loaded.playerSpawnY + kotlin.math.sin(wa) * wd).coerceIn(Tuning.TILE * 4f, map.height * Tuning.TILE - Tuning.TILE * 4f)
                snapToFloor(map, wx, wy)
            }
        }
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
                add(EcologyEventSystem()) // record ecology events into society memory before the dead are reaped
                add(MobDamageSystem(mobGrid))
                // Feed lines come from society-state EDGES, so this sits right after the tick's main
                // emitters (EcologyEventSystem). PickupSystem's relic claim lands a tick later — acceptable.
                add(EventFeedSystem())
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
                add(WildlifeSystem()) // mute wild animals: graze/herd/flee/hunt (AISystem skips WILDLIFE)
                add(WildPredationSystem(mobGrid)) // wild predators bite their prey (eats, drops hunger)
                add(MobActionSystem())
                add(DesyncSurgeSystem())
                add(BaseSystem())
                add(PickupSystem())
            }
        }

        val player = world.entity {
            it += Transform(x = spawnX, y = spawnY)
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
            it += Gear(ItemCatalog.starterLoadout(), ItemCatalog.starterBackpack()) // v2.33 装備+持物
        }
        carry?.applyTo(world, player) // carry HP/ammo/upgrades across a SPACE⇄SURFACE landing

        // ~Half of each tribe's rank-and-file (normal tier) dash; bosses/elites keep their own kit.
        val dashRng = Rng(seed xor 0x0DA54DA5L)
        // Initial enemies placed at the stage's markers (the wave spawner adds more over time).
        for (marker in loaded.spawns) {
            if (marker.kind != "enemy") continue
            val def = config.enemies[marker.name] ?: continue
            MobFactory.spawn(
                world, def, marker.worldX, marker.worldY, tribe = tribes.tribeOf(marker.worldX, marker.worldY),
                dashes = def.tier == "normal" && dashRng.nextFloat() < 0.5f,
            )
        }

        // v2.36: space is inhabited — drifters coast through the void on their own momentum from the
        // start. They aggro like any mob when approached, but DesyncSurgeSystem excludes them from wave
        // completion and live caps, so a drifter at the map's far edge never stalls the wave train.
        if (mode != WorldMode.SURFACE) {
            val driftersRng = Rng(seed xor 0x00D21F7E5L)
            val normalKeys = config.enemies.filterValues { it.tier == "normal" && it.biome == null }.keys.toList()
            if (normalKeys.isNotEmpty()) {
                repeat(SPACE_DRIFTERS) {
                    val def = config.enemies[normalKeys[driftersRng.nextInt(normalKeys.size)]] ?: return@repeat
                    val ang = driftersRng.nextFloat() * TAU
                    val dist = DRIFTER_MIN_DIST + driftersRng.nextFloat() * DRIFTER_RANGE
                    val rawX = (spawnX + kotlin.math.cos(ang) * dist).coerceIn(Tuning.TILE * 2f, map.width * Tuning.TILE - Tuning.TILE * 2f)
                    val rawY = (spawnY + kotlin.math.sin(ang) * dist).coerceIn(Tuning.TILE * 2f, map.height * Tuning.TILE - Tuning.TILE * 2f)
                    val (fx2, fy2) = snapToFloor(map, rawX, rawY)
                    val e = MobFactory.spawn(
                        world, def, fx2, fy2, tribe = tribes.tribeOf(fx2, fy2),
                        dashes = driftersRng.nextFloat() < 0.5f, drifter = true,
                    )
                    // Their opening momentum: a lazy coast in a random direction (below the ram threshold).
                    val va = driftersRng.nextFloat() * TAU
                    val sp = DRIFTER_SPEED_MIN + driftersRng.nextFloat() * (DRIFTER_SPEED_MAX - DRIFTER_SPEED_MIN)
                    with(world) {
                        e[Velocity].driftX = kotlin.math.cos(va) * sp
                        e[Velocity].driftY = kotlin.math.sin(va) * sp
                    }
                }
            }
        }

        // v2.46 難破船: each wreck carries a weapon cache + dust + a med pack, watched by a small
        // drifter picket (drifters — they aggro on approach but never stall the wave train).
        if (mode != WorldMode.SURFACE && worldState.wrecks.isNotEmpty()) {
            val lootRng = Rng(seed xor 0x100CCAFEL)
            val guardKeys = config.enemies.filterValues { it.tier == "normal" && it.biome == null }.keys.toList()
            for ((wx, wy) in worldState.wrecks) {
                Pickups.spawn(world, "item:" + ItemCatalog.gunFor(lootRng.nextInt(1000)).id, 1, wx, wy)
                Pickups.spawn(world, "dust", 25 + lootRng.nextInt(36), wx + 16f, wy + 6f)
                Pickups.spawn(world, "med", 25, wx - 16f, wy + 6f)
                if (guardKeys.isNotEmpty()) repeat(3) {
                    val def = config.enemies[guardKeys[lootRng.nextInt(guardKeys.size)]] ?: return@repeat
                    val ga2 = lootRng.nextFloat() * TAU
                    val gr = 60f + lootRng.nextFloat() * 60f
                    val (gx2, gy2) = snapToFloor(map, wx + kotlin.math.cos(ga2) * gr, wy + kotlin.math.sin(ga2) * gr)
                    val guard = MobFactory.spawn(
                        world, def, gx2, gy2, tribe = tribes.tribeOf(gx2, gy2),
                        dashes = lootRng.nextFloat() < 0.5f, drifter = true,
                    )
                    // A lazy patrol coast, same envelope as the void drifters (below the ram threshold).
                    val pa = lootRng.nextFloat() * TAU
                    val ps = 40f + lootRng.nextFloat() * 40f
                    with(world) {
                        guard[Velocity].driftX = kotlin.math.cos(pa) * ps
                        guard[Velocity].driftY = kotlin.math.sin(pa) * ps
                    }
                }
            }
        }

        // Living Planets: landing on a planet lays out its inhabitants once (its society/ecology), not a wave.
        if (mode == WorldMode.SURFACE && biome != null) {
            val worldW = map.width * Tuning.TILE; val worldH = map.height * Tuning.TILE
            val ecoRng = Rng(seed xor 0x5EED1234L)
            // A remembered planet greets the return visit in its layout (LP v2.27): watch-guards by the pad,
            // thinned grazers, hungrier predators. A first visit (blank society) is exactly NEUTRAL.
            val ctx = context ?: PlanetContext.NEUTRAL
            val tweaks = ReturnVisitEffects.spawnTweaks(worldState.society.toPressure(ctx))
            worldState.weather = weather // v2.75: one truth for ecology, rendering and (later) sound
            val ecology = SurfaceEcology.populate(biome, loaded.playerSpawnX, loaded.playerSpawnY, worldW, worldH, ecoRng, ctx, tweaks, weather)
            for (p in ecology.placements) {
                val def = config.enemies[p.key] ?: continue
                val (fx, fy) = snapToFloor(map, p.x, p.y)
                val e = MobFactory.spawn(
                    world, def, fx, fy, tribe = tribes.tribeOf(fx, fy),
                    dashes = def.tier == "normal" && dashRng.nextFloat() < 0.5f,
                )
                if (p.passive) with(world) { e[CreatureMind].state = CreatureState.Ignore } // pacifist until attacked
                // A disrupted world's hunters start the visit already hungry (placement-time state only).
                if (tweaks.predatorStartHunger > 0f) with(world) {
                    val mm = e[Mob]
                    if (mm.def.wildRole == WildRole.PREDATOR || mm.def.wildRole == WildRole.APEX) {
                        mm.hunger = maxOf(mm.hunger, tweaks.predatorStartHunger)
                    }
                }
            }
            worldState.facilities = ecology.facilities
            // v2.79 水域: the landing's lakes and rivers (world-space bodies, no collision).
            worldState.water = SurfaceWater.generate(biome, seed, worldW, worldH)
            // v2.78 装飾: scatter the biome's furniture, then drop anything buried in a wall
            // or standing in open water (a tree in a lake reads wrong).
            worldState.decor = SurfaceDecor.scatter(biome, seed, worldW, worldH).filter { d ->
                val tx = (d.x / Tuning.TILE).toInt(); val ty = (d.y / Tuning.TILE).toInt()
                !map.solidAt(tx, ty) && !SurfaceWater.inWater(worldState.water, d.x, d.y)
            }
            worldState.spawnTweaks = tweaks
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
            it.visited = VisitedMap(map.width, map.height)
        }
    }

    /** Nudge a desired spot to the centre of the nearest free floor tile (spiral out) so nothing spawns inside a wall. */
    private fun snapToFloor(map: TileMap, x: Float, y: Float): Pair<Float, Float> {
        val tx = floor(x / Tuning.TILE).toInt(); val ty = floor(y / Tuning.TILE).toInt()
        for (r in 0..8) {
            for (dx in -r..r) for (dy in -r..r) {
                if (kotlin.math.abs(dx) != r && kotlin.math.abs(dy) != r) continue // perimeter of the ring only
                val nx = tx + dx; val ny = ty + dy
                if (nx in 1 until map.width - 1 && ny in 1 until map.height - 1 && !map.solidAt(nx, ny)) {
                    return ((nx + 0.5f) * Tuning.TILE) to ((ny + 0.5f) * Tuning.TILE)
                }
            }
        }
        return x to y
    }

    private const val TAU = 6.2831855f
    private const val SPACE_DRIFTERS = 30      // v2.36: creatures adrift in space from world creation
    private const val DRIFTER_MIN_DIST = 600f  // ...scattered in a ring well clear of the spawn
    private const val DRIFTER_RANGE = 1800f
    private const val DRIFTER_SPEED_MIN = 40f  // opening coast speed; max stays below the 150 ram threshold
    private const val DRIFTER_SPEED_MAX = 140f
}
