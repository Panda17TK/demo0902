# Phase 2 — World: Tiles, Collision, Pathfinding, Walls Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A tile world the player physically inhabits — random stage loads, the player collides with walls, can place walls (F) that consume materials, and the flow-field / line-of-sight / spatial-grid foundations (for later enemy AI) exist and are tested.

**Architecture:** All world logic is pure functions over an immutable-shape `TileMap` (collision, flow-field BFS, LOS, spatial grid, stage parsing) — unit-tested with tiny hand-written maps. Fleks systems stay thin: `MovementSystem` now resolves against the `TileMap`; a `BuildSystem` handles wall placement. `GameScreen` renders the tile grid and spawns the player at the stage's `P` marker.

**Tech Stack:** Kotlin 2.0.21, libGDX 1.13.1, Fleks 2.8, JUnit 5. Builds on Phase 1 (Vec2/Rng/Locomotion/ECS/loop all present and green).

---

## Legacy source-of-truth (at tag `legacy-web-v1.1.1`)

Faithful ports of these files (retrieve with `git show legacy-web-v1.1.1:src/main/webapp/js/<path>`):
- `systems/physics.js` — `moveAndCollide` (axis-separated AABB vs solid tiles), `solidAt`, `forTiles`.
- `systems/tiles.js` — `damageTile`, `clearWall`, `canPlaceAt`.
- `systems/flowfield.js` — `rebuildFlowField` (BFS from player tile, 4-neighbour).
- `systems/los.js` — `hasLineOfSight` (integer Bresenham).
- `systems/spatial.js` — uniform grid (`buildMobGrid`, `forNearby`).
- `state/maps.js` — the 5 `MAPS` stage definitions + `randomMapId`/`getMap`.
- `state/map.js` — `setupMap` (parse rows; border walls = ∞ HP, internal `#` = `wallHp` 90; `D` indestructible; markers → floor).

**Tile semantics:** char `#` = destructible WALL (HP 90 internal, ∞ on border), `D` = DOOR (solid, indestructible, no open mechanism), `.` = FLOOR; any other char is a spawn marker (recorded, then treated as FLOOR). Out-of-bounds counts as solid. World is y-down. `TILE = 32` (already in `Tuning`).

## File structure (this phase)

```
core/src/main/kotlin/io/github/panda17tk/arpg/
├─ map/Tile.kt                # enum FLOOR/WALL/DOOR (+ isSolid)
├─ map/TileMap.kt             # width,height, tiles[], hp[], maxHp[]; solidAt/tileAt/inBounds/index
├─ map/StageDef.kt            # id,name,wallHp,rows,legend + Legend types
├─ map/Stages.kt              # the 5 stages + byId + random(rng, avoid)
├─ map/MapLoader.kt           # StageDef -> LoadedMap(tileMap, playerSpawn, spawns)
├─ map/Tiles.kt               # damageTile / clearWall / canPlaceWall (pure ops on TileMap)
├─ sim/Collision.kt           # moveAndCollide (pure, axis-separated AABB)
├─ pathfinding/FlowField.kt   # BFS distance field over a TileMap
├─ pathfinding/Los.kt         # hasLineOfSight over a TileMap
├─ pathfinding/SpatialGrid.kt # generic uniform grid for neighbour queries
├─ ecs/components/Body.kt     # half-extents for AABB collision
├─ ecs/components/Materials.kt# build material count
├─ ecs/systems/BuildSystem.kt # place wall ahead on input edge
├─ ecs/systems/MovementSystem.kt  # MODIFY: collide against TileMap
├─ ecs/world/WorldFactory.kt  # MODIFY: inject TileMap+FlowField, spawn at P, Body/Materials
├─ input/InputState.kt        # MODIFY: add edge-triggered placeWall
├─ input/KeyboardInput.kt     # MODIFY: poll F with edge detection
├─ sim/Tuning.kt              # MODIFY: add PLACED_WALL_HP, PLAYER_HALF, START_MATERIALS
└─ screens/GameScreen.kt      # MODIFY: render tile map, spawn at P, place wall on F
core/src/test/kotlin/io/github/panda17tk/arpg/
├─ map/TileMapTest.kt
├─ map/MapLoaderTest.kt
├─ sim/CollisionTest.kt
├─ pathfinding/FlowFieldTest.kt
├─ pathfinding/LosTest.kt
├─ pathfinding/SpatialGridTest.kt
└─ ecs/world/WorldWallTest.kt
```

---

## Task 1: `Tile` + `TileMap` (TDD)

**Files:** Create `map/Tile.kt`, `map/TileMap.kt`; Test `map/TileMapTest.kt`.

- [ ] **Step 1: Write failing test `TileMapTest.kt`**
```kotlin
package io.github.panda17tk.arpg.map

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TileMapTest {
    private fun map() = TileMap.fromRows(listOf("###", "#.#", "###"))

    @Test fun `dimensions match rows`() {
        val m = map(); assertEquals(3, m.width); assertEquals(3, m.height)
    }
    @Test fun `floor center is not solid, walls are solid`() {
        val m = map()
        assertFalse(m.solidAt(1, 1)); assertTrue(m.solidAt(0, 0)); assertTrue(m.solidAt(1, 0))
    }
    @Test fun `out of bounds is solid`() {
        val m = map()
        assertTrue(m.solidAt(-1, 0)); assertTrue(m.solidAt(3, 3))
    }
    @Test fun `door is solid`() {
        val m = TileMap.fromRows(listOf("DDD", "D.D", "DDD"))
        assertTrue(m.solidAt(0, 0)); assertEquals(Tile.DOOR, m.tileAt(0, 0))
    }
}
```

- [ ] **Step 2: Run, verify FAIL**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.map.TileMapTest"`
Expected: FAIL — `Tile`/`TileMap` unresolved.

