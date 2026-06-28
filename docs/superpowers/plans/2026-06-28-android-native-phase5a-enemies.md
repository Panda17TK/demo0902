# Phase 5a — Enemies: Spawn, Chase, Contact, and Die Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Enemies spawn from the stage's markers, chase the player via the flow-field (with separation so they don't stack), deal contact damage, and the player's bullets / melee / beam / grenade actually damage and **kill** them.

**Scope note:** This is the first slice of the (large) Phase 5. **In 5a:** mob entity + health, config-driven roster, flow-field chase + separation + contact, `hurtMob` wired into all four player-attack systems, player health + i-frames, spawn-at-markers, death removal. **Deferred to 5b+:** enemy ranged attacks (`shot`/enemy-bullets), the full 16-attack registry (`lunge/nova/summon/slam/charge/homing/heal/enrage/mine/barrage/guard/charge_melee/blink`), mid-boss/boss, dodge/guard passives, loot drops, death FX / kill-cam, and the game-over UI (a simple `GameOver` flag is set in 5a; the overlay is Phase 7).

**Architecture:** Pure AI movement (`ai/AiMove`) + pure mob damage (`combat/MobDamage` = `hurtMob`) are unit-tested. `AISystem` composes the Phase-2 primitives (FlowField, SpatialGrid, Los, Collision). The Phase-3 combat systems gain mob-damage by querying a per-frame mob `SpatialGrid` and calling `hurtMob`.

**Tech Stack:** Kotlin 2.0.21, libGDX 1.13.1, Fleks 2.8, kotlinx.serialization, JUnit 5. Builds on Phases 0–4.

---

## Source-of-truth (legacy `legacy-web-v1.1.1`: `config.js` enemies, `enemies.js`, `ai.js`, `combat-core.js`)

**Roster (CONFIG.enemies, tier=normal):**
- `zombie`: hp 55, speed 72, w/h 22, seeRange 240, contactKB 220, color `#b24a4a`, attacks `[melee cd0.9 dmg10 range12 arc360, lunge cd3.5 range90 power360]`.
- `spitter`: hp 65, speed 35, seeRange 320, contactKB 220, color `#3aa06f`, attacks `[melee cd3.0 dmg10 range9 arc90, shot cd1.2 dmg12 speed220 kite]`.
- `stalker`: hp 60, speed 64, seeRange 340, contactKB 200, color `#9a6ad0`, dodge{0.18/0.15/2.0}, attacks `[blink ..., charge_melee ...]`.

In 5a only the **`melee`** attack type executes; `shot/lunge/blink/charge_melee` are recognized but **skipped** (implemented in 5b). So spitter/stalker behave as melee chasers for now.

**hurtMob (combat-core.js):** `m.hp -= dmg; if (kb) m.vel += n*kb; m.hitFlash = 0.12; (dodge/guard skipped in 5a)`.

**AI (ai.js):** per mob — velocity decay (`v *= 0.02^dt`); HP-half slow for normal tier (`CONFIG.ai.hpSlowMul`); see = `dist<seeRange && LOS`; separation over neighbors within `sepRadius`; movement = flow-field follow (pick the 4-neighbor tile with the lowest flow value below the current tile) else LOS-direct else wander; `moveAndCollide`; contact knockback when overlapping the player (push player by 260, mob by `contactKB`); `runAttacks` (melee). `CONFIG.ai`: `sepRadius`, `hpSlowMul`, `wanderSlow`, `wanderStuck`. Use these defaults (legacy): `sepRadius=26`, `hpSlowMul=0.6`, `wanderSlow=0.35`, `wanderStuck=0.8`, flow rebuild every `0.35s`. Melee i-frames `iFrameMelee=0.9`.

## File structure (this phase)

