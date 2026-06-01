# ARPG サバイバル — 設計・要件ドキュメント

見下ろし型のサバイバル・アクションシューター（「のびハザ」風）。
Java サーブレット/JSP をホストに、ゲーム本体は Canvas + JavaScript（ES Modules）で実装する。

---

## 1. 全体像（アーキテクチャ）

```
ブラウザ                                   サーバ (Tomcat 9 / Servlet 3.1 / Java 17)
─────────────────────────────────────────  ─────────────────────────────────────────
game.jsp (Canvas + UI)                      ControllerServlet  /controller
  └─ js/main.js (ゲームループ/DI)             └─ セッションに uid を付与し game.jsp へ forward
       ├─ core/      constants, events, input
       ├─ state/     state, data, map, binds  ApiScoreServlet        POST /api/score
       ├─ systems/   combat, ai, spawner,      ApiScoreListServlet    GET  /api/score/list
       │             tiles, items, fx,         ApiGameStateV2Servlet  /api/state2(/list)
       │             flowfield, los, physics,
       │             save-remote               DAO (InMemory実装)
       ├─ render/    renderer, hud, overlay,     ├─ ScoreDAO
       │             glyphs                       └─ GameSaveDAO
       └─ services/  audio, storage
```

- 配信経路: `welcome-file = /controller` → `ControllerServlet` がセッションに `uid` を設定 → `/game.jsp` を forward。
- `game.jsp` は `window.CTX`（コンテキストパス）を注入し、`js/main.js`（type="module"）を読み込む。
- ゲームは単一の `state` オブジェクトを各 system 関数が読み書きする ECS 風の構成。`createEventBus` で SE / トースト / ゲームオーバー / リスタートを疎結合に連携。
- 永続化はサーバ側（インメモリ DAO）。スコア（生存時間ランキング）とスロットセーブの2種。MySQL コネクタ jar は同梱されるが現状未使用（将来の DB 化用）。

---

## 2. やりたいこと（コンセプト）

波状に湧くゾンビ／スピッターを生き延びつつ、弾薬・回復・バフ・武器を回収し、
近接・複数銃器・ダッシュ・壁の設置/破壊を駆使して可能な限り長く生存する。
死亡したら「生存時間」をサーバのランキングに登録して競う。

---

## 3. 機能要件

### 3.1 プレイヤー操作
- 移動: WASD / 矢印キー。8方向。
- ダッシュ: Shift（移動中のみ・スタミナ消費・速度2倍）。ダッシュ中は接触で敵を強く弾き、敵弾を反射。
- 近接: J（扇形・多段ヒット・残像 FX・敵弾相殺・壁破壊）。
- 射撃/投擲: K。武器ごとに弾速・連射・拡散・弾数が異なる。
- 武器切替: 1–5（Pistol / Shotgun / MG / Beam / Grenade）。
- リロード: R（無限マガジン仕様＋一定時間後の自動リロード）。
- 壁設置: F（資材を消費。前方タイルに設置）。
- ドア開閉: E（鍵が必要）。
- セーブ: P / ロード: L（サーバのスロットセーブ）。ポーズ: Esc。

#### スマホ／タブレット（タッチ操作）
- タッチ端末では画面上にツインスティック UI を自動表示（`js/core/touch.js`）。
  - 左スティック: 移動（**アナログ**：倒し具合で速度可変）。
  - 右スティック: 照準＋射撃（ドラッグ方向に向き、押下中は発射）。
  - ボタン: 近接 / ダッシュ / リロード / 武器切替 / 壁設置 / ポーズ / 設定。
- 設定（⚙、`js/core/settings.js` に localStorage 永続化）:
  - **左右入れ替え**（利き手対応）／**透明度**／**サイズ**／**自動射撃**。
  - 自動射撃 ON 時は最寄りの「視線が通る」敵へオート照準して発射（右スティック操作中はそちらを優先）。
- 既存の `input`（keys / aim / move / autoFire）へ書き込むだけでゲームロジックは不変。
- ビューポートはズーム/スクロール抑止、`AudioContext` は初回タップで resume。縦持ち時は横向き推奨ヒントを表示。

### 3.2 敵 AI
- ゾンビ: 近接追跡型。BFS フローフィールド＋視線(LOS)で追尾、群れの分離(separation)、スタック脱出。
- スピッター: 射撃型。一定距離を保ちつつ低頻度で射撃。近接は正面90°のみ・低頻度。
- HP 半減で減速。接触はノーダメージで相互ノックバック。

### 3.3 ワールド/アイテム
- タイルマップ（壁/床/ドア）。壁は HP を持ち、破壊で資材化。設置壁も HP を持つ。
- アイテム: 鍵 / 各種弾薬 / 回復 / バフ（範囲・近接火力・移動速度、時間制）/ 武器クレート。
- スポナー: 経過時間に応じて出現数・間隔を強化（難易度上昇）。
- 敵撃破でランダムドロップ。

