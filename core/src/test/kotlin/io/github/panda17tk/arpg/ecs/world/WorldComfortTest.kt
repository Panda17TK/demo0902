package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.combat.Weapons
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.ui.Modals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.tan

/** v2.112 快適性第2弾: the aim lean, and the pause menu naming the proving run honestly. */
class WorldComfortTest {
    private fun tankDef() = GameConfig().enemies.getValue("zombie").copy(attacks = emptyList(), hp = 5000f)

    /** Fire the rifle once at a target ~6° off-axis; return the bullet's angle. */
    private fun rifleShotAngle(assist: Boolean): Float {
        val input = InputState().apply { aimAssist = assist }
        val gw = WorldFactory.create(input, seed = 1L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        MobFactory.spawn(gw.world, tankDef(), px + 300f, py + 300f * tan(0.105f)) // ~6° above the barrel
        input.weaponSlot = Weapons.ALL.indexOfFirst { it.id == "rifle" }
        gw.world.update(1f / 60f)
        input.weaponSlot = -1
        input.fire = true
        gw.world.update(1f / 60f)
        input.fire = false
        var angle = Float.NaN
        with(gw.world) {
            gw.world.family { all(Bullet) }.forEach { e -> angle = atan2(e[Bullet].vy, e[Bullet].vx) }
        }
        assertTrue(!angle.isNaN(), "a bullet flew")
        return angle
    }

    @Test fun `the assist leans the barrel toward the off-axis target, and only when asked`() {
        val leaned = rifleShotAngle(assist = true)
        assertTrue(leaned > 0.05f, "with assist the shot leans up toward the mob (got $leaned)")
        val honest = rifleShotAngle(assist = false)
        assertTrue(abs(honest) < 0.02f, "without assist the rifle shoots where it points (got $honest)")
    }

    @Test fun `the pause menu names the mode it would end`() {
        val challenge = Modals.pauseButtons(360f, 800f, includeMemory = false, simActive = true, challengeActive = true)
        assertTrue(challenge.any { it.label == "検証ランを終了" }, "a proving run says so")
        val training = Modals.pauseButtons(360f, 800f, includeMemory = false, simActive = true)
        assertTrue(training.any { it.label == "訓練を終了" })
        val plain = Modals.pauseButtons(360f, 800f)
        assertTrue(plain.any { it.label == "旧式戦闘訓練" })
    }
}
