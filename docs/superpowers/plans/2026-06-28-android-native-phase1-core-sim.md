# Phase 1 — Core Simulation Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A controllable player that moves and dashes on screen at a stable frame rate, built on the ECS + fixed-timestep foundation the rest of the game will use.

**Architecture:** Gameplay math lives in pure, unit-tested functions (`sim/Locomotion`). A Fleks ECS world holds the player entity; thin systems (`SnapshotSystem`, `MovementSystem`) call the pure logic. `GameScreen` runs a fixed-timestep accumulator loop with render interpolation and a smooth-follow camera, exactly mirroring the legacy `main.js` loop. Input is PC keyboard for now (`input/InputState` + `KeyboardInput`); touch analog arrives in Phase 8.

**Tech Stack:** Kotlin 2.0.21, libGDX 1.13.1 (OrthographicCamera/Viewport/ShapeRenderer), Fleks 2.8 (ECS), JUnit 5. Builds on Phase 0 (modules `core`/`desktop`/`android` already exist and build).

---

## Source-of-truth values (ported from legacy `legacy-web-v1.1.1`)

From `core/config.js` (`CONFIG.player`) and `systems/combat.js`:

| Value | Legacy | Notes |
|---|---|---|
| baseSpeed | 110 | px/s |
| speedMul | 1.2 | global move multiplier |
| dashMul | 2 | dash speed multiplier |
| staMax | 100 | |
| staDrain | 35 | per second while dashing |
| staRegen | 22 | per second while not dashing |
| hpMax | 100 | (carried as a constant; HP system is Phase 3) |
| player size | w=h=22 | → render radius 11 |
| TILE | 32 | |
| FIXED_DT | 1/60 | already in `Constants` |
| MAX_DT | 0.05 | already in `Constants` |
| MAX_STEPS | 5 | already in `Constants` |

Movement rule (legacy `updateCombat`): keyboard is 8-direction normalized at full speed; `dash = shiftHeld && moving && sta>0`; `spd = baseSpeed*speedMul*(dash?dashMul:1)` (buffs/mods are 1 in Phase 1); `sta = dash ? max(0, sta-staDrain*dt) : min(staMax, sta+staRegen*dt)`; facing follows move direction. **World is y-down** (up = −y), matching all legacy data we port later.

> **Fleks 2.8 note:** runtime classes are in package `com.github.quillraven.fleks` (the Maven group is `io.github.quillraven.fleks` but the package kept `com.github`). The DSL below (`configureWorld`, `Component<T>`+`ComponentType<T>` companion, `IteratingSystem(family{...})`, `onTickEntity`, `world.inject()`, `world.entity{ it += ... }`, `with(world){ entity[Type] }`) targets Fleks 2.x. If a symbol doesn't resolve, verify against the Fleks wiki (github.com/Quillraven/Fleks) and adjust only the thin system/world wrappers — never the pure `Locomotion` logic.

## File structure (this phase)

```
core/src/main/kotlin/io/github/panda17tk/arpg/
├─ math/Vec2.kt                    # immutable 2D float vector (TDD)
├─ math/Rng.kt                     # seedable deterministic RNG (TDD)
├─ sim/Tuning.kt                   # gameplay tuning constants (Phase 4 migrates to ConfigStore)
├─ sim/Locomotion.kt              # pure movement/dash/stamina logic (TDD)
├─ input/InputState.kt            # unified input snapshot (PC booleans now)
├─ input/KeyboardInput.kt         # libGDX keyboard → InputState (PC)
├─ ecs/components/Transform.kt    # x,y + prevX,prevY (interpolation)
├─ ecs/components/PlayerTag.kt    # marker
├─ ecs/components/Facing.kt       # x,y unit facing
├─ ecs/components/Stamina.kt      # value,max
├─ ecs/systems/SnapshotSystem.kt  # prev = cur (runs first)
├─ ecs/systems/MovementSystem.kt  # thin adapter over Locomotion
├─ ecs/world/GameWorld.kt         # World + player Entity holder
├─ ecs/world/WorldFactory.kt      # builds world, spawns player
└─ screens/GameScreen.kt          # REWRITE: fixed-step loop, camera, render
core/src/test/kotlin/io/github/panda17tk/arpg/
├─ math/Vec2Test.kt
├─ math/RngTest.kt
├─ sim/LocomotionTest.kt
└─ ecs/world/WorldSmokeTest.kt
```

