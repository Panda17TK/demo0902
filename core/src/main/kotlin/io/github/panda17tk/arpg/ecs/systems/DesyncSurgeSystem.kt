package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.WaveConfig
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.WaveState
import io.github.panda17tk.arpg.ecs.world.MobFactory
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.Tribes
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WaveEvent
import io.github.panda17tk.arpg.sim.WaveEvents
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.sim.WorldState
import kotlin.math.cos
import kotlin.math.sin

/**
 * v2.52 同期乱流 (formerly SpawnerSystem — the wave spawner, reread through the 惑星サーバー
 * fiction): the broken preservation network periodically vents unprocessed persona fragments
 * and defence processes into open space. A "wave" is one surge; the intermission is the network
 * catching its breath; deeper surges leak deeper layers of the roster (see [surgePool]).
 * Surfaces never surge — planets place their society once and let it live (SurfaceEcology).
 */
class DesyncSurgeSystem : IteratingSystem(family { all(PlayerTag, Transform) }) {
    private val wave: WaveState = world.inject()
    private val config: GameConfig = world.inject()
    private val map: TileMap = world.inject()
    private val rng: Rng = world.inject()
    private val fx: Fx = world.inject()
    private val tribes: Tribes = world.inject()
    private val worldState: WorldState = world.inject()

    private val mobs by lazy { world.family { all(Mob) } }
    // Space waves draw only from generic enemies; biome creatures (biome != null) live on their planet's surface.
    private val normalKeys: List<String> = config.enemies.filterValues { it.tier == "normal" && it.biome == null }.keys.toList()
    private val midBossKeys: List<String> = config.enemies.filterValues { it.tier == "midboss" && it.biome == null }.keys.toList()
    private val bossKeys: List<String> = config.enemies.filterValues { it.tier == "boss" && it.biome == null }.keys.toList()

    override fun onTickEntity(entity: Entity) {
        // On a planet's surface the inhabitants are placed once by SurfaceEcology; no endless waves.
        if (worldState.mode == WorldMode.SURFACE) return
        val pt = entity[Transform]
        val w = wave
        val c = config.waves
        val dt = deltaTime
        w.elapsed += dt

        // v2.36: drifters don't count — a coasting stranger at the far edge of space neither eats
        // the live cap nor keeps a cleared wave open.
        var waveMobs = 0
        mobs.forEach { m -> if (!m[Mob].drifter) waveMobs++ }

        if (w.phase == "active") {
            // v2.87 流星群: through a METEOR wave the sky keeps falling — one rock every beat.
            if (w.event == WaveEvent.METEOR) {
                w.meteorCd -= dt
                if (w.meteorCd <= 0f) {
                    pickTile(pt)?.let { (mx, my) ->
                        world.entity {
                            it += Transform(x = mx, y = my)
                            it += io.github.panda17tk.arpg.ecs.components.Meteor(WaveEvents.METEOR_FALL)
                        }
                    }
                    w.meteorCd = WaveEvents.METEOR_INTERVAL
                }
            }
            if (w.toSpawn > 0) {
                w.spawnCd -= dt
                if (w.spawnCd <= 0f && waveMobs < liveCap(w.num, c)) {
                    val n = spawnNormal(pt, w.num)
                    if (n > 0) { w.toSpawn -= n; w.spawnCd = spawnInterval(w.num, c) }
                    else w.spawnCd = 0.2f
                }
            }
            if (w.toSpawn <= 0 && waveMobs == 0 && w.elapsed > c.firstWaveDelay) {
                w.phase = "intermission"
                w.interT = c.intermission
            }
        } else {
            w.interT -= dt
            if (w.interT <= 0f) startWave(pt, w.num + 1)
        }
    }

    private fun startWave(pt: Transform, n: Int) {
        val c = config.waves
        wave.num = n
        wave.phase = "active"
        wave.toSpawn = minOf(c.maxQuota, c.baseQuota + (n - 1) * c.quotaPerWave)
        wave.spawnCd = 0.4f
        // v2.45 イベントウェーブ: the wave's flavor is fixed by its number (learnable rhythm).
        wave.event = WaveEvents.eventFor(n)
        if (wave.event == WaveEvent.HORDE) {
            wave.toSpawn = (wave.toSpawn * WaveEvents.HORDE_QUOTA_MUL).toInt()
        }
        if (wave.event == WaveEvent.PURGE) {
            wave.toSpawn = (wave.toSpawn * WaveEvents.PURGE_QUOTA_MUL).toInt()
        }
        var bountyName: String? = null
        if (wave.event == WaveEvent.BOUNTY) bountyName = spawnBounty(pt, n)
        // Boss waves: a boss every bossEvery, else a midboss every midBossEvery (legacy spawner).
        val bossLine = when {
            c.bossEvery > 0 && n % c.bossEvery == 0 -> { spawnBoss(pt, n, bossKeys); "⚠ 強大な気配が近づく" }
            c.midBossEvery > 0 && n % c.midBossEvery == 0 -> { spawnBoss(pt, n, midBossKeys); "強敵の気配" }
            else -> null
        }
        // One toast line for the screen to pick up: the boss telegraph and/or the event flavor.
        wave.announce = listOfNotNull(bossLine, WaveEvents.announceFor(wave.event, bountyName))
            .joinToString("　").ifEmpty { null }
    }

