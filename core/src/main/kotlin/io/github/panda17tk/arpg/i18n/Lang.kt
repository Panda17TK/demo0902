package io.github.panda17tk.arpg.i18n

/**
 * v2.115 英語化第1弾 — a presentation-layer dictionary. Logic, hit-tests, saves, and every
 * panel's label consts stay keyed on the Japanese strings; tr() maps them to English at the
 * draw funnels only, so no contract moves. Unknown strings pass through untouched — dynamic
 * sentences (logbook, lore, quests) stay Japanese until a later tranche.
 */
object Lang {
    /** Set by the screens from the settings pref; the sim never reads this. */
    var en = false

    fun tr(s: String): String {
        if (!en) return s
        TABLE[s]?.let { return it }
        if (s.none { it.code >= 0x2E80 }) return s // nothing CJK to translate
        cache[s]?.let { return it }
        var out = s
        for ((ja, e) in SUB) if (ja in out) out = out.replace(ja, e)
        if (cache.size >= 256) cache.clear() // v2.121: tiny memo — draw funnels repeat the same lines
        cache[s] = out
        return out
    }

    private val cache = HashMap<String, String>()

    /** v2.142: an English display name from a stable snake_case id ("void_aji" → "Void Aji"). */
    private fun enName(id: String): String {
        val parts = id.split('_').filter { it.isNotEmpty() }
        val ordered = when (parts.firstOrNull()) {
            "thruster", "armor" -> parts.drop(1) + parts.first() // category reads better at the tail
            "gun", "melee", "acc", "use", "mat", "read", "lore" -> parts.drop(1)
            else -> parts
        }
        return ordered.joinToString(" ") { w ->
            if (w.all { it.isDigit() }) "Mk.$w" else w.replaceFirstChar { c -> c.uppercase() }
        }
    }

