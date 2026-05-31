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