    /** v2.45 賞金首: one named, leveled-up midboss whose fall bursts into a dust pile. */
    private fun spawnBounty(pt: Transform, n: Int): String? {
        if (midBossKeys.isEmpty()) return null
        val tile = pickTile(pt) ?: return null
        val def = config.enemies[midBossKeys[rng.nextInt(midBossKeys.size)]] ?: return null
        val name = WaveEvents.bountyName(rng)
        val e = MobFactory.spawn(world, def, tile.first, tile.second, n, config.waves.hpScalePerWave, config.waves.speedScalePerWave, tribe = tribes.tribeOf(tile.first, tile.second))
        with(world) {
            val m = e[Mob]
            m.level = 7 // a bounty head outclasses an ordinary midboss (5) but not a boss (10)
            m.bountyDust = WaveEvents.bountyReward(n)
            fx.spawnWarnRing(e[Transform].x, e[Transform].y) // v2.86: the head's arrival is marked
        }
        return name
    }

    private fun spawnBoss(pt: Transform, n: Int, keys: List<String>) {
        if (keys.isEmpty()) return
        val tile = pickTile(pt) ?: return
        val def = config.enemies[keys[rng.nextInt(keys.size)]] ?: return
        val e = MobFactory.spawn(world, def, tile.first, tile.second, n, config.waves.hpScalePerWave, config.waves.speedScalePerWave, tribe = tribes.tribeOf(tile.first, tile.second))
        with(world) { fx.spawnWarnRing(e[Transform].x, e[Transform].y) } // v2.86: the arrival is marked
    }

    private fun liveCap(n: Int, c: WaveConfig): Int = minOf(c.maxLiveCap, c.liveCapBase + n * c.liveCapPerWave)
    private fun spawnInterval(n: Int, c: WaveConfig): Float {
        val base = maxOf(c.minSpawnInterval, c.spawnIntervalBase - n * c.spawnIntervalPerWave)
        return if (wave.event == WaveEvent.HORDE) base * WaveEvents.HORDE_INTERVAL_MUL else base
    }

    // v2.48 惑星サーバー: the purge sweep fields only the preservation machinery.
    private val purgeKeys: List<String> = WaveEvents.PURGE_KEYS.filter { it in normalKeys }

    /**
     * v2.52: which slice of the roster this surge depth can vent. Early surges leak only the
     * shallow processes; as the desync deepens, older and stranger layers surface — the roster's
     * insertion order doubles as its stratigraphy (basic → v2.39 → v2.41 → the machinery).
     */
    private fun surgePool(waveNum: Int): List<String> =
        if (wave.event == WaveEvent.PURGE && purgeKeys.isNotEmpty()) purgeKeys
        else normalKeys.take((3 + waveNum).coerceAtMost(normalKeys.size))

    /** Spawn a same-tribe herd (3..6) clustered around one tile so tribes pop in groups. Returns the count. */
    private fun spawnNormal(pt: Transform, waveNum: Int): Int {
        val pool = surgePool(waveNum)
        if (pool.isEmpty()) return 0
        val tile = pickTile(pt) ?: return 0
        val tribe = tribes.tribeOf(tile.first, tile.second)
        var spawned = 0
        repeat(3 + rng.nextInt(4)) {
            val def = config.enemies[pool[rng.nextInt(pool.size)]] ?: return@repeat
            val a = rng.nextFloat() * 6.2831855f; val r = rng.nextFloat() * Tuning.TILE * 2.5f
            // v2.45: a magnetic storm agitates everyone — nearly the whole herd dashes.
            val dashChance = if (wave.event == WaveEvent.STORM) WaveEvents.STORM_DASH_CHANCE else 0.5f
            MobFactory.spawn(world, def, tile.first + cos(a) * r, tile.second + sin(a) * r, waveNum, config.waves.hpScalePerWave, config.waves.speedScalePerWave, tribe = tribe, dashes = rng.nextFloat() < dashChance)
            spawned++
        }
        return spawned
    }

    /** A free floor tile at least 8 tiles from the player (legacy pickTile; LOS check dropped for spawn reliability). */
    private fun pickTile(pt: Transform): Pair<Float, Float>? {
        val minDist = Tuning.TILE * 8f
        val min2 = minDist * minDist
        repeat(200) {
            val tx = 1 + rng.nextInt(map.width - 2)
            val ty = 1 + rng.nextInt(map.height - 2)
            if (map.solidAt(tx, ty)) return@repeat
            val cx = (tx + 0.5f) * Tuning.TILE
            val cy = (ty + 0.5f) * Tuning.TILE
            val dx = cx - pt.x; val dy = cy - pt.y
            if (dx * dx + dy * dy < min2) return@repeat
            return cx to cy
        }
        return null
    }
}