```
core/src/main/kotlin/io/github/panda17tk/arpg/
├─ config/EnemyDef.kt              # @Serializable enemy archetype + AttackSpec
├─ config/GameConfig.kt            # MODIFY: add `enemies: Map<String, EnemyDef>` + `ai: AiConfig`
├─ config/AiConfig.kt              # @Serializable AI tuning
├─ ecs/components/{Mob,Health,Velocity,GameOver}.kt
├─ combat/MobDamage.kt             # pure hurtMob (Health + knockback + flash)
├─ ai/AiMove.kt                    # pure flow-field follow direction
├─ ecs/world/MobFactory.kt         # build a mob entity from an EnemyDef (+ wave scaling)
├─ ecs/systems/AISystem.kt         # move/separate/contact/melee
├─ ecs/systems/MobDamageSystem.kt  # builds the per-frame mob SpatialGrid; reaps dead mobs
├─ ecs/systems/{ProjectileSystem,MeleeSystem,FireSystem,MovementSystem}.kt  # MODIFY: damage mobs / player knockback
├─ ecs/world/WorldFactory.kt       # MODIFY: spawn mobs at markers; inject mob grid + ai config; periodic flow rebuild
├─ screens/GameScreen.kt           # MODIFY: render mobs + player HP HUD
core/src/test/kotlin/io/github/panda17tk/arpg/
├─ combat/MobDamageTest.kt
├─ ai/AiMoveTest.kt
└─ ecs/world/WorldEnemyTest.kt
```

> **Fleks note:** mob entities are created in `WorldFactory` and removed in `MobDamageSystem` (`world -= entity`). The Phase-3 systems already do entity spawn/remove; reuse those patterns. If a Fleks symbol differs, adjust only the systems.

---

## Task 1: Enemy config (EnemyDef + AiConfig) + GameConfig section

**Files:** Create `config/EnemyDef.kt`, `config/AiConfig.kt`; MODIFY `config/GameConfig.kt`.

- [ ] **Step 1: `config/EnemyDef.kt`**
```kotlin
package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** One enemy attack spec (data-driven; AISystem interprets `type`). */
@Serializable
data class AttackSpec(
    val type: String,
    val cd: Float = 1f,
    val dmg: Float = 0f,
    val range: Float = 0f,
    val arc: Float = 360f,
    val speed: Float = 0f,
)

/** Enemy archetype (legacy CONFIG.enemies entry). tier: normal/midboss/boss. */
@Serializable
data class EnemyDef(
    val name: String,
    val tier: String = "normal",
    val color: String = "#b24a4a",
    val hp: Float,
    val speed: Float,
    val w: Float = 22f,
    val h: Float = 22f,
    val seeRange: Float = 240f,
    val contactKB: Float = 220f,
    val attacks: List<AttackSpec> = emptyList(),
)
```

- [ ] **Step 2: `config/AiConfig.kt`**
```kotlin
package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** AI tuning (legacy CONFIG.ai). */
@Serializable
data class AiConfig(
    val sepRadius: Float = 26f,
    val hpSlowMul: Float = 0.6f,
    val wanderSlow: Float = 0.35f,
    val wanderStuck: Float = 0.8f,
    val flowRebuildInterval: Float = 0.35f,
    val playerKnockback: Float = 260f,
    val contactDmg: Float = 6f,        // contact tick damage to player
    val iFrameContact: Float = 0.6f,
)
```

- [ ] **Step 3: MODIFY `config/GameConfig.kt`** — add the `enemies` map and `ai`:
```kotlin
@Serializable
data class GameConfig(
    val player: PlayerConfig = PlayerConfig(),
    val weapons: List<WeaponDef> = Weapons.ALL,
    val ai: AiConfig = AiConfig(),
    val enemies: Map<String, EnemyDef> = defaultEnemies(),
)

private fun defaultEnemies(): Map<String, EnemyDef> = mapOf(
    "zombie" to EnemyDef(
        name = "ゾンビ", tier = "normal", color = "#b24a4a", hp = 55f, speed = 72f,
        seeRange = 240f, contactKB = 220f,
        attacks = listOf(AttackSpec("melee", cd = 0.9f, dmg = 10f, range = 12f, arc = 360f), AttackSpec("lunge", cd = 3.5f, range = 90f)),
    ),
    "spitter" to EnemyDef(
        name = "スピッター", tier = "normal", color = "#3aa06f", hp = 65f, speed = 35f,
        seeRange = 320f, contactKB = 220f,
        attacks = listOf(AttackSpec("melee", cd = 3.0f, dmg = 10f, range = 9f, arc = 90f), AttackSpec("shot", cd = 1.2f, dmg = 12f, speed = 220f)),
    ),
    "stalker" to EnemyDef(
        name = "ストーカー", tier = "normal", color = "#9a6ad0", hp = 60f, speed = 64f,
        seeRange = 340f, contactKB = 200f,
        attacks = listOf(AttackSpec("blink", cd = 3.0f), AttackSpec("charge_melee", cd = 2.4f, range = 40f, dmg = 18f)),
    ),
)
```
(Add `private fun defaultEnemies()` at file scope below the class. Keep the existing `weapons` default.)

