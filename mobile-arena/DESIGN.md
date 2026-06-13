# Wave Arena — 要件定義（モバイルアプリ主軸）

`mobile-arena`（静的PWA / Capacitor ネイティブ）の要件定義。親 `demo0902`（Java版）の
`../DESIGN.md` を土台に、**モバイルを主軸**としつつ **PWA デスクトップ版（Windows版）を併存**させる。

本書は「機能一覧」ではなく**実装指示**を意図する。まず 0 章で **共通設計前提（状態機械・
オーバーレイスタック・入力契約・永続化契約・テスト方針）を固定**し、2 章以降の個別要件は
それに従う。`REQ-xx` で識別し、各 REQ に **AC（受入基準）／対象／仕様**を伴う。

> 凡例: ✅実装済 / 🟡一部 / ⬜未実装　｜　優先度: P1必須 / P2重要 / P3任意

---

## 0. 共通設計前提（最初に固定する）

### 0.1 実行状態モデル（UI状態機械）

`state.paused` は**ゲームロジック更新停止を表す低レベルフラグ**に限定する。
UI上の状態は **`state.ui.overlayStack`（配列）**で管理し、両者を混同しない。

| 状態（最上位overlay） | 意味 | ゲーム更新 | 有効入力 | 表示 |
|---|---|---|---|---|
| （なし・playing） | 通常プレイ | 進む | プレイヤー操作 | HUD＋タッチUI |
| `pause` | ポーズメニュー | 止まる | UIのみ | pause overlay |
| `settings` | 設定 | 止まる | UIのみ | settings overlay |
| `save` | セーブUI | 止まる | UIのみ | save overlay |
| `load` | ロードUI | 止まる | UIのみ | load overlay |
| `confirm` | 終了/リスタート確認 | 止まる | UIのみ | confirm dialog |
| `gameover` | ゲームオーバー | 止まる | UIのみ | game over overlay |
| `dev` | 開発者モード | 止まる | UIのみ | dev overlay |

- 規則: `overlayStack` が非空のとき `state.paused = true`。空かつ非 gameover のとき `false`。
- `gameover` と `dev` も overlayStack に積むが、`gameover` は最下位固定（他を積める）。

### 0.2 オーバーレイスタック（共通制御）

pause/settings/save/load/confirm/gameover/dev はすべて**1つのスタック**で管理する。
- `pushOverlay(name)` / `closeTopOverlay()` / `closeAllOverlays()` を**唯一の遷移API**とする
  （新規 `js/core/ui-state.js` に純ロジックとして実装。DOM 非依存でテスト可能）。
- **Esc（デスクトップ）/ Android Back / 各overlayの閉じるボタン**は、すべて
  `closeTopOverlay()` という**同一処理**を呼ぶ。
- スタックが空で playing 中に Esc/Back → `pushOverlay('pause')`。
- スタックが空で paused（理論上発生しない想定だが保険）→ 何もしない。
- `gameover` 表示中は `pause` を積まない。
- 最上位以外の overlay は操作不可（背面は `pointer-events:none`）。

### 0.3 入力契約（不変）

ゲームロジック（`systems/*`）が読む入力は次のみ：
```
input = { keys, pressed(k), aim:{x,y,active}, move:{x,y,active}, autoFire }
```
- UI／プラットフォーム層は**この契約に書き込むだけ**。`systems/*` は DOM/touch/keyboard/
  Capacitor API を直接参照しない。
- 入力優先度:
  1. **overlayStack 非空のときはゲーム操作入力を破棄**（`move/aim` を中立化、`keys` は無視）。UI操作のみ有効。
  2. 右スティック active → 手動 aim 優先。
  3. 右スティック inactive かつ `autoFire=true` → 自動照準。
  4. 同時入力時は**最後に active になったポインティング入力**を優先。
- **暴発防止**: overlay を閉じた直後（再開直後）の 1 フレームは `pressed(k)` を無効化し、
  タッチの押下状態（`keys['j']` 等の hold）もリセットする。

### 0.4 永続化契約

- ローカル永続化の**唯一の入口は `js/services/kv.js`**（`getItem/setItem/removeItem`）。
  Web/PWA=`localStorage`、Native=`Preferences`（必要に応じ localStorage ミラー）。