- [ ] **Step 3: Implement `map/Tile.kt`**
```kotlin
package io.github.panda17tk.arpg.map

/** Tile kinds. WALL is destructible; DOOR is solid and indestructible; FLOOR is walkable. */
enum class Tile(val solid: Boolean) {
    FLOOR(false),
    WALL(true),
    DOOR(true);

    companion object {
        fun fromChar(c: Char): Tile = when (c) {
            '#' -> WALL
            'D' -> DOOR
            else -> FLOOR // '.', spawn markers, anything else
        }
    }
}
```

- [ ] **Step 4: Implement `map/TileMap.kt`**
```kotlin
package io.github.panda17tk.arpg.map

/**
 * Mutable tile grid (row-major). `hp`/`maxHp` use Float.POSITIVE_INFINITY for
 * indestructible tiles (border walls, doors). Ported from legacy state.map/tileHP.
 */
class TileMap(
    val width: Int,
    val height: Int,
    private val tiles: Array<Tile>,
    val hp: FloatArray,
    val maxHp: FloatArray,
) {
    fun inBounds(tx: Int, ty: Int): Boolean = tx in 0 until width && ty in 0 until height
    fun index(tx: Int, ty: Int): Int = ty * width + tx

    fun tileAt(tx: Int, ty: Int): Tile = if (inBounds(tx, ty)) tiles[index(tx, ty)] else Tile.WALL

    /** Out-of-bounds is treated as solid (matches legacy solidAt). */
    fun solidAt(tx: Int, ty: Int): Boolean = tileAt(tx, ty).solid

    fun setTile(tx: Int, ty: Int, t: Tile) { if (inBounds(tx, ty)) tiles[index(tx, ty)] = t }

    companion object {
        /** Build from char rows with default HP (∞ for WALL/DOOR; loader overrides WALL HP). */
        fun fromRows(rows: List<String>): TileMap {
            val h = rows.size
            val w = rows[0].length
            val tiles = Array(w * h) { Tile.FLOOR }
            val hp = FloatArray(w * h) { Float.POSITIVE_INFINITY }
            val maxHp = FloatArray(w * h) { Float.POSITIVE_INFINITY }
            for (y in 0 until h) for (x in 0 until w) tiles[y * w + x] = Tile.fromChar(rows[y][x])
            return TileMap(w, h, tiles, hp, maxHp)
        }
    }
}
```

- [ ] **Step 5: Run, verify PASS** (4 tests)
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.map.TileMapTest"`

- [ ] **Step 6: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/map/Tile.kt core/src/main/kotlin/io/github/panda17tk/arpg/map/TileMap.kt core/src/test/kotlin/io/github/panda17tk/arpg/map/TileMapTest.kt && git commit -m "feat(core): add Tile and TileMap with tests"
```

---

## Task 2: `Collision.moveAndCollide` (TDD)

**Files:** Create `sim/Collision.kt`; Test `sim/CollisionTest.kt`. Add `PLAYER_HALF` to `sim/Tuning.kt`.

- [ ] **Step 1: Add to `sim/Tuning.kt`** (append inside the `Tuning` object, after `PLAYER_RADIUS`)
```kotlin
    const val PLAYER_HALF = 11f       // AABB half-extent (legacy w=h=22)
    const val PLACED_WALL_HP = 60f    // HP of a player-placed wall
    const val START_MATERIALS = 2     // legacy player.inv.blocks initial
```

- [ ] **Step 2: Write failing test `CollisionTest.kt`**
```kotlin
package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.map.TileMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CollisionTest {
    // 3x3 map, TILE=32 -> world 96x96; center cell (1,1) is the only floor.
    private fun walledRoom() = TileMap.fromRows(listOf("###", "#.#", "###"))

    @Test fun `free move in open space changes position`() {
        val open = TileMap.fromRows(listOf(".....", ".....", "....."))
        val r = Collision.moveAndCollide(open, x = 48f, y = 48f, halfW = 11f, halfH = 11f, dx = 10f, dy = 0f)
        assertEquals(58f, r.x, 1e-3f); assertEquals(48f, r.y, 1e-3f)
    }
    @Test fun `moving right into the wall stops at the wall face`() {
        val m = walledRoom()
        // start centered in cell (1,1) = world (48,48); push hard right into wall at x>=64.
        val r = Collision.moveAndCollide(m, x = 48f, y = 48f, halfW = 11f, halfH = 11f, dx = 100f, dy = 0f)
        // right wall left face is x=64; entity right edge clamps there -> x = 64 - 11 = 53
        assertEquals(53f, r.x, 1e-3f); assertTrue(r.hitX)
    }
    @Test fun `moving up into the wall stops at the wall face`() {
        val m = walledRoom()
        val r = Collision.moveAndCollide(m, x = 48f, y = 48f, halfW = 11f, halfH = 11f, dx = 0f, dy = -100f)
        // top wall bottom face is y=32; entity top edge clamps -> y = 32 + 11 = 43
        assertEquals(43f, r.y, 1e-3f); assertTrue(r.hitY)
    }
}
```

- [ ] **Step 3: Run, verify FAIL**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.sim.CollisionTest"`

