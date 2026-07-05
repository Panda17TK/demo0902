package io.github.panda17tk.arpg.sim

/**
 * v2.60 チュートリアル第1弾 — 「保守員人格の起動診断」。The tutorial is not a shooting lesson;
 * in-fiction it is the boot diagnostic of an old keeper persona. Layer 1 covers the ship:
 * move → shoot → pick up the dust it sheds → dash → find and scan a memory star → land.
 * (Layer 2 — the child, the predator, the star remembering — arrives in the next pass.)
 * Pure state machine: the screen feeds events in, prompts come out. Skippable at every step.
 */
enum class TutorialStep {
    BOOT_PROMPT, // 起動診断を実行しますか？ [診断する][スキップ]
    MOVE,        // 推進制御を確認します
    SHOOT,       // 防衛工具を確認します
    PICKUP_DUST, // 記憶片(星屑)を回収
    DASH,        // 緊急推進を確認します
    FIND_PLANET, // 記憶星を検出 — 信号へ
    LAND,        // 接続可能 — 着陸
    COMPLETE,    // 起動診断 完了
}

class TutorialController {
    var step: TutorialStep = TutorialStep.BOOT_PROMPT
        private set
    var skipped = false
        private set
    val done: Boolean get() = step == TutorialStep.COMPLETE

    private var moved = 0f

    /** [診断する] on the boot prompt. */
    fun begin() {
        if (step == TutorialStep.BOOT_PROMPT) step = TutorialStep.MOVE
    }

    /** Skip from anywhere — the diagnostic ends, hints stay available in play. No penalty. */
    fun skip() {
        skipped = true
        step = TutorialStep.COMPLETE
    }

    fun onMoved(dist: Float) {
        if (step != TutorialStep.MOVE) return
        moved += dist
        if (moved >= MOVE_GOAL) step = TutorialStep.SHOOT
    }

    fun onKill() {
        if (step == TutorialStep.SHOOT) step = TutorialStep.PICKUP_DUST
    }

    fun onDustPicked() {
        if (step == TutorialStep.PICKUP_DUST) step = TutorialStep.DASH
    }

    fun onDash() {
        if (step == TutorialStep.DASH) step = TutorialStep.FIND_PLANET
    }

    fun onScan() {
        if (step == TutorialStep.FIND_PLANET) step = TutorialStep.LAND
    }

    fun onLanded() {
        if (step == TutorialStep.LAND || step == TutorialStep.FIND_PLANET) step = TutorialStep.COMPLETE
    }

    /** The current diagnostic prompt — UI line(s), fiction first, control second. */
    fun prompt(touch: Boolean): List<String> = when (step) {
        TutorialStep.BOOT_PROMPT -> listOf(
            "保守員人格 起動中……　記憶同期: 失敗　星間ネットワーク: 応答なし",
            "ローカル保全モードで再開します — 起動診断を実行しますか？",
        )
        TutorialStep.MOVE -> listOf(
            "起動診断 1/6　推進制御を確認します",
            if (touch) "左スティックで船体を移動" else "WASD / 方向キーで船体を移動",
        )
        TutorialStep.SHOOT -> listOf(
            "起動診断 2/6　防衛工具を確認します",
            if (touch) "右スティックで照準・離して発射　対象を1体破壊" else "マウスで照準・クリックで発射　対象を1体破壊",
        )
        TutorialStep.PICKUP_DUST -> listOf(
            "起動診断 3/6　記憶片を検出: 星屑",
            "破損プロセスから剥離した記憶片です — 回収してください",
        )
        TutorialStep.DASH -> listOf(
            "起動診断 4/6　緊急推進を確認します",
            if (touch) "DASH ボタンで距離を取る" else "Shift でダッシュして距離を取る",
        )
        TutorialStep.FIND_PLANET -> listOf(
            "起動診断 5/6　近傍に記憶星を検出",
            "ナビの矢印へ — この星は、あなたをまだ知りません",
        )
        TutorialStep.LAND -> listOf(
            "起動診断 6/6　接続可能",
            if (touch) "惑星かスキャンカードをタップで着陸" else "[L] で着陸",
        )
        TutorialStep.COMPLETE -> emptyList()
    }

    /** The completion line — same whether diagnosed or skipped (skipping never punishes). */
    fun completionToast(): String =
        if (skipped) "起動診断をスキップ — ヒントはプレイ中にも表示されます"
        else "起動診断 完了 — 以後、星々はあなたの行動を記録します"

    companion object {
        const val MOVE_GOAL = 480f // ~15 tiles of drifting — feel the inertia, don't rush it
        const val REWARD_DUST = 10 // the same either way: skipping must never cost the player
    }
}
