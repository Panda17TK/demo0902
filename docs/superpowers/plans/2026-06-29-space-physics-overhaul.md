# 宇宙物理刷新（Space Physics Overhaul）実装計画

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 宇宙的な慣性（離すと滑空・ごく僅かに減衰）と、惑星＋小惑星の重力（速度に効く）、ダッシュによる重力脱出、段階的な敵重力応答＋高速衝突ダメージ（重力の武器化）を実装する。

**Architecture:** 既存システムをその場で拡張。計算は libGDX/Fleks 非依存の純粋モジュール（`sim/`）に置いて JUnit5 で TDD し、ECS システム（`MovementSystem`/`AISystem`/`GravitySystem`）は薄く保つ。`Velocity.driftX/driftY` を全エンティティの momentum として共有。

**Tech Stack:** Kotlin / libGDX / Fleks ECS / JUnit 5 / Gradle（`:core:test`）。

**設計書:** [2026-06-29-space-physics-overhaul-design.md](../specs/2026-06-29-space-physics-overhaul-design.md)

**フェーズ構成（重要）:** 本計画は **SP-1 を完全な bite-sized TDD で詳細化**し、SP-2〜SP-5 は確定したファイル／インターフェース／テスト項目として記述する。各後続フェーズは、その着手時点で（先行フェーズの実機チューニング結果を反映して）bite-sized ステップへ展開する。これは後続コードが先行フェーズの数値に依存するための意図的な段階化であり、プレースホルダではない。

**全フェーズ共通の完了条件:** `./gradlew :core:test` 緑、既存テスト（175）を壊さない、可能なら `./gradlew :android:assembleDebug` 緑、1フェーズ1コミット。

---

## ファイル構成（全フェーズ）

新規（純粋モジュール・`core/src/main/kotlin/io/github/panda17tk/arpg/`）:
- `sim/Inertia.kt` — momentum 積分（加速度→速度、宇宙減衰、上限クランプ）。プレイヤー＆敵共有。【SP-1】
- `sim/PlanetGravity.kt` — 惑星リストからの重力加速度＋小惑星と合算する `combinedGravityAccel`。【SP-2/SP-3】
- `sim/CircleCollision.kt` — 円（惑星）対 AABB の押し出し＋内向き速度取得。【SP-3】
- `sim/CrashModel.kt` — 内向き速度と閾値からダメージ量＋反発後速度。【SP-4】
- `ecs/components/Planet.kt` — 惑星エンティティ（radius/mass/biome/gravityRange）。【SP-3】
- `ecs/world/Planets.kt` — 惑星配置（純粋な座標決定ロジック）。【SP-3】

改修:
- `ecs/systems/MovementSystem.kt` — プレイヤー慣性ハイブリッド化【SP-1】、重力 drift 適用＋ダッシュ脱出【SP-2】、惑星衝突＋クラッシュ【SP-3/SP-4】。
- `ecs/systems/AISystem.kt` — 敵 drift 積分【SP-2】、惑星衝突＋クラッシュ【SP-3/SP-4】。
- `ecs/systems/GravitySystem.kt` — 座標加算→drift 加算、惑星源追加、応答係数【SP-2/SP-3/SP-5】。
- `ecs/world/WorldFactory.kt` — 惑星生成＋inject、システム順序【SP-3】。
- `config/EnemyDef.kt` — `gravityResponse` 追加【SP-5】。
- 描画（`render/`・`screens/GameScreen.kt`・`ecs/systems/SnapshotSystem.kt`）— 惑星＋重力井戸描画【SP-3】。

テスト（`core/src/test/kotlin/.../sim/` ほか）:
- `sim/InertiaTest.kt`【SP-1】, `sim/PlanetGravityTest.kt`【SP-2】, `sim/CircleCollisionTest.kt`【SP-3】, `sim/CrashModelTest.kt`【SP-4】, `ecs/world/PlanetsTest.kt`【SP-3】, 既存 `WorldConfigTest`/`config` 系へ `gravityResponse`【SP-5】。

---

## SP-1: 慣性インテグレータ＋プレイヤー滑空（ハイブリッド）