- [ ] **Step 4: Compile + verify config still round-trips**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.config.*"`
Expected: PASS (existing config tests still green; the new fields serialize with defaults).

- [ ] **Step 5: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/config/EnemyDef.kt core/src/main/kotlin/io/github/panda17tk/arpg/config/AiConfig.kt core/src/main/kotlin/io/github/panda17tk/arpg/config/GameConfig.kt && git commit -m "feat(core): add enemy roster + AI config to GameConfig"
```

---

## Task 2: Mob/Health/Velocity/GameOver components + MobFactory

**Files:** Create `ecs/components/Mob.kt`, `Health.kt`, `Velocity.kt`, `GameOver.kt`, `ecs/world/MobFactory.kt`.

- [ ] **Step 1: `ecs/components/Health.kt`** (shared by player and mobs)
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class Health(var hp: Float, var hpMax: Float, var iTime: Float = 0f, var hitFlash: Float = 0f) : Component<Health> {
    override fun type() = Health
    companion object : ComponentType<Health>()
}
```

- [ ] **Step 2: `ecs/components/Velocity.kt`** (knockback impulse; decays)
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class Velocity(var vx: Float = 0f, var vy: Float = 0f) : Component<Velocity> {
    override fun type() = Velocity
    companion object : ComponentType<Velocity>()
}
```

- [ ] **Step 3: `ecs/components/Mob.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.config.EnemyDef

/** A mob: its archetype, scaled speed, per-attack cooldown timers, facing, contact timer. */
class Mob(
    val kind: String,
    val def: EnemyDef,
    var speed: Float,
    val attackCd: FloatArray,
    var faceX: Float = 1f,
    var faceY: Float = 0f,
    var bumpCd: Float = 0f,
    var stuckT: Float = 0f,
) : Component<Mob> {
    val tier: String get() = def.tier
    override fun type() = Mob
    companion object : ComponentType<Mob>()
}
```

- [ ] **Step 4: `ecs/components/GameOver.kt`** (singleton-ish flag entity-less; use a holder)
```kotlin
package io.github.panda17tk.arpg.ecs.components

/** Mutable run flag injected into the world (set when the player dies). UI overlay is Phase 7. */
class GameOver(var isOver: Boolean = false, var kills: Int = 0)
```

- [ ] **Step 5: `ecs/world/MobFactory.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.github.panda17tk.arpg.config.EnemyDef
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity

object MobFactory {
    /** Spawn one mob entity at (x,y). waveNum scales hp/speed (legacy makeMobFromKey). */
    fun spawn(world: World, def: EnemyDef, x: Float, y: Float, waveNum: Int = 1, hpScalePerWave: Float = 0f, speedScalePerWave: Float = 0f): Entity {
        val hs = 1f + (waveNum - 1) * hpScalePerWave
        val ss = 1f + (waveNum - 1) * speedScalePerWave
        val hp = def.hp * hs
        return world.entity {
            it += Transform(x = x, y = y, prevX = x, prevY = y)
            it += Body(def.w / 2f, def.h / 2f)
            it += Health(hp, hp)
            it += Velocity()
            it += Facing()
            it += Mob(
                kind = "${def.name}", def = def, speed = def.speed * ss,
                attackCd = FloatArray(def.attacks.size),
            )
        }
    }
}
```

- [ ] **Step 6: Compile + commit**
Run: `cd "V:/src/demo0902" && ./gradlew :core:compileKotlin`
```bash
git add core/src/main/kotlin/io/github/panda17tk/arpg/ecs/components/Mob.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/components/Health.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/components/Velocity.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/components/GameOver.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/world/MobFactory.kt
git commit -m "feat(core): Mob/Health/Velocity/GameOver components and MobFactory"
```

---

## Task 3: Pure `hurtMob` (MobDamage) + flow-field follow (AiMove) (TDD)

**Files:** Create `combat/MobDamage.kt`, `ai/AiMove.kt`; Tests `combat/MobDamageTest.kt`, `ai/AiMoveTest.kt`.

- [ ] **Step 1: Write failing tests**

`combat/MobDamageTest.kt`:
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Velocity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MobDamageTest {
    @Test fun `damage reduces hp and applies knockback + flash`() {
        val h = Health(50f, 50f); val v = Velocity()
        MobDamage.hurt(h, v, dmg = 20f, nx = 1f, ny = 0f, kb = 100f)
        assertEquals(30f, h.hp, 1e-3f)
        assertEquals(100f, v.vx, 1e-3f)
        assertTrue(h.hitFlash > 0f)
    }
    @Test fun `lethal damage drops hp to zero or below`() {
        val h = Health(10f, 10f); val v = Velocity()
        MobDamage.hurt(h, v, dmg = 25f, nx = 0f, ny = 1f, kb = 0f)
        assertTrue(h.hp <= 0f)
    }
}
```

