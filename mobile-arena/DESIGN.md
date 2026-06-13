# Wave Arena — 要件定義（モバイルアプリ主軸）

本書は `mobile-arena`（静的PWA / Capacitor ネイティブ）の要件定義。
親プロジェクト `demo0902`（Java サーブレット版）の要件 `../DESIGN.md` を土台に、
**モバイルアプリを主軸**としたうえで **PWA デスクトップ版（Windows版）も併存**させる。

各要件は `REQ-xx-n` で識別し、**受入基準（AC: Acceptance Criteria）**・**対象**・
**仕様**を伴う。実装は本書の REQ をトレース元とする。

> 凡例: ✅実装済み / 🟡一部 / ⬜未実装　｜　優先度: P1(主軸必須) / P2(重要) / P3(任意)

---

## 0. プラットフォーム方針

| 項目 | 内容 |
|---|---|
| 主軸 | モバイル（タッチ）。スマホ縦/横・タブレットを第一に設計 |
| 併存 | PWA デスクトップ版（キーボード＋マウス）。機能は削らない |
| 配布 | ①PWA（ブラウザ/ホーム画面追加）②Capacitor ネイティブ（iOS/Android） |
| サーバ | 不要。スコア/セーブはローカル永続（ネイティブは Preferences ミラー） |
| 不変条件 | ゲームロジックは入力契約 `input = { keys, pressed(k), aim{x,y,active}, move{x,y,active}, autoFire }` 経由でのみ操作する。UI/プラットフォーム層はこの契約に書き込むだけで、`systems/*` を改変しない |

---

## 1. 既存要件（親 DESIGN.md から継承・実装済み）

`../DESIGN.md` 3〜10章を継承。mobile-arena では実装済みで、変更点のみ本書 2 章で上書きする。
- ゲームループ（ウェーブ制ローグライト・強化カード・中ボス/ボス）／データ駆動の敵・攻撃／開発者モード。
- 演出（カメラ追従・シェイク・FX・補間描画・被弾方向インジケータ・ボス撃破スロー）。
- タッチ操作（ツインスティック・操作設定・横向きヒント・AudioContext resume）。
- 永続化（スコアランキング＝到達WAVE優先／スロットセーブ、`localStorage` 接頭辞 `arena_`）。
- PWA（`manifest.webmanifest` / `sw.js` 相対パス cache-first）、単体テスト＋CI、Pages デプロイ、Capacitor 雛形。

---

## 2. 新要件（モバイル主軸化）

### REQ-DISP: 画面適応（フェーズ1）

**REQ-DISP-1 カメラズーム** ✅ P1
- 仕様: 画面サイズ/DPR に依らず、可視ワールドの**縦幅を一定タイル数**に保つ。既定
  `VIEW_TILES_Y = 15`。マップが画面に収まる場合ははみ出さない最小ズーム（fitZoom）以上。
- 対象: `js/render/renderer.js`（`renderFrame` 冒頭で `zoom` 算出、`ctx.scale(zoom)`）。
- 公開状態: `state.viewZoom`（倍率）, `state.viewW/viewH`（可視ワールドpx）, `state.camX/camY`
  （カメラ原点・ワールドpx）。スクリーン→ワールド変換は
  `worldX = camX + screenPxX / viewZoom`（screenPxX はバッキングpx）。
- AC:
  - `1080×2160`（縦）で可視縦タイル数が `15`（±0.5）であること。
  - `2160×1080`（横）で可視縦タイル数が `15` かつ横は全マップ幅（30タイル）が収まること。
  - 画面がマップより大きい場合でも、カメラ外（マップ外）の黒余白が出ないこと。
- テスト: ズーム算出関数を純関数として切り出し `test/` で `{zoom, tilesX, tilesY}` を検証。

**REQ-DISP-2 セーフエリア対応** ⬜ P1
- 仕様: ノッチ／ホームインジケータ／角丸を避けて HUD・タッチUI・オーバーレイを配置。
  `env(safe-area-inset-top/right/bottom/left)` を CSS 変数化して各UIのオフセットに加える。
- 対象: `css/game.css`（`:root` に `--sat/--sar/--sab/--sal` を定義し `#hud` `#help`
  `.touch-controls .tc-*` `.overlay .panel` の位置に反映）、`index.html`（`viewport-fit=cover` は設定済み）。
- AC:
  - iPhone 風セーフエリア（上44px/下34px）を想定した検証で、HUD左上・DEVボタン・
    ツインスティックの可視ボタンがセーフエリア矩形の内側に収まること（手動確認チェックリスト）。
  - セーフエリアが 0 の環境（デスクトップ）でレイアウトが現状と同一であること。

### REQ-TOUCH: タッチで完結する操作系（フェーズ2）

要件: **キーボード非接続の端末だけで全機能に到達できる**。キー専用の入口を撤廃する。
現状、セーブ/ロードは `state/binds.js` の `P`/`L` キー専用で、タッチからは到達不能。

