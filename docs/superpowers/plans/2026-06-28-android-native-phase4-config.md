# Phase 4 — Data / Config Layer (GameConfig + ConfigStore) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Player and weapon tuning live in a serializable `GameConfig` loaded by a `ConfigStore` (built-in defaults + optional local-file override + export/import/reset), and the gameplay systems read from it — so a future dev-editor (Phase 8) can live-edit balance.

**Architecture:** A pure `ConfigCodec` does `GameConfig ↔ JSON` (kotlinx.serialization, fully unit-tested). A thin `ConfigStore` holds the active `GameConfig`, merges a local override file via `Gdx.files`, and supports export/import/reset. The pure combat/movement resolvers (`Locomotion`, `MeleeResolve`, `Explosion`) take a `PlayerConfig` parameter instead of reading compile-time `Tuning`; systems inject the `GameConfig` and pass `config.player`. Engine constants (TILE, viewport, fixed-step, offsets) stay in `Constants`/`Tuning`.

**Tech Stack:** Kotlin 2.0.21, kotlinx.serialization 1.7.3, libGDX 1.13.1, Fleks 2.8, JUnit 5. Builds on Phase 3 (combat).

---

## Scope

**In:** `kotlinx.serialization` setup; `PlayerConfig`/`GameConfig` (all current player gameplay numbers + the 5 weapons); `ConfigCodec` (JSON); `ConfigStore` (default/override/export/import/reset); refactor `Locomotion`/`MeleeResolve`/`Explosion` to take `PlayerConfig`; wire `GameConfig` into the world; `WorldFactory` builds the arsenal from `config.weapons`.

**Out (later phases):** `enemies`/`waves`/`drops`/`upgrades` config sections (added in Phases 5/6 when those systems exist); the dev-editor UI (Phase 8). Engine constants (TILE, FIXED_DT, VIEW_*, CAM_*, PLAYER_HALF, MUZZLE_OFFSET, MELEE_WALL_OFFSET, PLACED_WALL_HP, START_MATERIALS, START_AMMO*) stay in `Constants`/`Tuning` — they are not balance knobs.

## File structure (this phase)

```
core/build.gradle.kts                 # MODIFY: kotlinx.serialization plugin + dep
build.gradle.kts                      # MODIFY (root): serialization plugin (apply false)
core/src/main/kotlin/io/github/panda17tk/arpg/
├─ config/PlayerConfig.kt             # @Serializable player tuning (defaults = legacy)
├─ config/GameConfig.kt               # @Serializable { player, weapons }
├─ config/ConfigCodec.kt              # GameConfig <-> JSON (pure)
├─ config/ConfigStore.kt              # active config + override/export/import/reset + Gdx disk I/O
├─ combat/WeaponDef.kt                # MODIFY: @Serializable
├─ sim/Locomotion.kt                  # MODIFY: speed/nextStamina take PlayerConfig
├─ combat/MeleeResolve.kt             # MODIFY: resolve takes PlayerConfig
├─ combat/Explosion.kt                # MODIFY: applyWallDamage takes PlayerConfig
├─ sim/Tuning.kt                      # MODIFY: remove player-balance consts (moved to PlayerConfig)
├─ ecs/systems/{MovementSystem,MeleeSystem,FireSystem,ReloadSystem,ProjectileSystem}.kt  # MODIFY: inject GameConfig
└─ ecs/world/WorldFactory.kt          # MODIFY: load config, inject it, arsenal from config.weapons
core/src/test/kotlin/io/github/panda17tk/arpg/
├─ config/ConfigCodecTest.kt
├─ config/ConfigStoreTest.kt
├─ (UPDATE) sim/LocomotionTest.kt, combat/MeleeResolveTest.kt, combat/ExplosionTest.kt  # pass default config
└─ ecs/world/WorldConfigTest.kt
```

---

## Task 1: kotlinx.serialization setup + serializable WeaponDef

**Files:** MODIFY root `build.gradle.kts`, `core/build.gradle.kts`, `combat/WeaponDef.kt`.