- キー接頭辞: `arena_`（ゲーム本体データ：セーブ/スコア）、`arpg_touch_settings_v1`（設定）。
- **セーブデータ schema**（REQ-SAVE-1 で詳細化）には
  `schemaVersion, appVersion, createdAt, updatedAt, slotName, summary{wave,score,weapon,playTimeSec}, state` を含める。
  現行は `schema:2`（`state` 相当のフラット構造）。本要件で **v3** に拡張し、`migrate()` で 2→3 移行。
- 読込時に `schemaVersion` を検証し、未対応バージョンは読まずエラー表示。**破損データは
  そのスロットのみ「読み込み不可」**とし、アプリ全体をクラッシュさせない。

### 0.5 テスト方針

| 対象 | 方法 |
|---|---|
| ズーム算出 `computeView()` | 純関数テスト |
| overlay stack `ui-state.js` | 純関数テスト |
| Back/Esc 共通処理 `closeTopOverlay` 相当 | ハンドラ単体テスト（状態列で検証） |
| スティック正規化 `normalizeStick()` | 純関数テスト |
| save/load（schema/migrate/破損耐性） | mock kv による単体テスト |
| native プラグイン | 動的 import の成功/失敗 mock |
| safe-area レイアウト | 代表端末チェックリスト（手動） |

新規ロジックは**純関数に分離**し `test/` に追加（NFR-5）。

---

## 1. 既存要件の継承

`../DESIGN.md` 3〜10章（ループ／データ駆動の敵・攻撃／開発者モード／演出／永続化／PWA／
テスト・CI）を継承。実装済み。変更点のみ本書 2 章で上書きする。

---

## 2. 新要件

### REQ-DISP: 画面適応（F1）

**REQ-DISP-1 カメラズーム** ✅ P1
- 仕様: 純関数 `computeView({canvasW,canvasH,mapW,mapH,tileSize,viewTilesY})` →
  `{zoom, viewW, viewH, camBounds:{maxX,maxY}}` を返す。`renderFrame` はこれを使う。
  - 通常は可視縦 `viewTilesY=15` を基準。
  - 横画面でマップ横が収まりそうなら `fitZoom` を下限に横全体を収める。
  - 「15タイル維持」と「全マップ表示」が衝突する場合は**黒余白防止を最優先**にズームを clamp。
  - viewport がマップより大きい場合はカメラ原点 clamp ＋描画原点補正で**マップ外を描かない**。
- 対象: `js/render/renderer.js`（`computeView` を分離）, `test/view.test.mjs`(新)。
- AC:
  - `computeView` が `fitZoom ≤ zoom ≤ maxZoom` を満たす。
  - `camX/camY` が常に `0 ≤ camX ≤ max(0, mapW-viewW)`（Yも同様）に clamp。
  - `1080×2160` で可視縦タイル≈15、`2160×1080` で縦≈15 かつ横30タイルが収まる。
  - `mapW≤viewW` でも黒余白が出ない。

**REQ-DISP-2 セーフエリア対応** ✅ P1（CSS）/🟡（touch当たり判定は据置）
- 仕様（CSS）: `:root` に `--sat/--sar/--sab/--sal = env(safe-area-inset-*, 0px)`。
  - タッチUI最低余白: `bottom: calc(16px + var(--sab))`、`left: calc(16px + var(--sal))`、`right: calc(16px + var(--sar))`。
  - overlay panel: `max-height: calc(100dvh - var(--sat) - var(--sab) - 32px)`。高さは `100dvh` 基準（`100vh` 不可）。
- 仕様（当たり判定）: `touch.js` のスティック base 位置も safe-area を考慮（見た目だけでなく操作領域も内側へ）。
- 対象: `css/game.css`, `index.html`(viewport-fit=cover済), `js/core/touch.js`。
- AC:
  - iPhone15Pro 相当（393×852 / top59 / bottom34）で HUD・ポーズボタン・左右スティックが safe-area 内。
  - Android gesture nav（bottom 24〜48px）で右スティック下端がホームジェスチャー領域に重ならない。
  - 横向きで左右 inset を考慮しスティックが角丸/カメラ領域に重ならない。
  - safe-area=0（デスクトップ）でレイアウトが現状と同一。

### REQ-UI: UI共通基盤（F2a）