**REQ-TOUCH-1 ポーズメニュー** ⬜ P1
- 仕様: ポーズ中に表示するモーダル。項目＝**再開 / セーブ / ロード / 設定 / リスタート**。
  ゲーム中は `paused=true` で時間停止。ツインスティックのポーズボタン（`api.pause`）と
  デスクトップの `Esc` の双方から開く。
- 対象: 新規 `js/render/pause-menu.js`（`mountPauseMenu(overlayEl, bus, state, api)`）、
  `index.html`（`#pause-overlay`）、`css/game.css`、`js/main.js`（mount＋api配線）。
- 連携: `bus` イベント `game:pause` / `game:resume` を新設、または既存 `api.setPaused` を使用。
  セーブ/ロードは `save-local.js` の `saveChooser/loadChooser` を呼ぶ。
- AC:
  - タッチのポーズボタン押下でメニューが開き、`state.paused === true`。
  - 「再開」で閉じて `state.paused === false`。
  - 「セーブ」「ロード」が `saveChooser/loadChooser` を起動する（現状の prompt ベースでも可）。
  - ゲームオーバー中（`state.gameOver`）はポーズメニューを開かない。

**REQ-TOUCH-2 セーブ/ロードのUI到達性** ⬜ P1
- 仕様: REQ-TOUCH-1 のメニュー項目から到達。`P`/`L` キーは**デスクトップ専用の補助**として残す
  （撤廃ではなく「タッチで代替経路を必ず用意する」）。
- 対象: `js/render/pause-menu.js`、（任意）スロット選択を prompt から簡易リストUIへ。
- AC: タッチのみの操作列「ポーズ→セーブ→（名前）→保存」「ポーズ→ロード→選択」が完了できる。

**REQ-TOUCH-3 設定の導線統合** ⬜ P2
- 仕様: 操作設定（`⚙`：左右入替・透明度・サイズ・自動射撃。`settings.js`／キー
  `arpg_touch_settings_v1`）をポーズメニューの「設定」からも開けるようにする。既存の `⚙`
  ボタンは維持。
- 対象: `js/render/pause-menu.js` → 既存設定パネルのトグルを呼ぶ（`touch.js` の `togglePanel`
  を外部公開、または設定パネルを独立モジュール化）。
- AC: ポーズ→設定で既存の設定項目が編集でき、変更が `arpg_touch_settings_v1` に永続化される。

**REQ-TOUCH-4 タッチUIの常時利用可否** ⬜ P2
- 仕様: タッチUIは `isTouchDevice()` 検出依存だが、**タッチ対応デスクトップ**や検出漏れに備え、
  設定で「タッチUIを常に表示」を選べる（既定はオート）。
- 対象: `settings.js`（`forceTouchUi: 'auto'|'on'|'off'`）、`js/main.js`（表示判定）。
- AC: 設定 `on` でデスクトップでもツインスティックが表示される／`off` で非表示。

### REQ-CTRL: 操作系のモバイル最適化（フェーズ3）

**REQ-CTRL-1 自動照準の既定切替** ⬜ P2
- 仕様: モバイルで `autoFire`（最寄りの視線の通る敵へオート照準）を**設定の既定値**として
  選べる。`settings.js` の `autoFire` を起動時に `input.autoFire` へ反映する。
- 対象: `settings.js`（既存キー流用）、`js/main.js`（初期反映）、`js/core/touch.js`（設定UI）。
- AC: 設定 `autoFire=true` で起動直後から、右スティック未操作でも最寄り敵へ自動発射される。

**REQ-CTRL-2 スティック追従/デッドゾーン調整** ⬜ P3
- 仕様: 左スティックのデッドゾーン・最大倒し量・指を離した時の中立復帰を調整し誤操作を低減。
  既存のアナログ移動契約（`input.move`）は不変。
- 対象: `js/core/touch.js`。
- AC: デッドゾーン内（中心付近）で `move.active=false`、最大倒しで `|move|≈1` になる
  （ユニットテスト可能なら純関数化して検証、不可なら手動）。

**REQ-CTRL-3 武器切替の改善** ⬜ P3
- 仕様: 現状の「武器」ボタンは巡回切替。現在武器をHUDに明示（実装済み）し、操作性は維持。
  ラジアル等の追加UIは任意（本要件では必須としない）。
- AC: 武器ボタンで 5 種を順に切替でき、HUD に現在武器が表示される（既存挙動の確認）。

### REQ-NATIVE: ネイティブ機能統合（フェーズ4・Capacitor 前提）

すべて**任意依存**。プラグイン不在の Web/PWA では no-op フォールバックし、例外を出さない。
実装は `js/services/native.js`（動的 import でプラグインを読み込むラッパ）に集約する。

**REQ-NATIVE-1 ハプティクス** ⬜ P2
- 仕様: 被弾・ボス撃破・強化カード確定などで短い振動（`@capacitor/haptics`）。設定で ON/OFF。
- 対象: 新規 `js/services/native.js`（`haptic(kind)`）、`bus` 購読（`game:over`/被弾イベント等）。
- AC: ネイティブで対応イベント時に `Haptics.impact` が呼ばれる（モックで検証）／Web では無音で
  例外なし。