**目的:** `Inertia` 純粋モジュールを導入し、プレイヤーが「入力を離しても滑空し、ごく僅かに減衰する」ハイブリッド慣性で動くようにする。ダッシュ後も momentum が残る。

**Files:**
- Create: `core/src/main/kotlin/io/github/panda17tk/arpg/sim/Inertia.kt`
- Test: `core/src/test/kotlin/io/github/panda17tk/arpg/sim/InertiaTest.kt`
- Modify: `core/src/main/kotlin/io/github/panda17tk/arpg/ecs/systems/MovementSystem.kt`

- [ ] **Step 1: 失敗するテストを書く**

`core/src/test/kotlin/io/github/panda17tk/arpg/sim/InertiaTest.kt`:

```kotlin
package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Momentum integrator: accel adds velocity, light space-drag bleeds it slowly, speed is capped. */
class InertiaTest {
    @Test fun `acceleration increases velocity from rest`() {
        val (vx, vy) = Inertia.step(0f, 0f, ax = 100f, ay = 0f, decay = 0.5f, maxSpeed = 999f, dt = 0.1f)
        assertTrue(vx > 0f, "vx was $vx")
        assertEquals(0f, vy, 1e-4f)
    }

    @Test fun `momentum persists with light decay (space glide)`() {
        // No acceleration, light decay → keeps almost all velocity over one tick.
        val (vx, _) = Inertia.step(100f, 0f, ax = 0f, ay = 0f, decay = 0.9f, maxSpeed = 999f, dt = 0.1f)
        assertTrue(vx in 95f..99.9f, "vx was $vx — should retain most momentum")
    }

    @Test fun `heavier decay bleeds momentum faster than lighter decay`() {
        val (light, _) = Inertia.step(100f, 0f, 0f, 0f, decay = 0.9f, maxSpeed = 999f, dt = 0.2f)
        val (heavy, _) = Inertia.step(100f, 0f, 0f, 0f, decay = 0.3f, maxSpeed = 999f, dt = 0.2f)
        assertTrue(heavy < light, "heavy=$heavy light=$light")
    }

    @Test fun `speed is clamped to maxSpeed`() {
        val (vx, _) = Inertia.step(1000f, 0f, 0f, 0f, decay = 1f, maxSpeed = 150f, dt = 0.1f)
        assertEquals(150f, vx, 1f)
    }
}
```

- [ ] **Step 2: テストが失敗することを確認**

Run: `./gradlew :core:test --tests "io.github.panda17tk.arpg.sim.InertiaTest"`
Expected: コンパイルエラー（`Inertia` 未定義）で FAIL。

- [ ] **Step 3: Inertia を実装**

`core/src/main/kotlin/io/github/panda17tk/arpg/sim/Inertia.kt`:

```kotlin
package io.github.panda17tk.arpg.sim

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Pure momentum integrator shared by the player and mobs. Adds acceleration to velocity, applies a
 * light "space" drag (decay^dt) so momentum persists across frames, then clamps to maxSpeed.
 * Kept free of libGDX/Fleks for deterministic unit testing.
 */
object Inertia {
    fun step(
        vx: Float, vy: Float, ax: Float, ay: Float, decay: Float, maxSpeed: Float, dt: Float,
    ): Pair<Float, Float> {
        var nx = vx + ax * dt
        var ny = vy + ay * dt
        val d = decay.pow(dt)
        nx *= d; ny *= d
        val sp = sqrt(nx * nx + ny * ny)
        if (sp > maxSpeed && sp > 1e-4f) { nx = nx / sp * maxSpeed; ny = ny / sp * maxSpeed }
        return nx to ny
    }
}
```

- [ ] **Step 4: テストが通ることを確認**

Run: `./gradlew :core:test --tests "io.github.panda17tk.arpg.sim.InertiaTest"`
Expected: PASS（4件）。

- [ ] **Step 5: コミット**

```bash
git add core/src/main/kotlin/io/github/panda17tk/arpg/sim/Inertia.kt core/src/test/kotlin/io/github/panda17tk/arpg/sim/InertiaTest.kt
git commit -m "feat: 慣性インテグレータInertia追加（SP-1）"
```

- [ ] **Step 6: MovementSystem をハイブリッド慣性へ切替**

`MovementSystem.kt` の現行ブロック（68-71 行付近）:

