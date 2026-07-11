package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.math.Rng
import kotlin.math.abs

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
                "#............................#",
                "#....######.....A......#####.#",
                "#....#....#.............#....#",
                "#....#....#....M........#....#",
                "#....#....#.............#....#",
                "#....##.###....#######..#....#",
                "#.............W..............#",
                "#..A.....#......Z.........V..#",
                "#........#...................#",
                "#....T...#....S..............#",
                "#........#...................#",
                "#........#....Y..............#",
                "#........###########.........#",
                "#........#.........#.........#",
                "#........#....K....#....M....#",
                "#........#.........#.....W...#",
                "#..S.....#.............Z.....#",
                "##############################",
            ),
        ),
        StageDef(
            id = "corridors", name = "回廊", wallHp = 90f, legend = DEFAULT_LEGEND,
            rows = listOf(
                "##############################",
                "#..P.......##........A.......#",
                "#..........##................#",
                "#####..#####........####..####",
                "#......#......T.....#.....M..#",
                "#..A...#..####......#..####..#",
                "#......#..#  ........  #..V..#",
                "#..#####..#..#######..#..#####",
                "#..#...W..#..#.....#..#......#",
                "#..#......#..#..K..#..#..Z...#",
                "#..#..Z...#..#.....#..#......#",
                "#..#......####.....####..S...#",
                "#..#.................#.......#",
                "#..#####..#####..#####..######",
                "#........#....M..#...........#",
                "#...Y....#..####.#....####...#",
                "#........#..#.W..#....#..#...#",
                "#..S.....#..#....##...#..#.X.#",
                "#........T..........T........#",
                "##############################",
            ),
        ),
        StageDef(
            id = "pillars", name = "ピラー", wallHp = 90f, legend = DEFAULT_LEGEND,
            rows = listOf(
                "##############################",
                "#..P....A..........M......X..#",
                "#............................#",
                "#...##.....##.....##....##...#",
                "#...##.....##.....##....##...#",
                "#............................#",
                "#....Z...A..........Z........#",
                "#............................#",
                "#...##.....##.....##....##...#",
                "#...##.....##.....##....##...#",
                "#.....M................V.....#",
                "#............................#",
                "#....S...Z.........Z....S....#",
                "#............................#",
                "#...##.....##.....##....##...#",
                "#...##.....##.....##....##...#",
                "#..........K.................#",
                "#.....Y................M.....#",
                "#..T....................T....#",
                "##############################",
            ),
        ),
        StageDef(
            id = "cross", name = "クロス", wallHp = 90f, legend = DEFAULT_LEGEND,
            rows = listOf(
                "##############################",
                "#..P......#........#......X..#",
                "#.........#........#.........#",
                "#..A....................M....#",
                "#............................#",
                "#####..######..########..#####",
                "#.........#........#.........#",
                "#.........#........#.M.......#",
                "#....W....#........#...Z.....#",
                "#.............K..............#",
                "#............................#",
                "#...S.....#........#....V....#",
                "#.........#..Z.....#.........#",
                "#.........#........#.........#",
                "#####..######..########..#####",
                "#............................#",
                "#............................#",
                "#.....M...#........#..Y......#",
                "#...T.....#........#.....T...#",
                "##############################",
            ),
        ),
        StageDef(
            id = "fortress", name = "要塞", wallHp = 90f, legend = DEFAULT_LEGEND,
            rows = listOf(
                "##############################",
                "#..P..#.........#......#.....#",
                "#.........A..M..#......#.X...#",
                "#.....#.........#............#",
                "#######.........#........W...#",
                "#..A............#......#.....#",
                "#.........Z.....#......####.##",
                "#............................#",
                "#............................#",
                "#...M........K..#....Z...S...#",
                "#...............#............#",
                "#...............#............#",
                "#..S......................V..#",
                "#......Z.....................#",
                "#..........######............#",
                "#.......Y..#...##............#",
                "#............M..#............#",
                "#..........#...##............#",
                "#..T.......#...##........T...#",
                "##############################",
            ),
        ),
    )

    // --- Space / asteroid-belt stages (large open arenas strewn with rocks). Gameplay uses these. ---
    // Bigger, sparser space (~2.5× the area again). A literal 24× would make the dense tile grid ~650MB
    // (TileMap + FlowField) and OOM on Android, so this is scaled to a memory-safe size and the vastness is
    // carried by 8× planet spacing + drifting debris.
    // v2.166 宙域の九分割: the generator metadata lives apart from the built stages, so an
    // AREA slice can rebuild its ninth without ever materialising the full 4M-tile grid.
    data class SpaceGen(val id: String, val name: String, val w: Int, val h: Int, val genSeed: Long)
    val SPACE_GEN = listOf(
        SpaceGen("belt1", "アステロイドベルト α", 2100, 2100, 1L),
        SpaceGen("belt2", "アステロイドベルト β", 2390, 1810, 2L),
        SpaceGen("belt3", "アステロイドベルト γ", 1810, 2390, 3L),
    )
    private val SPACE: List<StageDef> by lazy { SPACE_GEN.map { asteroid(it.id, it.name, it.w, it.h, it.genSeed) } }

    const val AREA_N = 3 // v2.166: the sky divides 3×3

    /** v2.166: the same pick [random] makes, without building the full stages (area mode's entry). */
    fun randomSpaceId(rng: Rng, avoid: String? = null): String {
        if (SPACE_GEN.size <= 1) return SPACE_GEN[0].id
        val pool = if (avoid != null) SPACE_GEN.filter { it.id != avoid }.ifEmpty { SPACE_GEN } else SPACE_GEN
        return pool[rng.nextInt(pool.size)].id
    }

    fun spaceFullDims(id: String): Pair<Int, Int> = SPACE_GEN.first { it.id == id }.let { it.w to it.h }

    /** v2.166: tile origin of slice (ax, ay) — the last row/column absorbs the division remainder. */
    fun sliceOrigin(id: String, ax: Int, ay: Int): Pair<Int, Int> {
        val g = SPACE_GEN.first { it.id == id }
        return (ax * (g.w / AREA_N)) to (ay * (g.h / AREA_N))
    }

    /** v2.169: a slice's tile dims WITHOUT building it — the last row/column absorbs the
     *  division remainder, so the departing map is the wrong ruler for its neighbour's edge. */
    fun sliceDims(id: String, ax: Int, ay: Int): Pair<Int, Int> {
        val g = SPACE_GEN.first { it.id == id }
        val sw = if (ax == AREA_N - 1) g.w - ax * (g.w / AREA_N) else g.w / AREA_N
        val sh = if (ay == AREA_N - 1) g.h - ay * (g.h / AREA_N) else g.h / AREA_N
        return sw to sh
    }

    /** v2.166: one ninth of the space stage, rebuilt deterministically from the generator. */
    fun slice(id: String, ax: Int, ay: Int): StageDef {
        val g = SPACE_GEN.first { it.id == id }
        val x0 = ax * (g.w / AREA_N); val y0 = ay * (g.h / AREA_N)
        val sw = if (ax == AREA_N - 1) g.w - x0 else g.w / AREA_N
        val sh = if (ay == AREA_N - 1) g.h - y0 else g.h / AREA_N
        return StageDef("${g.id}-a$ax$ay", g.name, wallHp = 130f, rows = asteroidGrid(g.w, g.h, g.genSeed, x0, y0, sw, sh).map { String(it) }, legend = DEFAULT_LEGEND)
    }

    fun byId(id: String?): StageDef = (ALL + SPACE).firstOrNull { it.id == id } ?: SPACE[0]

    /** Random space stage (used in-game), avoiding [avoid] when possible. */
    fun random(rng: Rng, avoid: String? = null): StageDef {
        if (SPACE.size <= 1) return SPACE[0]
        val pool = if (avoid != null) SPACE.filter { it.id != avoid }.ifEmpty { SPACE } else SPACE
        return pool[rng.nextInt(pool.size)]
    }

    /** Procedurally build a large open arena scattered with asteroid clusters + enemies (deterministic). */
    private fun asteroid(id: String, name: String, w: Int, h: Int, seed: Long): StageDef =
        StageDef(id, name, wallHp = 130f, rows = asteroidGrid(w, h, seed, 0, 0, w, h).map { String(it) }, legend = DEFAULT_LEGEND)

    /**
     * v2.166 宙域の九分割: the generator, writing through a window — a slice replays the FULL
     * stage's deterministic draws but rasterises only its [x0,y0]+[sw,sh] rectangle (with its own
     * indestructible ring). Rock tiles inside a slice match the full stage exactly; only the seam
     * rings and the per-slice enemy scatter differ.
     */
    private fun asteroidGrid(w: Int, h: Int, seed: Long, x0: Int, y0: Int, sw: Int, sh: Int): Array<CharArray> {
        val rng = Rng(seed)
        val g = Array(sh) { CharArray(sw) { '.' } }
        for (x in 0 until sw) { g[0][x] = '#'; g[sh - 1][x] = '#' }
        for (y in 0 until sh) { g[y][0] = '#'; g[y][sw - 1] = '#' }
        fun cell(gx: Int, gy: Int): Char? {
            val lx = gx - x0; val ly = gy - y0
            return if (lx in 1 until sw - 1 && ly in 1 until sh - 1) g[ly][lx] else null
        }
        fun put(gx: Int, gy: Int, c: Char) {
            val lx = gx - x0; val ly = gy - y0
            if (lx in 1 until sw - 1 && ly in 1 until sh - 1) g[ly][lx] = c
        }
        val pcx = w / 2; val pcy = h / 2
        repeat((w * h) / 70) {
            val cx = 2 + rng.nextInt(w - 4); val cy = 2 + rng.nextInt(h - 4)
            if (abs(cx - pcx) < 6 && abs(cy - pcy) < 6) return@repeat // keep the spawn area clear
            val r = 1 + rng.nextInt(3)
            for (dy in -r..r) for (dx in -r..r) {
                val nx = cx + dx; val ny = cy + dy
                if (nx in 1 until w - 1 && ny in 1 until h - 1 && dx * dx + dy * dy <= r * r && rng.nextFloat() < 0.82f) put(nx, ny, '#')
            }
        }
        put(pcx, pcy, 'P')
        repeat(30 + rng.nextInt(16)) { // more initial enemies; waves + bases add many more
            val ex = 2 + rng.nextInt(w - 4); val ey = 2 + rng.nextInt(h - 4)
            if (cell(ex, ey) == '.' && (abs(ex - pcx) > 7 || abs(ey - pcy) > 7)) put(ex, ey, if (rng.nextFloat() < 0.5f) 'Z' else 'T')
        }
        return g
    }
}
