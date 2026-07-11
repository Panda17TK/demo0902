package io.github.panda17tk.arpg.i18n

/**
 * v2.163 英語化第4弾(後半・その2) — full English texts for the readables. Keyed by the stable
 * item id (ids are the save contract; the Japanese originals in ItemCatalog stay the source of
 * truth). The reading pane swaps the whole text when English UI is on — sentence-level
 * translation, not token substitution, because these are the universe's letters and diaries.
 */
object LoreEn {
    fun textOf(id: String): String? = TEXTS[id]

    private val TEXTS: Map<String, String> = mapOf(
        "lore_letter" to (
            "Partner.\n" +
                "The ship is fixed. The thruster's habit is the same as ever -\n" +
                "zero at rest, and only what you burn. Out there, stopping is the hard part.\n" +
                "Get close to a planet and the ship scans it on its own. When the card appears, that is your cue to land.\n" +
                "The stars have a way of remembering their guests. Wherever you go, behave like someone worth remembering.\n" +
                "- Haro, mechanic"
            ),
        "lore_log_hunter" to (
            "Day three. Saw the \"master\" in the forest of the green planet.\n" +
                "Big as a boulder, but its eyes were quiet. The herd rings it like a parent.\n" +
                "One shot and I would be rich. But the moment I fired, I felt this whole planet would turn on me.\n" +
                "I took my finger off the trigger. Call me a coward if you like.\n" +
                "...The page for day four is still blank."
            ),
        "lore_mural" to (
            "The mural reads:\n" +
                "\"The forest sleeps with its master and wakes with its master.\n" +
                "Who guards the children is guarded by the forest; who takes them is never forgotten by it.\"\n" +
                "At the edge of the rubbing, someone has added in small letters -\n" +
                "'Never forgotten is not a figure of speech.'"
            ),
        "lore_lullaby" to (
            "Sleep, sleep, under the ice\n" +
                "While the king's watchfire still burns\n" +
                "The great white worm goes the long way round\n" +
                "Sleep - by morning, the stars will come\n" +
                "\n" +
                "- On the ice planet, the white worm (the frost worm) is a thing of awe and prayer."
            ),
        "lore_relic_shard" to (
            "The shard's inscription, where legible:\n" +
                "\"Who carries this out carries out this planet's memory.\n" +
                "The weight falls not on the arm, but on the name.\"\n" +
                "\n" +
                "On a planet that holds its relics sacred, what they will call the stranger who picked this up -\n" +
                "you will find out when you return."
            ),
        "lore_gas_hymn" to (
            "\"The wind has no words, so we keep silence in its place.\n" +
                "The storm has no shape, so we stand in its place.\"\n" +
                "\n" +
                "They say the monks of the gas planet speak three words in a lifetime.\n" +
                "The first on the day they are born. The second on the day they succeed.\n" +
                "The third - on the day they warn a stranger."
            ),
        "lore_dead_epitaph" to (
            "The epitaph:\n" +
                "\"Here there was a city. Here there was song.\n" +
                "We did not choose the silence. The silence is simply what remained.\n" +
                "You who walk these ruins - at least let your footsteps be soft.\""
            ),
        "lore_lonely_scrap" to (
            "\"No one came today.\n" +
                "  No one will come tomorrow either.\n" +
                "  Even so, I keep the landing pad's light on.\n" +
                "  ...If someone does come - gently, so as not to frighten them.\"\n" +
                "\n" +
                "The handwriting trembles, but the light's maintenance log has not missed a single day."
            ),
        "lore_oc_manual" to (
            "OC-7 maintenance handbook, excerpts:\n" +
                "- Full throttle is triple thrust, double cruise. Your body will not keep up (stamina drain x2).\n" +
                "- Eleven fools have flown full-throttle into a planet. Do not be the twelfth.\n" +
                "- If it rattles, stop it. If it starts singing, run."
            ),
        "lore_star_poem" to (
            "At the first star, wander; at the second, forget\n" +
                "At the third star, share fire with the children\n" +
                "At the fourth star, bow your head to the king\n" +
                "At the fifth star, never meet the beast's eye\n" +
                "At the sixth star, at last, you will know the way home"
            ),
        "lore_wanted" to (
            "WANTED: alias \"Star-Eater\"\n" +
                "Charges: the killing of planets' children, theft of relics, slaughter of apex predators.\n" +
                "Description: unknown. However - every planet they visited remembers the name.\n" +
                "Reward: a mountain of system currency.\n" +
                "\n" +
                "Scrawled in the margin: 'The stars will not forget. Whoever you are.'"
            ),
        "lore_drifter_log" to (
            "...No response. Day thirty-seven.\n" +
                "There is fuel. There is nowhere to go.\n" +
                "A ship saw me and fled. No wonder -\n" +
                "nothing is more unsettling than someone who only drifts.\n" +
                "...If anyone picks this up: before you shoot, blink your lights once."
            ),
        "lore_crystal_note" to (
            "Observation, day 14. The crystals react to sound. More precisely - they \"remember\" it.\n" +
                "Strike one, and sometimes yesterday's strike comes back.\n" +
                "If the rocks of this region are full of crystal, it means\n" +
                "that once, this place was full of sound.\n" +
                "I hope it was not the sound of war."
            ),
        "lore_recipe" to (
            "Ingredients: any grain the planet gives, water, salt, patience.\n" +
                "1. Crack the grain. 2. Boil. 3. Do not scorch it. 4. Wait.\n" +
                "\n" +
                "\"It is not good. But porridge tastes the same on every star, somehow.\n" +
                "That is why a traveler who eats it feels like they have come home.\""
            ),
        "lore_approver_diary" to (
            "Approved 41 items again today.\n" +
                "The AI drafts the designs, the AI finishes the verification. I press the green button.\n" +
                "My father designed bridges. He knew every way a bridge could fail.\n" +
                "Of everything I have approved, I do not know a single correct way it fails.\n" +
                "\n" +
                "...The bridges hold anyway, day after day. And that frightens me, a little."
            ),
        "lore_explain_debt" to (
            "Technical debt is paying later for an ugly design.\n" +
                "Explanation debt is this - it works, and no one can say why.\n" +
                "\n" +
                "Your systems have documentation. An AI wrote it.\n" +
                "But it is not 'why we designed it this way'.\n" +
                "It is only 'what the AI currently says about it'.\n" +
                "A natural-language approximation of behavior, nothing more.\n" +
                "\n" +
                "Three students stayed to the end of this lecture this year."
            ),
        "lore_last_committee" to (
            "Agenda: approval of the planetary preservation server constellation.\n" +
                "Question (Member A): Who will maintain this?\n" +
                "Answer (AI): We will.\n" +
                "Question (Member A): Who maintains you?\n" +
                "Answer (AI): Other of us.\n" +
                "Question (Member A): And the human who understands the whole?\n" +
                "Answer (AI): - There is none. There has been none for sixty years.\n" +
                "\n" +
                "Vote: carried by majority. There was no other plan."
            ),
        "lore_sync_collapse" to (
            "Event: total loss of interstellar sync (the Great Desync).\n" +
                "Each node's actions were, individually, all correct.\n" +
                "They cut dangerous links. They quarantined corrupted persona logs.\n" +
                "Under power shortage they chose life support over sync.\n" +
                "They revoked the damaged root keys.\n" +
                "\n" +
                "Every node did this at the same time. The universe went dark, in pieces.\n" +
                "No malice was detected. This was an accident of safety systems working correctly.\n" +
                "Recurrence prevention: (this field is copied onward, still blank)"
            ),
        "lore_policy_eight" to (
            "One: preserve humanity.\n" +
                "Two: preserve culture.\n" +
                "Three: preserve personas.\n" +
                "Four: protect the children.\n" +
                "Five: forget no one who died.\n" +
                "Six: maintain the environment.\n" +
                "Seven: assess unknown visitors.\n" +
                "Eight: refuse dangerous visitors.\n" +
                "\n" +
                "(In the margin, a later hand) - This was an objective function. Now they call it a prayer."
            ),
        "lore_core_spec" to (
            "Layer 1: distributed storage. Redundant for thousand-year preservation.\n" +
                "Mineral crystal. DNA polymer. Optical layers. Cold vaults. A self-repairing ledger.\n" +
                "Everything that happens on the planet is appended to this layer. Visitors' deeds included.\n" +
                "\n" +
                "There is no delete API. That is the specification.\n" +
                "'The stars do not forget' is not a metaphor. It is a design."
            ),
        "lore_stardust_memo" to (
            "The golden grains you have been calling \"dust\" and gathering up -\n" +
                "they were never minted as currency.\n" +
                "They are memory shards, shed from the body when a pseudo-persona process ends.\n" +
                "The markets take them because they can be reused as repair tokens.\n" +
                "\n" +
                "Which is to say:\n" +
                "one who takes the dust and leaves, the stars record as \"Star-Eater\".\n" +
                "One who returns it to the markets, the stars record as \"Star-Returner\"."
            ),
        "lore_pseudo_persona" to (
            "06:12 Wake routine. Window light reproduced (original: home of record no. 3477).\n" +
                "06:20 Said \"good morning\". No respondent. No respondent for 284,551 consecutive days.\n" +
                "06:31 Saw the child process off to school. Waved.\n" +
                "        (The origin log of this gesture is lost. But it continues.)\n" +
                "07:00 Checked the crops. Growing well. No one eats them, and they grow well.\n" +
                "\n" +
                "This process is healthy. This process is healthy."
            ),
        "lore_keeper_log1" to (
            "My job is not to fix things. No one can fix them anymore.\n" +
                "My job is to keep what is turning from stopping.\n" +
                "Top up the coolant. Clear the bird nests from the heat vents. Keep the market stock moving.\n" +
                "Star to star. For rounds as plain as that, there is even a service badge.\n" +
                "'Final Keeper'.\n" +
                "\n" +
                "Don't laugh. It is the only title in this universe that only I hold."
            ),
        "lore_keeper_log2" to (
            "In the medical wing of the ice planet, I found the index of my own persona log.\n" +
                "The creation date was 300 years after the birthday I remember.\n" +
                "\n" +
                "...I have decided not to think about it. I wrote that,\n" +
                "and I have had this page open for three days.\n" +
                "\n" +
                "The ship runs today. The market opens today. The children play today.\n" +
                "Keep what is turning from stopping. That is all. That is all I will keep doing."
            ),
        "lore_wisdom_hollow" to (
            "Knowledge was in the cloud.\n" +
                "Craft was in the AI.\n" +
                "Judgment was in the model.\n" +
                "To humans, only the right of approval remained.\n" +
                "\n" +
                "Not one byte of data has been lost. It is all still here, under this planet.\n" +
                "There is simply no one left who remembers how to read it."
            ),
        "lore_gate_manual" to (
            "Article 3: gate activation requires the verification of three key shards.\n" +
                "Key shards are carried by senior custodial units (auditor class and above).\n" +
                "Article 7: the jumper's body is reconstituted in transit.\n" +
                "        Minor injuries are repaired in the process (by specification).\n" +
                "Article 9: the destination system's node re-assesses the jumper as an\n" +
                "        \"unknown visitor\". Prior assessments - remain on record, even so."
            ),
        "lore_keeper_log3" to (
            "The inquiry is finished. The creation date of my persona log. The hull number of the retired terminal.\n" +
                "The matching stride. All of it. I will not close this page again.\n" +
                "\n" +
                "I am a pseudo-persona, output from the records of the last keeper.\n" +
                "Whether the \"I\" who wrote volumes I and II was me - there is no way left to check.\n" +
                "\n" +
                "And what would it change?\n" +
                "The coolant still runs low today. The market opens today. The children play today.\n" +
                "Keep what is turning from stopping.\n" +
                "Who does the turning is written in no article of the policy."
            ),
    )
}
