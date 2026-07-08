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

    fun tr(s: String): String = if (!en) s else TABLE[s] ?: s

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
        // 行商船
        "離れる" to "Leave",
        "売る" to "Sell",
        "棚へ戻る" to "Back to Shelves",
        "前へ" to "Prev",
        "戻す" to "Undo",
        "次へ" to "Next",
    )
}
