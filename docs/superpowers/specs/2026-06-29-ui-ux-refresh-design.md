# UI/UX 刷新 設計書 — demo0902

- **日付**: 2026-06-29
- **ブランチ**: `redesign/android-native-kotlin`
- **対象**: top-down ARPG survival（Kotlin + libGDX + Fleks ECS、`core/ desktop/ android/`）
- **状態**: 方針＋P1詳細を承認済み（2026-06-29）
- **関連**: 現行 v2.2.2 / 次バッチで P1 をリリース

## 1. 目的

UI/UX をリファクタリングし、**より直感的・快適に**プレイできるよう洗練する。特にモバイル実機での操作破綻を解消し、情報の見やすさと手触りを段階的に向上させる。

## 2. 現状の主要課題（コード根拠）

| # | 課題 | 根拠 |
|---|------|------|
| ④ | **強化選択がタッチ未対応** — `NUM_1/2/3` キーのみ。実機ではウェーブクリア後に選択不能（sim 凍結） | `screens/GameScreen.kt:336-351`（`updateUpgradeFlow`, `Keys.NUM_1..3`） |
| ⑤ | **リスタートがタッチ未対応** — `R` キーのみ。実機でゲームオーバーから再開不能 | `screens/GameScreen.kt:165`（`Keys.R` → `newRun()`） |
| ⑥ | ポーズ／メニュー／設定が無い。タイトル無しで起動即プレイ | 単一 `GameScreen` のみ（独立 Screen クラス無し） |
| ① | HUD がテキスト過密・アイコン無し・ウェーブ番号が目立たない | `GameScreen.kt:286-316` |
| ② | 右下5ボタンが密集・薄い・押下フィードバック無し | `input/TouchLayout.kt` / `GameScreen.kt:419-457` |
| ③ | エイムスティックが不定位置で定位置ガイド無し | `input/TouchControls.kt` |
| — | `GameScreen.kt`（504行）に描画＋ロジックが集中（肥大化） | `screens/GameScreen.kt` |

## 3. スコープ（段階出荷）

各フェーズを**個別にゲート通過→リリース**する。手戻りと文脈肥大を抑えるため、詳細は到達時に確定する。

| フェーズ | 内容 | 本書での扱い |
|---|------|------|
| **P1** | タッチ操作フロー修復＋ポーズ | **詳細設計（本書）／最優先・個別リリース（v2.3.0 想定）** |
| P2 | HUD 刷新（アイコン化・情報階層・視認性） | 概要のみ |
| P3 | 操作の配置・反応（ボタン整理・押下FB・スティック定位置） | 概要のみ |
| P4 | ゲームフィール（ダメージ数字・ヒットストップ・触覚/音） | 概要のみ |

## 4. 決定事項（確定）

- ポーズ機能を **P1 に含める**。
- 強化選択に**リロール／スキップは作らない**（3枚タップのみ）。
- **タイトル画面は作らない**（起動即プレイ、再挑戦＝即 `newRun()`）。
- キーボード操作（`1/2/3`, `R`, 追加で `Esc`/`P`）は**タッチと併存**。
- 既存 108 tests を壊さない。

## 5. P1 詳細設計

### 5.1 コンポーネント（分離・テスト可能化）

- 新規 `ui/` パッケージ（`core`、libGDX 非依存の純ロジック）:
  - `UiButton` — dp 矩形＋ラベル＋`contains(x, y): Boolean`。純粋・テスト可能。
  - `Modals` — 現在のモーダル状態（intermission / gameOver / pause）と画面サイズ(`hudW, hudH`)から、ボタン矩形リストとカード矩形を返す**純関数**。`TouchLayout` と同じ「ジオメトリは純粋・描画と分離」方針。
  - `hitModal(rects, x, y): Int?` — タップ点→インデックス（無ければ null）。
- 新規 `render/Hud.kt` — HUD バー/テキストとモーダル描画を `GameScreen` から委譲分離。
- `GameScreen.kt` — 状態フラグ（`choosing`, `paused`, `gameOver`）とタップ配線、描画委譲のみ保持（行数削減）。

### 5.2 入力（タップ）配線

