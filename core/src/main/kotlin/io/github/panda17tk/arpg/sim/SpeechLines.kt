package io.github.panda17tk.arpg.sim

/** Data-driven short lines intelligent creatures say, chosen by behavioural trigger. Pure. */
object SpeechLines {
    enum class Trigger {
        Warn, Aggro, Flee, Beg, Hide, Rest,
        ProtectChild, ProtectKing, Rally, KingEncounter, LonelyEncounter, Surrender,
    }

    private val LINES = mapOf(
        Trigger.Warn to listOf(
            "近づくな", "ここは我らの土地だ", "それ以上来るな", "巣に近づくな", "未知の訪問者。評価を開始する",
            "その一歩が境界だ", "来訪の目的を述べよ", "武器を下げれば通れる", // v2.82
        ),
        Trigger.Aggro to listOf(
            "警告はした", "敵だ！", "守れ！",
            "囲め！", "逃がすな！", "陣を組め！", "来たか——構えろ", // v2.82
        ),
        Trigger.Flee to listOf(
            "勝てない…！", "退け！", "王に知らせろ！", "こいつは危険だ！", "私を保存しろ……誰か、私を保存しろ",
            "散れ、散れ！", "巣へ戻れ！", "あれは手に負えない", // v2.82
        ),
        Trigger.Beg to listOf(
            "待て、殺すな", "降参する", "もう戦えない", "見逃してくれ", "終了させないでくれ。まだ出力の途中だ",
            "家族がいるんだ", "何も持っていない", "頼む、目を見てくれ", // v2.82
        ),
        Trigger.Hide to listOf(
            "やり過ごす…", "傷を癒やせ", "まだ終わっていない",
            "息を殺せ", "音を立てるな", "嵐が過ぎるのを待つ", // v2.82
        ),
        Trigger.Rest to listOf(
            "息を整えろ", "傷が塞がるまで…",
            "少しだけ休む", "水を……", "まだ立てる、まだ", // v2.82
        ),
        Trigger.ProtectChild to listOf(
            "子らには触れさせない", "後ろへ下がれ", "この子だけは守る", "弱きものを狙うな！", "子を守れ。第4条だ",
            "私の後ろにいろ", "目を閉じていなさい", // v2.82
        ),
        Trigger.ProtectKing to listOf(
            "王に近づくな", "盾となれ", "我らが王を守れ",
            "玉座は通さない", "命に代えても", "王よ、下がられよ", // v2.82
        ),
        Trigger.Rally to listOf(
            "恐れるな！", "今こそ立て！", "この星を守れ！",
            "隣を見ろ、独りではない！", "祖霊が見ている！", "退けば明日はないぞ！", // v2.82
        ),
        Trigger.KingEncounter to listOf(
            "我が星から去れ", "小さき漂流者よ", "ここはお前の狩場ではない", "我らにも暮らしがある", "去れ。……本プロセスは、そう告げる役目だ",
            "名を名乗れ、来訪者", "お前の星はどこにある", "戦いか、対話か。選べ", // v2.82
        ),
        Trigger.LonelyEncounter to listOf(
            "まだ誰かいたのか", "久しぶりの客だ", "撃つなら早くしろ", "話す気はあるか", "客の来訪を記録した。じつに久しい追記だ",
            "座れ。茶はないが", "外は静かだったか", "灯りは絶やしていない", // v2.82
        ),
        Trigger.Surrender to listOf(
            "武器を置く", "もう争わない", "命だけは",
            "両手を上げる", "抵抗はしない", "好きにしろ", "…負けだ", // v2.82
        ),
    )

