package io.github.panda17tk.arpg.planet

import io.github.panda17tk.arpg.sim.WeatherKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.109 天候×依頼: the sky's premium — pure, shared by the chip and the settlement. */
class WeatherQuestTest {
    @Test fun `a clear sky pays the listed price`() {
        for (kind in QuestKind.entries) {
            assertEquals(1f, WeatherQuest.mul(kind, WeatherKind.CLEAR), 1e-4f)
        }
    }

    @Test fun `the harder-in-this-weather work earns the premium`() {
        assertEquals(1.5f, WeatherQuest.mul(QuestKind.OBSERVE, WeatherKind.THUNDER), 1e-4f, "watching through thunder")
        assertEquals(1.25f, WeatherQuest.mul(QuestKind.PROTECT, WeatherKind.RAIN), 1e-4f, "guarding in the rain")
        assertEquals(1.25f, WeatherQuest.mul(QuestKind.KILLS, WeatherKind.FOG), 1e-4f, "hunting blind")
        assertEquals(1f, WeatherQuest.mul(QuestKind.DUST, WeatherKind.THUNDER), 1e-4f, "unrelated work stays flat")
    }

    @Test fun `the settlement rounds down and never underpays the listed reward on a clear day`() {
        val q = QuestDef(QuestKind.OBSERVE, target = 30, rewardDust = 90)
        assertEquals(90, WeatherQuest.rewardFor(q, WeatherKind.CLEAR))
        assertEquals(135, WeatherQuest.rewardFor(q, WeatherKind.THUNDER))
        assertTrue(WeatherQuest.rewardFor(q, WeatherKind.AURORA) >= q.rewardDust)
    }
}