- HUD 空間のタップ取得は既存パターン踏襲: `Gdx.input.justTouched()` ＋ `hudViewport.unproject(...)`（`TouchControls` の `unproject` 利用と同様）。
- **強化選択（choosing）**: タップ点が `card[i]` 矩形内なら `applyUpgrade(choices[i])`。`NUM_1/2/3` 併存。
- **ゲームオーバー**: 「再挑戦」矩形タップで `newRun()`。`R` 併存。
- **ポーズ**: プレイ中の右上 `⏸` 矩形タップで `paused=true`。オーバーレイの「再開／最初からやり直す／操作説明」をタップ配線。`Esc`/`P` でトグル。
- `paused` 中: `accumulator=0` で sim 凍結、`step` スキップ。`drawTouchControls()` の表示条件（現 `GameScreen.kt:318`）に `&& !paused` を追加し、ツインスティック/射撃を無効化。

### 5.3 状態遷移

```
play  ⇄  pause           （⏸ / Esc / P）
play  →  intermission(choosing)  → applyUpgrade → play
play  →  gameOver        → 再挑戦         → newRun → play
pause →  最初からやり直す  → newRun → play
pause →  操作説明（静的ヘルプ） → 戻る → pause
```

### 5.4 レイアウト（dp・タップ領域 ≥ 約56dp）

- **強化カード**: 幅 `min(360, hudW*0.86)`、高さ ~96dp（現 64 から拡大）、3枚縦積み中央。番号バッジ＋名称＋説明。押下ハイライト（枠＋発光）。
- **再挑戦ボタン**: 中央、幅 ~`hudW*0.5`、高さ ~64dp、緑系。補助で「タップ / R」の小ヒント。
- **⏸ ボタン**: 右上 ~44dp 角。ポーズオーバーレイは 3 ボタン縦積み中央。

### 5.5 テスト（先行 / AAA）

- `UiButton.contains` 境界テスト（内/外/辺上）。
- `Modals`: 画面サイズ→矩形（個数・非重複・画面内）テスト（`TouchLayoutTest` と同形式）。
- `hitModal`: タップ点→index（命中/null/複数矩形）テスト。
- ポーズ遷移ロジックを純粋抽出できる範囲で単体テスト。
- 既存テスト緑維持。

### 5.6 非目標（P1）

タイトル画面、設定/音量 UI、リロール/スキップ、ビジュアルアート刷新、HUD アイコン化（P2）、ボタン配置の大幅変更（P3）、ダメージ数字等（P4）。

## 6. P2–P4 概要（後続フェーズで詳細化）

- **P2 HUD 刷新**: HP/スタミナのアイコン化、ウェーブ・残弾の視認性強化、`render/Hud.kt` を土台に情報階層を再設計。色覚に依存しすぎない表現。
- **P3 操作の配置・反応**: 5ボタンの整理（文脈依存化/集約）、押下フィードバック（色/拡縮/触覚）、エイムスティックの定位置ガイド、デッドゾーン調整。
- **P4 ゲームフィール**: フローティングダメージ数字、軽いヒットストップ、被弾/撃破の手触り、触覚・録音SFXのメリハリ。

## 7. リリース手順（各フェーズ完了時）

1. ゲート: `& 'V:\src\demo0902\gradlew.bat' -p 'V:\src\demo0902' :core:test :desktop:build :android:assembleDebug`
2. `android/build.gradle.kts` の `versionCode`/`versionName` を上げる（P1 = v2.3.0 想定）
3. commit（日本語・**AI署名なし**）→ `git push` → `gh pr create`（base `main`）→ `gh pr merge --merge`
4. `gh release create vX.Y.Z` で debug APK 添付（ユーザーは GitHub Releases から実機インストール）

## 8. P2 詳細設計（確定 2026-06-29）

P2 = HUD 刷新。確定した方向性（ユーザー承認）:
- **レイアウト = 画面端に分散**。ただし下端隅はタッチ操作の占有域（移動スティック=左下、アクション群=右下、エイムスティック=右側）のため、**HUD は上端帯に分散**し操作と干渉させない（「親指位置と整合」の意図に合致）。
- **HP/スタミナ = アイコン＋セグメントバー＋数値**（色＋形＋位置＋数値の冗長表現＝色覚対応）。
- **残弾/武器 = 武器アイコン＋大きな装填数**（装填中はリング、予備弾は種別アイコン＋数）。

