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
