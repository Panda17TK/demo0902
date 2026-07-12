package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.GameOver
import io.github.panda17tk.arpg.ecs.components.WaveState
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.sim.Base
import io.github.panda17tk.arpg.sim.GravityField
import io.github.panda17tk.arpg.sim.PlanetBody
import io.github.panda17tk.arpg.sim.VisitedMap
import io.github.panda17tk.arpg.sim.WorldState

class GameWorld(val world: World, val player: Entity) {
    lateinit var map: TileMap
    /** v2.175 描画の倹約IV: the sim's broad-phase, shared with the draw side — the renderer and
     *  HUD used to walk all ~5000 mobs per frame while this grid sat rebuilt every tick. Within
     *  any weapon's reach (WildLod.GRID_KEEP=1650px > every camera half-extent) the grid is
     *  COMPLETE each tick, so an on-screen query never misses; a mob that died this tick may
     *  linger one frame — readers guard with getOrNull. */
    lateinit var mobGrid: io.github.panda17tk.arpg.pathfinding.SpatialGrid<Entity>
    lateinit var flow: FlowField
    lateinit var waveState: WaveState
    lateinit var gameOver: GameOver
    lateinit var fx: Fx
    var bases: List<Base> = emptyList()
    var gravityField: GravityField = GravityField()
    var planets: List<PlanetBody> = emptyList()
    var worldState: WorldState = WorldState()
    lateinit var visited: VisitedMap // v2.33: fog-of-war record for the inventory MAP tab
}