- [ ] **Step 1: Root `build.gradle.kts`** — add the serialization plugin (apply false), alongside the existing plugins:
```kotlin
plugins {
    id("com.android.application") version "8.7.2" apply false
    kotlin("android") version "2.0.21" apply false
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
}
```

- [ ] **Step 2: `core/build.gradle.kts`** — apply the serialization plugin and add the JSON dependency. Add `kotlin("plugin.serialization")` to the `plugins {}` block and the dependency:
```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api("com.badlogicgames.gdx:gdx:1.13.1")
    api("io.github.quillraven.fleks:Fleks:2.8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}
```
(Keep the existing `java{}`/`kotlin{ compilerOptions }`/`tasks.test{}` blocks.)

- [ ] **Step 3: MODIFY `combat/WeaponDef.kt`** — make it serializable:
```kotlin
package io.github.panda17tk.arpg.combat

import kotlinx.serialization.Serializable

/** Static weapon stats. magSize null = no magazine (Beam consumes reserve directly). */
@Serializable
data class WeaponDef(
    val id: String,
    val name: String,
    val dmg: Float,
    val fireRate: Float,
    val magSize: Int?,
    val spread: Float,
    val pellets: Int,
    val ammoType: String,
)
```

- [ ] **Step 4: Compile + commit**
Run: `cd "V:/src/demo0902" && ./gradlew :core:compileKotlin`
Expected: BUILD SUCCESSFUL (serialization plugin resolves; WeaponDef gets a generated serializer).
```bash
git add build.gradle.kts core/build.gradle.kts core/src/main/kotlin/io/github/panda17tk/arpg/combat/WeaponDef.kt
git commit -m "build(core): add kotlinx.serialization; make WeaponDef serializable"
```

---

## Task 2: PlayerConfig + GameConfig + ConfigCodec (TDD)

**Files:** Create `config/PlayerConfig.kt`, `config/GameConfig.kt`, `config/ConfigCodec.kt`; Test `config/ConfigCodecTest.kt`.

- [ ] **Step 1: `config/PlayerConfig.kt`** (defaults = the legacy values currently in `Tuning`)
```kotlin
package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** Editable player/combat balance. Defaults mirror the legacy CONFIG.player. */
@Serializable
data class PlayerConfig(
    val baseSpeed: Float = 110f,
    val speedMul: Float = 1.2f,
    val dashMul: Float = 2f,
    val hpMax: Float = 100f,
    val staMax: Float = 100f,
    val staDrain: Float = 35f,
    val staRegen: Float = 22f,
    val meleeDmg: Float = 22f,
    val meleeReach: Float = 51f,
    val meleeCd: Float = 0.32f,
    val meleeSlashDmg: Float = 8f,
    val meleeStaWeakBelow: Float = 0.40f,
    val meleeStaSwordMin: Float = 0.20f,
    val meleeWeakMul: Float = 0.6f,
    val fistDmg: Float = 8f,
    val bulletSpeed: Float = 360f,
    val bulletLife: Float = 0.9f,
    val grenadeSpeed: Float = 280f,
    val grenadeFuse: Float = 1.0f,
    val autoReloadDelay: Float = 0.8f,
    val explodeRadius: Float = 70f,
    val explodeDmg: Float = 110f,
    val explodeSelfDmg: Float = 25f,
    val explodeWallDmg: Float = 120f,
)
```

- [ ] **Step 2: `config/GameConfig.kt`**
```kotlin
package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.combat.Weapons
import io.github.panda17tk.arpg.combat.WeaponDef
import kotlinx.serialization.Serializable

/** Root editable config. Phases 5/6 add enemies/waves/drops/upgrades sections. */
@Serializable
data class GameConfig(
    val player: PlayerConfig = PlayerConfig(),
    val weapons: List<WeaponDef> = Weapons.ALL,
)
```