### 8.1 コンポーネント（分離・テスト可能化）
- 新規 `ui/HudLayout.kt`（純・libGDX非依存）: `hudW,hudH` → 各領域矩形（`wave/hp/stamina/ammo/stats`）。P1 `Modals` と同方針（ジオメトリ純粋・描画分離）。上端帯配置で P1 の ⏸（右上隅 `Modals.pauseButton`）と非重複。
- 純関数 `filledSegments(value,max,count)`: セグメントバーの点灯数（クランプ込み）。
- `render/Hud.kt` に `liveHud(...)` 追加: wave バッジ／HP・スタミナ（アイコン＋セグメント＋数値）／残弾パネル（武器アイコン＋大装填数＋予備＋装填リング）／補助 stats を描画。アイコンは P1 ⏸ と同様 **ShapeRenderer 図形**で描画（フォント字形非依存）。
- `GameScreen`: 現行 HUD ブロック（バー＋3行テキスト, 旧 `GameScreen.kt:323-353`）を `Hud.liveHud(...)` へ置換。HUD 専用色は `Hud` へ移動。

### 8.2 レイアウト（上端帯・dp）
- **ウェーブ**: 上中央バッジ（幅 `min(160,hudW*0.5)`・高 ~40dp）。小ラベル＋大きな番号（title フォント）。直下に「残り N」。
- **HP/スタミナ**: 左に2段スタック（♥/⚡アイコン＋セグメントバー幅 ~`hudW*0.42`＋数値）。ウェーブ行の下。
- **残弾/武器**: 右（⏸の下・HP と水平非重複）。武器アイコン＋大 `mag/size`＋予備弾小アイコン。装填中ゲージ/リング＋「装填中」。
- **補助 stats**: 時間/撃破/資材を小さく左下段（最低優先度）。

### 8.3 テスト（先行/AAA）
- `HudLayout`: 各領域が画面内・主要4領域（wave/hp/stamina/ammo）が相互非重複・ammo は ⏸ と非重複・HP は スタミナの上（複数画面サイズ）。
- `filledSegments`: 満杯=count／空=0／半分／value>max クランプ／max≤0=0。
- 既存テスト緑維持。

### 8.4 非目標（P2）
ボタン配置変更（P3）、ダメージ数字/ヒットストップ（P4）、タイトル/設定。出荷 = **v2.4.0**。

## 9. P3 詳細設計（確定 2026-06-29）

P3 = 操作の配置・反応。確定（ユーザー承認）:
- **ボタン整理 = 文脈依存で減らす**（壁=資材>0、装填=未満タン時のみ表示。常時=ダッシュ/近接/武器）。
- **エイム = 定位置ガイドを表示**（右側に半透明リング。押下時は実親指位置へフロート）。
- **押下フィードバック（色＋拡縮＋触覚）／デッドゾーン調整**は実装で対応。

### 9.1 コンポーネント
- 純関数 `input/TouchButtons.visible(blocks,mag,magSize): Set<TouchButton>`（文脈依存の表示集合）。TDD。
- `input/TouchLayout` に `aimGuideCx/Cy/Radius`（右側固定ガイド・純）。TDD（画面内・右半・全アクションボタンと非重複）。
- `TouchControls.poll(.., blocks, mag, magSize)`: `visible` で当たり判定/描画をゲート、`pressedButtons` を公開、押下エッジで触覚、デッドゾーン微調整。
- `GameScreen.drawTouchControls`: エイム定位置ガイド描画、`visible` のみ描画、`pressed` を強調（明度＋拡大）。

### 9.2 テスト（先行/AAA）
- `TouchButtons`: 既定=ダッシュ/近接/武器、装填=未満タン時のみ、壁=資材>0時のみ、magSize=null は装填無し、満タンは装填無し。
- `TouchLayout` 追加: aim ガイドが画面内・右半・全アクションボタンと非重複。
- 既存緑維持。

### 9.3 非目標（P3）
ダメージ数字/ヒットストップ（P4）、HUD 再変更、タイトル/設定。出荷 = **v2.5.0**。

## 10. ゲームプレイ追加バッチ（v2.6.0）

UI/UX 刷新とは別系統のゲームプレイ拡張（ユーザー要望）。全5項目を一括実装・出荷。