`ai/AiMoveTest.kt`:
```kotlin
package io.github.panda17tk.arpg.ai

import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiMoveTest {
    @Test fun `mob follows the flow-field toward the player`() {
        val m = TileMap.fromRows(listOf("........", "........", "........"))
        val ff = FlowField(m.width, m.height)
        // player at tile (7,1): flow distance decreases toward x=7
        ff.rebuild(m, startTileX = 7, startTileY = 1)
        // mob at world (16,48) = tile (0,1); should want to move +x (toward player)
        val dir = AiMove.followDir(m, ff, mobX = 16f, mobY = 48f)
        assertTrue(dir.first > 0.5f, "expected +x follow, got ${dir.first}")
    }
    @Test fun `mob with no downhill neighbour returns zero`() {
        val m = TileMap.fromRows(listOf("###", "#.#", "###"))
        val ff = FlowField(m.width, m.height)
        ff.rebuild(m, startTileX = 1, startTileY = 1)
        // mob sits on the source tile; no lower neighbour
        val dir = AiMove.followDir(m, ff, mobX = 1 * Tuning.TILE + 16f, mobY = 1 * Tuning.TILE + 16f)
        assertTrue(dir.first == 0f && dir.second == 0f)
    }
}
```

- [ ] **Step 2: Run, verify FAIL**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.combat.MobDamageTest" --tests "io.github.panda17tk.arpg.ai.AiMoveTest"`

- [ ] **Step 3: Implement `combat/MobDamage.kt`**
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Velocity

/** Single choke-point for damaging a mob (legacy combat-core.hurtMob). Dodge/guard: Phase 5b. */
object MobDamage {
    fun hurt(health: Health, vel: Velocity, dmg: Float, nx: Float, ny: Float, kb: Float) {
        health.hp -= dmg
        if (kb != 0f) { vel.vx += nx * kb; vel.vy += ny * kb }
        health.hitFlash = 0.12f
    }
}
```

- [ ] **Step 4: Implement `ai/AiMove.kt`** (pick the lowest-flow 4-neighbour below the current tile)
```kotlin
package io.github.panda17tk.arpg.ai

import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor
import kotlin.math.sqrt

object AiMove {
    private val DIRS = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))

    /** Unit direction toward the player along the flow-field, or (0,0) if no downhill walkable neighbour. */
    fun followDir(map: TileMap, flow: FlowField, mobX: Float, mobY: Float): Pair<Float, Float> {
        val tx = floor(mobX / Tuning.TILE).toInt()
        val ty = floor(mobY / Tuning.TILE).toInt()
        val here = flow.distAt(tx, ty)
        var best = here
        var bx = 0; var by = 0
        for (d in DIRS) {
            val nx = tx + d[0]; val ny = ty + d[1]
            if (map.solidAt(nx, ny)) continue
            val v = flow.distAt(nx, ny)
            if (v < best) { best = v; bx = d[0]; by = d[1] }
        }
        if (bx == 0 && by == 0) return 0f to 0f
        val len = sqrt((bx * bx + by * by).toFloat())
        return (bx / len) to (by / len)
    }
}
```

- [ ] **Step 5: Run, verify PASS** (2 + 2 tests)
Run: same as Step 2.

- [ ] **Step 6: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/combat/MobDamage.kt core/src/main/kotlin/io/github/panda17tk/arpg/ai/AiMove.kt core/src/test/kotlin/io/github/panda17tk/arpg/combat/MobDamageTest.kt core/src/test/kotlin/io/github/panda17tk/arpg/ai/AiMoveTest.kt && git commit -m "feat(core): pure hurtMob and flow-field follow direction (TDD)"
```