---

## Task 1: `Vec2` immutable vector (TDD)

**Files:** Create `core/src/main/kotlin/io/github/panda17tk/arpg/math/Vec2.kt`; Test `core/src/test/kotlin/io/github/panda17tk/arpg/math/Vec2Test.kt`

- [ ] **Step 1: Write failing test `Vec2Test.kt`**
```kotlin
package io.github.panda17tk.arpg.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Vec2Test {
    @Test fun `length of 3-4 vector is 5`() {
        assertEquals(5f, Vec2(3f, 4f).length(), 1e-5f)
    }
    @Test fun `normalized vector has unit length`() {
        assertEquals(1f, Vec2(3f, 4f).normalized().length(), 1e-5f)
    }
    @Test fun `normalizing zero vector returns zero`() {
        val n = Vec2(0f, 0f).normalized()
        assertEquals(0f, n.x, 1e-5f); assertEquals(0f, n.y, 1e-5f)
    }
    @Test fun `scale multiplies both components`() {
        val v = Vec2(2f, -3f) * 2f
        assertEquals(4f, v.x, 1e-5f); assertEquals(-6f, v.y, 1e-5f)
    }
}
```

- [ ] **Step 2: Run, verify FAIL**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.math.Vec2Test"`
Expected: FAIL — `Vec2` unresolved.

- [ ] **Step 3: Implement `Vec2.kt`**
```kotlin
package io.github.panda17tk.arpg.math

import kotlin.math.sqrt

/** Immutable 2D float vector. World convention is y-down. */
data class Vec2(val x: Float, val y: Float) {
    fun length(): Float = sqrt(x * x + y * y)
    fun normalized(): Vec2 {
        val len = length()
        return if (len == 0f) ZERO else Vec2(x / len, y / len)
    }
    operator fun plus(o: Vec2): Vec2 = Vec2(x + o.x, y + o.y)
    operator fun times(s: Float): Vec2 = Vec2(x * s, y * s)

    companion object { val ZERO = Vec2(0f, 0f) }
}
```

- [ ] **Step 4: Run, verify PASS**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.math.Vec2Test"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/math/Vec2.kt core/src/test/kotlin/io/github/panda17tk/arpg/math/Vec2Test.kt && git commit -m "feat(core): add immutable Vec2 with tests"
```

---

## Task 2: `Rng` seedable deterministic RNG (TDD)

**Files:** Create `core/src/main/kotlin/io/github/panda17tk/arpg/math/Rng.kt`; Test `core/src/test/kotlin/io/github/panda17tk/arpg/math/RngTest.kt`

- [ ] **Step 1: Write failing test `RngTest.kt`**
```kotlin
package io.github.panda17tk.arpg.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RngTest {
    @Test fun `same seed produces same sequence`() {
        val a = Rng(42L); val b = Rng(42L)
        repeat(100) { assertEquals(a.nextFloat(), b.nextFloat(), 0f) }
    }
    @Test fun `different seeds diverge`() {
        val a = Rng(1L); val b = Rng(2L)
        val same = (0 until 100).count { a.nextFloat() == b.nextFloat() }
        assertTrue(same < 100, "sequences should differ")
    }
    @Test fun `nextFloat is in 0 until 1`() {
        val r = Rng(7L)
        repeat(1000) { val v = r.nextFloat(); assertTrue(v >= 0f && v < 1f, "out of range: $v") }
    }
    @Test fun `range returns within bounds`() {
        val r = Rng(7L)
        repeat(1000) { val v = r.range(5f, 10f); assertTrue(v >= 5f && v < 10f, "out of range: $v") }
    }
}
```

- [ ] **Step 2: Run, verify FAIL**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.math.RngTest"`
Expected: FAIL — `Rng` unresolved.

- [ ] **Step 3: Implement `Rng.kt`**
```kotlin
package io.github.panda17tk.arpg.math

import kotlin.random.Random

/**
 * Seedable deterministic RNG used by all gameplay randomness so runs/saves
 * reproduce (spec §12). Wraps kotlin.random.Random for a stable algorithm.
 */
class Rng(seed: Long) {
    private val random = Random(seed)
    fun nextFloat(): Float = random.nextFloat()           // [0,1)
    fun nextInt(untilExclusive: Int): Int = random.nextInt(untilExclusive)
    fun range(min: Float, max: Float): Float = min + (max - min) * random.nextFloat()
}
```

