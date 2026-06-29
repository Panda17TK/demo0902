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
    private val SPACE: List<StageDef> = listOf(
        asteroid("belt1", "アステロイドベルト α", 1313, 1313, 1L), // ~32x area (√32× linear vs the old 232²)
        asteroid("belt2", "アステロイドベルト β", 1494, 1131, 2L),
        asteroid("belt3", "アステロイドベルト γ", 1131, 1494, 3L),
    )

    fun byId(id: String?): StageDef = (ALL + SPACE).firstOrNull { it.id == id } ?: SPACE[0]

    /** Random space stage (used in-game), avoiding [avoid] when possible. */
    fun random(rng: Rng, avoid: String? = null): StageDef {
        if (SPACE.size <= 1) return SPACE[0]
        val pool = if (avoid != null) SPACE.filter { it.id != avoid }.ifEmpty { SPACE } else SPACE
        return pool[rng.nextInt(pool.size)]
    }

    /** Procedurally build a large open arena scattered with asteroid clusters + enemies (deterministic). */
    private fun asteroid(id: String, name: String, w: Int, h: Int, seed: Long): StageDef {
        val rng = Rng(seed)
        val g = Array(h) { CharArray(w) { '.' } }
        for (x in 0 until w) { g[0][x] = '#'; g[h - 1][x] = '#' }
        for (y in 0 until h) { g[y][0] = '#'; g[y][w - 1] = '#' }
        val pcx = w / 2; val pcy = h / 2
        repeat((w * h) / 70) {
            val cx = 2 + rng.nextInt(w - 4); val cy = 2 + rng.nextInt(h - 4)
            if (abs(cx - pcx) < 6 && abs(cy - pcy) < 6) return@repeat // keep the spawn area clear
            val r = 1 + rng.nextInt(3)
            for (dy in -r..r) for (dx in -r..r) {
                val nx = cx + dx; val ny = cy + dy
                if (nx in 1 until w - 1 && ny in 1 until h - 1 && dx * dx + dy * dy <= r * r && rng.nextFloat() < 0.82f) g[ny][nx] = '#'
            }
        }
        g[pcy][pcx] = 'P'
        repeat(14 + rng.nextInt(8)) { // cap initial enemies regardless of map size (waves add more)
            val ex = 2 + rng.nextInt(w - 4); val ey = 2 + rng.nextInt(h - 4)
            if (g[ey][ex] == '.' && (abs(ex - pcx) > 7 || abs(ey - pcy) > 7)) g[ey][ex] = if (rng.nextFloat() < 0.5f) 'Z' else 'T'
        }
        return StageDef(id, name, wallHp = 130f, rows = g.map { String(it) }, legend = DEFAULT_LEGEND)
    }
}