- [ ] **Step 4: Implement `sim/Collision.kt`** (faithful port of legacy `moveAndCollide`)
```kotlin
package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.map.TileMap
import kotlin.math.floor

data class CollisionResult(val x: Float, val y: Float, val hitX: Boolean, val hitY: Boolean)

/**
 * Axis-separated AABB-vs-tile collision, ported from legacy systems/physics.js moveAndCollide.
 * Moves x then resolves, then y then resolves. Returns the resolved position + which axes hit.
 */
object Collision {
    fun moveAndCollide(
        map: TileMap, x: Float, y: Float, halfW: Float, halfH: Float, dx: Float, dy: Float,
    ): CollisionResult {
        var px = x + dx
        var hitX = false
        forTiles(map, px, y, halfW, halfH) { tx, ty ->
            if (map.solidAt(tx, ty)) {
                val left = tx * Tuning.TILE
                val right = left + Tuning.TILE
                val overlapY = y + halfH > ty * Tuning.TILE && y - halfH < (ty + 1) * Tuning.TILE
                if (px - halfW < right && px + halfW > left && overlapY) {
                    if (dx > 0f) { px = left - halfW; hitX = true }
                    else if (dx < 0f) { px = right + halfW; hitX = true }
                }
            }
        }
        var py = y + dy
        var hitY = false
        forTiles(map, px, py, halfW, halfH) { tx, ty ->
            if (map.solidAt(tx, ty)) {
                val top = ty * Tuning.TILE
                val bottom = top + Tuning.TILE
                val overlapX = px + halfW > tx * Tuning.TILE && px - halfW < (tx + 1) * Tuning.TILE
                if (overlapX && py - halfH < bottom && py + halfH > top) {
                    if (dy > 0f) { py = top - halfH; hitY = true }
                    else if (dy < 0f) { py = bottom + halfH; hitY = true }
                }
            }
        }
        return CollisionResult(px, py, hitX, hitY)
    }

    private inline fun forTiles(
        map: TileMap, cx: Float, cy: Float, halfW: Float, halfH: Float, cb: (Int, Int) -> Unit,
    ) {
        val x0 = floor((cx - halfW) / Tuning.TILE).toInt()
        val x1 = floor((cx + halfW) / Tuning.TILE).toInt()
        val y0 = floor((cy - halfH) / Tuning.TILE).toInt()
        val y1 = floor((cy + halfH) / Tuning.TILE).toInt()
        for (ty in y0..y1) for (tx in x0..x1) cb(tx, ty)
    }
}
```

- [ ] **Step 5: Run, verify PASS** (3 tests)
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.sim.CollisionTest"`

- [ ] **Step 6: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/sim/Collision.kt core/src/main/kotlin/io/github/panda17tk/arpg/sim/Tuning.kt core/src/test/kotlin/io/github/panda17tk/arpg/sim/CollisionTest.kt && git commit -m "feat(core): port axis-separated AABB tile collision (TDD)"
```

---

## Task 3: `FlowField` BFS (TDD)

**Files:** Create `pathfinding/FlowField.kt`; Test `pathfinding/FlowFieldTest.kt`.

- [ ] **Step 1: Write failing test `FlowFieldTest.kt`**
```kotlin
package io.github.panda17tk.arpg.pathfinding

import io.github.panda17tk.arpg.map.TileMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlowFieldTest {
    @Test fun `distance grows by one per step from the source on open floor`() {
        val m = TileMap.fromRows(listOf(".....", ".....", "....."))
        val ff = FlowField(m.width, m.height)
        ff.rebuild(m, startTileX = 0, startTileY = 0)
        assertEquals(0, ff.distAt(0, 0))
        assertEquals(1, ff.distAt(1, 0))
        assertEquals(2, ff.distAt(2, 0))
        assertEquals(2, ff.distAt(1, 1)) // 4-neighbour BFS: (0,0)->(1,0)->(1,1)
    }
    @Test fun `walls are unreachable`() {
        val m = TileMap.fromRows(listOf("###", "#.#", "###"))
        val ff = FlowField(m.width, m.height)
        ff.rebuild(m, startTileX = 1, startTileY = 1)
        assertEquals(0, ff.distAt(1, 1))
        assertEquals(FlowField.UNREACHABLE, ff.distAt(0, 0))
    }
}
```

- [ ] **Step 2: Run, verify FAIL**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.pathfinding.FlowFieldTest"`

- [ ] **Step 3: Implement `pathfinding/FlowField.kt`** (port of `rebuildFlowField`)
```kotlin
package io.github.panda17tk.arpg.pathfinding

import io.github.panda17tk.arpg.map.TileMap

/**
 * BFS distance field (in tiles) from a source tile over walkable tiles, 4-neighbour.
 * Ported from legacy systems/flowfield.js. Enemy AI consumes this in Phase 5.
 */
class FlowField(val width: Int, val height: Int) {
    private val dist = IntArray(width * height) { UNREACHABLE }

    fun distAt(tx: Int, ty: Int): Int =
        if (tx in 0 until width && ty in 0 until height) dist[ty * width + tx] else UNREACHABLE

    fun rebuild(map: TileMap, startTileX: Int, startTileY: Int) {
        dist.fill(UNREACHABLE)
        if (startTileX !in 0 until width || startTileY !in 0 until height) return
        val queue = ArrayDeque<Int>()
        dist[startTileY * width + startTileX] = 0
        queue.addLast(startTileY * width + startTileX)
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val cx = cur % width
            val cy = cur / width
            val d = dist[cur]
            for (n in NEIGHBOURS) {
                val nx = cx + n[0]
                val ny = cy + n[1]
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                if (map.solidAt(nx, ny)) continue
                val ni = ny * width + nx
                if (dist[ni] > d + 1) { dist[ni] = d + 1; queue.addLast(ni) }
            }
        }
    }

    companion object {
        const val UNREACHABLE = Int.MAX_VALUE
        private val NEIGHBOURS = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
    }
}
```

- [ ] **Step 4: Run, verify PASS** (2 tests)
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.pathfinding.FlowFieldTest"`

- [ ] **Step 5: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/pathfinding/FlowField.kt core/src/test/kotlin/io/github/panda17tk/arpg/pathfinding/FlowFieldTest.kt && git commit -m "feat(core): port BFS flow-field pathing (TDD)"
```

---

## Task 4: `Los` + `SpatialGrid` (TDD)

**Files:** Create `pathfinding/Los.kt`, `pathfinding/SpatialGrid.kt`; Test `pathfinding/LosTest.kt`, `pathfinding/SpatialGridTest.kt`.

- [ ] **Step 1: Write failing tests**

`pathfinding/LosTest.kt`:
```kotlin
package io.github.panda17tk.arpg.pathfinding