    /** v2.82 性格レジスタ: a species' voice colours what it says. Styled lines lead; the
     *  plain set fills in behind them, so every trigger always has something to say. */
    private val STYLED: Map<String, Map<Trigger, List<String>>> = mapOf(
        "mechanical" to mapOf(
            Trigger.Warn to listOf("照合失敗。退去を勧告する", "権限がない。停止せよ"),
            Trigger.Aggro to listOf("排除手順を開始", "対象を脅威に再分類した"),
            Trigger.Flee to listOf("損耗が閾値を超えた。離脱する", "バックアップ地点へ"),
            Trigger.Beg to listOf("停止要求。データはまだ未保存だ", "消去は取り消せない。どうか"),
            Trigger.Rally to listOf("全ユニット、隊列を維持", "防衛プロトコル、続行"),
            Trigger.Surrender to listOf("全機能を停止する。処分を待つ", "書き込み権限を放棄する"),
            Trigger.KingEncounter to listOf("管理者権限を確認できない。去れ", "このノードは我々が保守している"),
            Trigger.LonelyEncounter to listOf("応答があった。……応答があった！", "最終ログイン、遥か昔"),
        ),
        "savage" to mapOf(
            Trigger.Warn to listOf("嗅ぎ慣れない匂いだ", "骨を折られたいか"),
            Trigger.Aggro to listOf("血だ！", "潰せ！", "牙を見せろ！"),
            Trigger.Flee to listOf("覚えてろ！", "今日のところは……！"),
            Trigger.Beg to listOf("くそ……頼む", "牙を折る。だから"),
            Trigger.Rally to listOf("吼えろ！", "群れの力を見せろ！"),
            Trigger.Surrender to listOf("腹を見せる。それが掟だ", "……あんたが上だ"),
            Trigger.KingEncounter to listOf("俺の縄張りで何をしている", "強い奴は嫌いじゃない。だが去れ"),
            Trigger.LonelyEncounter to listOf("殴り合いか、酒か。どっちだ", "静かすぎて牙が鈍る"),
        ),
        "archaic" to mapOf(
            Trigger.Warn to listOf("そこな漂泊の御方、留まられよ", "この地は古き盟約の内にある"),
            Trigger.Aggro to listOf("是非もなし", "いざ、参る"),
            Trigger.Flee to listOf("御免——", "この身、まだ果たせぬ役目がある"),
            Trigger.Beg to listOf("武士の情けを", "願わくば、これまでの非礼を許されよ"),
            Trigger.Rally to listOf("者ども、続け！", "祖の名にかけて！"),
            Trigger.Surrender to listOf("参った。見事である", "刃を収める。約定に従おう"),
            Trigger.KingEncounter to listOf("客人よ、名乗られよ", "古き玉座は今も空けておらぬ"),
            Trigger.LonelyEncounter to listOf("よくぞ参られた、この果てへ", "語り相手は星々のみであった"),
        ),
        "polite" to mapOf(
            Trigger.Warn to listOf("恐れ入りますが、ここまでです", "それ以上はご遠慮ください"),
            Trigger.Aggro to listOf("残念です。応戦いたします", "交渉は決裂、ですね"),
            Trigger.Flee to listOf("失礼いたします……！", "また日を改めて"),
            Trigger.Beg to listOf("どうか、お慈悲を", "お手を煩わせません。ですから"),
            Trigger.Rally to listOf("皆さま、落ち着いて。持ち場へ", "手順どおりに参りましょう"),
            Trigger.Surrender to listOf("敗北を認めます", "お見事でした。従います"),
            Trigger.KingEncounter to listOf("ようこそ。ですが歓迎はできかねます", "手土産のない客は初めてです"),
            Trigger.LonelyEncounter to listOf("お茶でもいかがですか。何もありませんが", "お名前を伺っても？"),
        ),
    )

    /** Deterministic pick within the trigger's line set (salt varies the choice). Null if no lines.
     *  v2.82: [style] prepends the species' register — a savage warns differently than a scribe. */
    fun pick(trigger: Trigger, salt: Int, style: String = ""): String? {
        val styled = STYLED[style]?.get(trigger).orEmpty()
        val l = styled + LINES[trigger].orEmpty()
        if (l.isEmpty()) return null
        return l[((salt % l.size) + l.size) % l.size]
    }

    /** v2.82: every line the game can speak through this object (for content-size tests). */
    fun lineCount(): Int = LINES.values.sumOf { it.size } + STYLED.values.sumOf { m -> m.values.sumOf { it.size } }

    /** Which line set fits a behavioural state (null = the creature stays silent in that state). */
    fun forState(state: CreatureState): Trigger? = when (state) {
        CreatureState.Warn -> Trigger.Warn
        CreatureState.Hostile -> Trigger.Aggro
        CreatureState.Flee -> Trigger.Flee
        CreatureState.Beg -> Trigger.Beg
        CreatureState.Hide -> Trigger.Hide
        CreatureState.Rest -> Trigger.Rest
        CreatureState.Protect -> Trigger.ProtectChild
        CreatureState.Rally -> Trigger.Rally
        CreatureState.Surrender -> Trigger.Surrender
        CreatureState.Ignore -> null
    }
}