- [ ] **Step 4: Run, verify PASS**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.math.RngTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/math/Rng.kt core/src/test/kotlin/io/github/panda17tk/arpg/math/RngTest.kt && git commit -m "feat(core): add seedable deterministic Rng with tests"
```

---

## Task 3: `Tuning` + `Locomotion` pure movement logic (TDD)

**Files:** Create `core/src/main/kotlin/io/github/panda17tk/arpg/sim/Tuning.kt`, `core/src/main/kotlin/io/github/panda17tk/arpg/sim/Locomotion.kt`; Test `core/src/test/kotlin/io/github/panda17tk/arpg/sim/LocomotionTest.kt`

- [ ] **Step 1: Create `Tuning.kt`** (gameplay constants; Phase 4 migrates these to `ConfigStore`)
```kotlin
package io.github.panda17tk.arpg.sim

/** Gameplay tuning ported from legacy CONFIG.player / combat.js. */
object Tuning {
    const val BASE_SPEED = 110f
    const val SPEED_MUL = 1.2f
    const val DASH_MUL = 2f
    const val HP_MAX = 100f
    const val STA_MAX = 100f
    const val STA_DRAIN = 35f   // stamina/sec while dashing
    const val STA_REGEN = 22f   // stamina/sec while not dashing
    const val PLAYER_RADIUS = 11f

    const val TILE = 32f
    const val VIEW_W = 800f
    const val VIEW_H = 480f
    const val CAM_LOOK_AHEAD = 36f
}
```

- [ ] **Step 2: Write failing test `LocomotionTest.kt`**
```kotlin
package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class LocomotionTest {
    @Test fun `no keys means not moving`() {
        val mv = Locomotion.keyboardDirection(left = false, right = false, up = false, down = false)
        assertFalse(mv.isMoving)
        assertEquals(0f, mv.speedScale, 0f)
    }
    @Test fun `right only moves along plus x at full scale`() {
        val mv = Locomotion.keyboardDirection(false, true, false, false)
        assertEquals(1f, mv.dirX, 1e-5f); assertEquals(0f, mv.dirY, 1e-5f)
        assertEquals(1f, mv.speedScale, 0f); assertTrue(mv.isMoving)
    }
    @Test fun `up-right is normalized with y negative (y-down world)`() {
        val mv = Locomotion.keyboardDirection(false, true, true, false)
        val inv = 1f / sqrt(2f)
        assertEquals(inv, mv.dirX, 1e-5f); assertEquals(-inv, mv.dirY, 1e-5f)
    }
    @Test fun `dash requires held, moving, and stamina`() {
        assertTrue(Locomotion.isDashing(dashHeld = true, moving = true, sta = 50f))
        assertFalse(Locomotion.isDashing(dashHeld = true, moving = true, sta = 0f))
        assertFalse(Locomotion.isDashing(dashHeld = true, moving = false, sta = 50f))
        assertFalse(Locomotion.isDashing(dashHeld = false, moving = true, sta = 50f))
    }
    @Test fun `speed is 132 normally and 264 dashing`() {
        assertEquals(132f, Locomotion.speed(dashing = false), 1e-3f)
        assertEquals(264f, Locomotion.speed(dashing = true), 1e-3f)
    }
    @Test fun `stamina drains while dashing and clamps at zero`() {
        assertEquals(65f, Locomotion.nextStamina(100f, dashing = true, dt = 1f), 1e-3f)
        assertEquals(0f, Locomotion.nextStamina(10f, dashing = true, dt = 1f), 1e-3f)
    }
    @Test fun `stamina regens while not dashing and clamps at max`() {
        assertEquals(72f, Locomotion.nextStamina(50f, dashing = false, dt = 1f), 1e-3f)
        assertEquals(100f, Locomotion.nextStamina(95f, dashing = false, dt = 1f), 1e-3f)
    }
}
```

- [ ] **Step 3: Run, verify FAIL**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.sim.LocomotionTest"`
Expected: FAIL — `Locomotion` unresolved.