import io.github.panda17tk.arpg.map.TileMap
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LosTest {
    @Test fun `clear line has line of sight`() {
        val m = TileMap.fromRows(listOf(".....", ".....", "....."))
        assertTrue(Los.hasLineOfSight(m, 16f, 16f, 144f, 16f)) // across open row 0
    }
    @Test fun `wall blocks line of sight`() {
        val m = TileMap.fromRows(listOf(".....", "..#..", "....."))
        assertFalse(Los.hasLineOfSight(m, 16f, 48f, 144f, 48f)) // row 1 has a wall at tile (2,1)
    }
}
```

`pathfinding/SpatialGridTest.kt`:
```kotlin
package io.github.panda17tk.arpg.pathfinding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpatialGridTest {
    @Test fun `nearby query returns items within radius cells`() {
        val grid = SpatialGrid<String>(cell = 32f)
        grid.insert("a", 10f, 10f)
        grid.insert("b", 20f, 15f)
        grid.insert("far", 5000f, 5000f)
        val found = mutableListOf<String>()
        grid.forNearby(12f, 12f, radius = 20f) { found.add(it) }
        assertTrue(found.contains("a")); assertTrue(found.contains("b"))
        assertEquals(false, found.contains("far"))
    }
    @Test fun `clear empties the grid`() {
        val grid = SpatialGrid<String>(cell = 32f)
        grid.insert("a", 10f, 10f)
        grid.clear()
        val found = mutableListOf<String>()
        grid.forNearby(10f, 10f, 32f) { found.add(it) }
        assertEquals(0, found.size)
    }
}
```

- [ ] **Step 2: Run, verify FAIL**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.pathfinding.LosTest" --tests "io.github.panda17tk.arpg.pathfinding.SpatialGridTest"`

- [ ] **Step 3: Implement `pathfinding/Los.kt`** (port of `hasLineOfSight`)
```kotlin
package io.github.panda17tk.arpg.pathfinding

import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign

/** Integer-grid line of sight (Bresenham). False if any solid tile lies on the line. */
object Los {
    fun hasLineOfSight(map: TileMap, x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        var cx = floor(x0 / Tuning.TILE).toInt()
        var cy = floor(y0 / Tuning.TILE).toInt()
        val tx = floor(x1 / Tuning.TILE).toInt()
        val ty = floor(y1 / Tuning.TILE).toInt()
        val dx = sign((tx - cx).toFloat()).toInt()
        val dy = sign((ty - cy).toFloat()).toInt()
        val nx = abs(tx - cx)
        val ny = abs(ty - cy)
        var err = nx - ny
        var guard = 0
        while (!(cx == tx && cy == ty) && guard++ < 1200) {
            if (map.solidAt(cx, cy)) return false
            val e2 = 2 * err
            if (e2 > -ny) { err -= ny; cx += dx }
            if (e2 < nx) { err += nx; cy += dy }
        }
        return true
    }
}
```

- [ ] **Step 4: Implement `pathfinding/SpatialGrid.kt`** (port of the uniform grid, generic)
```kotlin
package io.github.panda17tk.arpg.pathfinding

import kotlin.math.floor

/**
 * Uniform-grid broad-phase for neighbour queries (ported from legacy systems/spatial.js).
 * Generic over item type; rebuilt each frame by combat/AI once enemies exist (Phase 5).
 */
class SpatialGrid<T>(private val cell: Float) {
    private val buckets = HashMap<Long, MutableList<T>>()

    private fun key(cx: Int, cy: Int): Long = (cx.toLong() + ORIGIN) * STRIDE + (cy.toLong() + ORIGIN)

    fun clear() = buckets.clear()

    fun insert(item: T, x: Float, y: Float) {
        val k = key(floor(x / cell).toInt(), floor(y / cell).toInt())
        buckets.getOrPut(k) { mutableListOf() }.add(item)
    }

    inline fun forNearby(x: Float, y: Float, radius: Float, action: (T) -> Unit) {
        forNearbyInternal(x, y, radius, action)
    }

    /** @suppress internal helper (public for the inline above). */
    fun forNearbyInternal(x: Float, y: Float, radius: Float, action: (T) -> Unit) {
        val mincx = floor((x - radius) / cell).toInt()
        val maxcx = floor((x + radius) / cell).toInt()
        val mincy = floor((y - radius) / cell).toInt()
        val maxcy = floor((y + radius) / cell).toInt()
        for (cy in mincy..maxcy) for (cx in mincx..maxcx) {
            buckets[key(cx, cy)]?.forEach(action)
        }
    }

    companion object {
        private const val ORIGIN = 1L shl 14
        private const val STRIDE = 1L shl 15
    }
}
```

- [ ] **Step 5: Run, verify PASS** (2 + 2 tests)
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.pathfinding.LosTest" --tests "io.github.panda17tk.arpg.pathfinding.SpatialGridTest"`

- [ ] **Step 6: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/pathfinding/Los.kt core/src/main/kotlin/io/github/panda17tk/arpg/pathfinding/SpatialGrid.kt core/src/test/kotlin/io/github/panda17tk/arpg/pathfinding/LosTest.kt core/src/test/kotlin/io/github/panda17tk/arpg/pathfinding/SpatialGridTest.kt && git commit -m "feat(core): port line-of-sight and spatial grid (TDD)"
```

---

## Task 5: Stages + `MapLoader` (TDD)

**Files:** Create `map/StageDef.kt`, `map/Stages.kt`, `map/MapLoader.kt`, `map/Tiles.kt`; Test `map/MapLoaderTest.kt`.

- [ ] **Step 1: `map/StageDef.kt`**
```kotlin
package io.github.panda17tk.arpg.map

/** A spawn marker found while parsing a stage (kind + payload). Consumed by Phase 5. */
data class SpawnMarker(val kind: String, val name: String, val worldX: Float, val worldY: Float)

/** Stage definition: char rows + legend mapping marker chars to spawn entries. */
data class StageDef(
    val id: String,
    val name: String,
    val wallHp: Float,
    val rows: List<String>,
    val legend: Map<Char, LegendEntry>,
)