1. **強い慣性**: ダッシュ drift を強化（`dashThrust` 520→760、decay `0.5^dt`→`0.72^dt`、cap 220→360）。
2. **32倍マップ**: SPACE ステージを面積32倍（1313²等）。性能対策＝`FlowField.rebuild(.., maxDist)` で BFS をプレイヤー周辺に上限化（O(maxDist²)、`FlowRebuildSystem.MAX_DIST=70`）。重力のクラスタ検出もウィンドウ限定。
3. **壁の重力**: 純 `sim/WallGravity`（TDD）= 連結する破壊可能壁の塊を検出し半径≥2タイルなら重心方向へ微弱重力。`GravitySystem`（`IntervalSystem`）がプレイヤー/モブ/弾(ビーム以外)/敵弾/グレネードへ位置ベース重力（壁ブロック）。クラスタはプレイヤー周辺ウィンドウ＋1.5s throttle。
4. **ドロップ拡充**: 純 `ecs/world/Loot.wallDrops`（TDD）= 資材＋1〜3の多様ドロップ（弾/回復/稀に煙玉・スタミナ無限・ダッシュ強化）。
5. **新アイテム＋バフ**: `Buff`（staminaInfT/dashUpT、MovementSystem 適用）、煙玉=`Smoke`＋`SmokeSystem`（一定時間・敵弾を消去、GameScreen で白/グレー濃淡パフ描画）、回復、スタミナ無限(6s)、ダッシュ速度+50%(8s)。

### 10.1 テスト
- 新規純テスト: `WallGravity`（クラスタ検出/重力場）、`Loot`（ドロップ表）、`FlowField`（半径上限）。World系テストが巨大マップ上で新システムをステップし回帰なしを確認。
- 非目標: 重力/バフの config 化（定数で出荷）、HUD バフ表示。

## 11. 敵のトライブ／群れ（v2.7.0）

ユーザー要望。敵に部族（トライブ）概念を付与し、群れ行動＋一部トライブ間の敵対を実装。

- 純 `sim/Tribes`（TDD）: K個のトライブ中心を毎ラン配置し、mob は**最近傍中心**のトライブに所属（空間的に近い敵が同族＝群れの素）。対称な**敵対行列**を確率(0.35)で生成（一部トライブ対が相互敵対）。
- `Mob.tribe` 追加。`MobFactory.spawn(tribe)`、WorldFactory/SpawnerSystem が `tribes.tribeOf(spawn座標)` で割当。
- `AISystem`: 既存の近傍ループに相乗りし、(a)同トライブ**結束(cohesion)**（分離より弱い重み→群れが崩壊しない）、(b)最寄りの**敵対トライブmob**を検出。敵対mobが範囲内なら索敵をそちらへ切替し直進、**接触で相互ダメージ**（プレイヤーが漁夫の利を狙える）。敵対mobが無ければ従来通りプレイヤーを追跡。
- 構成: 5トライブ・敵対35%・cohesion半径5タイル・敵対範囲7タイル・相互ダメージ14。
- テスト: 純 `Tribes`（最近傍割当・敵対の対称性・確率0/1境界）。World テストが巨大マップ上でトライブ込みAIをステップし回帰なし。非目標= 遠隔攻撃のトライブ間適用（接触のみ）、HUD トライブ表示。出荷 = **v2.7.0**。

## 12. 大規模ゲームプレイ拡張（v2.8.0）

ユーザー要望の9項目を一括実装・出荷。

1. **同トライブ大群スポーン**: `SpawnerSystem` が1タイル周辺に同トライブ3〜6体をまとめて pop。
2. **通常移動も慣性**: `MovementSystem` が通常移動でも drift を加算（`MOVE_THRUST` 340、ダッシュ 760）。停止時に滑る。
3. **重力をブロック数比例**: `WallGravity.gravityAt` が pull を √(count) でスケール（大きい星ほど強い・TDD）。
4. **星＝トライブ拠点**: 純 `Bases.pickLargest`（最大クラスタ選定・TDD）＋ `BaseField` ＋ `BaseSystem`（一定時間ごと拠点近傍で自トライブを pop）。GameScreen でトライブ色オーラ描画。
5. **戦術AI（知性比例）**: 純 `Leveling.smarts(intelligence,level)`。AISystem が smarts に応じ (a)壁を盾＝LOS を切る方向へ退避(`coverDir`)、(b)遠距離後衛＝キティング（接近しすぎたら後退）、(c)前衛前進（既定追跡）。`Tribes` に intelligence(0..1) 追加。
6. **ピストル**: reloadTime 1.0・`WeaponDef.infiniteAmmo`（リロードで予備を消費せずマガジン充填）。
7. **敵に武器＋レベル**: `Mob.level/xp`。武器＝技セット(`def.attacks`)、レベルで使用技数を解禁。
8-9. **敵キル経験値**: 純 `Leveling.xpForKill/threshold/attacksForLevel`。AISystem のトライブ同士討ちで止めを刺すと killer に XP、threshold 超でレベルアップ。XP はレベル比例で増加、レベルで技種類(`attacksForLevel`)＋賢さ(`smarts`)増、**基礎攻撃力は不変**。

