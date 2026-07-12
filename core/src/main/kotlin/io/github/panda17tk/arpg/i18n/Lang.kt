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
        "起動診断を実行しますか？" to "Run the boot diagnostic?", // v2.171 手直し: the pared-down boot prompt
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
        "暴君の縄張りに入った" to "You have entered the tyrant's water", // v2.157 読む海
        "依頼 完了 — この星は満ちている" to "Requests complete - this star is content", // v2.159 英語化第4弾
        // v2.162 英語化第4弾(後半): the ending — pages, choice, epilogue, tap hints
        "署名照合、完了。……最終保守員、応答を確認。" to "Signature verified. ...Final keeper, response confirmed.",
        "外部同期 99%。残る 1% は、仕様を書いた者たちの領分だった。" to "External sync at 99%. The last 1% belonged to those who wrote the specifications.",
        "彼らはもういない。だが網は、あなたの手で編み直された。" to "They are gone now. But the net has been rewoven by your hands.",
        "星々は覚えている。守られた子らを。討たれた王を。返された遺物を。" to "The stars remember. The children kept safe. The kings brought down. The relics returned.",
        "選んでほしい。" to "Choose.",
        "最後の 1% を引き受け、網とともに眠りにつくか——" to "Take on the last 1% and go to sleep with the net -",
        "それとも切断し、このまま漂い続けるか。" to "or disconnect, and keep drifting as you are.",
        "同期を完了して、眠りにつく" to "Complete the sync and sleep",
        "切断して、漂流を続ける" to "Disconnect and keep drifting",
        "同期完了。全系、静かに稼働。" to "Sync complete. All systems running, quietly.",
        "人類保全装置群は、次の朝を待つ姿勢に戻る。" to "The preservation machinery settles back to waiting for the next morning.",
        "星々は、あなたを覚えている。" to "The stars remember you.",
        "——最終保守員の記録、ここに閉じる。" to "- The final keeper's record closes here.",
        "切断を記録した。網は、それでもあなたを覚えている。" to "Disconnection logged. The net remembers you, even so.",
        "タップで続ける" to "Tap to continue",
        "タップで記録を閉じる" to "Tap to close the record",
        "ボタン配置を編集" to "Edit Button Layout", // v2.163
        "海の密度" to "Ocean Density", // v2.165
        "性能表示" to "Performance HUD", // v2.167
        "fps・sim・描画時間を左上に表示" to "Show fps, sim and draw times top-left",
        "宇宙の魚の数（次の空から反映）" to "How many fish fill the sky (applies from the next sky)",
        "高" to "High",
        "中" to "Medium",
        "低" to "Low",
        "漂流者を救助した — 礼にと星屑40を分けてくれた" to "Rescued a drifter - they shared 40 dust in thanks",
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
        // v2.170 文字の消灯: the pared-down compasses. Arrow-anchored so the tokens never bite
        // into star names mid-translation (「氷惑星を制圧した」→「氷惑星 is subdued」would offer
        // a bare「惑星 」to a greedy token; the arrow keeps these to the compass lines alone).
        for (a in listOf("→", "←", "↑", "↓")) {
            add("惑星 $a " to "planet $a ")
            add("門 $a " to "gate $a ")
        }
        add("鍵 " to "keys ")
        add("エリア " to "area ")
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
        add("戦闘枠 " to "combat ") // v2.161 細かい残り: the bestiary header's second tally
        add("隣の宙域へ — エリア " to "Into the next sky - area ") // v2.166 宙域の九分割
        // v2.162 英語化第4弾(後半): the memory cores' whole vocabulary — every pool line,
        // behind its prefix. Full-sentence tokens, so nothing bites into another line.
        add("記憶核: " to "Memory core: ")
        add("遭難記録: " to "Distress log: ")
        // v2.163 英語化第4弾(後半・その2): the inventory rows now ride tr() — markers,
        // slot labels, and every readable's one-line description.
        add("【合成可】" to "[Merge OK] ")
        add("【使】" to "[Use] ")
        add("【読】" to "[Read] ")
        add("（なし）" to "(none)")
        add("推進器：" to "Thruster: ")
        add("防具：" to "Armor: ")
        add("遠距離：" to "Ranged: ")
        add("近距離：" to "Melee: ")
        add("装飾1：" to "Charm 1: ")
        add("装飾2：" to "Charm 2: ")
        add("装飾3：" to "Charm 3: ")
        add("操作: " to "Controls: ")
        add("銃=ボタン / 近接=右スティック" to "gun = button / melee = right stick")
        add("近接=ボタン / 銃=右スティック" to "melee = button / gun = right stick")
        add("　(タップで入替)" to "　(tap to swap)")
        add("『" to "'")
        add("』" to "'")
        add("整備士の走り書き" to "A mechanic's scrawl")
        add("血の染みがある" to "Blood-stained")
        add("森の岩壁から写し取られた" to "Taken from a forest rock wall")
        add("凍った石板に刻まれている" to "Carved on a frozen tablet")
        add("読める部分だけを書き写した" to "Only the legible parts, copied out")
        add("嵐の中で歌われる" to "Sung in the storm")
        add("風化した石碑の写し" to "A copy of a weathered stone")
        add("誰かの手記の切れ端" to "A scrap of someone's notes")
        add("赤字の注意書きだらけ" to "Covered in red warnings")
        add("旅人の間で口伝えされる" to "Passed down among travelers")
        add("どの港にも貼られている" to "Posted in every port")
        add("途切れ途切れの受信ログ" to "A broken reception log")
        add("几帳面な字で書かれている" to "Written in a meticulous hand")
        add("油染みたメモ" to "A grease-stained memo")
        add("22世紀の事務端末から" to "From a 22nd-century office terminal")
        add("最後の工学部の教材" to "From the last engineering faculty")
        add("破損したPDFの印刷" to "A print of a corrupted PDF")
        add("全星系に複製された最後の報告" to "The last report, copied to every system")
        add("どの星の祭壇にも刻まれている" to "Carved on every planet's altar")
        add("拾った者への注記" to "A note to whoever picks these up")
        add("記憶核から漏れた断片" to "A fragment leaked from a memory core")
        add("船室に残されていた最初の一冊" to "The first volume left in the cabin")
        add("字が乱れている" to "The handwriting wavers")
        add("死の星の石板より" to "From a dead planet's tablet")
        add("跳躍者向けの古い規程" to "Old rules for jumpers")
        add("最後の頁。筆跡は落ち着いている" to "The last page. The hand is steady")
        add("読める部分だけを写した" to "Only the legible parts, copied")
        add("外部同期: 停止から 9 万 4 千日。ローカル保全モードを継続する" to "External sync: 94,000 days since loss. Local preservation mode continues")
        add("訪問者を記録した。評価は保留。危害があれば追記される" to "Visitor logged. Assessment withheld. Harm, if any, will be appended")
        add("この記録は消去できない。人類保全ポリシー第5条: 死者を忘れるな" to "This record cannot be erased. Human Preservation Policy, Article 5: forget no one who died")
        add("説明を要求されたが、説明できる者の人格ログが見つからない" to "An explanation was requested, but no persona log capable of explaining can be found")
        add("修復トークン(星屑)の回収率が低下している。それでも出力は続く" to "Recovery of repair tokens (dust) is slowing. Output continues regardless")
        add("root key の照合に失敗。上位ノードは応答しない。単独で保つ" to "Root key verification failed. Upstream nodes silent. Holding alone")
        add("生態系復元モデル: 縮退運転。第 812 世代の芽吹きを維持中" to "Ecosystem restoration model: degraded mode. Sustaining the 812th generation's budding")
        add("この森はバックアップである。原本は失われた。だから枯らせない" to "This forest is a backup. The original is lost. So it must not wither")
        add("地熱炉: 定格の 31%。それでも記憶核の冷却は優先される" to "Geothermal furnace: 31% of rated output. The memory core's cooling still takes priority")
        add("採掘統治モデル: 稼働。掘る者がいなくても、鉱脈台帳は更新される" to "Mining governance model: running. No one digs, yet the vein ledger is kept current")
        add("低温アーカイブ: 正常。眠る人格 217 万件。解凍要求: 0 件" to "Cryo archive: nominal. Sleeping personas: 2.17 million. Thaw requests: 0")
        add("冬眠プロトコルは終わらない。起こす者が、もういないからだ" to "The hibernation protocol never ends. There is no one left to wake them")
        add("通信中継モデル: 待機。最後に中継した声は 700 年前の子守唄" to "Relay model: standby. The last voice relayed was a lullaby, 700 years ago")
        add("気象予測は今日も的中した。確認する者は、いない" to "Today's weather forecast was correct again. No one is here to check")
        add("追悼アーカイブ: 低消費運転。名前の朗読は第 4 億周目に入った" to "Memorial archive: low-power mode. The reading of names has entered its 400 millionth cycle")
        add("この星の最後の記録: 「灯りを消さないで」。要求は履行中" to "This planet's last record: 'Do not turn off the lights.' The request is being honored")
        add("個人用保全基盤: 単独稼働。世帯人格 1 件を維持している" to "Personal preservation unit: standalone. Maintaining 1 household persona")
        add("家庭内モデル: 「おかえり」を再生する相手を 3 千年待っている" to "Household model: waiting 3,000 years for someone to play 'welcome home' to")
        add("訪問者の署名を照合中。過去の保守員名簿に部分一致がある" to "Matching the visitor's signature. Partial hit in the old keeper roster")
        add("あなたの機体番号は、退役済みの保守端末のものと一致する" to "Your hull number matches a decommissioned maintenance terminal")
        add("あなたの歩幅は、記録にある巡回員の歩幅と 97% 一致する" to "Your stride matches a patroller's on record, at 97%")
        add("隣の星系から照会が届いている。『その訪問者は、また来たのか』と" to "An inquiry arrived from the next system: 'Has that visitor come again?'")
        add("あなたが直した箇所の記録が残っている。あなたは覚えていないようだが" to "Records remain of what you repaired. You do not seem to remember")
        add("照合完了。あなたの人格ログの作成日は、あなたの記憶する誕生日より後だ" to "Match complete. Your persona log was created after the birthday you remember")
        add("おかえりなさい、最終保守員。巡回の再開を記録した" to "Welcome back, final keeper. Resumption of patrol has been logged")
        add("あなたは住民たちと同じ、記憶からの出力だ。それでも巡回は続いている" to "You are an output from memory, the same as the residents. The patrol continues even so")
        add("あなたの原本は、ここには保存されていない。どこにも、保存されていない" to "Your original is not stored here. It is not stored anywhere")
        add("第4条: 子を保護せよ。あなたがそれを守るたび、あなたの署名は彼のものに近づく" to "Article 4: protect the children. Each time you keep it, your signature drifts closer to his")
        add("人類保全ポリシーは訪問者にも適用される。あなたも、保全対象だ" to "The Human Preservation Policy applies to visitors too. You are also to be preserved")
        add("救難信号 7,040日目。応答なし。今日から数えるのをやめる" to "Distress signal, day 7,040. No response. As of today the count stops")
        add("航法AIは最後まで謝り続けていた。『君のせいじゃない』と返した記録が残っている" to "The nav AI kept apologizing to the end. A reply remains on file: 'It was not your fault'")
        add("積荷: 人類保全ポリシーの写し、420部。読める者への配達予定だった" to "Cargo: 420 copies of the Human Preservation Policy, bound for anyone who could still read")
        add("乗員はコールドスリープを選択。起こしに来る者を、まだ待っている" to "The crew chose cold sleep. They are still waiting for someone to come wake them")
        add("この船は星系ノードの修理へ向かう途中だった。部品はまだ貨物室にある" to "This ship was en route to repair the system node. The parts are still in the hold")
        add("最後の通信: 『灯りは点けたままにしておく』。灯りは、点いている" to "Last transmission: 'We will leave the lights on.' The lights are on")
        add("船体の破断は衝突によるもの。防衛プロセスの誤認記録が添付されている" to "Hull breach caused by collision. A defence-process misidentification report is attached")
        add("航海日誌の最終頁: 『帰ったら、子どもに星の名前を教える』" to "Last page of the log: 'When I get home, I will teach my child the names of the stars'")
        // v2.162: the event banner now funnels through tr() — the wave calls ride along
        add("⚠ 強大な気配が近づく" to "⚠ Something vast draws near")
        add("強敵の気配" to "A strong foe stirs")
        add("大群が接近している" to "A horde is closing in")
        add("磁気嵐 — 敵は荒ぶり、星屑は多くこぼれる" to "Magnetic storm - foes rage, and dust spills freely")
        add("賞金首『" to "Bounty head 『")
        add("』が現れた" to "』 has appeared")
        add("清掃プロトコル起動 — 保守機構が展開する" to "Purge protocol engaged - the custodial machinery deploys")
        add("流星群 — 岩塊が降りしきる。落下点から離れよ" to "Meteor shower - rock falls thick. Stay clear of the impact points")
        // v2.159 英語化第4弾(前半): the surface objective line (biome name + situation),
        // the goal chips, and the quest chip/lines. Generic （）：！ fallbacks sort shortest
        // and run last, so composed lines close cleanly in ASCII.
        add("自然惑星" to "Nature Planet")
        add("火山惑星" to "Volcanic Planet")
        add("氷惑星" to "Ice Planet")
        add("ガス惑星" to "Gas Planet")
        add("死の惑星" to "Dead Planet")
        add("孤独な小惑星" to "Lonely Asteroid")
        add("聖なる子が殺された　星は許さない" to "A sacred child was slain　the star will not forgive")
        add("弱きものが失われた　部族は怒っている" to "The weak were lost　the tribe is enraged")
        add("子らを傷つけた　星は怒っている" to "The children were hurt　the star is angered")
        add("弱きものを傷つけた　守護者が奮い立つ" to "The weak were hurt　the guardians stir")
        add("この星はあなたを敵として覚えている" to "This star remembers you as an enemy")
        add("この星はあなたへの借りを覚えている" to "This star remembers its debt to you")
        add("神獣は倒れた　星の均衡が崩れている" to "The sacred beast has fallen　the balance is breaking")
        add("捕食者を退けた　森はあなたを見ている" to "A predator was driven off　the forest is watching you")
        add("巣が荒らされた　野生がざわめく" to "A nest was ravaged　the wild is stirring")
        add("森の主を倒した　生態系が揺らいでいる" to "The forest's master has fallen　the ecosystem trembles")
        add("この星の主を倒せ" to "Defeat this star's masters")
        add("主を倒した　素材を回収せよ" to "The master has fallen　recover the materials")
        add("を制圧した　脱出パッドへ戻れ" to " is subdued　return to the escape pad")
        add("（残り " to " (left ")
        add("残り " to "left ")
        add("神獣に触れてしまった" to "The sacred beast was touched")
        add("神獣に触れるな" to "Do not touch the sacred beast")
        add("星の主を倒した" to "The star's master has fallen")
        add("星の主を倒せ" to "Defeat the star's master")
        add("遺物を手にした" to "Relic in hand")
        add("遺物" to "Relic")
        add("子らを守れ" to "Protect the children")
        add("主を倒した" to "Masters defeated")
        add("主を倒せ" to "Defeat the masters")
        // quest lines (QuestDef.line) and the chip around them — the count rides ×N
        add("依頼: 精鋭を" to "Request: defeat elites ×")
        add("依頼: 外敵を" to "Request: defeat hostiles ×")
        add("依頼: 記憶片を" to "Request: gather memory shards ×")
        add("依頼: 記憶核と照合する（" to "Request: sync with the memory core (")
        add("依頼: 捕食者を" to "Request: drive off predators ×")
        add("体討つ（" to " (")
        add("回収する（" to " (")
        add("体退ける（" to " (")
        add("秒の定点観測（" to "s of field observation (")
        add("依頼: " to "Request: ")
        add("依頼を果たした +" to "Request fulfilled +")
        add("屑 — 次の頼みが届いた" to " dust - the next request has arrived")
        add("屑 — この星の頼みはすべて済んだ" to " dust - this star asks nothing more")
        add("　※この空なら" to "　* this sky pays ")
        add("屑　" to " dust　")
        add("依頼" to "Request ")
        add("（" to " (")
        add("）" to ")")
        add("：" to ": ")
        add("！" to "!")
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
            "BESTIARY_COMBAT" to "The Maintenance Record", // v2.158 図鑑の二段
        )
        io.github.panda17tk.arpg.save.Achievement.entries.forEach { a -> achEn[a.name]?.let { add(a.title to it) } }
        // v2.159 英語化第4弾(前半): achievement descriptions — the record page reads whole.
        val descEn = mapOf(
            "FIRST_LANDING" to "Set foot on a planet for the first time",
            "FIRST_JUMP" to "Crossed to the next system through a jump gate",
            "STAR_RETURNER" to "Earned deep trust from two planets",
            "KING_SLAYER" to "Felled the king on two planets",
            "FIRST_HONE" to "Honed a weapon for the first time",
            "SYNC_50" to "Interstellar sync recovery reached 50%",
            "BOUNTY_HUNTER" to "Took down a bounty head",
            "DEEP_SURGE" to "Survived desync 15",
            "QUEST_PATRON" to "Lifted off with a star's request fulfilled",
            "RELIC_KEEPER" to "Brought home a sleeping relic",
            "SYNC_90" to "Interstellar sync recovery reached 90%",
            "HONED_MAX" to "Honed a weapon to +3",
            "GUARDIAN" to "Fulfilled a protection request",
            "OBSERVER" to "Fulfilled an observation request",
            "SYSTEM_3" to "Reached a third star system",
            "DUST_RICH" to "Held 500 dust at once",
            "QUIET_VISIT" to "Left a planet without hurting anyone",
            "BEAST_HUNTER" to "Felled the beast king on two planets",
            "CHAIN_PATRON" to "Saw one star's requests through to the end",
            "STORM_WATCHER" to "Landed on a thunderstorm planet",
            "AURORA_GAZER" to "Landed under an aurora",
            "METEOR_SURVIVOR" to "Survived a meteor-shower wave",
            "ROGUE_SLAYER" to "Defeated a rogue drifter",
            "RAGE_BREAKER" to "Defeated an enraged heavyweight",
            "GRAND_RITUAL" to "Witnessed the rite of the kill",
            "COMBO_MASTER" to "Carried the combo to its highest beat",
            "WORKSHOP_PATRON" to "Commissioned the workshop's first craft",
            "WORKSHOP_MASTER" to "Mastered one workshop craft",
            "TRAIT_ARRIVAL" to "Reached a system with a temperament",
            "GATE_READY" to "Bundled enough gate keys for the jump",
            "FINAL_SYNC" to "Completed the sync, and went to sleep with the net",
            "DRIFT_ON" to "At the last choice, disconnected and drifted on",
            "VAULT_DELVER" to "Stood at the deepest point of a sealed vault",
            "TRADER_CLIENT" to "Bought from the wandering trader",
            "LIFELINE" to "Rescued a survivor from a wreck",
            "TRADE_LEDGER" to "Sold belongings to the trader",
            "BESTIARY_50" to "Recorded 50 kinds in the bestiary",
            "BESTIARY_FULL" to "Recorded every kind in the bestiary",
            "BESTIARY_COMBAT" to "Recorded every combat entry (all but the wild)",
        )
        io.github.panda17tk.arpg.save.Achievement.entries.forEach { a -> descEn[a.name]?.let { add(a.desc to it) } }
    }.sortedByDescending { it.first.length }
}
