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
import io.github.panda17tk.arpg.sim.WorldState

class GameWorld(val world: World, val player: Entity) {
    lateinit var map: TileMap
    lateinit var flow: FlowField
    lateinit var waveState: WaveState
    lateinit var gameOver: GameOver
    lateinit var fx: Fx
    var bases: List<Base> = emptyList()
    var gravityField: GravityField = GravityField()
    var planets: List<PlanetBody> = emptyList()
    var worldState: WorldState = WorldState()
}