**REQ-UI-1 オーバーレイ階層とフォーカス** ✅ P1
- 仕様: 0.2 の overlay stack を `js/core/ui-state.js`（純）＋ `js/render/overlay-host.js`（DOM）で実装。
  - overlay 表示中は背面 UI 不操作（`pointer-events`）。
  - overlay の最初の操作対象に focus 移動。デスクトップは Tab 移動可。
  - ボタンは最低 **44×44 CSS px** のタップ領域。
- AC: スタックの push/closeTop/closeAll が 0.2 の規則どおり遷移する（純関数テスト）。

**REQ-UI-2 画面向き/リサイズ対応** ✅ P2
- 仕様: 縦/横を正式対応。回転・resize で canvas サイズ・`viewZoom`・touch base を再計算。
  回転直後は `move/aim` を中立化。
- 対象: `js/main.js`（resize/orientationchange）, `js/core/touch.js`。
- AC: 回転後に極端な縦長/横長でも HUD とタッチUIが重ならず、入力暴発が起きない。

### REQ-TOUCH: タッチで完結（F2a〜F2c）

**REQ-TOUCH-1 ポーズメニュー** ✅ P1（F2a）
> 注: セーブ/ロードは暫定で既存 prompt 版を呼ぶ（F2b でスロットUIに置換）。
> 設定ボタンは F2c で overlay 化するまで非表示。終了は F4（Native Android）で有効化。
- 仕様: `pushOverlay('pause')` で開くモーダル。項目と条件：

  | 項目 | 条件 | 動作 |
  |---|---|---|
  | 再開 | 常時 | `closeTopOverlay()` → paused 解除 |
  | セーブ | `!gameOver` | `pushOverlay('save')` |
  | ロード | スロットあり | `pushOverlay('load')` |
  | 設定 | 常時 | `pushOverlay('settings')` |
  | リスタート | 常時 | `pushOverlay('confirm')`→OKで再開始 |
  | 終了 | Native Android のみ | `confirm`→OKで `exitApp()` |
- 対象: `js/render/pause-menu.js`(新), `index.html`(`#pause-overlay`), `css/game.css`, `js/main.js`。
- AC:
  - playing 中にポーズボタンで `paused=true` かつ `overlayStack=['pause']`。
  - pause 中、敵/弾/タイマー/wave 進行が更新されない。`move/aim` 中立化。
  - 再開時、残留 `pressed(k)` で攻撃/決定/キャンセルが暴発しない（0.3 暴発防止）。
  - gameOver 中はポーズを開けない。リスタートは confirm 経由（誤タップ事故防止）。

**REQ-TOUCH-2 セーブ/ロードのスロットUI** ⬜ P1（F2b）
- 仕様: **簡易スロットUI**（prompt はデスクトップ互換/緊急フォールバックのみ）。スロット数 **3**。

  | 項目 | 内容 |
  |---|---|
  | 表示 | スロット名 / WAVE / スコア / 更新日時（空は「Empty」） |
  | 上書き | 確認あり |
  | 破損 | 「読み込み不可」表示＋削除導線 |
  | ロード後 | **`paused=true` のまま** pause に戻る（直後の被弾理不尽を防ぐ） |
- 対象: `js/render/save-menu.js`(新)/`load-menu.js`(新) or 統合, `js/systems/save-local.js`（summary対応）。
- AC:
  - タッチのみでスロット選択→上書き確認→保存完了まで到達。
  - 空スロットのロードは disabled/「空」表示。破損スロットがあってもクラッシュしない。
  - ロード後は paused のまま（明示的「再開」が必要）。

**REQ-TOUCH-3 設定の独立モジュール化＋導線統合** ⬜ P2（F2c）
- 仕様: 設定UIを `js/render/settings-panel.js`(新) に独立。API:
  `mountSettingsPanel({ rootEl, settings, onChange, onClose })`。`touch.js` は設定値を**読むだけ**。
  ポーズメニューと既存 `⚙` の両方から開ける。
- 設定項目（既存キーは維持、★が新規）:

  | key | 型 | 既定 | 内容 |
  |---|---|---|---|
  | `swap` | bool | false | 左右スティック入替 |
  | `opacity` | num | 0.9 | タッチUI透明度 0.3–1 |
  | `scale` | num | 1.0 | タッチUIサイズ 0.8–1.4 |
  | `autoFire` | bool | false | 自動射撃（モバイル既定は REQ-CTRL-1 で true 検討） |
  | `forceTouchUi`★ | enum | `'auto'` | auto/on/off |
  | `haptics`★ | bool | true(native)/false(web) | 振動 |
  | `deadZone`★ | num | 0.18 | スティック無効域 |