- [ ] **Step 3: Write failing test `config/ConfigCodecTest.kt`**
```kotlin
package io.github.panda17tk.arpg.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfigCodecTest {
    @Test fun `default config round-trips through JSON unchanged`() {
        val cfg = GameConfig()
        val restored = ConfigCodec.fromJson(ConfigCodec.toJson(cfg))
        assertEquals(cfg, restored)
    }
    @Test fun `edited values survive a round-trip`() {
        val cfg = GameConfig(player = PlayerConfig(baseSpeed = 999f))
        val restored = ConfigCodec.fromJson(ConfigCodec.toJson(cfg))
        assertEquals(999f, restored.player.baseSpeed, 1e-3f)
        assertEquals(5, restored.weapons.size)
    }
    @Test fun `partial JSON fills missing fields from defaults`() {
        // only baseSpeed provided; everything else should default
        val restored = ConfigCodec.fromJson("""{"player":{"baseSpeed":200.0}}""")
        assertEquals(200f, restored.player.baseSpeed, 1e-3f)
        assertEquals(1.2f, restored.player.speedMul, 1e-3f) // default
        assertEquals(5, restored.weapons.size)              // default weapons
    }
}
```

- [ ] **Step 4: Run, verify FAIL**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.config.ConfigCodecTest"`

- [ ] **Step 5: Implement `config/ConfigCodec.kt`**
```kotlin
package io.github.panda17tk.arpg.config

import kotlinx.serialization.json.Json

/** Pure GameConfig <-> JSON. Lenient so partial/edited files fill from defaults. */
object ConfigCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    fun toJson(config: GameConfig): String = json.encodeToString(GameConfig.serializer(), config)
    fun fromJson(text: String): GameConfig = json.decodeFromString(GameConfig.serializer(), text)
}
```

- [ ] **Step 6: Run, verify PASS** (3 tests)
Run: same as Step 4.

- [ ] **Step 7: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/config/PlayerConfig.kt core/src/main/kotlin/io/github/panda17tk/arpg/config/GameConfig.kt core/src/main/kotlin/io/github/panda17tk/arpg/config/ConfigCodec.kt core/src/test/kotlin/io/github/panda17tk/arpg/config/ConfigCodecTest.kt && git commit -m "feat(core): GameConfig/PlayerConfig and JSON codec (TDD)"
```

---

## Task 3: ConfigStore (TDD)

**Files:** Create `config/ConfigStore.kt`; Test `config/ConfigStoreTest.kt`.

- [ ] **Step 1: Write failing test `config/ConfigStoreTest.kt`** (covers the in-memory logic; disk I/O is exercised manually on device)
```kotlin
package io.github.panda17tk.arpg.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfigStoreTest {
    @Test fun `starts at defaults`() {
        val store = ConfigStore()
        assertEquals(GameConfig(), store.config)
    }
    @Test fun `import replaces the active config`() {
        val store = ConfigStore()
        store.import("""{"player":{"baseSpeed":250.0}}""")
        assertEquals(250f, store.config.player.baseSpeed, 1e-3f)
    }
    @Test fun `export then import is stable`() {
        val store = ConfigStore()
        store.import("""{"player":{"meleeDmg":50.0}}""")
        val exported = store.export()
        val store2 = ConfigStore()
        store2.import(exported)
        assertEquals(store.config, store2.config)
    }
    @Test fun `reset returns to defaults`() {
        val store = ConfigStore()
        store.import("""{"player":{"baseSpeed":1f}}""")
        store.reset()
        assertEquals(GameConfig(), store.config)
    }
}
```

- [ ] **Step 2: Run, verify FAIL**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.config.ConfigStoreTest"`