- [ ] **Step 4: Implement `Locomotion.kt`**
```kotlin
package io.github.panda17tk.arpg.sim

import kotlin.math.sqrt

/** Resolved movement direction + analog scale (0 = not moving). */
data class MoveVector(val dirX: Float, val dirY: Float, val speedScale: Float) {
    val isMoving: Boolean get() = speedScale > 0f
    companion object { val NONE = MoveVector(0f, 0f, 0f) }
}

/**
 * Pure movement/dash/stamina logic, ported 1:1 from legacy combat.js updateCombat.
 * Kept free of libGDX/Fleks so it is fully unit-testable and deterministic.
 */
object Locomotion {
    /** Keyboard 8-direction, normalized, full speed. World is y-down (up = -1). */
    fun keyboardDirection(left: Boolean, right: Boolean, up: Boolean, down: Boolean): MoveVector {
        val ax = (if (left) -1f else 0f) + (if (right) 1f else 0f)
        val ay = (if (up) -1f else 0f) + (if (down) 1f else 0f)
        if (ax == 0f && ay == 0f) return MoveVector.NONE
        val len = sqrt(ax * ax + ay * ay)
        return MoveVector(ax / len, ay / len, 1f)
    }

    fun isDashing(dashHeld: Boolean, moving: Boolean, sta: Float): Boolean =
        dashHeld && moving && sta > 0f

    fun speed(dashing: Boolean): Float =
        Tuning.BASE_SPEED * Tuning.SPEED_MUL * (if (dashing) Tuning.DASH_MUL else 1f)

    fun nextStamina(sta: Float, dashing: Boolean, dt: Float): Float =
        if (dashing) (sta - Tuning.STA_DRAIN * dt).coerceAtLeast(0f)
        else (sta + Tuning.STA_REGEN * dt).coerceAtMost(Tuning.STA_MAX)
}
```

- [ ] **Step 5: Run, verify PASS**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.sim.LocomotionTest"`
Expected: PASS (7 tests).

- [ ] **Step 6: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/sim/ core/src/test/kotlin/io/github/panda17tk/arpg/sim/ && git commit -m "feat(core): port player movement/dash/stamina as pure Locomotion logic (TDD)"
```

---

## Task 4: ECS components + `InputState`

**Files:** Create `ecs/components/Transform.kt`, `ecs/components/PlayerTag.kt`, `ecs/components/Facing.kt`, `ecs/components/Stamina.kt`, `input/InputState.kt` (all under `core/src/main/kotlin/io/github/panda17tk/arpg/`). No new test (exercised by Task 5's world smoke test).

- [ ] **Step 1: `ecs/components/Transform.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Position plus previous-step position for render interpolation. */
class Transform(
    var x: Float = 0f,
    var y: Float = 0f,
    var prevX: Float = 0f,
    var prevY: Float = 0f,
) : Component<Transform> {
    override fun type() = Transform
    companion object : ComponentType<Transform>()
}
```

- [ ] **Step 2: `ecs/components/PlayerTag.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Marker for the player-controlled entity. */
class PlayerTag : Component<PlayerTag> {
    override fun type() = PlayerTag
    companion object : ComponentType<PlayerTag>()
}
```

- [ ] **Step 3: `ecs/components/Facing.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Unit facing direction (defaults to +x). */
class Facing(var x: Float = 1f, var y: Float = 0f) : Component<Facing> {
    override fun type() = Facing
    companion object : ComponentType<Facing>()
}
```

- [ ] **Step 4: `ecs/components/Stamina.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.sim.Tuning

class Stamina(var value: Float = Tuning.STA_MAX, var max: Float = Tuning.STA_MAX) : Component<Stamina> {
    override fun type() = Stamina
    companion object : ComponentType<Stamina>()
}
```

- [ ] **Step 5: `input/InputState.kt`**
```kotlin
package io.github.panda17tk.arpg.input

/**
 * Unified per-frame input snapshot. PC keyboard booleans for now; Phase 8 adds
 * analog move/aim and autoFire for on-screen touch sticks.
 */
class InputState {
    var left = false
    var right = false
    var up = false
    var down = false
    var dash = false
}
```

- [ ] **Step 6: Verify it compiles**
Run: `cd "V:/src/demo0902" && ./gradlew :core:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/ecs/components/ core/src/main/kotlin/io/github/panda17tk/arpg/input/InputState.kt && git commit -m "feat(core): add Fleks components (Transform/PlayerTag/Facing/Stamina) and InputState"
```

---

## Task 5: Fleks world + systems (with world smoke test)

**Files:** Create `ecs/systems/SnapshotSystem.kt`, `ecs/systems/MovementSystem.kt`, `ecs/world/GameWorld.kt`, `ecs/world/WorldFactory.kt`; Test `ecs/world/WorldSmokeTest.kt`. (See the Fleks 2.8 note at the top — keep these wrappers thin; if a Fleks symbol differs, fix it here only.)

- [ ] **Step 1: `ecs/systems/SnapshotSystem.kt`** (runs first each tick — copies current → prev for interpolation)
```kotlin
package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Transform