- 対象: `js/core/settings.js`（キー拡張）, `js/render/settings-panel.js`(新), `js/core/touch.js`。
- AC: ポーズ→設定で全項目編集でき、`arpg_touch_settings_v1` に永続化。変更は即時反映。

**REQ-TOUCH-4 タッチUI常時表示の制御** ⬜ P2（F2c）
- 仕様: 判定順 ①`forceTouchUi==='on'`→表示 ②`'off'`→非表示 ③`'auto'`→
  `navigator.maxTouchPoints>0` または `matchMedia('(pointer: coarse)').matches` または Native なら表示。
- 対象: `js/core/settings.js`, `js/main.js`。
- AC: Windows タッチPCの auto で表示／デスクトップ on で表示・off で非表示。変更は即時かつ再起動後も維持。

### REQ-CTRL: 操作最適化（F3）

**REQ-CTRL-1 autoFire 初期値** ⬜ P2
- 仕様: 起動時に `settings.autoFire` を `input.autoFire` へ反映。モバイル既定 true を検討（設定で上書き可）。
- AC: `autoFire=true` 起動で右スティック未操作でも最寄り敵へ発射。操作中は手動方向優先。

**REQ-CTRL-1b autoAim 対象選定** ⬜ P2
- 仕様（純関数 `pickAutoTarget(player, mobs, los)`）: 右スティック inactive かつ autoFire 時に
  ①最寄り ②射線が通る敵を優先 ③同距離なら HP 低 ④同じなら id 昇順。敵不在なら発射しない。
- 対象: `js/systems/combat.js`（既存 `nearestVisibleMob` を仕様準拠に）, `test/autoaim.test.mjs`(新)。
- AC: 上記順で1体を選ぶ／敵不在で無駄撃ちしない（純関数テスト）。

**REQ-CTRL-2 スティック正規化** ⬜ P3
- 仕様（純関数 `normalizeStick({dx,dy,radius,deadZone,maxZone})` → `{x,y,magnitude,active}`）:
  deadZone 未満は中立、以上は 0–1 再マップ、maxZone 以上は magnitude=1 clamp。指を離せば即中立
  （visual knob は 80–120ms で復帰可）。
- 対象: `js/core/touch.js`, `test/stick.test.mjs`(新)。
- AC: 半径18%未満で `active=false`／100%以上で `magnitude∈[0.98,1.0]`／斜めでも縦横より速くならない。

**REQ-CTRL-3 武器切替** 🟡 P3
- 仕様: 巡回切替（実装済み）＋ HUD に現在武器表示（実装済み）。ラジアル等は任意（必須でない）。
- AC: 武器ボタンで 5 種巡回／HUD に現在武器表示。

### REQ-NATIVE: ネイティブ統合（F4）

すべて**任意依存**。`js/services/native.js`(新) に集約し、プラグイン不在の Web では **no-op**・例外なし。

**REQ-NATIVE-1 ハプティクス** ⬜ P2（F4c）
- 仕様: `@capacitor/haptics`。被弾/ボス撃破/強化確定で短い振動。`settings.haptics` で ON/OFF。
  非対応端末では API が resolve するだけ（no-op）。
- AC: 対応イベントで `Haptics.impact` 呼出（mock 検証）／Web で無音・例外なし。

**REQ-NATIVE-2 Android 戻るボタン** ⬜ P1（F4b・Android）
- 仕様: `@capacitor/app` の `backButton`。**いきなり終了しない**。状態遷移：

  | 現在状態 | Back 動作 |
  |---|---|
  | settings/save/load/confirm | その overlay を閉じる（=`closeTopOverlay`） |
  | playing | `pushOverlay('pause')` |
  | pause（最上位） | `pushOverlay('confirm')`（終了確認） |
  | gameover | 何もしない（または confirm でリスタート） |
  | confirm でOK | `App.exitApp()` |
  | confirm でCancel | pause に戻る |