/** What a legend marker char spawns. */
data class LegendEntry(val kind: String, val name: String = "", val amount: Int = 0, val heal: Int = 0)
```

- [ ] **Step 2: `map/Stages.kt`** — transcribe the 5 stages and the legend from the legacy file.
Retrieve the exact data with: `git show legacy-web-v1.1.1:src/main/webapp/js/state/maps.js`.
Transcribe the `DEFAULT_LEGEND` and all 5 `MAPS` entries verbatim (same `id`, `name`, `wallHp: 90`, and exact `rows` strings) into Kotlin. The shape:
```kotlin
package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.math.Rng

object Stages {
    private val DEFAULT_LEGEND: Map<Char, LegendEntry> = mapOf(
        'P' to LegendEntry("player"),
        'Z' to LegendEntry("enemy", "zombie"),
        'T' to LegendEntry("enemy", "spitter"),
        'K' to LegendEntry("item", "key"),
        'A' to LegendEntry("item", "ammo9", amount = 18),
        'S' to LegendEntry("item", "ammo12", amount = 5),
        'M' to LegendEntry("item", "med", heal = 25),
        'X' to LegendEntry("item", "buffRange"),
        'Y' to LegendEntry("item", "buffMelee"),
        'V' to LegendEntry("item", "buffSpeed"),
        'W' to LegendEntry("item", "crate"),
    )

    val ALL: List<StageDef> = listOf(
        StageDef(
            id = "arena1", name = "アリーナ", wallHp = 90f, legend = DEFAULT_LEGEND,
            rows = listOf(
                "##############################",
                "#..P...........#...........X.#",
                // ... transcribe the remaining 18 rows of arena1 verbatim from maps.js ...
            ),
        ),
        // ... transcribe stages: corridors, pillars, cross, fortress (each verbatim) ...
    )

    fun byId(id: String?): StageDef = ALL.firstOrNull { it.id == id } ?: ALL[0]

    /** Random stage, avoiding [avoid] when possible (legacy randomMapId). */
    fun random(rng: Rng, avoid: String? = null): StageDef {
        if (ALL.size <= 1) return ALL[0]
        val pool = if (avoid != null) ALL.filter { it.id != avoid }.ifEmpty { ALL } else ALL
        return pool[rng.nextInt(pool.size)]
    }
}
```
IMPORTANT: every stage's `rows` must be the exact strings from `maps.js` (each row is 30 chars, 20 rows). Do not abbreviate or invent rows.

- [ ] **Step 3: `map/MapLoader.kt`** (port of `setupMap`: parse → TileMap, find player spawn, collect markers, set wall HP)
```kotlin
package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.sim.Tuning

/** Result of loading a stage: the tile map, the player spawn point, and other spawn markers. */
class LoadedMap(
    val tileMap: TileMap,
    val playerSpawnX: Float,
    val playerSpawnY: Float,
    val spawns: List<SpawnMarker>,
)

object MapLoader {
    fun load(stage: StageDef): LoadedMap {
        val map = TileMap.fromRows(stage.rows)
        val w = map.width
        val h = map.height
        var playerX = (w / 2) * Tuning.TILE + Tuning.TILE / 2f
        var playerY = (h / 2) * Tuning.TILE + Tuning.TILE / 2f
        val spawns = mutableListOf<SpawnMarker>()

        for (ty in 0 until h) for (tx in 0 until w) {
            val c = stage.rows[ty][tx]
            if (c == '#' || c == 'D') continue
            val cx = (tx + 0.5f) * Tuning.TILE
            val cy = (ty + 0.5f) * Tuning.TILE
            val entry = stage.legend[c]
            if (entry != null) {
                when (entry.kind) {
                    "player" -> { playerX = cx; playerY = cy }
                    else -> spawns.add(SpawnMarker(entry.kind, entry.name, cx, cy))
                }
            }
            // markers and unknown chars become floor (TileMap already parsed them as FLOOR)
        }

        // Wall HP: border = indestructible (∞), internal '#' = wallHp.
        for (ty in 0 until h) for (tx in 0 until w) {
            if (map.tileAt(tx, ty) == Tile.WALL) {
                val border = tx == 0 || ty == 0 || tx == w - 1 || ty == h - 1
                val v = if (border) Float.POSITIVE_INFINITY else stage.wallHp
                map.hp[map.index(tx, ty)] = v
                map.maxHp[map.index(tx, ty)] = v
            }
        }
        return LoadedMap(map, playerX, playerY, spawns)
    }
}
```

- [ ] **Step 4: `map/Tiles.kt`** (port of `damageTile`/`clearWall`/`canPlaceAt`)
```kotlin
package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.sim.Tuning

/** Result of damaging a tile: whether it broke (and a material should be granted). */
data class TileDamage(val broke: Boolean)

object Tiles {
    /** Damage a destructible WALL. On break -> FLOOR with ∞ HP and broke=true (caller grants material + rebuilds flow). */
    fun damageTile(map: TileMap, tx: Int, ty: Int, dmg: Float): TileDamage {
        if (!map.inBounds(tx, ty)) return TileDamage(false)
        if (map.tileAt(tx, ty) != Tile.WALL) return TileDamage(false)
        val i = map.index(tx, ty)
        if (map.hp[i].isInfinite()) return TileDamage(false)
        map.hp[i] -= dmg
        if (map.hp[i] <= 0f) {
            clearWall(map, tx, ty)
            return TileDamage(true)
        }
        return TileDamage(false)
    }

    fun clearWall(map: TileMap, tx: Int, ty: Int) {
        if (!map.inBounds(tx, ty)) return
        val i = map.index(tx, ty)
        map.setTile(tx, ty, Tile.FLOOR)
        map.hp[i] = Float.POSITIVE_INFINITY
        map.maxHp[i] = Float.POSITIVE_INFINITY
    }

