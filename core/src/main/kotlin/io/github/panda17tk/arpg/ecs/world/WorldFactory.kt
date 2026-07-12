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
import io.github.panda17tk.arpg.ecs.systems.BoomerangSystem
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
import io.github.panda17tk.arpg.ecs.systems.MeteorSystem
import io.github.panda17tk.arpg.ecs.systems.MovementSystem
import io.github.panda17tk.arpg.ecs.systems.PickupSystem
import io.github.panda17tk.arpg.ecs.systems.ProjectileSystem
import io.github.panda17tk.arpg.ecs.systems.ReloadSystem
import io.github.panda17tk.arpg.ecs.systems.RestRegenSystem
import io.github.panda17tk.arpg.ecs.systems.SmokeSystem
import io.github.panda17tk.arpg.ecs.systems.SnapshotSystem
import io.github.panda17tk.arpg.ecs.systems.TraderRaidSystem
import io.github.panda17tk.arpg.ecs.systems.DesyncSurgeSystem
import io.github.panda17tk.arpg.ecs.systems.WeaponSwitchSystem
import io.github.panda17tk.arpg.ecs.systems.SchoolFishSystem
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
        boons: io.github.panda17tk.arpg.config.WorkshopBoons = io.github.panda17tk.arpg.config.WorkshopBoons.NONE, // v2.90 工房
        trait: io.github.panda17tk.arpg.sim.SystemTrait = io.github.panda17tk.arpg.sim.SystemTrait.NONE, // v2.91 星系の個性
        difficulty: io.github.panda17tk.arpg.sim.Difficulty = io.github.panda17tk.arpg.sim.Difficulty.NORMAL, // v2.97
        ngClears: Int = 0, // v2.160 周回の印II: completed syncs deepen the surge (0 = untouched)
        oceanKeep: Int = 1, // v2.165 海の密度: every n-th school member spawns (1 = the full 30× ocean)
        area: Pair<Int, Int>? = null, // v2.166 宙域の九分割: which 3×3 slice to build (SPACE only; null = the whole legacy sky)
        lootedWrecks: Set<Int> = emptySet(), // v2.169 診断修正: sky-wide wreck indices already emptied this run
        survivorRescued: Boolean = false,    // v2.169: the sky's survivor was already rescued this run
        cometSwept: Boolean = false,         // v2.169: the comet's dust beads were already collected
    ): GameWorld {
        // v2.166 宙域の九分割: an AREA world is one 3×3 slice of the space stage — 1/9 the
        // tiles, 1/9 the flow field, 1/9 the ocean. The full stage never materialises in area mode.
        val spaceStageId = if (mode == WorldMode.SURFACE) null else Stages.randomSpaceId(Rng(seed))
        val loaded = MapLoader.load(
            when {
                mode == WorldMode.SURFACE -> SurfaceStages.forBiome(biome, seed)
                area != null -> Stages.slice(spaceStageId!!, area.first, area.second)
                else -> Stages.byId(spaceStageId)
            },
        )
        val map = loaded.tileMap
        // v2.166: global plans (planets, fish, far POIs) draw over the FULL stage extents and
        // filter into this slice — every area agrees on the one shared sky. The global anchor is
        // the cell the undivided world spawned at, so both modes place the same plan.
        val fullDims = if (area != null) Stages.spaceFullDims(spaceStageId!!) else (map.width to map.height)
        val fullW = fullDims.first * Tuning.TILE
        val fullH = fullDims.second * Tuning.TILE
        val origin = if (area != null) Stages.sliceOrigin(spaceStageId!!, area.first, area.second) else (0 to 0)
        val originX = origin.first * Tuning.TILE
        val originY = origin.second * Tuning.TILE
        val gAnchorX = (fullDims.first / 2 + 0.5f) * Tuning.TILE
        val gAnchorY = (fullDims.second / 2 + 0.5f) * Tuning.TILE
        fun inArea(x: Float, y: Float) = area == null ||
            (x >= originX && x < originX + map.width * Tuning.TILE && y >= originY && y < originY + map.height * Tuning.TILE)
        // The player normally starts at the stage's spawn; a return-to-space override re-emerges them beside a planet.
        var spawnX = playerSpawn?.first ?: loaded.playerSpawnX
        var spawnY = playerSpawn?.second ?: loaded.playerSpawnY
        if (area != null && playerSpawn != null) {
            // an area entry point comes from a neighbouring slice's edge — keep it inside, out of
            // rock, and (v2.169) clear of the EDGE_TRIGGER band: snapToFloor can slide up to 8
            // tiles, which could drop the arrival back into the band and re-fire the crossing
            // every frame (a per-frame world rebuild). A failed attempt pulls the search deeper
            // into the slice and tries again.
            val safe = io.github.panda17tk.arpg.sim.AreaGrid.EDGE_TRIGGER + Tuning.TILE
            val mwPx = map.width * Tuning.TILE; val mhPx = map.height * Tuning.TILE
            var tryInset = io.github.panda17tk.arpg.sim.AreaGrid.ENTRY_INSET
            for (attempt in 0 until 8) {
                val cx = spawnX.coerceIn(tryInset, mwPx - tryInset)
                val cy = spawnY.coerceIn(tryInset, mhPx - tryInset)
                val sp = snapToFloor(map, cx, cy)
                val ok = sp.first > safe && sp.first < mwPx - safe && sp.second > safe && sp.second < mhPx - safe
                if (ok || attempt == 7) { spawnX = sp.first; spawnY = sp.second }
                if (ok) break
                tryInset += Tuning.TILE * 4f
            }
        }
        // v2.95 地下遺構: stamped BEFORE the flow field reads the map, so pathing knows the plating.
        val vaultPos = if (mode == WorldMode.SURFACE && biome != null) {
            io.github.panda17tk.arpg.map.SurfaceVault.place(map, spawnX, spawnY, seed)
        } else null
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
        // Discrete planets: 10..14 per stage (v2.134 星の数 — half again more worlds to land on;
        // 6..8 since v2.39), deterministic from the seed, clear of the player spawn, with a wider
        // size range so systems read as varied. Every planet is landable.
        val planetCount = 10 + Rng(seed xor 0x50A4E70BL).nextInt(5)
        val planetField = PlanetField(
            if (mode == WorldMode.SURFACE) emptyList()
            else Planets.place(
                fullW, fullH,
                if (area != null) gAnchorX else loaded.playerSpawnX,
                if (area != null) gAnchorY else loaded.playerSpawnY,
                planetCount, Rng(seed xor 0x91A2B3C4L),
                minRadius = 56f, maxRadius = 200f, // dwarf moons up to near-giants (v2.39)
                margin = 520f, // slightly denser than the old 768 so a planet is never far to find
                seed = seed, // stable planet ids per star system → society memory persists across landings
            ).let { all -> // v2.166: an area keeps only its own planets, in local coordinates
                if (area == null) all
                else all.filter { inArea(it.cx, it.cy) }.map { it.copy(cx = it.cx - originX, cy = it.cy - originY) }
            },
        )
        val worldState = WorldState(mode = mode, biome = biome, context = context, society = society ?: PlanetSocietyState(), worldSeed = seed, ngClears = ngClears)
        worldState.areaX = area?.first ?: -1 // v2.166 宙域の九分割
        worldState.areaY = area?.second ?: -1
        worldState.areaOriginX = originX; worldState.areaOriginY = originY
        // v2.166: the global ring placer — area mode draws the near-anchor POIs in GLOBAL space
        // and each lands in whichever slice contains it (in practice the centre, where runs begin)
        fun gPlace(rng: Rng, base: Float, range: Float, marginTiles: Float = 4f): Pair<Float, Float> {
            val a = rng.nextFloat() * TAU
            val d = base + rng.nextFloat() * range
            val m = Tuning.TILE * marginTiles
            return (gAnchorX + kotlin.math.cos(a) * d).coerceIn(m, fullW - m) to
                (gAnchorY + kotlin.math.sin(a) * d).coerceIn(m, fullH - m)
        }
        fun localize(p: Pair<Float, Float>): Pair<Float, Float> = snapToFloor(map, p.first - originX, p.second - originY)
        // The escape pad sits at the surface landing point; standing on it lets the player take off again.
        if (mode == WorldMode.SURFACE) worldState.escapePad = loaded.playerSpawnX to loaded.playerSpawnY
        worldState.vault = vaultPos // v2.95
        // v2.48 惑星サーバー: every surface carries its memory core — the star's Layer-1 archive,
        // placed deterministically a walk away from the pad. Stand before it and it speaks once.
        if (mode == WorldMode.SURFACE) {
            worldState.memoryCore = placeNear(map, Rng(seed xor 0x3E3C0DEL), loaded.playerSpawnX, loaded.playerSpawnY, 700f, 900f)
        }
        // In space, scatter a flowing field of debris + asteroids around the player (cosmetic; fills the void).
        if (mode != WorldMode.SURFACE) worldState.drift = Drift.field(Rng(seed xor 0x0DEB712L), 120, spawnX, spawnY, 1400f)
        // v2.44: the system's jump gate — one per system, deterministic, out past the near planets.
        if (mode != WorldMode.SURFACE) {
            if (area == null) {
                worldState.gate = placeNear(map, Rng(seed xor 0x6A7E9A7EL), loaded.playerSpawnX, loaded.playerSpawnY, 1800f, 1200f)
                worldState.gateGlobal = worldState.gate // v2.169: local == global in the legacy sky
            } else { // v2.166: drawn globally — present only in the slice that contains it
                val gGate = gPlace(Rng(seed xor 0x6A7E9A7EL), 1800f, 1200f)
                worldState.gateGlobal = gGate // v2.169: every slice keeps the bearing home
                worldState.gate = gGate.takeIf { inArea(it.first, it.second) }?.let { localize(it) }
            }
        }
        // v2.46 難破船: 2..3 wrecked hulls drift in each system, nearer than the gate. Their loot
        // and guards are placed after the ECS world exists (below); the sites live here for drawing.
        if (mode != WorldMode.SURFACE) {
            val wRng = Rng(seed xor 0x37EC57A1L)
            if (area == null) {
                worldState.wrecks = List(2 + wRng.nextInt(2)) {
                    placeNear(map, wRng, loaded.playerSpawnX, loaded.playerSpawnY, 900f, 1500f)
                }
                worldState.wreckIndices = worldState.wrecks.indices.toList() // v2.169
            } else { // v2.166: the survivor is chosen against the GLOBAL list, then mapped local
                val globalWrecks = List(2 + wRng.nextInt(2)) { gPlace(wRng, 900f, 1500f) }
                val svRng = Rng(seed xor 0x5A110B0AL)
                val svIdx = if (svRng.nextFloat() < SURVIVOR_CHANCE) svRng.nextInt(globalWrecks.size) else -1
                val kept = globalWrecks.withIndex().filter { inArea(it.value.first, it.value.second) }
                worldState.wrecks = kept.map { localize(it.value) }
                worldState.wreckIndices = kept.map { it.index } // v2.169: the sky-wide identity
                worldState.survivorWreck = kept.indexOfFirst { it.index == svIdx }
            }
        }
        // v2.110 彗星: some skies carry a comet — a bright head trailing dust beads worth sweeping.
        if (mode != WorldMode.SURFACE) {
            val cRng = Rng(seed xor 0x0C0EE70AL)
            if (cRng.nextFloat() < COMET_CHANCE) { // (the head still places when swept — only the beads stay gone)
                val head = if (area == null) placeNear(map, cRng, loaded.playerSpawnX, loaded.playerSpawnY, 600f, 900f, marginTiles = 6f)
                else gPlace(cRng, 600f, 900f, marginTiles = 6f)
                val ta = cRng.nextFloat() * TAU
                if (inArea(head.first, head.second)) { // v2.166
                    worldState.comet = if (area == null) head else localize(head)
                    worldState.cometDir = kotlin.math.cos(ta) to kotlin.math.sin(ta)
                }
            }
        }
        // v2.100 行商船: some systems host a friendly trading vessel — nearer than the gate,
        // stocked deterministically per seed (item/Trader.kt). The screen opens the shop on approach.
        if (mode != WorldMode.SURFACE) {
            val tRng = Rng(seed xor 0x7124DE72L)
            if (tRng.nextFloat() < TRADER_CHANCE) {
                worldState.trader = if (area == null) placeNear(map, tRng, loaded.playerSpawnX, loaded.playerSpawnY, 700f, 1200f)
                else gPlace(tRng, 700f, 1200f).takeIf { inArea(it.first, it.second) }?.let { localize(it) } // v2.166
            }
        }
        // v2.110 生存者: one wreck sometimes shelters a survivor — approaching it is the rescue.
        if (area == null && mode != WorldMode.SURFACE && worldState.wrecks.isNotEmpty()) {
            val sRng = Rng(seed xor 0x5A110B0AL)
            if (sRng.nextFloat() < SURVIVOR_CHANCE) worldState.survivorWreck = sRng.nextInt(worldState.wrecks.size)
        }
        // v2.169 診断修正: a survivor rescued earlier this run doesn't reappear on a rebuild —
        // crossing out of the slice and back was a repeatable 星屑40 grant.
        if (survivorRescued) worldState.survivorRescued = true
        val waveState = WaveState(
            num = 1,
            phase = "active",
            toSpawn = minOf(config.waves.maxQuota, config.waves.baseQuota +
                io.github.panda17tk.arpg.sim.NewGamePlus.depth(ngClears) * config.waves.quotaPerWave), // v2.160 周回の印II
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
                add(boons) // v2.90 工房: systems read the permanent boons
                add(difficulty) // v2.97 難易度: pure multipliers the sim consults
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
                add(RestRegenSystem(mobGrid)) // v2.83: stand still, heal 1/s
                add(GravitySystem())
                add(LandingSystem())
                add(BuildSystem())
                add(WeaponSwitchSystem())
                add(MeleeSystem(mobGrid))
                add(FireSystem(mobGrid))
                add(ReloadSystem())
                add(ProjectileSystem(mobGrid))
                add(BoomerangSystem(mobGrid)) // v2.101 帰還刃: out, turn, and home
                add(MeteorSystem()) // v2.87 流星群: telegraphed falls during METEOR waves
                add(EBulletSystem())
                add(SmokeSystem())
                add(FlowRebuildSystem())
                add(AISystem(mobGrid))
                add(WildlifeSystem(mobGrid)) // mute wild animals: graze/herd/flee/hunt (AISystem skips WILDLIFE)
                add(WildPredationSystem(mobGrid)) // wild predators bite their prey (eats, drops hunger)
                add(SchoolFishSystem()) // v2.131 魚群: boids for SCHOOL fish (separation/alignment/cohesion+flee)
                add(MobActionSystem())
                add(DesyncSurgeSystem())
                add(BaseSystem())
                add(TraderRaidSystem()) // v2.110 行商船襲撃
                add(PickupSystem())
            }
        }

        val player = world.entity {
            it += Transform(x = spawnX, y = spawnY)
            it += PlayerTag()
            it += Facing()
            it += Stamina((config.player.staMax + boons.stamina) * boons.allMul, (config.player.staMax + boons.stamina) * boons.allMul) // v2.90; v2.104 周回の印
            it += Body(Tuning.PLAYER_HALF, Tuning.PLAYER_HALF)
            it += Materials()
            it += Mods(gunMul = boons.allMul, meleeMul = boons.allMul, moveMul = boons.allMul) // v2.104 周回の印
            it += Buff()
            it += Arsenal(config.weapons.map { d -> WeaponRuntime(d, d.magSize ?: 0) })
            it += Ammo()
            it += Cooldowns()
            it += Health((config.player.hpMax + boons.hull) * boons.allMul, (config.player.hpMax + boons.hull) * boons.allMul) // v2.90; v2.104 周回の印
            it += Velocity()
            it += Gear(ItemCatalog.starterLoadout(), ItemCatalog.starterBackpack()) // v2.33 装備+持物
        }
        carry?.applyTo(world, player) // carry HP/ammo/upgrades across a SPACE⇄SURFACE landing
        worldState.trait = if (mode == WorldMode.SPACE) trait else io.github.panda17tk.arpg.sim.SystemTrait.NONE // v2.91
        if (boons.loot > 0f) { // v2.90 拾集の目: rides on top of the visit's own tweaks
            worldState.spawnTweaks = worldState.spawnTweaks.copy(
                bonusMaterialChance = worldState.spawnTweaks.bonusMaterialChance + boons.loot,
            )
        }

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
            val normalKeys = config.enemies.filterValues { it.tier == "normal" && it.biome == null && it.lifeKind != io.github.panda17tk.arpg.config.LifeKind.WILDLIFE }.keys.toList() // v2.130/v2.141: fish are not drifters — sapient wanderers are
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

        // v2.130 宙を泳ぐもの: most skies host fish — a sardine school, sometimes a koi pair or a
        // lone angler. Their own rng stream; the waves never field them (the pools filter lifeKind)
        // and the surge counts hostiles only, so a far-off fish never stalls a wave.
        if (mode != WorldMode.SURFACE) {
            val fRng = Rng(seed xor 0x0F15E5L)
            val fww = fullW; val fwh = fullH // v2.166 宙域の九分割: ONE global fish plan for the sky
            val gSpawnX = originX + spawnX; val gSpawnY = originY + spawnY
            // global plan points snap to floor only where they actually spawn (this slice's map)
            fun oceanSnap(x: Float, y: Float): Pair<Float, Float> = if (area == null) snapToFloor(map, x, y) else x to y
            var schoolSeq = 0 // v2.144 大群衆: every shoal call is its own flock — boids school per flock
            fun shoalAt(cx0: Float, cy0: Float, id: String, count: Int, spread: Float) {
                config.enemies[id]?.let { def ->
                    schoolSeq++
                    repeat(count) { i ->
                        val a = fRng.nextFloat() * TAU
                        val r = fRng.nextFloat() * spread
                        // v2.165 海の密度: the rng is ALWAYS drawn — every tier sees the same
                        // stream, so a thinner ocean is a strict subset of the full one. The
                        // stride only decides which members become entities; count-1 spawns
                        // (whales, tyrants, hunters, giants) ride index 0 and survive every tier.
                        if (i % oceanKeep != 0) return@repeat
                        val gx = cx0 + kotlin.math.cos(a) * r
                        val gy = cy0 + kotlin.math.sin(a) * r
                        if (!inArea(gx, gy)) return@repeat // v2.166: another slice's fish — drawn, never spawned
                        val (lx, ly) = if (area == null) gx to gy else snapToFloor(map, gx - originX, gy - originY)
                        MobFactory.spawn(world, def, lx, ly, schoolGroup = schoolSeq)
                    }
                }
            }
            fun site(): Pair<Float, Float> = oceanSnap(
                Tuning.TILE * 6f + fRng.nextFloat() * (fww - Tuning.TILE * 12f),
                Tuning.TILE * 6f + fRng.nextFloat() * (fwh - Tuning.TILE * 12f),
            )
            // v2.131: ~a hundred tiny fish move as one — the boid school still rises near the pad,
            // so the first minutes of every sortie have life in view.
            val fa = fRng.nextFloat() * TAU
            val fd = 500f + fRng.nextFloat() * 800f
            val rawX = (gSpawnX + kotlin.math.cos(fa) * fd).coerceIn(Tuning.TILE * 4f, fww - Tuning.TILE * 4f)
            val rawY = (gSpawnY + kotlin.math.sin(fa) * fd).coerceIn(Tuning.TILE * 4f, fwh - Tuning.TILE * 4f)
            val (sx, sy) = oceanSnap(rawX, rawY)
            shoalAt(sx, sy, io.github.panda17tk.arpg.config.SpaceFishRoster.TINY[if (fRng.nextFloat() < 0.5f) 0 else 1], 90 + fRng.nextInt(21), 130f)
            val hunters = io.github.panda17tk.arpg.config.SpaceFishRoster.HUNTERS // v2.141: the roster object is the spawn truth
            if (fRng.nextFloat() < 0.55f) shoalAt(sx, sy, hunters[fRng.nextInt(hunters.size)], 1, 320f)
            // v2.135 満ちる海 → v2.144 大群衆: the ocean fills the WHOLE sky, thirtyfold. Every
            // sector hosts several boid flocks of its own plus medium schools, loners and teeth —
            // ~5000 fish per sky. The boid pass stays cheap because each fish scans only its own
            // flock (SchoolFishSystem.poolKey), and the pools carry unboxed float buffers.
            val mediums = io.github.panda17tk.arpg.config.SpaceFishRoster.MEDIUMS
            val singles = io.github.panda17tk.arpg.config.SpaceFishRoster.SINGLES
            val tiny = io.github.panda17tk.arpg.config.SpaceFishRoster.TINY
            // v2.157 読む海: one flock per sector schools around a LANDMARK instead — following
            // the fish now leads somewhere (a wreck, the comet, the trader).
            val fishPoi = buildList {
                addAll(worldState.wrecks)
                worldState.comet?.let { add(it) }
                worldState.trader?.let { add(it) }
            }.map { (it.first + originX) to (it.second + originY) } // v2.166: back to global for the plan
            // own rng stream: the landmark draw must not shift fRng — every fish position of
            // every existing seed would move, and the seed-tuned tests with them.
            val poiRng = Rng(seed xor 0x0F15D0C5L)
            fun poiPoint(): Pair<Float, Float>? {
                if (fishPoi.isEmpty()) return null
                val (bx, by) = fishPoi[poiRng.nextInt(fishPoi.size)]
                return oceanSnap(
                    (bx + (poiRng.nextFloat() - 0.5f) * 500f).coerceIn(Tuning.TILE * 4f, fww - Tuning.TILE * 4f),
                    (by + (poiRng.nextFloat() - 0.5f) * 500f).coerceIn(Tuning.TILE * 4f, fwh - Tuning.TILE * 4f),
                )
            }
            for (gy in 0 until 3) for (gx in 0 until 3) {
                fun sectorPoint(): Pair<Float, Float> = snapToFloor(
                    map,
                    fww / 3f * (gx + 0.15f + fRng.nextFloat() * 0.7f),
                    fwh / 3f * (gy + 0.15f + fRng.nextFloat() * 0.7f),
                )
                repeat(5) { k -> // the tiny schools ARE the ocean now — five flocks per sector, ~100 strong each
                    val sp = sectorPoint() // always drawn — fRng's sequence stays byte-identical to v2.156
                    val (px2, py2) = (if (k == 0) poiPoint() else null) ?: sp // v2.157: the first schools by a landmark
                    shoalAt(px2, py2, tiny[fRng.nextInt(tiny.size)], 80 + fRng.nextInt(41), 150f)
                }
                repeat(3) {
                    val (px2, py2) = sectorPoint()
                    shoalAt(px2, py2, mediums[fRng.nextInt(mediums.size)], 10 + fRng.nextInt(11), 100f)
                }
                repeat(2) {
                    val (px2, py2) = sectorPoint()
                    shoalAt(px2, py2, singles[fRng.nextInt(singles.size)], 1 + fRng.nextInt(2), 220f)
                }
                repeat(2) { // the teeth follow the plenty
                    val (px2, py2) = sectorPoint()
                    shoalAt(px2, py2, hunters[fRng.nextInt(hunters.size)], 1, 320f)
                }
                if (fRng.nextFloat() < 0.35f) {
                    val (px2, py2) = sectorPoint()
                    shoalAt(px2, py2, hunters[fRng.nextInt(hunters.size)], 1, 320f)
                }
            }
            // the vast ones are no longer rare — a crowded ocean feeds more giants
            val giants = io.github.panda17tk.arpg.config.SpaceFishRoster.GIANTS
            repeat(3) { val (vx2, vy2) = site(); shoalAt(vx2, vy2, giants[fRng.nextInt(giants.size)], 1, 200f) }
            if (fRng.nextFloat() < 0.25f) { val (vx2, vy2) = site(); shoalAt(vx2, vy2, giants[fRng.nextInt(giants.size)], 1, 200f) }
            // v2.135 暴君鮫: every sky has its tyrant now — some have two
            run { val (tx2, ty2) = site(); shoalAt(tx2, ty2, io.github.panda17tk.arpg.config.SpaceFishRoster.TYRANT, 1, 0f) }
            if (fRng.nextFloat() < 0.45f) { val (tx2, ty2) = site(); shoalAt(tx2, ty2, io.github.panda17tk.arpg.config.SpaceFishRoster.TYRANT, 1, 0f) }
            // v2.135 島鯨: two drifting islands, each trailing its retinue of pilot fish
            repeat(2) {
                val (wx2, wy2) = site()
                shoalAt(wx2, wy2, io.github.panda17tk.arpg.config.SpaceFishRoster.WHALE, 1, 0f)
                shoalAt(wx2, wy2, io.github.panda17tk.arpg.config.SpaceFishRoster.PILOT, 24 + fRng.nextInt(13), 110f)
            }
        }

        // v2.83 ならず者: two player-like drifters coast the system, at war with EVERY tribe —
        // they fight the machines, the tribes, and you, and never stall the wave train.
        if (mode != WorldMode.SURFACE) {
            val rogueRng = Rng(seed xor 0x0906_0DEAL)
            config.enemies["rogue_drifter"]?.let { def ->
                repeat(2) {
                    val ang = rogueRng.nextFloat() * TAU
                    val dist = DRIFTER_MIN_DIST + rogueRng.nextFloat() * DRIFTER_RANGE
                    val rawX = (spawnX + kotlin.math.cos(ang) * dist).coerceIn(Tuning.TILE * 2f, map.width * Tuning.TILE - Tuning.TILE * 2f)
                    val rawY = (spawnY + kotlin.math.sin(ang) * dist).coerceIn(Tuning.TILE * 2f, map.height * Tuning.TILE - Tuning.TILE * 2f)
                    val (rx2, ry2) = snapToFloor(map, rawX, rawY)
                    val e = MobFactory.spawn(world, def, rx2, ry2, tribe = Tribes.ROGUE, dashes = true, drifter = true)
                    val va = rogueRng.nextFloat() * TAU
                    with(world) {
                        e[Velocity].driftX = kotlin.math.cos(va) * DRIFTER_SPEED_MAX
                        e[Velocity].driftY = kotlin.math.sin(va) * DRIFTER_SPEED_MAX
                    }
                }
            }
        }

        // v2.110 彗星の尾: dust beads strung along the tail — a quiet sweep for careful pilots.
        if (mode != WorldMode.SURFACE) {
            val head = worldState.comet
            val dir = worldState.cometDir
            if (head != null && dir != null && !cometSwept) { // v2.169: swept beads stay swept
                val bRng = Rng(seed xor 0x0C0EE70BL)
                repeat(COMET_BEADS) { i ->
                    val dist = 60f + i * 46f
                    val bx = (head.first + dir.first * dist).coerceIn(Tuning.TILE * 2f, map.width * Tuning.TILE - Tuning.TILE * 2f)
                    val by = (head.second + dir.second * dist).coerceIn(Tuning.TILE * 2f, map.height * Tuning.TILE - Tuning.TILE * 2f)
                    val (fbx, fby) = snapToFloor(map, bx, by)
                    Pickups.spawn(world, "dust", 6 + bRng.nextInt(5), fbx, fby)
                }
            }
        }
        // v2.46 難破船: each wreck carries a weapon cache + dust + a med pack, watched by a small
        // drifter picket (drifters — they aggro on approach but never stall the wave train).
        if (mode != WorldMode.SURFACE && worldState.wrecks.isNotEmpty()) {
            val lootRng = Rng(seed xor 0x100CCAFEL)
            val guardKeys = config.enemies.filterValues { it.tier == "normal" && it.biome == null && it.lifeKind != io.github.panda17tk.arpg.config.LifeKind.WILDLIFE }.keys.toList() // v2.130/v2.141
            for ((wi, wreckPos) in worldState.wrecks.withIndex()) {
                val (wx, wy) = wreckPos
                // v2.169 診断修正: a cache emptied this run stays empty. The rolls still draw so
                // the OTHER wrecks' loot and the guards land exactly where they always did.
                val cacheGun = ItemCatalog.gunFor(lootRng.nextInt(1000)).id
                val cacheDust = 25 + lootRng.nextInt(36)
                if (worldState.wreckIndices.getOrElse(wi) { wi } !in lootedWrecks) {
                    Pickups.spawn(world, "item:" + cacheGun, 1, wx, wy)
                    Pickups.spawn(world, "dust", cacheDust, wx + 16f, wy + 6f)
                    Pickups.spawn(world, "med", 25, wx - 16f, wy + 6f)
                }
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
            // v2.79 水域 / v2.133 適所の生態: the landing's lakes and rivers are generated FIRST so
            // placement can read the banks — otters move to the shore, land animals step out of the wade.
            worldState.water = SurfaceWater.generate(biome, seed, worldW, worldH)
            for (p in ecology.placements) {
                val def = config.enemies[p.key] ?: continue
                // v2.133 適所の生態: a waterside creature is drawn to the nearest bank (if the world has water).
                val (wx2, wy2) = if (p.key in SurfaceEcology.WATERSIDE) {
                    SurfaceWater.nearestShore(worldState.water, p.x, p.y) ?: (p.x to p.y)
                } else p.x to p.y
                var (fx, fy) = snapToFloor(map, wx2, wy2)
                // ...and a land creature never begins its day standing in open water.
                if (p.key !in SurfaceEcology.WATERSIDE && SurfaceWater.wadingAt(worldState.water, fx, fy)) {
                    val (dx2, dy2) = snapToDry(map, worldState.water, fx, fy)
                    fx = dx2; fy = dy2
                }
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
            // v2.78 装飾: scatter the biome's furniture, then drop anything buried in a wall
            // or standing in open water (a tree in a lake reads wrong).
            worldState.decor = SurfaceDecor.scatter(biome, seed, worldW, worldH).filter { d ->
                val tx = (d.x / Tuning.TILE).toInt(); val ty = (d.y / Tuning.TILE).toInt()
                !map.solidAt(tx, ty) && !SurfaceWater.inWater(worldState.water, d.x, d.y)
            }
            worldState.spawnTweaks = tweaks
            // v2.95 地下遺構: the sealed chamber holds a keeper and its hoard.
            vaultPos?.let { (vxp, vyp) ->
                val form = io.github.panda17tk.arpg.map.SurfaceVault.formFor(seed) // v2.109 深掘り
                val gKey = config.enemies.entries.firstOrNull { it.value.tier == "midboss" && it.value.biome == biome }?.key
                val gDef = config.enemies[gKey] ?: config.enemies["brute"]
                if (gDef != null) {
                    // 二重輪 (form 1): the heart is walled — the keeper waits at the exact centre.
                    val gy = if (form == 1) vyp else vyp - Tuning.TILE
                    val g = MobFactory.spawn(world, gDef, vxp, gy, tribe = tribes.tribeOf(vxp, vyp))
                    with(world) { g[Mob].level = 6 } // the vault's keeper outranks the field
                    if (form == 2) { // 大房: two mouths, two watchers
                        val g2 = MobFactory.spawn(world, gDef, vxp, vyp + Tuning.TILE, tribe = tribes.tribeOf(vxp, vyp))
                        with(world) { g2[Mob].level = 5 }
                    }
                }
                Pickups.spawn(world, "dust", if (form == 2) 140 else 80, vxp, vyp + 8f) // 大房 pays for its second watcher
                Pickups.spawn(world, "med", 40, vxp - 14f, vyp)
                Pickups.spawn(world, "mat_" + biome.name.lowercase(), 1, vxp + 14f, vyp)
                if (form == 2) { // ...and a weapon cache, like the wrecks carry
                    Pickups.spawn(world, "item:" + ItemCatalog.gunFor(Rng(seed xor 0x7A03CAFEL).nextInt(1000)).id, 1, vxp, vyp - 8f)
                }
            }
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
            it.mobGrid = mobGrid // v2.175 描画の倹約IV: the draw side reads the same broad-phase
        }
    }

    /**
     * v2.143: one polar-offset placement — an angle then a distance drawn from [rng] (the exact
     * draw order every call site used, so layouts stay byte-stable), clamped into the arena and
     * snapped to free floor. Collapses five hand-inlined copies (memory core / gate / wrecks /
     * comet head / trader).
     */
    private fun placeNear(map: TileMap, rng: Rng, cx: Float, cy: Float, base: Float, range: Float, marginTiles: Float = 4f): Pair<Float, Float> {
        val a = rng.nextFloat() * TAU
        val d = base + rng.nextFloat() * range
        val m = Tuning.TILE * marginTiles
        val x = (cx + kotlin.math.cos(a) * d).coerceIn(m, map.width * Tuning.TILE - m)
        val y = (cy + kotlin.math.sin(a) * d).coerceIn(m, map.height * Tuning.TILE - m)
        return snapToFloor(map, x, y)
    }

    /** v2.133 適所の生態: like [snapToFloor], but the tile must also be out of open water. */
    private fun snapToDry(map: TileMap, water: io.github.panda17tk.arpg.map.WaterBodies, x: Float, y: Float): Pair<Float, Float> {
        val tx = floor(x / Tuning.TILE).toInt(); val ty = floor(y / Tuning.TILE).toInt()
        for (r in 0..14) {
            for (dx in -r..r) for (dy in -r..r) {
                if (kotlin.math.abs(dx) != r && kotlin.math.abs(dy) != r) continue
                val nx = tx + dx; val ny = ty + dy
                val cxp = (nx + 0.5f) * Tuning.TILE; val cyp = (ny + 0.5f) * Tuning.TILE
                if (nx in 1 until map.width - 1 && ny in 1 until map.height - 1 &&
                    !map.solidAt(nx, ny) && !SurfaceWater.wadingAt(water, cxp, cyp)
                ) return cxp to cyp
            }
        }
        return x to y // a drowned world keeps the wet spot rather than losing the creature
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
    private const val TRADER_CHANCE = 0.4f     // v2.100: not every sky hosts the vessel
    private const val SURVIVOR_CHANCE = 0.5f   // v2.110: half the wreck fields shelter a survivor
    private const val COMET_CHANCE = 0.5f      // v2.110: half the skies carry the comet
    private const val COMET_BEADS = 8          // dust pickups strung along the tail
}