- 対象: `js/services/native.js`, `js/main.js`。Back は 0.2 の `closeTopOverlay` を再利用。
- AC: Android 実機で overlay が順に閉じ、最後に終了確認が出る（いきなり exit しない）。

**REQ-NATIVE-3 ステータスバー / スプラッシュ** ⬜ P2（F4c）
- 仕様: 起動時テーマを `#0b0e13` に揃える。`@capacitor/status-bar` で style 設定。
  **Android 15/16+ の edge-to-edge により背景色制御が効かない場合があるため、色の完全制御では
  なく「safe-area＋アプリ背景色で破綻しない」を目標**にする。`@capacitor/splash-screen` は
  初期化完了後に `hide()`。`launchAutoHide=false` 採用時は**失敗時フェイルセーフ**（一定時間後 hide）。
- 対象: `capacitor.config.json`, `js/services/native.js`。
- AC: ネイティブ起動でステータスバー周辺が破綻せず、スプラッシュが必ず消える（失敗時も）。

**REQ-NATIVE-4 中断/復帰ポーズ** ⬜ P1（F4a）
- 仕様: `@capacitor/app` `appStateChange`（Web は `visibilitychange`）。バックグラウンド化で
  **自動ポーズ**（`pushOverlay('pause')` 相当 or `paused=true`）。復帰時は**自動再開しない**。
- 対象: `js/services/native.js`, `js/main.js`。
- AC: ネイティブでホームに戻すと paused=true／Web のタブ非表示でも自動ポーズ（デスクトップ含む）。

### REQ-SAVE: セーブデータ（横断・F2b）

**REQ-SAVE-1 schema v3** ⬜ P1
- 仕様: 0.4 の構造に拡張。`migrate()` で v2→v3（v2 は summary 欠落→ロード時に算出 or 既定値）。
  `summary` をスロット一覧表示に使う。保存失敗はユーザーへエラー表示。
- 対象: `js/systems/save-local.js`, `test/save.test.mjs`(新)。
- AC: schemaVersion 不一致でクラッシュしない／一覧に summary 表示／保存失敗でエラー表示。

### REQ-A11Y / REQ-PERF（横断）

**REQ-A11Y-1 メニューのアクセシビリティ** ⬜ P2
- overlay ボタンに `aria-label`。設定/セーブ/ロードはスクリーンリーダーで最低限読める。
  タップ領域 44×44 CSS px 以上。OS 文字拡大で破綻しない範囲で追従。

**REQ-PERF-2 Canvas/DPR 制御** 🟡 P1
- 仕様: `DPR=min(devicePixelRatio,2.5)`（実装済）。backing store は resize 時のみ更新（毎フレーム再生成しない）。
  回転/resize 後に renderer と touch layout を再計算。低性能向けに FX density を落とす余地を残す。
- AC: 連続 resize で backing store 再確保が過剰に起きない。

---

## 3. 非機能要件

| ID | 要件 | AC |
|---|---|---|
| NFR-1 性能 | 中位スマホ 60fps 目標、DPR≤2.5 | 敵上限時もフレーム落ちが顕著でない |
| NFR-2 互換 | iOS Safari / Android Chrome / Capacitor WebView | 起動・操作可能 |
| NFR-3 耐久性 | Native で localStorage を Preferences ミラー | `kv.js` 実装済 |
| NFR-4 後方互換 | デスクトップ操作とロジック契約を壊さない | 0.3 契約維持 |
| NFR-5 回帰防止 | 各フェーズで `node --test` 緑、新ロジックは純関数＋テスト | CI 緑 |
| NFR-6 例外安全 | Native 専用処理は Web で no-op・例外なし | 動的 import 失敗を握る |

---

## 4. 共通 AC（横断・最重要）

### タッチ完結性
キーボードを一切使わず、次の操作列が完了できる：
起動→開始→移動→攻撃→武器切替→ポーズ→設定変更→セーブ→ロード→リスタート→GO後の再開。

### 入力分離
- `systems/*` は DOM/touch/keyboard/Capacitor を直接参照しない。
- UI層は `input` 契約と `api` のみでゲームに影響。
- overlay 表示中はゲーム操作入力が反映されない。

### オーバーレイ
- overlay は stack 管理。Esc / Android Back / 閉じるボタンは同一 `closeTopOverlay` を使う。
- 最上位以外は操作不可。閉じた直後に入力が暴発しない。