- [ ] **Step 3: Implement `config/ConfigStore.kt`**
```kotlin
package io.github.panda17tk.arpg.config

import com.badlogic.gdx.Gdx

/**
 * Holds the active [GameConfig]. In-memory ops (import/export/reset) are pure and tested;
 * disk load/save use Gdx.files (only available inside a running libGDX app).
 */
class ConfigStore {
    var config: GameConfig = GameConfig()
        private set

    /** Replace the active config from a JSON string (partial JSON fills from defaults). */
    fun import(json: String) { config = ConfigCodec.fromJson(json) }

    /** Serialize the active config to JSON. */
    fun export(): String = ConfigCodec.toJson(config)

    /** Back to built-in defaults. */
    fun reset() { config = GameConfig() }

    /** Mutate in place (used by the dev-editor in Phase 8). */
    fun update(newConfig: GameConfig) { config = newConfig }

    // ---- disk I/O (Gdx-dependent; not unit-tested) ----
    private val fileName = "config.json"

    /** Load an override file from local storage if present; otherwise keep defaults. */
    fun loadFromDisk() {
        val handle = Gdx.files.local(fileName)
        if (handle.exists()) {
            runCatching { import(handle.readString("UTF-8")) }
                .onFailure { Gdx.app.error("ConfigStore", "bad config.json, using defaults", it) }
        }
    }

    /** Persist the active config to local storage. */
    fun saveToDisk() {
        Gdx.files.local(fileName).writeString(export(), false, "UTF-8")
    }
}
```

- [ ] **Step 4: Run, verify PASS** (4 tests)
Run: same as Step 2.

- [ ] **Step 5: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/config/ConfigStore.kt core/src/test/kotlin/io/github/panda17tk/arpg/config/ConfigStoreTest.kt && git commit -m "feat(core): ConfigStore (default/import/export/reset + disk I/O) (TDD)"
```

---

## Task 4: Make the pure resolvers config-driven (TDD refactor)

**Files:** MODIFY `sim/Locomotion.kt`, `combat/MeleeResolve.kt`, `combat/Explosion.kt`; UPDATE their tests `sim/LocomotionTest.kt`, `combat/MeleeResolveTest.kt`, `combat/ExplosionTest.kt`.

- [ ] **Step 1: MODIFY `sim/Locomotion.kt`** — `speed` and `nextStamina` take a `PlayerConfig`; `keyboardDirection`/`isDashing` are unchanged (no balance values).
```kotlin
package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.PlayerConfig
import kotlin.math.sqrt

data class MoveVector(val dirX: Float, val dirY: Float, val speedScale: Float) {
    val isMoving: Boolean get() = speedScale > 0f
    companion object { val NONE = MoveVector(0f, 0f, 0f) }
}

object Locomotion {
    fun keyboardDirection(left: Boolean, right: Boolean, up: Boolean, down: Boolean): MoveVector {
        val ax = (if (left) -1f else 0f) + (if (right) 1f else 0f)
        val ay = (if (up) -1f else 0f) + (if (down) 1f else 0f)
        if (ax == 0f && ay == 0f) return MoveVector.NONE
        val len = sqrt(ax * ax + ay * ay)
        return MoveVector(ax / len, ay / len, 1f)
    }

    fun isDashing(dashHeld: Boolean, moving: Boolean, sta: Float): Boolean =
        dashHeld && moving && sta > 0f

    fun speed(dashing: Boolean, cfg: PlayerConfig): Float =
        cfg.baseSpeed * cfg.speedMul * (if (dashing) cfg.dashMul else 1f)

    fun nextStamina(sta: Float, dashing: Boolean, dt: Float, cfg: PlayerConfig): Float =
        if (dashing) (sta - cfg.staDrain * dt).coerceAtLeast(0f)
        else (sta + cfg.staRegen * dt).coerceAtMost(cfg.staMax)
}
```

- [ ] **Step 2: UPDATE `sim/LocomotionTest.kt`** — pass a default `PlayerConfig` to `speed`/`nextStamina` (same expected numbers). Add `import io.github.panda17tk.arpg.config.PlayerConfig` and a `private val cfg = PlayerConfig()`; change the affected tests:
```kotlin
    private val cfg = PlayerConfig()

    @Test fun `speed is 132 normally and 264 dashing`() {
        assertEquals(132f, Locomotion.speed(dashing = false, cfg = cfg), 1e-3f)
        assertEquals(264f, Locomotion.speed(dashing = true, cfg = cfg), 1e-3f)
    }
    @Test fun `stamina drains while dashing and clamps at zero`() {
        assertEquals(65f, Locomotion.nextStamina(100f, dashing = true, dt = 1f, cfg = cfg), 1e-3f)
        assertEquals(0f, Locomotion.nextStamina(10f, dashing = true, dt = 1f, cfg = cfg), 1e-3f)
    }
    @Test fun `stamina regens while not dashing and clamps at max`() {
        assertEquals(72f, Locomotion.nextStamina(50f, dashing = false, dt = 1f, cfg = cfg), 1e-3f)
        assertEquals(100f, Locomotion.nextStamina(95f, dashing = false, dt = 1f, cfg = cfg), 1e-3f)
    }