```kotlin
        // Acceleration-based movement: input/dash push acceleration; drift ramps to a capped max + coasts.
        val (nvx, nvy) = Locomotion.applyMove(v.driftX, v.driftY, mv.dirX, mv.dirY, mv.isMoving, accel, friction, maxV, dt)
        v.driftX = nvx; v.driftY = nvy
```

を次へ置換（`val friction = ...` の行は不要になるので削除）:

```kotlin
        // Newtonian-ish momentum: input/dash accelerate the drift; light space-drag lets it coast.
        val thrustX = if (mv.isMoving) mv.dirX * accel else 0f
        val thrustY = if (mv.isMoving) mv.dirY * accel else 0f
        val decay = if (mv.isMoving) MOVE_DECAY else COAST_DECAY
        val (nvx, nvy) = Inertia.step(v.driftX, v.driftY, thrustX, thrustY, decay, maxV, dt)
        v.driftX = nvx; v.driftY = nvy
```

`companion object` の定数を更新（`MOVE_FRICTION`/`STOP_FRICTION` を改名・調整）:

```kotlin
        private const val MOVE_DECAY = 0.6f  // light drag while thrusting (still reaches the cap)
        private const val COAST_DECAY = 0.5f // space glide on release — momentum lingers (halves ~1s); main feel tunable
```

import を追加: `import io.github.panda17tk.arpg.sim.Inertia`（`Locomotion` の import は他で使用継続のため残す）。

- [ ] **Step 7: ビルド＆全テストが通ることを確認**

Run: `./gradlew :core:test`
Expected: 既存175＋新規4 = 179 PASS。`Locomotion.applyMove` の既存テスト（`LocomotionMoveTest`）は純粋関数を直接呼ぶため影響なし。

- [ ] **Step 8: 実機/デスクトップで滑空を確認（任意）**

Run: `./gradlew :desktop:run`
確認: 移動して入力を離すと、すぐ止まらず**滑空**する。ダッシュ後に momentum が残る。違和感あれば `COAST_DECAY` を調整（大きいほど長く滑空、小さいほど早く止まる）。

- [ ] **Step 9: コミット**

```bash
git add core/src/main/kotlin/io/github/panda17tk/arpg/ecs/systems/MovementSystem.kt
git commit -m "feat: プレイヤー移動をハイブリッド慣性へ（SP-1）— 離すと滑空"
```

---

## SP-2: 重力を momentum へ＋ダッシュ脱出＋敵 momentum

**目的:** 重力を「座標加算」から「drift 加算」へ変え、軌道に落ちる挙動にする。プレイヤーはダッシュ中に重力が弱まる。敵も drift を積分して重力で流れる（吹き飛ばしの土台）。惑星はまだ無く、既存の小惑星クラスタのみでよい。

**Files:**
- Modify: `ecs/systems/GravitySystem.kt`（`pull` を座標→drift 加算へ。プレイヤーはダッシュ中 ×0.25。対象に grenade を含め、bullet/ebullet は除外）
- Modify: `ecs/systems/AISystem.kt`（移動式に drift を加算積分。`v.vx/vy` ノックバックとは別に `v.driftX/driftY` を `Inertia` で軽く減衰）
- Modify: `ecs/components/PlayerTag.kt`（既に `dashing` フラグあり＝GravitySystem から参照可能か確認。なければ参照経路を用意）
- Test: `sim/PlanetGravityTest.kt`（`combinedGravityAccel` の素地。SP-2 ではクラスタのみでも、合算関数の純粋テストを先に置く）