    private val TABLE: Map<String, String> = mapOf(
        // タイトル
        "つづきから" to "Continue",
        "はじめから" to "New Game",
        "旧式戦闘訓練" to "Legacy Combat Drill",
        "検証ラン" to "Proving Run",
        "記録" to "Records",
        "設定" to "Settings",
        "工房" to "Workshop",
        "調整" to "Tuning",
        "安定運転" to "Steady",
        "標準" to "Standard",
        "過負荷" to "Overload",
        // ポーズ / ゲームオーバー / モーダル
        "ポーズ" to "Paused",
        "再開" to "Resume",
        "最初からやり直す" to "Restart",
        "操作説明" to "Controls",
        "この星の記憶" to "This Planet's Memory",
        "訓練を終了" to "End Drill",
        "検証ランを終了" to "End Proving Run",
        "タイトルへ" to "To Title",
        "宇宙の記憶を消す" to "Erase All Memory",
        "ゲームオーバー" to "Game Over",
        "もう一度" to "Retry",
        "診断する" to "Run Diagnostic",
        "スキップ" to "Skip",
        "消す" to "Erase",
        "戻る" to "Back",
        "大きく" to "Bigger",
        "小さく" to "Smaller",
        "リセット" to "Reset",
        "完了" to "Done",
        "閉じる" to "Close",
        // 設定パネル
        "サウンド" to "Sound",
        "音量" to "Volume",
        "振動" to "Haptics",
        "左利き配置" to "Left-handed",
        "操作ヒント" to "Control Hints",
        "世界観ヒント" to "Lore Hints",
        "画面の揺れ" to "Screen Shake",
        "閃光をやわらげる" to "Soften Flashes",
        "エイム補助" to "Aim Assist",
        "English表示" to "English UI",
        // 設定のささやき (hintFor)
        "効果音と環境音" to "Sound effects and ambience",
        "タップで 100→75→50→25→0 と巡回" to "Tap cycles 100 > 75 > 50 > 25 > 0",
        "被弾の振動" to "Vibration on hits",
        "スティック右・ボタン左に反転" to "Stick right, buttons left",
        "操作ガイド" to "Control hints",
        "記憶核の語り・遭難記録" to "Memory-core voices, distress logs",
        "画面シェイクと反動" to "Shake & recoil",
        "撃破の白フラッシュを弱める" to "Dim the white kill flashes",
        "射撃が近くの敵へわずかに寄る" to "Shots lean gently toward foes",
        "主要ラベルを英語で表示" to "Show main labels in English",
        // v2.148 ヒント帯 (hintPanel) — 起動診断
        "保守員人格 起動中……　記憶同期: 失敗　星間ネットワーク: 応答なし" to "Keeper persona booting...　memory sync: failed　network: silent",
        "ローカル保全モードで再開します — 起動診断を実行しますか？" to "Resuming in local maintenance mode - run the boot diagnostic?",
        "起動診断 1/6　推進制御を確認します" to "Diagnostic 1/6　checking thrusters",
        "左スティックで船体を移動" to "Move with the left stick",
        "WASD / 方向キーで船体を移動" to "Move with WASD / arrow keys",
        "起動診断 2/6　防衛工具を確認します" to "Diagnostic 2/6　checking defense tools",
        "右スティックで照準・離して発射　対象を1体破壊" to "Aim with the right stick, release to fire　destroy one target",
        "マウスで照準・クリックで発射　対象を1体破壊" to "Aim with the mouse, click to fire　destroy one target",
        "起動診断 3/6　記憶片を検出: 星屑" to "Diagnostic 3/6　memory shards detected: dust",
        "回収してください" to "Collect them",
        "起動診断 4/6　緊急推進を確認します" to "Diagnostic 4/6　checking emergency thrust",
        "DASH ボタンで距離を取る" to "DASH to break away",
        "Shift でダッシュして距離を取る" to "Shift to dash away",
        "起動診断 5/6　近傍に記憶星を検出" to "Diagnostic 5/6　memory star detected nearby",
        "ナビの矢印へ" to "Follow the nav arrow",
        "起動診断 6/6　接続可能" to "Diagnostic 6/6　connection ready",
        "惑星かスキャンカードをタップで着陸" to "Tap the planet or its card to land",
        "[L] で着陸" to "[L] to land",
        "地表サーバーに接続　住民出力を確認" to "Connected to the surface server　observe the residents",
        "すべてが敵とは限りません" to "Not everything is hostile",
        "帰還パッドへ戻ると離陸できます" to "Return to the pad to lift off",
        "起動診断をスキップ" to "Boot diagnostic skipped",
        "起動診断 完了" to "Boot diagnostic complete",
        // v2.148 ヒント帯 — オンボーディング
        "左スティックで移動" to "Move with the left stick",
        "WASD で移動" to "Move with WASD",
        "右スティックで照準・離して発射" to "Aim with the right stick, release to fire",
        "マウスで照準・クリックで発射" to "Aim with the mouse, click to fire",
        "DASH でダッシュ" to "DASH to dash",
        "Shift でダッシュ" to "Shift to dash",
        "惑星に近づいて着陸" to "Approach a planet and land",
        // v2.148 ヒント帯 — 宇宙/地表の操作ヒント
        "訓練環境 — 模擬戦闘のみ" to "Training sim - combat drills only",
        "惑星をタップで着陸" to "Tap a planet to land",
        "惑星に近づいて [L] で着陸" to "Approach a planet, [L] to land",
        "[L] 着陸" to "[L] Land",
        "タップで着陸" to "Tap to land",
        "[L] 離陸" to "[L] Lift off",
        "[L] 離陸して宇宙へ" to "[L] Lift off to space",
        // 持物 / インベントリ
        "装備" to "Gear",
        "アイテム" to "Items",
        "マップ" to "Map",
        "市" to "Market",
        "セーブ" to "Save",
        "セーブする" to "Save Here",
        "推進器" to "Thruster",
        "防具" to "Armor",
        "遠距離" to "Ranged",
        "近距離" to "Melee",
        "装飾1" to "Charm 1",
        "装飾2" to "Charm 2",
        "装飾3" to "Charm 3",
        // 記録
        "起動診断をもう一度" to "Replay Boot Diagnostic",
        "討伐図鑑を見る" to "View Bestiary",
        "実績を見る" to "View Achievements",
        "記録へ戻る" to "Back to Records",
        "引き継ぎ" to "Transfer",
        "勤続記録" to "Service Record",
        "これから — 最初の出撃を待っている" to "Not yet - awaiting the first sortie",
        "書き出す（コピー）" to "Export (copy)",
        "取り込む（貼り付け）" to "Import (paste)",
        "書き出せなかった" to "Could not export",
        "取り込んだ — 記録と設定に反映した" to "Imported - records and settings applied",
        "取り込めなかった — クリップボードに引き継ぎ文がない" to "Nothing to import - no transfer text on the clipboard",
        "書き出し: 全記録と設定をクリップボードの一文に" to "Export: every record and setting becomes one line of text on the clipboard",
        "取り込み: 同じ文を貼って復元 — 今の記録は上書き" to "Import: paste it to restore - overwrites this device",
        // 行商船
        "離れる" to "Leave",
        "売る" to "Sell",
        "棚へ戻る" to "Back to Shelves",
        "前へ" to "Prev",
        "戻す" to "Undo",
        "次へ" to "Next",
    )