### 12.1 テスト
- 新規純テスト: `Leveling`(4)、`Bases`(3)、`WallGravity` 比例(1)、`Tribes` 知性(1)。World テストが拠点/群れ/戦術AI/レベルを巨大マップ上でステップし回帰なし。
- 非目標: 遠隔攻撃のトライブ間適用、HUD レベル/トライブ表示、戦術の高度経路探索（cover は近傍 LOS サンプリング近似）。出荷 = **v2.8.0**。

## 13. UI/UX・移動・敵数の調整（v2.9.0）

実機スクショのフィードバック反映（3点）。
1. **HUD 重なり修正**: `HudLayout` バー幅 0.42→0.30W、`Hud.liveHud` で HP/スタ数値を**バー内に右寄せオーバーレイ**、武器パネルは**現在武器の予備のみ**（無限弾は「無限」）右寄せ、残り N 中央隔離。スティックガイドの不透明度↓。
2. **移動を加速度モデルへ**: 純 `Locomotion.applyMove`（加速度→速度・摩擦・速度上限・TDD）。`MovementSystem` は直接移動(dx/dy)を撤廃し drift を移動速度に統一（二重計上解消）。`baseSpeed` 110→85（鈍重化）。移動・ダッシュとも移動方向に加速度を与える。
3. **敵増量**: `WaveConfig`（baseQuota 6→14 / maxQuota 40→90 / liveCapBase 8→18 / maxLiveCap 28→60 / quotaPerWave・liveCapPerWave↑）、`Stages` 初期敵 14-21→30-45、`BaseSystem` CAP 80→120。

### 13.1 テスト
- 新規 `Locomotion.applyMove`(3)。既存2件を新挙動に更新（`speed` 102/204、baseSpeed=速度上限）。出荷 = **v2.9.0**。

## 14. 重力範囲・宇宙背景・リロード（v2.10.0）

1. **重力影響範囲**: `GravitySystem` RANGE 5→**12タイル**（10ブロック以上）。1ブロックごとの線形減衰は維持。
2. **宇宙背景**: `WorldView` の床をタイル状チェッカー（SPACE_A/B 交互）から **7×7 領域のネビュラ＋星空**へ（タイルグリッド感を排除）。アステロイド壁は据え置き。
3. **リロード**: `ReloadSystem` が **全武器**の進行中 `reloadT` を毎フレーム消化（他武器に切替後も装填継続）。**弾切れ(mag≤0)で即オートリロード**。手動（高速）/オート（無操作後）は従来どおり。

### 14.1 テスト
- 純ロジック追加なし（定数/描画/システム挙動）。`:core:test` 回帰なし（`Reload.reload` 純関数は不変、World テストが新 ReloadSystem をステップ）。出荷 = **v2.10.0**。

## 15. 重力1/3・スタミナ1.5・地形ブロック・壁這い・ダッシュ強化・円形重力描写（v2.11.0）

1. **重力 1/3**: `GravitySystem` STRENGTH 38→13。
2. **スタミナ 1.5倍**: `PlayerConfig` staMax 100→150。
3. **壁集合体を重力中心の同心円でプロシージャル描写**: `GravityField` holder に重力クラスタ（世界座標）を公開（GravitySystem が更新）、`GameScreen` が重心中心の同心円リング＋外周リングを描画。
4. **ブロックの地形種**: 純 `Biomes.of`（9×9領域 → ROCK/GRASS/SNOW/MAGMA・TDD）。`WorldView` が地形別に壁を着色。`MovementSystem` が隣接ブロックの地形で効果（マグマ=被ダメ8/s、雪=移動0.62倍、草=スタミナ回復16/s）。
5. **壁這い歩き**: `MovementSystem` の衝突を1回→**X/Y 2軸分離**（壁沿いに滑る＝隣ブロックへの引っかかり解消）。
6. **ダッシュ馬力**: `DASH_ACCEL` 1000→1600、`dashMul` 2→2.5。

### 15.1 テスト
- 新規 `Biome`(2)。`LocomotionTest` 更新（speed dashing 204→255、stamina clamp 150）。`:core:test` 回帰なし。出荷 = **v2.11.0**。
