package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.WaveConfig
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.WaveState
import io.github.panda17tk.arpg.ecs.world.MobFactory
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.Tuning

/**
 * Wave-based spawner (legacy spawner.js). Spawns normal enemies over time during a wave;
 * when the wave is cleared, an intermission passes, then the next (scaled) wave starts.
 * Boss waves (midBossEvery/bossEvery) are wired in Phase 6b once boss entities/attacks exist.
 */
class SpawnerSystem : IteratingSystem(family { all(PlayerTag, Transform) }) {
    private val wave: WaveState = world.inject()
    private val config: GameConfig = world.inject()
    private val map: TileMap = world.inject()
    private val rng: Rng = world.inject()

    private val mobs by lazy { world.family { all(Mob) } }
    private val normalKeys: List<String> = config.enemies.filterValues { it.tier == "normal" }.keys.toList()
    private val midBossKeys: List<String> = config.enemies.filterValues { it.tier == "midboss" }.keys.toList()
    private val bossKeys: List<String> = config.enemies.filterValues { it.tier == "boss" }.keys.toList()

    override fun onTickEntity(entity: Entity) {
        val pt = entity[Transform]
        val w = wave
        val c = config.waves
        val dt = deltaTime
        w.elapsed += dt

        if (w.phase == "active") {
            if (w.toSpawn > 0) {
                w.spawnCd -= dt
                if (w.spawnCd <= 0f && mobs.numEntities < liveCap(w.num, c)) {
                    if (spawnNormal(pt, w.num)) { w.toSpawn--; w.spawnCd = spawnInterval(w.num, c) }
                    else w.spawnCd = 0.2f
                }
            }
            if (w.toSpawn <= 0 && mobs.numEntities == 0 && w.elapsed > c.firstWaveDelay) {
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
        // Boss waves: a boss every bossEvery, else a midboss every midBossEvery (legacy spawner).
        when {
            c.bossEvery > 0 && n % c.bossEvery == 0 -> spawnBoss(pt, n, bossKeys)
            c.midBossEvery > 0 && n % c.midBossEvery == 0 -> spawnBoss(pt, n, midBossKeys)
        }
    }

    private fun spawnBoss(pt: Transform, n: Int, keys: List<String>) {
        if (keys.isEmpty()) return
        val tile = pickTile(pt) ?: return
        val def = config.enemies[keys[rng.nextInt(keys.size)]] ?: return
        MobFactory.spawn(world, def, tile.first, tile.second, n, config.waves.hpScalePerWave, config.waves.speedScalePerWave)
    }

    private fun liveCap(n: Int, c: WaveConfig): Int = minOf(c.maxLiveCap, c.liveCapBase + n * c.liveCapPerWave)
    private fun spawnInterval(n: Int, c: WaveConfig): Float = maxOf(c.minSpawnInterval, c.spawnIntervalBase - n * c.spawnIntervalPerWave)

    private fun spawnNormal(pt: Transform, waveNum: Int): Boolean {
        if (normalKeys.isEmpty()) return false
        val tile = pickTile(pt) ?: return false
        val key = normalKeys[rng.nextInt(normalKeys.size)]
        val def = config.enemies[key] ?: return false
        MobFactory.spawn(world, def, tile.first, tile.second, waveNum, config.waves.hpScalePerWave, config.waves.speedScalePerWave)
        return true
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
