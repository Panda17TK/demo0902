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
        SocietySpeechTrigger.ChildHarmed to listOf("弱きものに手を出したな", "子らを奥へ", "その名を覚えた"),
        SocietySpeechTrigger.ChildKilled to listOf("子を殺したな", "許さぬ", "星がその罪を見ていた", "第4条違反を記録した。星は消去APIを持たない"),
        SocietySpeechTrigger.PredatorRepelled to listOf("……今のは、我らの子を守ったのか", "借りはできた", "森は見ていた"),
        SocietySpeechTrigger.ApexKilled to listOf("森の主が沈んだ", "これで均衡は崩れる", "お前は勝った。だが星は痩せる"),
        SocietySpeechTrigger.NestDestroyed to listOf("巣が、壊された", "卵を、よくも", "生まれる前に奪うのか"),
        SocietySpeechTrigger.MercyHigh to listOf("撃つな。この者は子を救った", "ただの狩人ではない", "客人として迎えよ"),
        SocietySpeechTrigger.HostilityHigh to listOf("警告はいらない", "来たぞ、子殺しだ", "星がその名を拒んでいる"),
        SocietySpeechTrigger.RelicTaken to listOf("聖宝を持ち去る気か", "それは我らのものだ", "返せ", "それは聖宝ではない。鍵だ。持ち出せば扉は閉じる"),
        SocietySpeechTrigger.ReturnVisitHostile to listOf("また来たか、子殺し", "よくも戻れたものだ", "今度は帰さぬ", "あなたの署名は拒否リストにある"),
        SocietySpeechTrigger.ReturnVisitMerciful to listOf("戻ってきてくれたのか", "あの恩は忘れていない", "おかえり、星の友よ", "あなたの署名は白名簿に載っている"),
    )

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