### 3.4 演出/UI
- カメラ追従・画面シェイク・各種 FX（斬撃/火花/塵/ビーム/爆風）。
- WebAudio による簡易 SE。
- HUD（HP/スタミナ/資材/敵数/次スポーン/武器/弾/バフ）とトースト通知。

### 3.5 ゲームオーバー/戦績
- HP=0 で1度だけゲームオーバー → オーバーレイ表示。
- 生存時間・撃破数を表示。名前を入力して戦績をサーバへ保存。
- ランキング（上位10件、生存時間降順）を表示。
- **リスタート**でその場から再戦可能。

### 3.6 サーバ API
| メソッド/パス | 説明 | 認証 |
|---|---|---|
| `GET /controller` | セッション uid 付与 → ゲーム画面 | – |
| `POST /api/score` | 戦績（name, timeMs）保存 | uid 必須 |
| `GET /api/score/list?limit=N` | 生存時間ランキング | – |
| `GET/POST/DELETE /api/state2?slot=...` | スロットセーブの取得/保存/削除 | uid 必須 |
| `GET /api/state2/list` | スロット一覧 | uid 必須 |

---

## 4. 非機能要件

- **安定性**: 例外で全停止しない（ループは try/catch、I/O は握り潰さずトースト通知）。
  二重実装・孤児ファイルを排し、配信される実装を単一の正系に保つ。
- **性能**: フレームレート非依存（`dt` を 0.05s でクランプ）。敵上限24でも 60fps を維持。
- **保守性**: 機能ごとにモジュール分割。`state` を単一の出所とし、mob ファクトリ等は重複させない。
- **国際化/文字化け対策**: UTF-8 を JSP/Filter/レスポンスで一貫させる。
- **互換性**: モダンブラウザの ES Modules / Canvas2D / WebAudio を前提。

---

## 5. 既知の制約 / 今後の課題

- DAO はインメモリ実装のためサーバ再起動で永続データは消える（MySQL 実装へ差し替え余地あり）。
- マップは単一固定。ステージ進行やボスは未実装。
- セーブ/スコアはセッション `uid` 単位（ログイン基盤は簡易）。

---

## 6. 安定化リビルドでの主な修正点

- `game.jsp` の `window.CTX` 注入に改行が混入し全サーバ通信が壊れていた問題を修正（1行注入）。
- トースト要素がゲームオーバー用オーバーレイ（`display:none`）の内側にあり、ゲーム中に表示されなかった問題を修正（オーバーレイ外へ移動）。
- ゲームオーバー後に「閉じる」だけで永久停止していた UI を **リスタート** に変更（`game:restart`）。
- `updateCombat` の途中 `return` がフレーム後半処理（弾・敵弾・斬撃更新）を飛ばしていた問題を修正。
- 壁の破壊/設置時にフローフィールドが即時再計算されていなかった問題を修正（未 import の参照を解消）。
- mob ファクトリを `systems/ai.js` に一本化（`map.js` / `spawner.js` の重複と初期化欠落を解消）。
- 参照されない孤児ファイル（旧 `WEB-INF/jsp/game.jsp`、未使用 `save-inline.js`）を削除。

---

## 7. 体験の洗練（ジュース・整合性・経済）

セキュリティ/整合性:
- ランキング名の `innerHTML` 描画による**保存型XSSを修正**（エスケープ）。
- `renderer` の**ライティング（ビネット）を再有効化**。

手応え（ジュース）:
- **ダメージ数字**ポップ、敵の**被弾フラッシュ**、敵**HPバー**。
- **ヒットストップ**（命中/爆発で一瞬スロー：ループの sim dt を縮小）。
- **画面シェイク**（被弾/爆発/近接命中）。
- カメラの**スムーズ追従＋向きの先読み**（`state.cam`）。
- 敵ダメージ処理を `combat.hurtMob()` に集約（KB/火花/数字/フラッシュを一元化）。

UI:
- HUD の HP/スタミナを**バー表示**化し、武器/弾薬/生存時間/撃破数を整理。

弾薬経済（有限化）:
- 武器の `infiniteMag`/`infiniteAmmo` を撤去し**弾薬を有限**に（リロードは予備から補充）。
- **敵撃破でドロップ**（弾薬中心、たまに回復/バフ/補給クレート）＝`ai.dropLoot()`。
- 武器クレートは**弾薬補給（リサプライ）**に。マップの A/S は弾薬ピックアップへ戻す。

## 8. ゲームループの核（ウェーブ制＋恒久強化）

- **ウェーブ制**（`systems/spawner.js`）: `state.wave`（`num/phase/toSpawn/...`）で管理。
  各波は規定数を出し切り、全滅させると `intermission` へ。波が進むほど敵数・HP・速度がスケール。