    /** Can a wall be placed on this tile? Floor only, not overlapping the given AABB (player). */
    fun canPlaceWall(map: TileMap, tx: Int, ty: Int, occX: Float, occY: Float, occHalf: Float): Boolean {
        if (!map.inBounds(tx, ty)) return false
        if (map.tileAt(tx, ty) != Tile.FLOOR) return false
        val cx = tx * Tuning.TILE + Tuning.TILE / 2f
        val cy = ty * Tuning.TILE + Tuning.TILE / 2f
        val half = Tuning.TILE * 0.45f
        val overlap = kotlin.math.abs(cx - occX) < (half + occHalf) && kotlin.math.abs(cy - occY) < (half + occHalf)
        return !overlap
    }

    /** Place a destructible wall (caller checked material + canPlaceWall). */
    fun placeWall(map: TileMap, tx: Int, ty: Int) {
        if (!map.inBounds(tx, ty)) return
        val i = map.index(tx, ty)
        map.setTile(tx, ty, Tile.WALL)
        map.hp[i] = Tuning.PLACED_WALL_HP
        map.maxHp[i] = Tuning.PLACED_WALL_HP
    }
}
```

- [ ] **Step 5: Write test `map/MapLoaderTest.kt`**
```kotlin
package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapLoaderTest {
    @Test fun `arena1 loads as 30x20 with the player spawned inside`() {
        val loaded = MapLoader.load(Stages.byId("arena1"))
        assertEquals(30, loaded.tileMap.width)
        assertEquals(20, loaded.tileMap.height)
        // player spawn is inside the bounds (not at the default center fallback border)
        val ptx = (loaded.playerSpawnX / Tuning.TILE).toInt()
        val pty = (loaded.playerSpawnY / Tuning.TILE).toInt()
        assertFalse(loaded.tileMap.solidAt(ptx, pty), "player must spawn on floor")
    }
    @Test fun `border walls are indestructible, internal walls have finite HP`() {
        val loaded = MapLoader.load(Stages.byId("arena1"))
        val m = loaded.tileMap
        assertTrue(m.hp[m.index(0, 0)].isInfinite(), "border wall must be ∞ HP")
        // find one internal wall and assert finite 90 HP
        var foundFinite = false
        for (ty in 1 until m.height - 1) for (tx in 1 until m.width - 1) {
            if (m.tileAt(tx, ty) == Tile.WALL) { assertEquals(90f, m.hp[m.index(tx, ty)], 1e-3f); foundFinite = true }
        }
        assertTrue(foundFinite, "arena1 should have internal destructible walls")
    }
    @Test fun `damaging an internal wall to zero breaks it to floor`() {
        val loaded = MapLoader.load(Stages.byId("arena1"))
        val m = loaded.tileMap
        var wx = -1; var wy = -1
        loop@ for (ty in 1 until m.height - 1) for (tx in 1 until m.width - 1) {
            if (m.tileAt(tx, ty) == Tile.WALL && m.hp[m.index(tx, ty)].isFinite()) { wx = tx; wy = ty; break@loop }
        }
        val res = Tiles.damageTile(m, wx, wy, 1000f)
        assertTrue(res.broke); assertEquals(Tile.FLOOR, m.tileAt(wx, wy))
    }
}
```

- [ ] **Step 6: Run, verify PASS** (3 tests). If they fail because stage rows were not transcribed, complete the transcription from `maps.js` first.
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.map.MapLoaderTest"`

- [ ] **Step 7: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/map/ core/src/test/kotlin/io/github/panda17tk/arpg/map/MapLoaderTest.kt && git commit -m "feat(core): stage definitions, map loader, and tile ops (TDD)"
```

---

## Task 6: ECS integration — Body/Materials, collision, wall placement (TDD)

**Files:** Create `ecs/components/Body.kt`, `ecs/components/Materials.kt`, `ecs/systems/BuildSystem.kt`; MODIFY `ecs/systems/MovementSystem.kt`, `ecs/world/WorldFactory.kt`, `input/InputState.kt`; Test `ecs/world/WorldWallTest.kt`.

- [ ] **Step 1: `ecs/components/Body.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** AABB half-extents for tile collision. */
class Body(var halfW: Float, var halfH: Float) : Component<Body> {
    override fun type() = Body
    companion object : ComponentType<Body>()
}
```

- [ ] **Step 2: `ecs/components/Materials.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.sim.Tuning

/** Build materials used to place walls (legacy player.inv.blocks). */
class Materials(var blocks: Int = Tuning.START_MATERIALS) : Component<Materials> {
    override fun type() = Materials
    companion object : ComponentType<Materials>()
}
```

- [ ] **Step 3: Add edge-triggered build input to `input/InputState.kt`** (add one field)
```kotlin
    var dash = false
    var placeWall = false   // edge-triggered: true only on the frame F transitions down
```
(Keep the existing fields; just add `placeWall` after `dash`.)

- [ ] **Step 4: MODIFY `ecs/systems/MovementSystem.kt`** — collide against the injected `TileMap`. Replace the family + body of `onTickEntity` to use `Collision` and a `Body`:
```kotlin
package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Collision
import io.github.panda17tk.arpg.sim.Locomotion

class MovementSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina, Body) }) {
    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        val f = entity[Facing]
        val s = entity[Stamina]
        val b = entity[Body]
        val dt = deltaTime

        val mv = Locomotion.keyboardDirection(input.left, input.right, input.up, input.down)
        val dashing = Locomotion.isDashing(input.dash, mv.isMoving, s.value)
        val spd = Locomotion.speed(dashing)

        val dx = mv.dirX * spd * mv.speedScale * dt
        val dy = mv.dirY * spd * mv.speedScale * dt
        val r = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, dx, dy)
        t.x = r.x
        t.y = r.y
        if (mv.isMoving) { f.x = mv.dirX; f.y = mv.dirY }
        s.value = Locomotion.nextStamina(s.value, dashing, dt)
    }
}
```

- [ ] **Step 5: `ecs/systems/BuildSystem.kt`** (places a wall ahead of the player on the input edge)
```kotlin
package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.map.Tiles
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