---

## Task 4: AISystem (move / separate / contact / melee) + MobDamageSystem (grid + reap)

**Files:** Create `ecs/systems/AISystem.kt`, `ecs/systems/MobDamageSystem.kt`.

- [ ] **Step 1: `ecs/systems/MobDamageSystem.kt`** — builds the per-frame mob `SpatialGrid` (shared with player-attack systems) and removes dead mobs. Runs FIRST among combat systems.
```kotlin
package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.GameOver
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.pathfinding.SpatialGrid

/** Rebuilds the mob spatial grid each tick and reaps dead mobs (kills++). */
class MobDamageSystem(private val grid: SpatialGrid<Entity>) : IteratingSystem(family { all(Mob, Transform, Health) }) {
    private val gameOver: GameOver = world.inject()

    override fun onTick() {
        grid.clear()
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        if (entity[Health].hp <= 0f) { gameOver.kills++; world -= entity; return }
        grid.insert(entity, t.x, t.y)
    }
}
```

- [ ] **Step 2: `ecs/systems/AISystem.kt`** — flow-field chase + separation + contact + melee (the only attack type in 5a).
```kotlin
package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ai.AiMove
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.pathfinding.Los
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.sim.Collision
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

class AISystem(private val mobGrid: SpatialGrid<Entity>) : IteratingSystem(family { all(Mob, Transform, Velocity, Body, Health, Facing)}) {
    private val map: TileMap = world.inject()
    private val flow: FlowField = world.inject()
    private val config: GameConfig = world.inject()

    private val players by lazy { world.family { all(PlayerTag, Transform, Health, Velocity) } }

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]; val v = entity[Velocity]; val b = entity[Body]
        val m = entity[Mob]; val f = entity[Facing]; val h = entity[Health]
        val dt = deltaTime
        val ai = config.ai

        var px = t.x; var py = t.y
        var playerT: Transform? = null; var playerH: Health? = null; var playerV: Velocity? = null
        players.forEach { e -> playerT = e[Transform]; playerH = e[Health]; playerV = e[Velocity]; px = e[Transform].x; py = e[Transform].y }

        // decay knockback velocity + timers
        val decay = 0.02f.pow(dt)
        v.vx *= decay; v.vy *= decay
        if (m.bumpCd > 0f) m.bumpCd -= dt
        if (h.hitFlash > 0f) h.hitFlash -= dt
        for (i in m.attackCd.indices) if (m.attackCd[i] > 0f) m.attackCd[i] -= dt

        // effective speed (HP-half slow for normal tier)
        val slow = if (m.tier == "normal" && h.hp <= h.hpMax * 0.5f) ai.hpSlowMul else 1f
        val eff = m.speed * slow

        val dx = px - t.x; val dy = py - t.y
        val dist = hypot(dx, dy)
        val see = dist < m.def.seeRange && Los.hasLineOfSight(map, t.x, t.y, px, py)

        // separation (Phase-2 spatial grid)
        var sepX = 0f; var sepY = 0f
        mobGrid.forNearby(t.x, t.y, ai.sepRadius) { other ->
            if (other == entity) return@forNearby
            val ot = with(world) { other[Transform] }
            val ddx = t.x - ot.x; val ddy = t.y - ot.y
            val d = hypot(ddx, ddy)
            if (d in 0.0001f..ai.sepRadius) { val w = (ai.sepRadius - d) / ai.sepRadius; sepX += ddx / d * w; sepY += ddy / d * w }
        }
        if (sepX != 0f || sepY != 0f) { val l = sqrt(sepX * sepX + sepY * sepY); v.vx += sepX / l * eff * 0.5f; v.vy += sepY / l * eff * 0.5f }

        // movement: flow-field follow else LOS-direct else wander
        var mvx = 0f; var mvy = 0f
        val (fx, fy) = AiMove.followDir(map, flow, t.x, t.y)
        if (fx != 0f || fy != 0f) { mvx = fx * eff * dt; mvy = fy * eff * dt; f.x = fx; f.y = fy }
        else if (see && dist > 0f) { mvx = dx / dist * eff * dt; mvy = dy / dist * eff * dt; f.x = dx / dist; f.y = dy / dist }

        val r1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, mvx + v.vx * dt, 0f)
        val r2 = Collision.moveAndCollide(map, r1.x, r1.y, b.halfW, b.halfH, 0f, mvy + v.vy * dt)
        t.x = r2.x; t.y = r2.y

        // contact damage + knockback
        val pt = playerT; val ph = playerH; val pv = playerV
        if (pt != null && ph != null && pv != null && m.bumpCd <= 0f) {
            if (abs(t.x - pt.x) < (b.halfW + 11f) && abs(t.y - pt.y) < (b.halfH + 11f)) {
                val nx = if (dist > 0f) (pt.x - t.x) / dist else 1f
                val ny = if (dist > 0f) (pt.y - t.y) / dist else 0f
                pv.vx += nx * ai.playerKnockback; pv.vy += ny * ai.playerKnockback
                v.vx -= nx * m.def.contactKB; v.vy -= ny * m.def.contactKB
                if (ph.iTime <= 0f) { ph.hp -= ai.contactDmg; ph.iTime = ai.iFrameContact }
                m.bumpCd = 0.28f
            }
        }

        // melee attack (the only attack type implemented in 5a; others skipped)
        if (pt != null && ph != null) {
            m.def.attacks.forEachIndexed { i, atk ->
                if (atk.type == "melee" && m.attackCd[i] <= 0f && dist < atk.range + 11f) {
                    m.attackCd[i] = atk.cd
                    if (ph.iTime <= 0f) { ph.hp -= atk.dmg; ph.iTime = ai.iFrameContact }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Compile check** (wired into the world in Task 5)
Run: `cd "V:/src/demo0902" && ./gradlew :core:compileKotlin`

- [ ] **Step 4: Commit** (with Task 5 — systems need the world wiring to run)
(Commit happens in Task 5 Step 5.)

---

## Task 5: Wire mobs into the world; player damages mobs; player health (TDD)

**Files:** MODIFY `ecs/world/WorldFactory.kt`, `ecs/systems/ProjectileSystem.kt`, `MeleeSystem.kt`, `FireSystem.kt`, `MovementSystem.kt`; Test `ecs/world/WorldEnemyTest.kt`.

- [ ] **Step 1: MODIFY `ecs/world/WorldFactory.kt`**:
  - Create a `SpatialGrid<Entity>` mob grid (cell = TILE), a `GameOver()`, and inject both.
  - Give the player a `Health(config.player.hpMax, config.player.hpMax)` and a `Velocity()`.
  - Register systems in THIS order: `SnapshotSystem, MobDamageSystem(mobGrid), MovementSystem, BuildSystem, WeaponSwitchSystem, MeleeSystem, FireSystem, ReloadSystem, ProjectileSystem, FlowRebuildSystem, AISystem`. **`MobDamageSystem` runs 2nd** (reaps last-frame's dead mobs, then rebuilds the mob `SpatialGrid`) so every player-attack system this frame reads a CURRENT grid; `AISystem` runs last (moves/attacks mobs). Dead mobs killed this frame are reaped at the next frame's start (1-frame delay, invisible).
  - Spawn a mob for each enemy `SpawnMarker` from `MapLoader` (`loaded.spawns.filter { it.kind == "enemy" }`), via `MobFactory.spawn(world, config.enemies[marker.name] ?: return@forEach, marker.worldX, marker.worldY)`.
  - Add a `FlowField` periodic rebuild: store the flow + map (already), and add a lightweight `FlowRebuildSystem` (below) OR rebuild in `MobDamageSystem.onTick`. Simplest: add `FlowRebuildSystem` that every `ai.flowRebuildInterval` rebuilds `flow` from the player's tile.

  Add `ecs/systems/FlowRebuildSystem.kt`:
```kotlin
package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