**実装ノート（着手時に bite-sized 化）:**
- `GravitySystem.pull(t, v, dt, response, dashMul)`: `val (ax, ay) = WallGravity.gravityAt(...)`; `v.driftX += ax * response * dashMul * dt; v.driftY += ay * ...`。座標直接加算（`t.x += ...`）は廃止。
- プレイヤーのダッシュ参照: `MovementSystem` が毎フレーム `PlayerTag.dashing` を設定済み（[MovementSystem.kt:50](../../core/src/main/kotlin/io/github/panda17tk/arpg/ecs/systems/MovementSystem.kt)）。GravitySystem はプレイヤー family で `dashing` を読み、`dashMul = if (dashing) 0.25f else 1f`。
- 敵 drift 積分（AISystem）: 現行 `Collision.moveAndCollide(map, t.x, t.y, ..., mvx + v.vx*dt, 0f)` を `mvx + (v.vx + v.driftX)*dt` に。`v.driftX/driftY` は別途 `Inertia.step(v.driftX, v.driftY, 0f, 0f, MOB_DRIFT_DECAY, MOB_DRIFT_CAP, dt)` で減衰。AI 移動デルタ（mvx/mvy）は drift に積まない＝二重計上回避（v2.9.0 の轍を踏まない）。
- 重力免疫（応答0）は SP-5 で `gravityResponse` 導入後に効くが、SP-2 では全敵 response=1 として配線。

**テスト項目（純粋・`PlanetGravityTest` の合算関数）:**
- `combinedGravityAccel`: クラスタのみで範囲外0／範囲内で中心方向／距離減衰（`WallGravity.gravityAt` のラップが正しいこと）。
- ダッシュ倍率は数値ロジックとして純粋関数化できるなら別途テスト（`dashMul` 適用後の加速度）。

**完了条件:** 重力源近傍で軌道が曲がる。ダッシュで振り切れる。敵も重力で流れる。`:core:test` 緑。コミット `feat: 重力をmomentumへ＋ダッシュ脱出＋敵drift（SP-2）`。

---

## SP-3: 惑星エンティティ＋生成＋円衝突＋描画

**目的:** 離散惑星を導入。重力源かつソリッドな円として宇宙ステージに2〜4個出す。

**Files:**
- Create: `ecs/components/Planet.kt`（`class Planet(val radius: Float, val mass: Float, val biome: Biome, val gravityRange: Float)`）
- Create: `sim/PlanetGravity.kt`（`gravityAccelAt(planets: List<PlanetSrc>, x, y)` ＋ `combinedGravityAccel(planets, clusters, x, y)`。`PlanetSrc(cx, cy, mass, range)` の純粋データ）
- Create: `sim/CircleCollision.kt`（`resolve(x, y, bodyR, nx, ny, planets): CircleResult(rx, ry, hit, inwardSpeed)`）
- Create: `ecs/world/Planets.kt`（純粋: seed乱数で `List<PlacedPlanet>`（座標/半径/質量/biome）を決定。プレイヤー初期位置から最小距離・相互非重複）
- Test: `sim/PlanetGravityTest.kt`（惑星源を追加）, `sim/CircleCollisionTest.kt`, `ecs/world/PlanetsTest.kt`
- Modify: `GravitySystem.kt`（惑星源を inject して合算）, `WorldFactory.kt`（惑星エンティティ生成＋holder/ family inject、`GravitySystem` をシステム順で `MovementSystem` の前へ）, 描画（`SnapshotSystem`＋`render/` で惑星円＋重力井戸リング）

**テスト項目（純粋中心）:**
- `PlanetGravity.gravityAccelAt`: 範囲外0／中心方向／距離減衰／質量大で強い／複数惑星の合算。
- `CircleCollision.resolve`: 円外は不変（hit=false）／円内は表面へ押し出し（中心からの距離 = radius+bodyR）／内向き速度を正の大きさで返す／接線移動は素通り。
- `Planets`（配置）: 指定数を返す／全惑星がプレイヤー初期位置から最小距離以上／相互に重ならない／同seedで同結果（決定性）。

**実装ノート:** 惑星はタイルではなく円エンティティ。プレイヤー/敵の積分後位置に対し `CircleCollision.resolve` を適用して押し出し（クラッシュ判定は SP-4）。描画は純粋ロジック外（テスト対象外）。

**完了条件:** 宇宙ステージに惑星が出て重力源になり、ソリッドとして衝突（めり込まない）。`:core:test` 緑。コミット `feat: 惑星エンティティ＋重力＋円衝突＋描画（SP-3）`。

---

## SP-4: クラッシュダメージ（壁・惑星 / プレイヤー＆敵）

**目的:** 高速衝突でダメージ。引かれる敵を惑星／壁へ叩きつけて倒せる（重力の武器化）。プレイヤーも高速衝突で被弾。