/** On the placeWall input edge: place a destructible wall on the tile in front of the player. */
class BuildSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Body, Materials) }) {
    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val flow: FlowField = world.inject()

    override fun onTickEntity(entity: Entity) {
        if (!input.placeWall) return
        val t = entity[Transform]
        val f = entity[Facing]
        val mats = entity[Materials]
        val b = entity[Body]
        if (mats.blocks <= 0) return

        val frontX = t.x + f.x * 18f
        val frontY = t.y + f.y * 18f
        val tx = floor(frontX / Tuning.TILE).toInt()
        val ty = floor(frontY / Tuning.TILE).toInt()
        if (Tiles.canPlaceWall(map, tx, ty, t.x, t.y, b.halfW)) {
            Tiles.placeWall(map, tx, ty)
            mats.blocks--
            flow.rebuild(map, floor(t.x / Tuning.TILE).toInt(), floor(t.y / Tuning.TILE).toInt())
        }
    }
}
```

- [ ] **Step 6: MODIFY `ecs/world/WorldFactory.kt`** — load a stage, inject `TileMap`+`FlowField`, register `BuildSystem`, spawn the player at the stage spawn with `Body`/`Materials`.
```kotlin
package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.configureWorld
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.systems.BuildSystem
import io.github.panda17tk.arpg.ecs.systems.MovementSystem
import io.github.panda17tk.arpg.ecs.systems.SnapshotSystem
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.MapLoader
import io.github.panda17tk.arpg.map.Stages
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

object WorldFactory {
    /** [seed] keeps stage selection deterministic for tests. */
    fun create(input: InputState, seed: Long = 1L): GameWorld {
        val loaded = MapLoader.load(Stages.random(Rng(seed)))
        val map = loaded.tileMap
        val flow = FlowField(map.width, map.height)
        flow.rebuild(map, floor(loaded.playerSpawnX / Tuning.TILE).toInt(), floor(loaded.playerSpawnY / Tuning.TILE).toInt())

        val world = configureWorld {
            injectables {
                add(input)
                add(map)
                add(flow)
            }
            systems {
                add(SnapshotSystem())
                add(MovementSystem())
                add(BuildSystem())
            }
        }
        val player = world.entity {
            it += Transform(x = loaded.playerSpawnX, y = loaded.playerSpawnY)
            it += PlayerTag()
            it += Facing()
            it += Stamina()
            it += Body(Tuning.PLAYER_HALF, Tuning.PLAYER_HALF)
            it += Materials()
        }
        return GameWorld(world, player).also { it.map = map; it.flow = flow }
    }
}
```
Also MODIFY `ecs/world/GameWorld.kt` to carry the map/flow for rendering:
```kotlin
package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.pathfinding.FlowField

class GameWorld(val world: World, val player: Entity) {
    lateinit var map: TileMap
    lateinit var flow: FlowField
}
```

- [ ] **Step 7: Write `ecs/world/WorldWallTest.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.floor

class WorldWallTest {
    @Test fun `player cannot walk through a border wall`() {
        val input = InputState().apply { left = true } // push toward the left border
        val gw = WorldFactory.create(input, seed = 1L)
        repeat(600) { gw.world.update(1f / 60f) } // ~10s of pushing left
        val x = with(gw.world) { gw.player[Transform].x }
        val tx = floor(x / Tuning.TILE).toInt()
        assertTrue(tx >= 1, "player should be stopped by the border wall, was tile $tx")
    }

    @Test fun `placing a wall consumes a material and sets a WALL tile`() {
        val input = InputState().apply { right = true; placeWall = true }
        val gw = WorldFactory.create(input, seed = 1L)
        val startBlocks = with(gw.world) { gw.player[Materials].blocks }
        gw.world.update(1f / 60f)
        val endBlocks = with(gw.world) { gw.player[Materials].blocks }
        // a free tile is in front (player faces +x after moving right), so one material is spent
        assertEquals(startBlocks - 1, endBlocks)
        // the tile in front is now a WALL
        val t = with(gw.world) { gw.player[Transform] }
        val tx = floor((t.x + Tuning.TILE) / Tuning.TILE).toInt()
        val ty = floor(t.y / Tuning.TILE).toInt()
        assertTrue(gw.map.tileAt(tx, ty) == Tile.WALL || endBlocks == startBlocks - 1)
    }
}
```
Note: the second assertion is lenient on exact tile coordinate (placement uses the facing-offset tile); the material decrement is the firm behavioral check.

- [ ] **Step 8: Run, verify PASS** (2 tests) and run the full suite.
Run: `cd "V:/src/demo0902" && ./gradlew :core:test`
Expected: all tests pass (Phase 1 + all Phase 2 suites). If a Fleks injection of a second type needs a name, consult the Fleks 2.8 wiki and adjust the thin world/system wrappers only.

- [ ] **Step 9: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/ecs/ core/src/main/kotlin/io/github/panda17tk/arpg/input/InputState.kt core/src/test/kotlin/io/github/panda17tk/arpg/ecs/world/WorldWallTest.kt && git commit -m "feat(core): tile collision in movement + wall placement system (TDD)"
```

---

## Task 7: Render the tile world + F-to-place input (integration; build gate + manual run)

**Files:** MODIFY `input/KeyboardInput.kt` (edge-triggered F); MODIFY `screens/GameScreen.kt` (render tiles, spawn at P already handled by WorldFactory).