- **強化カード（ローグライト）**（`state/upgrades.js` + `render/upgrades.js`）:
  波クリアで3択を提示し時間停止。選んだ強化は**ラン中ずっと持続**（`player.mods` に累積）。
  火力/連射/近接/最大HP/移動/弾薬調達/吸血/築城術。
- **mods の適用**: `combat` が射撃・近接・移動・連射に `mods` を乗算、`ai` が吸血/ドロップ量、
  HP 上限は `player.hpMax`（最大HP強化で拡張）。
- **スコア**: 到達 WAVE を主指標化（生存時間・撃破数と併記）。HUD にも現在の WAVE と残数を表示。
- セーブ/ロードは `mods/hpMax/wave/stats` も含めて永続化。

## 9. 開発者モード（パラメータエディタ）＆データ駆動の敵

- **設定の単一の出所**（`core/config.js`）: player/waves/drops/upgrades/enemies を 1 オブジェクト
  `CONFIG` に集約。`localStorage` に永続化し、各システムは参照経由で読むため**ライブ反映**。
- **開発者モード**（`render/dev-editor.js`、`` ` `` キー or DEV ボタン）:
  数値パラメータのライブ編集、敵ロスター編集、保存/リセット/エクスポート/インポート、
  デバッグ操作（ゴッドモード・WAVEジャンプ・敵スポーン・全敵消去）。エディタ表示中はポーズ。
- **データ駆動の敵**（`systems/enemies.js`）: `CONFIG.enemies` の定義から mob を生成。
  `tier` により攻撃数の上限を管理（**通常=2 / 中ボス=5 / ボス=10**）。中ボス/ボスは
  組み込みテンプレート（ブルート/ウォーロック/オーバーロード）を提供。
- **攻撃タイプ・システム**（`systems/attacks.js`）: `melee/shot/lunge/burst/nova/summon/slam/`
  `charge/homing/heal/enrage/mine/barrage/guard` ＋ `charge_melee/blink` を登録。
  `CONFIG.enemies[*].attacks` を AI が解釈して実行（個別クールダウン管理）。
  新攻撃は REGISTRY に足すだけ。複数フレームにまたがる行動は `updateMobActions` で進める。
- **ウェーブ編成**: `midBossEvery`/`bossEvery` で中ボス・ボス波を自動編成。
- セーブ復元は `makeMobFromKey` 経由でボスも正しく復元（`kind/waveNum` を保存）。

### 敵の高度な行動（追加）
- **溜め近接 `charge_melee`**: `windup` 秒のテレグラフ（収縮リング）後に強ダメージの薙ぎ払い。
- **縮地 `blink`**: `dur`(≈0.1s) で最大 `maxTiles`(=5) マスを残像付きで瞬間移動。壁で停止。
- **回避 `dodge`**（敵定義のパッシブ、攻撃枠を消費しない）: 被弾の瞬間に低確率発動。
  発動中は白点滅＋当たり判定消失で一撃を無効化（`hurtMob` が単一チョークポイントで判定）。
- 新通常敵 **ストーカー**（縮地＋溜め近接＋回避）、ブルートに溜め近接、ウォーロック/オーバーロードに回避を付与。

## 10. 技術的完成度（永続化・テスト・CI・PWA）

永続化（ファイル/JSON、DB不要）:
- `AppPaths` がデータroot を解決（`-Darpg.data.dir` / `ARPG_DATA_DIR` / `~/.arpg-demo0902`）。
- `FileScoreDAO`: スコアを JSON Lines で追記保存（`scores.jsonl`）。
- `FileGameSaveDAO`: 1スロット=1ファイル（`saves/<uid>__<slot>.save`）。ファイル名はサニタイズ。
- `DAOFactory` が既定でファイル実装を使用。`-Darpg.persistence=memory` でインメモリへ切替（テスト用）。
- Tomcat 再起動後もランキング/セーブが残る。

テスト（JUnit 5 + Mockito）:
- `FileScoreDAOTest` / `FileGameSaveDAOTest`: 保存・整列・再起動相当の永続・サニタイズ。
- `ApiScoreServletTest`: 未ログイン拒否・正常保存・一覧反映（モック request/response）。
- `mvn test` で 15 件実行。`mvn package` 時にも surefire が走る。

CI（GitHub Actions `build.yml`）:
- push/PR で `mvn test` → `mvn clean package`。WAR と surefire レポートを成果物化。

PWA（インストール/オフライン）:
- `manifest.webmanifest`（フルスクリーン・横向き・アイコン）、`icons/`。
- `sw.js`: アプリシェル（JS/CSS/アイコン）を cache-first、`/api/*`・`/controller` はネットワーク優先。
- `game.jsp` で manifest をリンクし Service Worker を登録（HTTPS/localhost で有効）。