    /** v2.121 英語化第2弾: substring tokens for COMPOSED lines (HUD strip, notes, records).
     *  Applied longest-first so short tokens never bite into longer ones. Names come from the
     *  live catalogs, so a new weapon/card/achievement only needs its English name added here. */
    private val SUB: List<Pair<String, String>> = buildList {
        // HUD / records tokens
        add("まだ記録がない — 星々はこれからあなたを知る" to "No record yet - the stars have yet to know you")
        add("保守員は倒れた — 記録は星に残る" to "The keeper has fallen - the stars keep the record")
        add("旅の記録 — 星々はあなたを覚えている" to "The voyage - the stars remember you")
        add("最深 同期汚染 " to "Deepest desync ")
        add("最寄りの惑星 " to "Nearest planet ") // v2.148: the composed hint lines
        add("　工房へ送金 +" to "　wired to the workshop +") // v2.154 星屑の送金
        add("ジャンプゲート " to "Jump gate ")
        add("ゲート鍵 " to "Gate keys ")
        add("ウェーブ(旧式) " to "Wave (legacy) ")
        add("残プロセス " to "processes ")
        add("宙域安定 " to "stability ")
        add("同期汚染 " to "Desync ")
        add("総撃破 " to "total kills ")
        add("ウェーブ " to "Wave ")
        add("撃破 " to "kills ")
        add("時間 " to "Time ")
        add("資材 " to "mats ")
        add("所持 星屑 " to "Dust held ")
        add("星屑 " to "dust ")
        add("装填中" to "Reloading")
        add("予備 " to "Reserve ")
        add("過熱!" to "Overheat!")
        add("同期完了 " to "Syncs completed ")
        add("累計 " to "Total ")
        add("出撃 " to "sorties ")
        add("回 — 網は眠り、また編み直された" to " - the net slept, and was rewoven")
        add("実績解除" to "Unlocked ")
        add("実績 " to "Achievements ")
        // 記録 / スロット / 検証ラン
        add("スロット" to "Slot ")
        add("空き" to "empty")
        for (d in 1..7) add("残り${d}日" to "${d}d left")
        add("全員同じ宙域・同じ装備。" to "Same sky, same loadout for everyone. ")
        add("検証ラン " to "Proving Run ")
        add("（補助設定は有効）" to " (assist settings apply)")
        // 行商船
        add("行商船 — 買い取り " to "Trader - buyback ")
        add("行商船" to "Trader")
        add("売れる持物がない" to "Nothing to sell")
        add("─ 売約済 ─" to "- sold -")
        add("行をタップで売却" to "tap a row to sell")
        add("行をタップで購入" to "tap a row to buy")
        add(" を売った（+" to " sold (+")
        add(" を購入した（-" to " bought (-")
        add(" を棚から戻した（-" to " returned (-")
        add("屑が足りない — 戻せない" to "Not enough dust to take it back")
        add("屑）" to " dust)")
        add("屑】" to " dust]")
        add("【+" to " [+")
        add("【" to " [")
        add("討伐図鑑 " to "Bestiary ")
        // v2.142 英語化第3弾: species names — 162 kinds, auto-derived from their stable English ids
        // ("tyrant_shark" → "Tyrant Shark"). The bestiary, event feed and speech lines pick these up.
        io.github.panda17tk.arpg.config.GameConfig().enemies.forEach { (id, def) ->
            if (def.name.any { it.code >= 0x2E80 }) add(def.name to enName(id))
        }
        // v2.142: item names (thruster/armor/consumable/lore …) — id-derived with the category moved
        // to the tail ("armor_cloth" → "Cloth Armor"); gun items ride the weapon tokens as well.
        io.github.panda17tk.arpg.item.ItemCatalog.ALL.forEach { item ->
            if (item.name.any { it.code >= 0x2E80 }) add(item.name to enName(item.id))
        }
        // weapon names (from the live catalog — ids are stable)
        val weaponEn = mapOf(
            "pistol" to "Pistol", "shotgun" to "Shotgun", "mg" to "Machine Gun",
            "beam" to "Beam", "grenade" to "Grenade", "smg" to "SMG",
            "rifle" to "Rifle", "railgun" to "Railgun", "blade" to "Return Blade",
        )
        io.github.panda17tk.arpg.combat.Weapons.ALL.forEach { w -> weaponEn[w.id]?.let { add(w.name to it) } }
        // upgrade card names
        val cardEn = mapOf(
            "gun_dmg" to "Firepower", "fire_rate" to "Rapid Fire", "melee" to "Melee Art",
            "max_hp" to "Sturdy Frame", "speed" to "Swift Step", "ammo" to "Ammo Cache",
            "lifesteal" to "Lifesteal", "engineer" to "Fieldworks", "reload_fast" to "Deft Reload",
            "stamina_up" to "Thruster Extension", "blast_up" to "Wider Blast", "regen_up" to "Self-Repair",
            "dash_eff" to "Light Dash", "bullet_speed" to "Muzzle Velocity", "armor_up" to "Rolled Armor",
            "magnet_up" to "Gathering Hand",
        )
        io.github.panda17tk.arpg.upgrade.Upgrades.ALL.forEach { u -> cardEn[u.id]?.let { add(u.name to it) } }
        // achievement titles (they appear inside 『…』 in toasts and the record)
        val achEn = mapOf(
            "FIRST_LANDING" to "First Landing", "FIRST_JUMP" to "First Jump", "STAR_RETURNER" to "Star Returner",
            "KING_SLAYER" to "Kingslayer", "FIRST_HONE" to "First Honing", "SYNC_50" to "Signs of Recovery",
            "BOUNTY_HUNTER" to "Bounty Hunter", "DEEP_SURGE" to "Deep Surge", "QUEST_PATRON" to "Trusted Hand",
            "RELIC_KEEPER" to "Relic Keeper", "SYNC_90" to "Almost Reconnected", "HONED_MAX" to "Fully Honed",
            "GUARDIAN" to "Guardian", "OBSERVER" to "Observer", "SYSTEM_3" to "Third System",
            "DUST_RICH" to "Dust Collector", "QUIET_VISIT" to "Quiet Visit", "BEAST_HUNTER" to "Beast Hunter",
            "CHAIN_PATRON" to "Regular Caller", "STORM_WATCHER" to "Storm Record", "AURORA_GAZER" to "Under the Aurora",
            "METEOR_SURVIVOR" to "Through the Meteors", "ROGUE_SLAYER" to "Rogue Slayer", "RAGE_BREAKER" to "Rage Breaker",
            "GRAND_RITUAL" to "Ritual Witness", "COMBO_MASTER" to "Five-Beat Breath", "WORKSHOP_PATRON" to "Workshop Patron",
            "WORKSHOP_MASTER" to "Workshop Regular", "TRAIT_ARRIVAL" to "Tempered Skies", "GATE_READY" to "Keys in Hand",
            "FINAL_SYNC" to "The Last Keeper", "DRIFT_ON" to "Still Drifting", "VAULT_DELVER" to "Vault Delver",
            "TRADER_CLIENT" to "Trader's Client", "LIFELINE" to "Lifeline", "TRADE_LEDGER" to "Star Ledger",
            "BESTIARY_50" to "A Thicker Book", "BESTIARY_FULL" to "Complete Record",
        )
        io.github.panda17tk.arpg.save.Achievement.entries.forEach { a -> achEn[a.name]?.let { add(a.title to it) } }
    }.sortedByDescending { it.first.length }
}