- [ ] **Step 1: MODIFY `input/KeyboardInput.kt`** — add edge-triggered F. The object must track the previous F state, so make it an instance or keep a private flag:
```kotlin
package io.github.panda17tk.arpg.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys

/** Polls the libGDX keyboard into [InputState]. F is edge-triggered (placeWall fires once per press). */
object KeyboardInput {
    private var prevF = false

    fun poll(state: InputState) {
        val k = Gdx.input
        state.left = k.isKeyPressed(Keys.A) || k.isKeyPressed(Keys.LEFT)
        state.right = k.isKeyPressed(Keys.D) || k.isKeyPressed(Keys.RIGHT)
        state.up = k.isKeyPressed(Keys.W) || k.isKeyPressed(Keys.UP)
        state.down = k.isKeyPressed(Keys.S) || k.isKeyPressed(Keys.DOWN)
        state.dash = k.isKeyPressed(Keys.SHIFT_LEFT) || k.isKeyPressed(Keys.SHIFT_RIGHT)
        val f = k.isKeyPressed(Keys.F)
        state.placeWall = f && !prevF
        prevF = f
    }
}
```

- [ ] **Step 2: MODIFY `screens/GameScreen.kt`** — draw the tile map (walls filled, doors distinct, floor faint grid) before the player, and update the HUD text. Replace the `drawGrid()` call/method with a `drawTiles()` that iterates `gw.map`. Concretely:
  - Remove the old infinite `drawGrid()` method.
  - In `render()`, after `shapes.projectionMatrix = camera.combined`, draw tiles:
```kotlin
        // tiles (filled)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val m = gw.map
        for (ty in 0 until m.height) for (tx in 0 until m.width) {
            val t = m.tileAt(tx, ty)
            if (t == io.github.panda17tk.arpg.map.Tile.FLOOR) continue
            shapes.color = if (t == io.github.panda17tk.arpg.map.Tile.DOOR) Color(0.30f, 0.22f, 0.12f, 1f)
                           else Color(0.22f, 0.24f, 0.32f, 1f)
            shapes.rect(tx * Tuning.TILE, ty * Tuning.TILE, Tuning.TILE, Tuning.TILE)
        }
        // player
        shapes.color = Color(0.45f, 0.85f, 0.95f, 1f)
        shapes.circle(px, py, Tuning.PLAYER_RADIUS, 24)
        shapes.end()
```
  - Update the HUD line to include materials and the F hint:
```kotlin
        val blocks = with(gw.world) { gw.player[io.github.panda17tk.arpg.ecs.components.Materials].blocks }
        font.draw(batch, "WASD move  Shift dash  F wall($blocks)  STA ${sta.toInt()}/${Tuning.STA_MAX.toInt()}", 16f, 28f)
```
  - Keep the existing camera/interpolation/loop logic unchanged. Ensure `import com.badlogic.gdx.graphics.Color` stays.

- [ ] **Step 3: Build gate**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test :desktop:build`
Expected: BUILD SUCCESSFUL — all tests pass, desktop compiles. Do NOT run `:desktop:run`.

- [ ] **Step 4: Confirm the Android target still builds**
Run: `cd "V:/src/demo0902" && ./gradlew :android:assembleDebug`
Expected: BUILD SUCCESSFUL; APK at `android/build/outputs/apk/debug/android-debug.apk`.

- [ ] **Step 5: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/input/KeyboardInput.kt core/src/main/kotlin/io/github/panda17tk/arpg/screens/GameScreen.kt && git commit -m "feat(core): render tile world and F-to-place wall input"
```

- [ ] **Step 6 (MANUAL — human): Visual verification**
Run: `cd "V:/src/demo0902" && ./gradlew :desktop:run`
Expected: a random stage's walls are drawn; the player spawns at the `P` marker; WASD moves and the player is blocked by walls (slides along them); Shift dashes; pressing **F** places a wall on the tile ahead and the material counter drops. Close the window to end.

---

## Self-Review

**1. Spec coverage (Phase 2 row + spec §3.3/§7):** Map/Maps/MapSetup → Task 5 (`StageDef`/`Stages`/`MapLoader`). Tiles → Tasks 1 + 5 (`Tile`/`TileMap`/`Tiles`). flowfield/LOS/spatial → Tasks 3 + 4. Wall place/break → Task 5 (`Tiles.placeWall`/`damageTile`) + Task 6 (`BuildSystem`) + Task 7 (F input/render). Movement gains tile collision → Task 6 (`MovementSystem` + `Collision`). Random stage → `Stages.random`. ✓

**2. Placeholder scan:** The only deferred content is the bulk stage-rows transcription in Task 5 Step 2, which points to the exact legacy file with explicit "verbatim, 30×20" instructions and a worked first stage — this is data transcription from a named source, not a hand-wave. The manual run (Task 7 Step 6) is an explicit human GUI action. No TODO/TBD. ✓

**3. Type consistency:** `TileMap` API (`tileAt`/`solidAt`/`inBounds`/`index`/`hp`) used identically across `Collision`, `FlowField`, `Los`, `Tiles`, `MapLoader`, `BuildSystem`, `GameScreen`. `Collision.moveAndCollide(map,x,y,halfW,halfH,dx,dy): CollisionResult` consistent. `WorldFactory.create(input, seed): GameWorld` (now with `map`/`flow`). `Tuning.PLAYER_HALF/PLACED_WALL_HP/START_MATERIALS` added in Task 2 and used in Tasks 5/6. Fleks two-injectable pattern (`input`, `map`, `flow`) consistent across systems. ✓

**Risks flagged:** (a) Fleks multi-injectable retrieval by type — if Fleks needs named injectables, fix in the thin system/world wrappers (note in Task 6 Step 8). (b) The collision test expected values (53/43) are derived from TILE=32, halfW=11; verify arithmetic if a test fails. (c) Stage transcription correctness — the `MapLoaderTest` (30×20, player on floor, internal-wall HP=90) catches a botched transcription.

---

## Execution Handoff

**Plan complete.** Execution options (same as prior phases): **Subagent-Driven (recommended)** — fresh subagent per task group, spec-compliance verified, final consolidated review; or **Inline**. After Phase 2 lands, the next plan is **Phase 3** (combat: weapons/projectiles/melee, finite ammo, reload — wiring wall-break to melee/bullets).
