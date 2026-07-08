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
        "被弾などの振動" to "Vibration on hits",
        "スティック右・ボタン左に反転" to "Stick right, buttons left",
        "着陸方法などの操作ガイド" to "Guides like how to land",
        "記憶核の語り・遭難記録" to "Memory-core voices, distress logs",
        "画面シェイクと反動 (酔い対策はOFF)" to "Shake & recoil (OFF if motion-sick)",
        "撃破の白フラッシュを弱める" to "Dim the white kill flashes",
        "射撃が近くの敵へわずかに寄る" to "Shots lean gently toward foes",
        "主要ラベルを英語で表示" to "Show main labels in English",
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
        "記録へ戻る" to "Back to Records",
        "引き継ぎ" to "Transfer",
        "書き出す（コピー）" to "Export (copy)",
        "取り込む（貼り付け）" to "Import (paste)",
        "書き出せなかった" to "Could not export",
        "取り込んだ — 記録と設定に反映した" to "Imported - records and settings applied",
        "取り込めなかった — クリップボードに引き継ぎ文がない" to "Nothing to import - no transfer text on the clipboard",
        "書き出すと、すべての記録と設定がひとつの引き継ぎ文になり" to "Export turns every record and setting into one block of text",
        "クリップボードへ入る（メモ帳などに貼って持ち運べる）" to "on the clipboard (paste it into notes to carry it)",
        "新しい端末で同じ文をコピーしてから取り込むと、記録がこの形に戻る" to "Copy that text on the new device, then Import to restore",
        "取り込みは今の記録を上書きする——先に書き出しておくと安全" to "Import overwrites this device - export first to be safe",
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
        add("回 — 網は眠り、また編み直された" to " - the net slept, and was rewoven")
        add("実績解除" to "Unlocked ")
        add("実績 " to "Achievements ")
        // 記録 / スロット / 検証ラン
        add("スロット" to "Slot ")
        add("空き" to "empty")
        for (d in 1..7) add("残り${d}日" to "${d}d left")
        add("全員同じ宙域・同じ装備。" to "Same sky, same loadout for everyone. ")
        add("検証ラン " to "Proving Run ")
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