/** Rebuilds the flow-field toward the player on an interval so mobs path to the moving player. */
class FlowRebuildSystem : IntervalSystem() {
    private val map: TileMap = world.inject()
    private val flow: FlowField = world.inject()
    private val config: GameConfig = world.inject()
    private val players by lazy { world.family { all(PlayerTag, Transform) } }
    private var timer = 0f
    override fun onTick() {
        timer -= deltaTime
        if (timer > 0f) return
        timer = config.ai.flowRebuildInterval
        players.forEach { e ->
            val t = e[Transform]
            flow.rebuild(map, floor(t.x / Tuning.TILE).toInt(), floor(t.y / Tuning.TILE).toInt())
        }
    }
}
```
  Register `FlowRebuildSystem()` right before `AISystem`. (If `IntervalSystem` isn't the right Fleks base for a no-family tick system, use a plain `IteratingSystem` over the player family instead — adjust to the Fleks 2.8 API.)

- [ ] **Step 2: MODIFY `MovementSystem.kt`** — apply player knockback `Velocity` (decay + integrate) so contact pushes the player:
```kotlin
    // after computing dx,dy from input and BEFORE moveAndCollide, fold in knockback velocity:
    val v = entity[Velocity]
    v.vx *= 0.0001f.pow(dt); v.vy *= 0.0001f.pow(dt)   // import kotlin.math.pow
    val r = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, dx + v.vx * dt, dy + v.vy * dt)