class SnapshotSystem : IteratingSystem(family { all(Transform) }) {
    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        t.prevX = t.x
        t.prevY = t.y
    }
}
```

- [ ] **Step 2: `ecs/systems/MovementSystem.kt`** (thin adapter over `Locomotion`)
```kotlin
package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.Locomotion

class MovementSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina) }) {
    private val input: InputState = world.inject()

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        val f = entity[Facing]
        val s = entity[Stamina]
        val dt = deltaTime

        val mv = Locomotion.keyboardDirection(input.left, input.right, input.up, input.down)
        val dashing = Locomotion.isDashing(input.dash, mv.isMoving, s.value)
        val spd = Locomotion.speed(dashing)

        t.x += mv.dirX * spd * mv.speedScale * dt
        t.y += mv.dirY * spd * mv.speedScale * dt
        if (mv.isMoving) { f.x = mv.dirX; f.y = mv.dirY }
        s.value = Locomotion.nextStamina(s.value, dashing, dt)
    }
}
```

- [ ] **Step 3: `ecs/world/GameWorld.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/** Bundles the ECS world with the player entity for convenient rendering access. */
class GameWorld(val world: World, val player: Entity)
```

- [ ] **Step 4: `ecs/world/WorldFactory.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.configureWorld
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.systems.MovementSystem
import io.github.panda17tk.arpg.ecs.systems.SnapshotSystem
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.Tuning

object WorldFactory {
    fun create(input: InputState): GameWorld {
        val world = configureWorld {
            injectables { add(input) }
            systems {
                add(SnapshotSystem())
                add(MovementSystem())
            }
        }
        val player = world.entity {
            it += Transform(x = Tuning.VIEW_W / 2f, y = Tuning.VIEW_H / 2f)
            it += PlayerTag()
            it += Facing()
            it += Stamina()
        }
        return GameWorld(world, player)
    }
}
```

- [ ] **Step 5: Write the world smoke test `WorldSmokeTest.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldSmokeTest {
    @Test fun `player moves right when right is held`() {
        val input = InputState().apply { right = true }
        val gw = WorldFactory.create(input)
        val startX = with(gw.world) { gw.player[Transform].x }
        gw.world.update(0.1f)
        val endX = with(gw.world) { gw.player[Transform].x }
        assertTrue(endX > startX, "expected player.x to increase, was $startX -> $endX")
    }

    @Test fun `snapshot records previous position before movement`() {
        val input = InputState().apply { right = true }
        val gw = WorldFactory.create(input)
        val startX = with(gw.world) { gw.player[Transform].x }
        gw.world.update(0.1f)
        with(gw.world) {
            // prev should equal the position at the start of the tick (before this step's move)
            assertEquals(startX, gw.player[Transform].prevX, 1e-4f)
            assertTrue(gw.player[Transform].x > gw.player[Transform].prevX)
        }
    }
}
```

- [ ] **Step 6: Run, verify PASS**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.ecs.world.WorldSmokeTest"`
Expected: PASS (2 tests). If a Fleks symbol fails to resolve, fix the import/DSL per the Fleks 2.8 note (thin wrappers only), then re-run.

- [ ] **Step 7: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/ecs/systems/ core/src/main/kotlin/io/github/panda17tk/arpg/ecs/world/ core/src/test/kotlin/io/github/panda17tk/arpg/ecs/ && git commit -m "feat(core): Fleks world with snapshot+movement systems and smoke test"
```

---

## Task 6: PC input, fixed-step loop, camera, and `GameScreen` rewrite

**Files:** Create `input/KeyboardInput.kt`; Rewrite `screens/GameScreen.kt`. Verified by build + a manual desktop run (subagents cannot drive a GUI window).

- [ ] **Step 1: `input/KeyboardInput.kt`**
```kotlin
package io.github.panda17tk.arpg.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys

/** Polls the libGDX keyboard into [InputState] each frame (PC). Touch sticks arrive in Phase 8. */
object KeyboardInput {
    fun poll(state: InputState) {
        val k = Gdx.input
        state.left = k.isKeyPressed(Keys.A) || k.isKeyPressed(Keys.LEFT)
        state.right = k.isKeyPressed(Keys.D) || k.isKeyPressed(Keys.RIGHT)
        state.up = k.isKeyPressed(Keys.W) || k.isKeyPressed(Keys.UP)
        state.down = k.isKeyPressed(Keys.S) || k.isKeyPressed(Keys.DOWN)
        state.dash = k.isKeyPressed(Keys.SHIFT_LEFT) || k.isKeyPressed(Keys.SHIFT_RIGHT)
    }
}
```

- [ ] **Step 2: Rewrite `screens/GameScreen.kt`** (replace the Phase 0 placeholder entirely)
```kotlin
package io.github.panda17tk.arpg.screens

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import io.github.panda17tk.arpg.core.Constants
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.input.KeyboardInput
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.pow

/**
 * Fixed-timestep simulation + render interpolation, porting the legacy main.js loop.
 * World is y-down, so the world camera is set up y-down to match.
 */
class GameScreen : ScreenAdapter() {
    private val input = InputState()
    private val gw = WorldFactory.create(input)

    private lateinit var shapes: ShapeRenderer
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var camera: OrthographicCamera
    private lateinit var worldViewport: FitViewport
    private lateinit var hudViewport: ScreenViewport

    private var accumulator = 0f
    private var camX = Tuning.VIEW_W / 2f
    private var camY = Tuning.VIEW_H / 2f
    private var camInit = false

    override fun show() {
        shapes = ShapeRenderer()
        batch = SpriteBatch()
        font = BitmapFont()
        camera = OrthographicCamera().apply { setToOrtho(true, Tuning.VIEW_W, Tuning.VIEW_H) } // y-down
        worldViewport = FitViewport(Tuning.VIEW_W, Tuning.VIEW_H, camera)
        hudViewport = ScreenViewport()
    }

    override fun render(delta: Float) {
        KeyboardInput.poll(input)
        step(delta)
        val alpha = (accumulator / Constants.FIXED_DT).coerceIn(0f, 1f)

        // interpolated player position
        val px: Float; val py: Float; val fx: Float; val fy: Float; val sta: Float
        with(gw.world) {
            val t = gw.player[Transform]; val f = gw.player[Facing]; val s = gw.player[Stamina]
            px = t.prevX + (t.x - t.prevX) * alpha
            py = t.prevY + (t.y - t.prevY) * alpha
            fx = f.x; fy = f.y; sta = s.value
        }

        updateCamera(delta, px, py, fx, fy)

        ScreenUtils.clear(0.06f, 0.07f, 0.10f, 1f)

        // world
        worldViewport.apply()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color(0.16f, 0.18f, 0.24f, 1f)
        drawGrid()
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.45f, 0.85f, 0.95f, 1f)
        shapes.circle(px, py, Tuning.PLAYER_RADIUS, 24)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color.WHITE
        shapes.line(px, py, px + fx * Tuning.PLAYER_RADIUS * 1.8f, py + fy * Tuning.PLAYER_RADIUS * 1.8f)
        shapes.end()

