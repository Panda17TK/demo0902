package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Buff
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Smoke
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.item.ConsumeKind
import io.github.panda17tk.arpg.item.ItemDef

/**
 * Using a consumable from the inventory's ITEMS tab (v2.34). Applies the item's [ConsumeKind]
 * to the player and returns the toast line to flash — or null when the item isn't a consumable
 * or would be wasted (full HP heal, full stamina drink), in which case nothing is spent.
 */
object ItemUse {
    fun use(world: World, player: Entity, item: ItemDef): String? = with(world) {
        when (item.consume) {
            ConsumeKind.NONE -> null
            ConsumeKind.HEAL -> {
                val h = player[Health]
                if (h.hp >= h.hpMax) return null // don't waste a med on full HP
                h.hp = minOf(h.hpMax, h.hp + item.power)
                "HPが回復した"
            }
            ConsumeKind.STAMINA -> {
                val s = player[Stamina]
                if (s.value >= s.max) return null
                s.value = s.max
                "スタミナが全回復した"
            }
            ConsumeKind.STAMINA_INF -> { player[Buff].staminaInfT = item.power; "体が軽い　${item.power.toInt()}秒間スタミナ消費なし" }
            ConsumeKind.DASH_UP -> { player[Buff].dashUpT = item.power; "反応が研ぎ澄まされた　${item.power.toInt()}秒間ダッシュ強化" }
            ConsumeKind.HEAT_PROOF -> { player[Buff].heatProofT = item.power; "${item.power.toInt()}秒間マグマダメージ無効" }
            ConsumeKind.COLD_PROOF -> { player[Buff].coldProofT = item.power; "${item.power.toInt()}秒間 雪と氷の影響なし" }
            ConsumeKind.MAGNET -> { player[Buff].magnetT = item.power; "${item.power.toInt()}秒間ドロップ品を引き寄せる" }
            ConsumeKind.REGEN -> { player[Buff].regenT = item.power; "${item.power.toInt()}秒間HPが回復し続ける" }
            ConsumeKind.SMOKE -> {
                val t = player[Transform]
                world.entity {
                    it += Transform(x = t.x, y = t.y, prevX = t.x, prevY = t.y)
                    it += Smoke(radius = SMOKE_RADIUS, life = item.power)
                }
                "煙幕を張った"
            }
            ConsumeKind.BLOCKS -> { player[Materials].blocks += item.power.toInt(); "壁材 +${item.power.toInt()}" }
            ConsumeKind.AMMO_ALL -> {
                val a = player[Ammo]
                val g = item.power.toInt()
                for (pool in AMMO_POOLS) a.set(pool, a.get(pool) + g)
                "全弾薬 +$g"
            }
        }
    }

    private const val SMOKE_RADIUS = 80f // matches the smoke pickup's cloud
    private val AMMO_POOLS = arrayOf("ammo9", "ammo12", "ammoBeam", "ammoNade")
}