**REQ-NATIVE-2 Android 戻るボタン** ⬜ P1（Android）
- 仕様: `@capacitor/app` の `backButton`。ポーズメニュー/設定が開いていれば閉じる、ゲーム中なら
  ポーズ、最上位（タイトル相当が無いので「ポーズ済みで何も開いていない」状態）なら
  終了確認（`exitApp`）。
- 対象: `js/services/native.js`、`js/main.js`（ハンドラ登録）。
- AC: Android 実機/エミュレータで、開いているオーバーレイが順に閉じ、最後に終了確認が出る。

**REQ-NATIVE-3 ステータスバー / スプラッシュ** ⬜ P2
- 仕様: 全画面・暗色（`#0b0e13`）に整合。`@capacitor/status-bar` で overlay/style 設定、
  `@capacitor/splash-screen` を起動後に hide。
- 対象: `capacitor.config.json`、`js/services/native.js`。
- AC: ネイティブ起動時にステータスバーが暗色テーマ、スプラッシュが適切に消える。

**REQ-NATIVE-4 アプリ中断/復帰時のポーズ** ⬜ P1
- 仕様: `@capacitor/app` の `appStateChange`。バックグラウンド化で自動ポーズ
  （`state.paused=true`）、復帰時は**自動再開しない**（プレイヤーが意図的に再開）。
- 対象: `js/services/native.js`、`js/main.js`。
- AC: ネイティブでホームに戻すと `state.paused===true`。Web の `visibilitychange` でも同様に
  自動ポーズ（デスクトップ含む）。

---

## 3. 非機能要件（モバイル観点）

| ID | 要件 | 受入基準 |
|---|---|---|
| NFR-1 | 性能 | 中位スマホ想定で 60fps 目標。DPR 上限 2.5。敵上限時もフレーム落ちが顕著でない |
| NFR-2 | 互換 | iOS Safari / Android Chrome / Capacitor WebView で起動・操作可能 |
| NFR-3 | 耐久性 | ネイティブで `localStorage` を Preferences にミラー（実装済み `kv.js`） |
| NFR-4 | 後方互換 | デスクトップのキーボード/マウス操作とロジック契約を壊さない |
| NFR-5 | 回帰防止 | 各フェーズで `node --test` が緑。純関数化できる新ロジックはテストを追加 |
| NFR-6 | 例外安全 | ネイティブ専用処理はプラグイン不在の Web で例外を出さない（no-op） |

---

## 4. 実装フェーズと完了条件

| 区分 | 含む REQ | 完了条件 |
|---|---|---|
| **F1 画面適応** | DISP-1✅, DISP-2 | ズーム要件化済み＋セーフエリア対応。テスト緑 |
| **F2 タッチUX** | TOUCH-1,2,3,4 | タッチのみで開始→プレイ→ポーズ→セーブ→ロード→設定→GO→リスタートが通る |
| **F3 操作最適化** | CTRL-1,2,3 | 自動照準の既定切替・スティック調整。ロジック契約不変 |
| **F4 ネイティブ統合** | NATIVE-1〜4 | 各プラグインが任意依存で動作、Web で no-op |

各フェーズ完了時に本書の状態欄（✅/🟡/⬜）を更新する。

---

## 5. 既知の制約

- APK/IPA ビルドには各 OS の SDK が必要（本リポジトリに生成物を含めない）。
- 認証なし（実験用途・ローカル完結）。スコアは端末ローカルのみ。
- タイトル画面・ステージ進行・オンライン同期は本要件の範囲外（将来課題）。

---

## 6. トレーサビリティ早見表

| REQ | 状態 | 主対象ファイル |
|---|---|---|
| DISP-1 ズーム | ✅ | `render/renderer.js` |
| DISP-2 セーフエリア | ⬜ | `css/game.css`, `index.html` |
| TOUCH-1 ポーズメニュー | ⬜ | `render/pause-menu.js`(新), `index.html`, `main.js` |
| TOUCH-2 セーブ/ロード到達 | ⬜ | `render/pause-menu.js`, `systems/save-local.js` |
| TOUCH-3 設定統合 | ⬜ | `render/pause-menu.js`, `core/touch.js`, `core/settings.js` |
| TOUCH-4 タッチUI常時 | ⬜ | `core/settings.js`, `main.js` |
| CTRL-1 自動照準既定 | ⬜ | `core/settings.js`, `main.js`, `core/touch.js` |
| CTRL-2 スティック調整 | ⬜ | `core/touch.js` |
| CTRL-3 武器切替 | 🟡 | `core/touch.js`, `render/hud.js` |
| NATIVE-1 ハプティクス | ⬜ | `services/native.js`(新), `main.js` |
| NATIVE-2 戻るボタン | ⬜ | `services/native.js`, `main.js` |
| NATIVE-3 ステータスバー | ⬜ | `capacitor.config.json`, `services/native.js` |
| NATIVE-4 中断/復帰ポーズ | ⬜ | `services/native.js`, `main.js` |
