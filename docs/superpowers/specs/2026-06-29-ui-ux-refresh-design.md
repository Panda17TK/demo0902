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