```
(Leave the `keyboardDirection`/`isDashing` tests unchanged.)

- [ ] **Step 3: MODIFY `combat/MeleeResolve.kt`** — take `PlayerConfig`:
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.config.PlayerConfig

data class MeleeOutcome(val dmg: Float, val slashDmg: Float, val isFist: Boolean)

object MeleeResolve {
    fun resolve(staRatio: Float, cfg: PlayerConfig): MeleeOutcome = when {
        staRatio <= cfg.meleeStaSwordMin -> MeleeOutcome(cfg.fistDmg, 0f, true)
        staRatio < cfg.meleeStaWeakBelow -> MeleeOutcome(
            cfg.meleeDmg * cfg.meleeWeakMul, cfg.meleeSlashDmg * cfg.meleeWeakMul, false,
        )
        else -> MeleeOutcome(cfg.meleeDmg, cfg.meleeSlashDmg, false)
    }
}
```

- [ ] **Step 4: UPDATE `combat/MeleeResolveTest.kt`** — add `import io.github.panda17tk.arpg.config.PlayerConfig`, a `private val cfg = PlayerConfig()`, and pass `cfg = cfg` to each `resolve(...)` call (expected numbers unchanged: 22/8, 13.2/4.8, fist 8/0).

- [ ] **Step 5: MODIFY `combat/Explosion.kt`** — `applyWallDamage` takes `PlayerConfig` (radius + wall dmg from config); `falloff` stays generic.
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.config.PlayerConfig
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.map.Tiles
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor
import kotlin.math.hypot

object Explosion {
    fun falloff(dist: Float, radius: Float): Float = (1f - dist / radius).coerceIn(0f, 1f)

    fun applyWallDamage(map: TileMap, x: Float, y: Float, cfg: PlayerConfig) {
        val r = cfg.explodeRadius
        val tx0 = maxOf(1, floor((x - r) / Tuning.TILE).toInt())
        val ty0 = maxOf(1, floor((y - r) / Tuning.TILE).toInt())
        val tx1 = minOf(map.width - 2, floor((x + r) / Tuning.TILE).toInt())
        val ty1 = minOf(map.height - 2, floor((y + r) / Tuning.TILE).toInt())
        for (ty in ty0..ty1) for (tx in tx0..tx1) {
            if (map.tileAt(tx, ty) != Tile.WALL) continue
            val cx = tx * Tuning.TILE + Tuning.TILE / 2f
            val cy = ty * Tuning.TILE + Tuning.TILE / 2f
            val d = hypot(cx - x, cy - y)
            if (d <= r) Tiles.damageTile(map, tx, ty, cfg.explodeWallDmg * (1f - d / r))
        }
    }
}
```

- [ ] **Step 6: UPDATE `combat/ExplosionTest.kt`** — pass `PlayerConfig()` to `applyWallDamage`. Add `import io.github.panda17tk.arpg.config.PlayerConfig`; change the call to `Explosion.applyWallDamage(m, wx * Tuning.TILE + 16f, wy * Tuning.TILE + 16f, PlayerConfig())`. (Falloff test unchanged.)

- [ ] **Step 7: Run, verify PASS** (the refactored tests still pin the same numbers)
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.sim.LocomotionTest" --tests "io.github.panda17tk.arpg.combat.MeleeResolveTest" --tests "io.github.panda17tk.arpg.combat.ExplosionTest"`
Expected: all pass. (The code won't fully compile until Task 5 updates the callers; if `:core:test` fails to compile due to call-site errors in systems, proceed to Task 5 and run the suite there — OR temporarily expect compile errors only in `ecs/systems`. To keep this task green in isolation, do Task 5's system edits before running the full suite.)