```
(Add `Velocity` to the player family `all(PlayerTag, Transform, Facing, Stamina, Body, Velocity)`.)

- [ ] **Step 3: MODIFY `ProjectileSystem.kt`** — bullets and grenade explosions damage mobs via the injected mob grid + `MobDamage.hurt`. Inject `SpatialGrid<Entity>` (the mob grid) and `World` access; on a bullet step (before the wall check, or after), query nearby mobs and, if a mob AABB contains the bullet, `MobDamage.hurt(mobHealth, mobVel, b.dmg, n, 160f)` and despawn the bullet. For grenade explosion, after `Explosion.applyWallDamage`, query mobs within `explodeRadius` and apply falloff damage. (Use `with(world){ mob[Health] }` etc.)

- [ ] **Step 4: MODIFY `MeleeSystem.kt`** and `FireSystem.kt` (beam) — query the mob grid in the melee arc / along the beam ray and call `MobDamage.hurt`. (Melee: mobs within `meleeReach` and inside the 180° arc. Beam: mobs within a small perpendicular distance of the ray up to `reach`.)

  > These four edits all follow the same shape: inject the mob `SpatialGrid<Entity>`, query candidates, check geometry, call `MobDamage.hurt(mobHealth, mobVel, dmg, nx, ny, kb)`. Keep the existing wall-break behaviour intact.

- [ ] **Step 5: Write `ecs/world/WorldEnemyTest.kt`** and run the full suite
```kotlin
package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldEnemyTest {
    @Test fun `stage with enemy markers spawns mobs`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val mobs = gw.world.family { all(Mob, Transform) }.numEntities
        assertTrue(mobs >= 1, "expected mobs spawned from stage markers, got $mobs")
    }
    @Test fun `a mob takes damage and dies when its health hits zero`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        // directly damage the first mob to lethal and tick once -> it is reaped
        val before = gw.world.family { all(Mob) }.numEntities
        gw.world.family { all(Mob, Health) }.forEach { e -> with(gw.world) { e[Health].hp = -1f } }
        gw.world.update(1f / 60f)
        val after = gw.world.family { all(Mob) }.numEntities
        assertTrue(after < before, "dead mobs should be reaped: $before -> $after")
    }
}
```
Run: `cd "V:/src/demo0902" && ./gradlew :core:test`
Expected: all green (Phases 1–4 + the new enemy tests). Fix any Fleks API mismatches in the systems only.

- [ ] **Step 6: Commit (Tasks 4 + 5)**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/ecs/ core/src/test/kotlin/io/github/panda17tk/arpg/ecs/world/WorldEnemyTest.kt && git commit -m "feat(core): AI chase/contact/melee + player weapons damage mobs (TDD)"
```

---

## Task 6: Render mobs + player HP HUD (integration; build gate + manual)

**Files:** MODIFY `screens/GameScreen.kt`.

- [ ] **Step 1: MODIFY `screens/GameScreen.kt`** — draw mobs (filled circles in their `def.color`, with a hit-flash to white) before the player; add a player HP value to the HUD. Inside the filled-shapes block, before the player circle:
```kotlin
        with(gw.world) {
            gw.world.family { all(io.github.panda17tk.arpg.ecs.components.Mob, Transform, io.github.panda17tk.arpg.ecs.components.Health) }.forEach { e ->
                val mt = e[Transform]; val mm = e[io.github.panda17tk.arpg.ecs.components.Mob]; val mh = e[io.github.panda17tk.arpg.ecs.components.Health]
                shapes.color = if (mh.hitFlash > 0f) Color.WHITE else Color.valueOf(mm.def.color)
                shapes.circle(mt.x, mt.y, mm.def.w / 2f, 16)
            }
        }
```
Add the player HP + kills to the HUD line (read `gw.player[Health]` and the injected `GameOver.kills` — expose kills via `gw` or read the player Health for HP). Append to the existing HUD `font.draw`: `"  HP ${hp.toInt()}"` where `hp = with(gw.world){ gw.player[Health].hp }`.