**Files:**
- Create: `sim/CrashModel.kt`（`damage(inwardSpeed, threshold, k): Float`＝`k * max(0, inwardSpeed - threshold)`；`bounce(inwardComponent): Float`＝相殺＋軽い反発）
- Test: `sim/CrashModelTest.kt`
- Modify: `MovementSystem.kt`（壁: `r1.hitX/r2.hitY` 時の旧drift大きさで判定→`CrashModel`→`h.hp`減＋i-frame。惑星: `CircleCollision` の `inwardSpeed` で判定）
- Modify: `AISystem.kt`（同様に敵へ。撃破時は通常キル処理に乗せる）

**テスト項目（純粋）:**
- `CrashModel.damage`: 閾値以下=0／超過分に比例／係数で増減。
- `CrashModel.bounce`: 内向き速度を相殺し符号反転の軽い反発を返す。

**実装ノート:** 衝突直前の drift 速度を捕捉してから drift をゼロ/反発。プレイヤーは既存 `Health.iTime` を尊重（多重ヒット防止）。敵撃破は既存ドロップ/キル経路へ。

**完了条件:** 高速で惑星/壁に衝突するとダメージ。敵を叩きつけ撃破可。プレイヤーも事故ダメージ（閾値で理不尽回避）。`:core:test` 緑。コミット `feat: 高速衝突クラッシュダメージ（SP-4）`。

---

## SP-5: `gravityResponse`＋敵チューニング＋バランス

**目的:** 敵ごとの重力応答をデータ駆動化し、「重力無視」「重い」敵で非対称な立ち回りを作る。

**Files:**
- Modify: `config/EnemyDef.kt`（`val gravityResponse: Float = 1f` 追加）
- Modify: `GravitySystem.kt`（敵 pull に `m.def.gravityResponse` を反映。response=0 は加算0）
- Modify: 敵定義データ（`GameConfig` の enemies。数体を `0`／`1.5` に）
- Test: `config/ConfigCodecTest` 等に `gravityResponse` の往復＋既定値1、`WorldConfigTest` に反映確認

**テスト項目:**
- 既定 `gravityResponse == 1f`／JSON 往復で保持。
- GravitySystem が response=0 の敵に重力を加えない（純粋化できる部分はユニットで、難しければ world スモークで）。

**実装ノート:** 既存敵は既定1で挙動不変。軽い飛行体に0、重量級に1.5。最終バランスは実機調整。

**完了条件:** 重力無視/重い敵が存在し戦術が変わる。`:core:test` 緑。コミット `feat: 敵の段階的重力応答gravityResponse（SP-5）`。

---

## セルフレビュー

**1. 仕様カバレッジ（spec §3〜§6 対応）:**
- 慣性ハイブリッド（§5.1）→ SP-1。
- 重力を momentum へ＋ダッシュ脱出（§5.2）→ SP-2。
- 惑星エンティティ＋小惑星併存（§4.2/§5.3）→ SP-3（`PlanetGravity` 合算）。
- 円衝突（§5.4）→ SP-3。クラッシュ（§5.4）→ SP-4（壁・惑星・双方）。
- 敵 momentum／重力免疫（§5.1/§5.5）→ SP-2（drift積分）＋SP-5（`gravityResponse`）。
- 弾/グレネード（§5.6）→ SP-2（grenade のみ重力対象に含める）。
- 描画（§4.3）→ SP-3。
- 既定値（§6）→ SP-1 `COAST_DECAY`、SP-2 ダッシュ×0.25・敵drift減衰、SP-4 閾値/係数。
ギャップなし。

**2. プレースホルダ走査:** SP-1 は実コード完備。SP-2〜5 は「着手時に bite-sized 化」と明記した確定スペック（ファイル/シグネチャ/テスト項目）であり、TBD ではない。OK。

**3. 型整合:** `Inertia.step(vx,vy,ax,ay,decay,maxSpeed,dt): Pair<Float,Float>` を SP-1 で定義し SP-2 の敵 drift でも同シグネチャで使用。`Velocity.driftX/driftY`・`PlayerTag.dashing`・`EnemyDef.gravityResponse` の名称が全フェーズで一貫。`combinedGravityAccel`/`CircleCollision.resolve`/`CrashModel.damage` の名称も一貫。OK。