- [ ] **Step 8: Commit** (with Task 5, since callers must update together — see Task 5 Step 6)

---

## Task 5: Wire GameConfig into the world + systems; trim Tuning

**Files:** MODIFY `ecs/world/WorldFactory.kt`, `ecs/systems/MovementSystem.kt`, `MeleeSystem.kt`, `FireSystem.kt`, `ReloadSystem.kt`, `ProjectileSystem.kt`, `sim/Tuning.kt`.

- [ ] **Step 1: `ecs/world/WorldFactory.kt`** — accept/inject a `GameConfig`, build the arsenal from `config.weapons`, set player stamina/HP from `config.player`. Change the signature to `create(input: InputState, config: GameConfig = GameConfig(), seed: Long = 1L): GameWorld`. In `injectables { }` add `add(config)`. Build the arsenal from config:
```kotlin
            it += Arsenal(config.weapons.map { d -> WeaponRuntime(d, d.magSize ?: 0) })
            it += Stamina(config.player.staMax, config.player.staMax)
```
(Keep `Ammo()`, `Cooldowns()`, `Body(...)`, `Materials()`. Import `GameConfig`.)

- [ ] **Step 2: `ecs/systems/MovementSystem.kt`** — inject `GameConfig`, pass `config.player` to `Locomotion`:
```kotlin
    private val config: io.github.panda17tk.arpg.config.GameConfig = world.inject()
    // ... in onTickEntity:
    val spd = Locomotion.speed(dashing, config.player)
    // ...
    s.value = Locomotion.nextStamina(s.value, dashing, dt, config.player)
```

- [ ] **Step 3: `ecs/systems/MeleeSystem.kt`** — inject `GameConfig`; use `config.player` for resolve and `meleeCd`:
```kotlin
    private val config: io.github.panda17tk.arpg.config.GameConfig = world.inject()
    // ...
    cd.melee = config.player.meleeCd
    val outcome = MeleeResolve.resolve(if (s.max > 0f) s.value / s.max else 1f, config.player)
```

- [ ] **Step 4: `ecs/systems/FireSystem.kt`** — inject `GameConfig`; use `config.player` for `bulletSpeed`/`bulletLife`/`grenadeSpeed`/`grenadeFuse` (replace the `Tuning.BULLET_SPEED` etc. references):
```kotlin
    private val config: io.github.panda17tk.arpg.config.GameConfig = world.inject()
    // bullets:  cos(a) * config.player.bulletSpeed ;  Bullet(vx, vy, config.player.bulletLife, def.dmg)
    // grenade:  dirX * config.player.grenadeSpeed ;   Grenade(..., config.player.grenadeFuse)
```
(`Tuning.MUZZLE_OFFSET` stays — it is an engine offset, not balance.)

- [ ] **Step 5: `ecs/systems/ReloadSystem.kt`** and `ecs/systems/ProjectileSystem.kt`:
  - `ReloadSystem`: inject `GameConfig`; replace `Tuning.AUTO_RELOAD_DELAY` with `config.player.autoReloadDelay`.
  - `ProjectileSystem`: inject `GameConfig`; replace `Explosion.applyWallDamage(map, t.x, t.y)` with `Explosion.applyWallDamage(map, t.x, t.y, config.player)`.