- [ ] **Step 2: Build gates**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test :desktop:build`
Then: `cd "V:/src/demo0902" && ./gradlew :android:assembleDebug`
Expected: all green; APK builds. Do NOT run `:desktop:run`.

- [ ] **Step 3: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/screens/GameScreen.kt && git commit -m "feat(core): render mobs and player HP HUD"
```

- [ ] **Step 4 (MANUAL — human): Visual verification**
Run: `cd "V:/src/demo0902" && ./gradlew :desktop:run`
Expected: colored enemies spawn at the stage's markers and **chase** the player (pathing around walls, not stacking); touching them drains player HP and knocks both back; shooting/meleeing them flashes them white and **kills** them (they vanish). Close to end.

---

## Self-Review

**1. Scope coverage (Phase 5a):** enemies spawn from markers (Task 5 Step 1), chase via flow-field + separation (Tasks 3–4), contact damage (Task 4), melee attack (Task 4), player weapons kill mobs via `hurtMob` (Task 5 Steps 3–4), death removal (Task 4 MobDamageSystem), player health + i-frames (Tasks 2/5), render (Task 6). ✓ Deferred (noted): enemy ranged/`shot`/ebullets, full 16-attack registry, bosses, dodge/guard, loot, FX/kill-cam, game-over UI.

**2. Placeholder scan:** Task 5 Steps 3–4 describe the four `hurtMob` wirings by shape rather than full code — this is the one weak spot; the implementer must write the geometry queries. Mitigation: the shape is identical across the four (inject grid → query → geometry check → `MobDamage.hurt`), `MobDamage.hurt`/`SpatialGrid.forNearby`/`Los` are concrete, and `WorldEnemyTest` + the manual run verify the result. If the implementer needs exact code, follow the legacy `projectiles.js`/`melee.js`/`combat.js` mob-hit blocks (retrievable at the tag). **An executor should expand these into explicit code.**

**3. Type consistency:** `EnemyDef`/`AttackSpec`/`AiConfig` used in `GameConfig`, `MobFactory`, `AISystem`. `Mob(kind, def, speed, attackCd, ...)`, `Health(hp, hpMax, iTime, hitFlash)`, `Velocity(vx, vy)` consistent across factory/systems/render. `MobDamage.hurt(health, vel, dmg, nx, ny, kb)` consistent. `SpatialGrid<Entity>` mob grid shared (built in `MobDamageSystem`, read in AI + the four attack systems). ✓

**Risks flagged:** (a) Task 5 Steps 3–4 (the four `hurtMob` wirings) are described by shape, not full code — the executor must write the explicit geometry per attack, porting from the legacy mob-hit blocks (`projectiles.js` bullet/explosion, `melee.js` arc, `combat.js` beam), retrievable at the tag. The shape is identical (inject grid → `forNearby` → geometry check → `MobDamage.hurt`), and `WorldEnemyTest` + the manual run verify the result. (b) Fleks `IntervalSystem` for `FlowRebuildSystem` — if it doesn't fit, use a player-family `IteratingSystem`. (c) RESOLVED in Task 5 Step 1: `MobDamageSystem` runs 2nd (grid build + reap), so all player-attack systems read a current-frame mob grid.

---

## Execution Handoff

**Plan complete** (note self-review risk (a): the executor must write the explicit mob-hit geometry in the four player-attack systems, porting from legacy `projectiles.js`/`melee.js`/`combat.js`; the mob-grid ordering is resolved in Task 5 Step 1). Options: **Subagent-Driven (recommended)** — (Tasks 1–3 data+pure) / (Tasks 4–6 systems+wiring+render); or **Inline**. After 5a, **Phase 5b** adds enemy ranged attacks + the full 16-attack registry + mid-boss/boss + dodge/guard.
