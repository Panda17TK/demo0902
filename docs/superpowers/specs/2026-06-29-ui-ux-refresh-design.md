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