### 永続化
- 保存/読込はすべて `kv.js` 経由。セーブに `schemaVersion`。
- 破損/未対応データを読んでもクラッシュしない。Native の Preferences 不可時も Web/PWA として動作。

---

## 5. 実装フェーズ（依存順・F2 を分割）

| フェーズ | 含む REQ | 完了条件 |
|---|---|---|
| **F1** 画面適応 | DISP-1(純化), DISP-2, PERF-2 | computeView 純化＋テスト緑、safe-area 反映 |
| **F2a** UI基盤/ポーズ | UI-1, UI-2, TOUCH-1 | overlay stack 純関数テスト緑、ポーズが状態機械どおり |
| **F2b** セーブ/ロードUI | TOUCH-2, SAVE-1 | タッチのみで save→load 到達、schema v3、破損耐性テスト |
| **F2c** 設定統合 | TOUCH-3, TOUCH-4, A11Y-1 | 設定独立モジュール化、常時表示制御、aria |
| **F3** 操作最適化 | CTRL-1, 1b, 2, 3 | 純関数 normalizeStick/pickAutoTarget＋テスト |
| **F4a** 中断復帰 | NATIVE-4 | visibility/appState で自動ポーズ |
| **F4b** Android Back | NATIVE-2 | Back が closeTopOverlay 共通処理に乗る |
| **F4c** 仕上げ | NATIVE-1,3 | ハプティクス/ステータスバー/スプラッシュ（no-op 安全） |

> 重要: **Android Back と pause menu は同じ overlay stack に乗せる**（別実装にすると
> Back/Esc/閉じる/設定が後で噛み合わなくなる）。F2a の `ui-state.js` を F4b が再利用する。

各フェーズ完了時に本書の状態欄（✅/🟡/⬜）とトレーサビリティ表を更新する。

---

## 6. 既知の制約

- APK/IPA ビルドには各 OS SDK が必要（生成物は本リポジトリに含めない）。
- 認証なし・ローカル完結。タイトル画面/ステージ進行/オンライン同期は範囲外（将来課題）。
- StatusBar の色制御は Android 16+ の edge-to-edge で限定的（破綻回避を目標とする）。

---

## 7. トレーサビリティ早見表

| REQ | 状態 | 主対象 |
|---|---|---|
| DISP-1 ズーム | ✅ | `render/renderer.js`(`computeView` 純化), `test/view.test.mjs` |
| DISP-2 セーフエリア | ✅/🟡 | `css/game.css`, `core/touch.js`, `index.html` |
| UI-1 overlay stack | ✅ | `core/ui-state.js`(純), `render/pause-menu.js`, `test/ui-state.test.mjs` |
| UI-2 回転/resize | ✅ | `main.js`(orientation/resize で中立化) |
| TOUCH-1 ポーズ | ✅ | `render/pause-menu.js`(新), `main.js`, `state/binds.js` |
| TOUCH-2 save/load UI | ⬜ | `render/save-menu.js`(新), `systems/save-local.js` |
| TOUCH-3 設定統合 | ⬜ | `render/settings-panel.js`(新), `core/settings.js`, `core/touch.js` |
| TOUCH-4 タッチUI常時 | ⬜ | `core/settings.js`, `main.js` |
| CTRL-1/1b autoFire/autoAim | ⬜ | `core/settings.js`, `systems/combat.js`, `test/autoaim.test.mjs` |
| CTRL-2 スティック | ⬜ | `core/touch.js`, `test/stick.test.mjs` |
| CTRL-3 武器切替 | 🟡 | `core/touch.js`, `render/hud.js` |
| NATIVE-1 ハプティクス | ⬜ | `services/native.js`(新), `main.js` |
| NATIVE-2 Android Back | ⬜ | `services/native.js`, `core/ui-state.js` |
| NATIVE-3 ステータスバー | ⬜ | `capacitor.config.json`, `services/native.js` |
| NATIVE-4 中断復帰 | ⬜ | `services/native.js`, `main.js` |
| SAVE-1 schema v3 | ⬜ | `systems/save-local.js`, `test/save.test.mjs` |
| A11Y-1 / PERF-2 | ⬜/🟡 | overlay 各所 / `main.js`,`renderer.js` |
