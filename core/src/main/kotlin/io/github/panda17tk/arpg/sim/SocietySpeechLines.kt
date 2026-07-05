package io.github.panda17tk.arpg.sim

/** What a society's deeds-memory makes its people say — chosen by the gravest thing the player has done here. */
enum class SocietySpeechTrigger {
    ChildHarmed, ChildKilled, PredatorRepelled, ApexKilled, NestDestroyed,
    MercyHigh, HostilityHigh, RelicTaken, ReturnVisitHostile, ReturnVisitMerciful,
}

/**
 * Data-driven memory-flavoured lines. The society remembers ([PlanetSocietyState]); this picks what its people
 * say about it. Pure (mirrors [SpeechLines]) → unit-testable; AISystem wires it into the existing speech flow.
 */
object SocietySpeechLines {
    private val LINES = mapOf(
        SocietySpeechTrigger.ChildHarmed to listOf(
            "弱きものに手を出したな", "子らを奥へ", "その名を覚えた",
            "泣き声が聞こえなかったのか", "傷は癒える。記憶は癒えない", // v2.82
        ),
        SocietySpeechTrigger.ChildKilled to listOf(
            "子を殺したな", "許さぬ", "星がその罪を見ていた", "第4条違反を記録した。星は消去APIを持たない",
            "その手で、よくも", "あの子の名を言えるのか", // v2.82
        ),
        SocietySpeechTrigger.PredatorRepelled to listOf(
            "……今のは、我らの子を守ったのか", "借りはできた", "森は見ていた",
            "礼を言う。慣れないが", "今夜は火にあたっていけ", // v2.82
        ),
        SocietySpeechTrigger.ApexKilled to listOf(
            "森の主が沈んだ", "これで均衡は崩れる", "お前は勝った。だが星は痩せる",
            "主なき森は騒がしくなる", "強さの証か。重い証だ", // v2.82
        ),
        SocietySpeechTrigger.NestDestroyed to listOf(
            "巣が、壊された", "卵を、よくも", "生まれる前に奪うのか",
            "母は一晩中鳴いていた", "来年の春は静かになる", // v2.82
        ),
        SocietySpeechTrigger.MercyHigh to listOf(
            "撃つな。この者は子を救った", "ただの狩人ではない", "客人として迎えよ",
            "この者の通り道を空けよ", "星がこの者を柔らかく照らしている", // v2.82
        ),
        SocietySpeechTrigger.HostilityHigh to listOf(
            "警告はいらない", "来たぞ、子殺しだ", "星がその名を拒んでいる",
            "灯りを消せ、あれが来た", "祈っても遅い", // v2.82
        ),
        SocietySpeechTrigger.RelicTaken to listOf(
            "聖宝を持ち去る気か", "それは我らのものだ", "返せ", "それは聖宝ではない。鍵だ。持ち出せば扉は閉じる",
            "台座が空だと、夜が長い", "持ち出した先で、それが泣くぞ", // v2.82
        ),
        SocietySpeechTrigger.ReturnVisitHostile to listOf(
            "また来たか、子殺し", "よくも戻れたものだ", "今度は帰さぬ", "あなたの署名は拒否リストにある",
            "去った時のまま帰れると思ったか", "この星の夜は、お前を覚えている", // v2.82
        ),
        SocietySpeechTrigger.ReturnVisitMerciful to listOf(
            "戻ってきてくれたのか", "あの恩は忘れていない", "おかえり、星の友よ", "あなたの署名は白名簿に載っている",
            "席は空けてある", "子らが、あなたの真似をして遊ぶのだ", // v2.82
        ),
    )

    /** v2.82: every line this object can speak (for content-size tests). */
    fun lineCount(): Int = LINES.values.sumOf { it.size }

    /** Deterministic pick within the trigger's line set (salt varies the choice). Null if no lines. */
    fun pick(trigger: SocietySpeechTrigger, salt: Int): String? {
        val l = LINES[trigger] ?: return null
        if (l.isEmpty()) return null
        return l[((salt % l.size) + l.size) % l.size]
    }

    /**
     * The most salient line a reacting creature would say about what the player has done here, gravest first
     * (a slain child outweighs a high-hostility gauge). Null when the society has nothing pointed to say.
     */
    fun triggerFor(s: PlanetSocietyState): SocietySpeechTrigger? = when {
        s.childKilled -> SocietySpeechTrigger.ChildKilled
        s.apexKilled -> SocietySpeechTrigger.ApexKilled
        s.nestMotherKilled || s.hatchlingKilled -> SocietySpeechTrigger.NestDestroyed
        s.childHarmed -> SocietySpeechTrigger.ChildHarmed
        s.predatorKilledNearChild -> SocietySpeechTrigger.PredatorRepelled
        s.relicClaimed -> SocietySpeechTrigger.RelicTaken
        s.hostility >= HOSTILE_SPEAK -> SocietySpeechTrigger.HostilityHigh
        s.mercy >= MERCY_SPEAK -> SocietySpeechTrigger.MercyHigh
        else -> null
    }

    /** A greeting for a remembered planet at landing — hostile or merciful by which feeling dominates (or null). */
    fun returnGreeting(s: PlanetSocietyState): SocietySpeechTrigger? = when {
        s.hostility >= RETURN_SPEAK && s.hostility >= s.mercy -> SocietySpeechTrigger.ReturnVisitHostile
        s.mercy >= RETURN_SPEAK -> SocietySpeechTrigger.ReturnVisitMerciful
        else -> null
    }

    private const val HOSTILE_SPEAK = 0.5f
    private const val MERCY_SPEAK = 0.5f
    private const val RETURN_SPEAK = 0.3f
}