        // HUD (screen space)
        hudViewport.apply()
        batch.projectionMatrix = hudViewport.camera.combined
        batch.begin()
        font.draw(batch, "WASD/Arrows: move   Shift: dash   STA ${sta.toInt()}/${Tuning.STA_MAX.toInt()}", 16f, 28f)
        batch.end()
        shapes.projectionMatrix = hudViewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.2f, 0.2f, 0.25f, 1f)
        shapes.rect(16f, 40f, 200f, 10f)
        shapes.color = Color(0.95f, 0.8f, 0.3f, 1f)
        shapes.rect(16f, 40f, 200f * (sta / Tuning.STA_MAX), 10f)
        shapes.end()
    }

    private fun step(delta: Float) {
        val dt = minOf(Constants.MAX_DT, delta)
        accumulator += dt
        var steps = 0
        while (accumulator >= Constants.FIXED_DT && steps < Constants.MAX_STEPS) {
            gw.world.update(Constants.FIXED_DT)
            accumulator -= Constants.FIXED_DT
            steps++
        }
        if (steps >= Constants.MAX_STEPS) accumulator = 0f
    }

    private fun updateCamera(delta: Float, px: Float, py: Float, fx: Float, fy: Float) {
        val tgX = px + fx * Tuning.CAM_LOOK_AHEAD
        val tgY = py + fy * Tuning.CAM_LOOK_AHEAD
        if (!camInit) { camX = tgX; camY = tgY; camInit = true }
        val k = 1f - 0.0001f.pow(delta) // legacy smoothing
        camX += (tgX - camX) * k
        camY += (tgY - camY) * k
        camera.position.set(camX, camY, 0f)
        camera.update()
    }

    private fun drawGrid() {
        val t = Tuning.TILE
        var gx = camX - Tuning.VIEW_W
        val endX = camX + Tuning.VIEW_W
        gx = (gx / t).toInt() * t
        while (gx < endX) { shapes.line(gx, camY - Tuning.VIEW_H, gx, camY + Tuning.VIEW_H); gx += t }
        var gy = ((camY - Tuning.VIEW_H) / t).toInt() * t
        val endY = camY + Tuning.VIEW_H
        while (gy < endY) { shapes.line(camX - Tuning.VIEW_W, gy, camX + Tuning.VIEW_W, gy); gy += t }
    }

    override fun resize(width: Int, height: Int) {
        worldViewport.update(width, height)
        hudViewport.update(width, height, true)
    }

    override fun dispose() {
        shapes.dispose()
        batch.dispose()
        font.dispose()
    }
}
```

- [ ] **Step 3: Build all targets (compile gate)**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test :desktop:build`
Expected: BUILD SUCCESSFUL; all `:core` tests pass (Vec2, Rng, Locomotion, WorldSmoke) and desktop compiles. Do NOT run `:desktop:run` (blocking GUI).

- [ ] **Step 4: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/input/KeyboardInput.kt core/src/main/kotlin/io/github/panda17tk/arpg/screens/GameScreen.kt && git commit -m "feat(core): fixed-step loop, smooth camera, player render + PC input"
```

- [ ] **Step 5 (MANUAL — human, not a subagent): Visual verification**
Run on the dev PC: `cd "V:/src/demo0902" && ./gradlew :desktop:run`
Expected: a window opens; WASD/arrows move the cyan player dot over the grid, the white facing line points where you move, Shift dashes faster while the stamina bar drains and refills. Close the window to end.

---

## Self-Review

**1. Spec coverage (Phase 1 row + spec §7):** Constants/Time(`FIXED_DT` etc. in `Constants` from Ph0)/Rng/Vec2 → Tasks 1–2 + Tuning. Fleks world → Task 5. Fixed-step + interpolation → Task 6 `step()`/`alpha`/`prev`. Camera/Viewport → Task 6 `updateCamera` + FitViewport. Movement/dash → Task 3 (logic) + Task 5 (system). Input (PC) → Task 4 `InputState` + Task 6 `KeyboardInput`. Completion "player moves, dashes, 60fps" → Task 6 Step 5 manual run. ✓

**2. Placeholder scan:** No TBD/TODO. The single manual step (Task 6 Step 5) is an explicit human action (GUI), not a placeholder. The Fleks-API "verify if it doesn't resolve" note is bounded to thin wrappers with concrete fallback. ✓

**3. Type consistency:** Package `io.github.panda17tk.arpg.*` throughout. `MoveVector(dirX,dirY,speedScale)`/`isMoving` used identically in Locomotion + tests + MovementSystem. `Tuning.*` names consistent. `Constants.FIXED_DT/MAX_DT/MAX_STEPS` reused from Phase 0. `Transform(x,y,prevX,prevY)`, `Facing(x,y)`, `Stamina(value,max)` consistent across components, systems, world, and GameScreen. `WorldFactory.create(InputState): GameWorld(world, player)` consistent. ✓

**Risks flagged:** (a) Fleks 2.8 DSL exact symbols — isolated to Task 5 wrappers with a fallback note; pure logic is Fleks-free. (b) y-down camera (`setToOrtho(true,…)`) correctness is confirmed by the manual run (Task 6 Step 5).

---

## Execution Handoff

**Plan complete.** Two execution options (same as Phase 0):

1. **Subagent-Driven (recommended)** — fresh subagent per task, spec-compliance verified per task, final consolidated review.
2. **Inline Execution** — execute in this session with checkpoints.

After Phase 1 lands, the next plan is **Phase 2** (map/tiles, flow-field/LOS/spatial grid, wall place/break) — at which point movement gains tile collision (`moveAndCollide`).