- [ ] **Step 6: MODIFY `sim/Tuning.kt`** — REMOVE the player-balance constants that moved to `PlayerConfig`: `BASE_SPEED, SPEED_MUL, DASH_MUL, HP_MAX, STA_MAX, STA_DRAIN, STA_REGEN, MELEE_DMG, MELEE_REACH, MELEE_CD, MELEE_SLASH_DMG, MELEE_STA_WEAK_BELOW, MELEE_STA_SWORD_MIN, MELEE_WEAK_MUL, FIST_DMG, BULLET_SPEED, BULLET_LIFE, GRENADE_SPEED, GRENADE_FUSE, AUTO_RELOAD_DELAY, EXPLODE_RADIUS, EXPLODE_DMG, EXPLODE_SELF_DMG, EXPLODE_WALL_DMG`. KEEP engine constants: `APP_TITLE`? (that's in Constants) — keep in Tuning: `PLAYER_RADIUS, PLAYER_HALF, TILE, VIEW_W, VIEW_H, CAM_LOOK_AHEAD, MUZZLE_OFFSET, MELEE_WALL_OFFSET, PLACED_WALL_HP, START_MATERIALS, START_AMMO9, START_AMMO12, START_AMMO_BEAM, START_AMMO_NADE`. After removing, search the codebase for any remaining `Tuning.<removedName>` reference and fix it. NOTE: besides the systems above, the grep also catches `ecs/components/Stamina.kt`, whose `value`/`max` default to `Tuning.STA_MAX` — change those two defaults to the `100f` literal and drop the now-unused `import ...Tuning` (`WorldFactory` already sets the real stamina from `config.player.staMax`).
Run: `cd "V:/src/demo0902" && grep -rn "Tuning\.\(BASE_SPEED\|SPEED_MUL\|DASH_MUL\|STA_\|MELEE_DMG\|MELEE_REACH\|MELEE_CD\|MELEE_SLASH\|MELEE_STA\|MELEE_WEAK\|FIST_DMG\|BULLET_\|GRENADE_\|AUTO_RELOAD\|EXPLODE_\|HP_MAX\)" core/src/main` — expected: no matches after the edits.

- [ ] **Step 7: Run the full suite, verify PASS** (Tasks 4 + 5 together compile and pass)
Run: `cd "V:/src/demo0902" && ./gradlew :core:test`
Expected: all green (refactored tests pin the same numbers; systems read from config).

- [ ] **Step 8: Commit (Tasks 4 + 5 together — resolvers + callers + Tuning trim)**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/sim/Locomotion.kt core/src/main/kotlin/io/github/panda17tk/arpg/combat/MeleeResolve.kt core/src/main/kotlin/io/github/panda17tk/arpg/combat/Explosion.kt core/src/main/kotlin/io/github/panda17tk/arpg/sim/Tuning.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/ core/src/test/kotlin/io/github/panda17tk/arpg/sim/LocomotionTest.kt core/src/test/kotlin/io/github/panda17tk/arpg/combat/MeleeResolveTest.kt core/src/test/kotlin/io/github/panda17tk/arpg/combat/ExplosionTest.kt && git commit -m "refactor(core): make player/combat resolvers config-driven via GameConfig"
```

---

## Task 6: Load config at boot + verify config drives gameplay (TDD + manual)

**Files:** MODIFY `screens/GameScreen.kt`; Test `ecs/world/WorldConfigTest.kt`.

- [ ] **Step 1: MODIFY `screens/GameScreen.kt`** — create a `ConfigStore`, load any on-disk override at `show()`, and pass its `config` to `WorldFactory.create`. Add fields/imports:
```kotlin
    private val configStore = io.github.panda17tk.arpg.config.ConfigStore()
    // in show(), BEFORE WorldFactory.create:
        configStore.loadFromDisk()
        gw = WorldFactory.create(input, configStore.config)
```
(Replace the existing `gw = WorldFactory.create(input)` call.)

- [ ] **Step 2: Write `ecs/world/WorldConfigTest.kt`** (proves config actually drives gameplay)
```kotlin
package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.PlayerConfig
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldConfigTest {
    @Test fun `higher baseSpeed in config makes the player move farther`() {
        fun distAfterOneTick(baseSpeed: Float): Float {
            val input = InputState().apply { right = true }
            val cfg = GameConfig(player = PlayerConfig(baseSpeed = baseSpeed))
            val gw = WorldFactory.create(input, cfg, seed = 1L)
            val x0 = with(gw.world) { gw.player[Transform].x }
            gw.world.update(1f / 60f)
            val x1 = with(gw.world) { gw.player[Transform].x }
            return x1 - x0
        }
        val slow = distAfterOneTick(110f)
        val fast = distAfterOneTick(330f)
        assertTrue(fast > slow * 2.5f, "3x baseSpeed should move ~3x farther: slow=$slow fast=$fast")
    }
}
```

- [ ] **Step 3: Build gates**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test :desktop:build`
Expected: all green (incl. `WorldConfigTest`), desktop compiles. Do NOT run `:desktop:run`.

- [ ] **Step 4: Android target still builds**
Run: `cd "V:/src/demo0902" && ./gradlew :android:assembleDebug`
Expected: BUILD SUCCESSFUL; APK produced.

- [ ] **Step 5: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/screens/GameScreen.kt core/src/test/kotlin/io/github/panda17tk/arpg/ecs/world/WorldConfigTest.kt && git commit -m "feat(core): load config at boot; config drives gameplay (TDD)"
```

- [ ] **Step 6 (MANUAL — human, optional): override test**
Create `V:/src/demo0902/config.json` (or the desktop run's local dir) with `{"player":{"baseSpeed":300}}`, run `./gradlew :desktop:run`, and confirm the player moves noticeably faster — then delete the file to restore defaults. (Confirms the on-disk override path works end-to-end.)

---

## Self-Review

**1. Spec coverage (Phase 4 row):** `GameConfig`+`ConfigStore` (default + override) → Tasks 2–3. weapons config-driven → Task 5 Step 1 (`Arsenal` from `config.weapons`). player config-driven → Tasks 4–5 (resolvers take `PlayerConfig`, systems inject `GameConfig`). export/import → `ConfigStore`/`ConfigCodec`. ✓ Out-of-scope (enemies/waves/drops/upgrades sections, dev-editor UI) explicitly deferred.

**2. Placeholder scan:** No TBD/TODO. Task 4 Step 7 notes the inter-task compile dependency (resolvers + callers change together) and directs committing Tasks 4+5 together — that's sequencing guidance, not a placeholder. Manual override test is an explicit optional human step. ✓

**3. Type/name consistency:** `PlayerConfig` field names used identically in `PlayerConfig`, `Locomotion`, `MeleeResolve`, `Explosion`, and the systems. `GameConfig(player, weapons)` consistent. `ConfigCodec.toJson/fromJson` ↔ `ConfigStore.import/export`. `WorldFactory.create(input, config, seed)` matches all call sites (GameScreen + tests). Removed `Tuning.*` balance constants are grep-verified gone (Task 5 Step 6). ✓

**Risks flagged:** (a) `GameConfig` is injected by type and `Rng`/`InputState`/`TileMap`/`FlowField` are already distinct types — no ambiguity. (b) `Stamina` default changes from `Tuning.STA_MAX` to `config.player.staMax` (same value 100) — Stamina component's own default still references the old constant; since `Tuning.STA_MAX` is being removed, update `Stamina.kt`'s default to `100f` literal (or to a `PlayerConfig().staMax`). FLAG: Task 5 must also fix `ecs/components/Stamina.kt` which currently defaults `value/max` to `Tuning.STA_MAX` — change those defaults to `100f` and let `WorldFactory` set the real value from config. (c) kotlinx.serialization version 1.7.3 must match Kotlin 2.0.21 — if it fails to resolve, use the matching 1.7.x.

---

## Execution Handoff

**Plan complete.** Options: **Subagent-Driven (recommended)** — group as (Tasks 1–3 config layer) / (Tasks 4–6 refactor + wiring); or **Inline**. After Phase 4, **Phase 5** (enemies, AI, the 16-attack registry, mid-boss/boss) is next — and it adds `enemies` to `GameConfig`, wiring `hurtMob` into the combat hooks left in Phase 3.
