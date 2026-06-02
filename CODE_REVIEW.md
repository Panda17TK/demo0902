# 技術的負債・課題の棚卸し（CODE_REVIEW）

現状のコードベース（PR #2 マージ後）を精査し、**問題点・拡張性の欠如・簡易実装**の観点で
課題を洗い出したもの。各項目は実コードの `file:line` を根拠にしている。
修正は本ドキュメントには含めず、優先度と方向性のみ示す。

> 凡例: 🔴 Critical / 🟠 High / 🟡 Medium / 🟢 Low

---

## 1. アーキテクチャ / 拡張性

### 🟠 combat.js が肥大化した God-object（447行）
`systems/combat.js` 1ファイルが「移動・照準・近接・武器選択/リロード・射撃（全武器種）・
自動リロード・プレイヤー弾更新・グレネード・敵弾（ホーミング/地雷）・斬撃の継続ヒット・
爆発ダメージ・ゴッドモード」までを担う。`attacks.js`（敵攻撃）はデータ駆動なのに、
**プレイヤー側の攻撃は combat.js にハードコードされ非対称**。
→ 方向性: `weapons.js`（武器定義＋発射）/ `projectiles.js`（弾更新）/ `melee.js` に分割。

### 🟠 renderFrame() が単一巨大関数（485行）
`render/renderer.js` の `renderFrame` がカメラ・パララックス・タイル・アイテム・弾・敵・
プレイヤー・FX・ライティング・HUDオーバーレイを全部内包。FX 描画は **16分岐の if-else**
（`renderer.js` の `f.type === ...`）で、新FX追加に renderer 改修が必須。
→ 方向性: レイヤー単位の関数分割＋ FX を `type → 描画関数` のレジストリ化（`fx.js` の spawn と対で登録）。

### 🟠 プレイヤー武器が CONFIG 外（データ駆動の非対称）
敵は `core/config.js` の `CONFIG.enemies` でデータ駆動・エディタ編集可能だが、
プレイヤー武器は `state/state.js:50-56` にハードコードで **dev-editor から編集不可**。
→ 方向性: `CONFIG.weapons` を新設し、dev-editor に武器セクションを追加。

### 🟡 アイテムが長い if 連鎖（追加コスト高）
`systems/items.js`（取得処理の if 連鎖）と `render/renderer.js`（描画の if 連鎖）、
さらにドロップ（`ai.js`）・グリフ（`glyphs.js`）の4箇所を触らないと新アイテムを追加できない。
`CONFIG.items` テーブルやファクトリが無い。
→ 方向性: アイテム定義テーブル（取得効果・色・グリフ・ドロップ率）に集約。

### 🟡 マップが単一ハードコード文字列（複数ステージ不可）
`state/map.js:5` の `RAW` 1枚のみ。room/手続き生成/難易度差分なし。文字＝タイル種別で
拡張余地が乏しく、アイテム配置もマップ文字列に埋め込み。
→ 方向性: マップを JSON データ化し複数面・難易度・出口/目標を定義可能に。

### 🟡 状態が巨大なミュータブル bag
`state` を全 system が直接読み書き。**一時フィールド（`_charge`/`_blink`/`_see`/`_cd`/
`fireFlash`）と永続フィールド（`hp`/`x`/`kind`）が mob 上で混在**（`enemies.js:94` ほか）。
セーブ復元（`save-remote.js`）は永続だけ拾うため、保存時に進行中だった溜め/縮地/攻撃CDが
失われ **ロード後のランが保存時と非等価**になる。
→ 方向性: 一時状態を別オブジェクト（`m.runtime`）に隔離し、保存対象を明確化。

---

## 2. パフォーマンス / スケーラビリティ

### 🔴 敵分離が O(n²)（spatial partitioning 不在）
`systems/ai.js:116` の分離処理が、各 mob について全 mob を走査。`maxLiveCap=28` で
約 28²≈784 回/フレーム、各回に `Math.hypot`。さらに弾×敵・近接×敵・爆発×敵・ビーム×敵が
すべて線形走査（`combat.js:235/314/385/410`）。**敵やボス（召喚で増える）が増えるほど劣化**。
→ 方向性: 一様グリッド（uniform grid / spatial hash）で近傍だけ判定。`Math.hypot` を
平方距離 `dx*dx+dy*dy` に置換（範囲判定に sqrt 不要）。

### 🟡 描画の毎フレーム・アロケーション
`renderer.js` で `createRadialGradient`/`createLinearGradient` をアイテム毎・敵弾毎・FX毎に
生成（合計で1フレーム百数十回規模）。GC 圧。`m.def.attacks.find(...)` を**描画ループ内**で毎回
実行（`renderer.js:206`、`attacks.js:253`）。`performance.now()` を1フレームに複数回呼ぶ。
→ 方向性: グラデーションを種類別にキャッシュ、`find` 結果を mob にキャッシュ、`now` は1回取得。

### 🟢 ホットループでの Math.hypot 多用
範囲判定にも sqrt 付き `hypot` を使用（`ai.js:93/102/119` ほか）。
→ 方向性: 比較は平方距離で。

---

## 3. シミュレーション正確性

### 🟡 可変タイムステップ（固定ステップ蓄積なし）
`main.js:160` は `dt = min(MAX_DT, …)`。`MAX_DT=0.05` のため通常弾（360px/s→18px/frame < 32px
タイル）は概ね安全だが、**縮地（最大160px/0.1s）・強ノックバック・グレネードは
すり抜けリスク**。ヒットストップ/スローで `simDt` を縮める実装はフレーム率依存の挙動を残す。
→ 方向性: 固定ステップのアキュムレータ化（決定論・リプレイ・将来の同期に有利）。

### 🟢 セーブデータの範囲検証が薄い
`save-remote.js:84` 等で型チェックはあるが範囲検証が乏しく、`wave.num` や `mods.gunMul` に
異常値が入っても通る（クライアント改変で容易）。

---

## 4. 永続化（Java / ファイル DAO）

### 🟠 手書き JSON パース/エスケープが脆弱
- `ApiScoreServlet.java:20-21` は**正規表現で JSON を抽出**。順序・整形・エスケープ済み引用符に弱い。
- `FileScoreDAO.java:117` の `unescape` は `\"` `\\` のみ対応で、`JsonUtil.escape` が出す
  `\n`/`\t`/`\uXXXX` を復元できず**名前に改行等が入ると破損**。
- `ApiGameStateV2Servlet.java:139` の `escape` も制御文字未対応＝**スロット名に改行で JSON 破壊**の可能性。
→ 方向性: 最小 JSON ライブラリ（`javax.json` 等）に統一、もしくは堅牢なパーサを1つ用意。

### 🟠 入力検証なし（DoS / 異常データ）
スコアの `name` 長さ・`timeMs` 範囲とも未検証（`ApiScoreServlet.java:43-55`）。
リクエストボディ/セーブ blob に**サイズ上限なし**（`while readLine` で全読み）。
スロット数の上限もなし。
→ 方向性: 名前長・timeMs 範囲・本文サイズ・スロット数の上限を導入し 400 を返す。

### 🟡 list が毎回フルスキャン
`FileScoreDAO.listTop` は毎回全 `scores.jsonl` を読む（`:56`）。
`FileGameSaveDAO.list` は毎回ディレクトリ全列挙（`:77`）。件数増で O(n)。
→ 方向性: 上位スコアのメモリキャッシュ、uid 別サブディレクトリ化。

### 🟡 書き込み非アトミック / TOCTOU
`Files.write` の追記やセーブは、クラスタ/NFS では競合の余地。`exists`→`read` の間に削除され得る。
→ 方向性: テンポラリ書き＋ `ATOMIC_MOVE`、必要なら FileLock。単一ノード前提を明記。

### 🟡 削除の競合・全削除の非原子性
`ApiGameStateV2Servlet.java:113` の全削除は list→loop で、その間の新規作成を取りこぼす。
→ 方向性: DAO に原子的 deleteAll を追加。

---

## 5. セキュリティ / 認証

### 🟠 uid がセッションID由来でなりすまし余地
`ControllerServlet.java:18` が `"demo-" + session.getId()` を uid に。ログイン基盤がなく、
セッションID推測でスコア汚染やセーブ閲覧/改変の理論上の余地。レート制限なし。
→ 方向性: 実認証＋強乱数トークン、API レート制限。実験用途なら「非認証である」旨を明記。

### 🟡 セーブ blob を無検証でエコー
`ApiGameStateV2Servlet.java:92` は blob をそのまま JSON 応答に埋め込む。非 UTF-8/不正 JSON で
応答破損。クライアント由来データの無検証保存。
→ 方向性: 書き込み時に UTF-8/JSON 妥当性を検証。

### 🟢 未使用の MySQL コネクタ同梱
`WEB-INF/lib/mysql-connector-j-8.1.0.jar` は未使用なのに WAR 同梱（サイズ増・供給網リスク）。
→ 方向性: 使わないなら除去、使うなら DAO 実装を追加。

---

## 6. テスト / CI / 観測性

### 🟡 テストが Java DAO/サーブレットのみ（ゲーム本体0%）
JUnit 15件は価値があるが、**JS のゲームロジック（physics/los/flowfield/combat/attacks）に
自動テストが皆無**。回帰検知が手動依存。
→ 方向性: Node 上で純粋関数（physics/los/flowfield、ダメージ計算）の単体テストを追加し CI に組込み。

### 🟡 DAO がスタティック singleton で結合
`DAOFactory` の static singleton によりサーブレットのテストが
`-Darpg.persistence=memory` の副作用に依存（`ApiScoreServletTest`）。
→ 方向性: DAO を注入可能に（コンストラクタ/セッター）。

### 🟢 サーバ側ロギング不在
例外は stderr のみ。本番でのデバッグ/攻撃検知が困難。
→ 方向性: `java.util.logging` か SLF4J を導入。

### 🟢 エラー応答が不統一・HTTP ステータス不正確
`ApiScoreServlet` は検証失敗でも 200。`{ok:false}` の有無もエンドポイントで不統一。
→ 方向性: 失敗時は適切な 4xx＋ `{ok:false,error}` に統一。

---

## 7. ドキュメント / 設定の不整合

### 🟢 ゲーム定数が CONFIG を素通り
`config.js` があるのに、移動倍率1.2・近接CD0.32・各種ノックバック・スタミナ増減・弾速360・
分離半径24 などが `combat.js`/`ai.js` にハードコード（dev-editor で調整不可）。
→ 方向性: バランス系定数を `CONFIG.player`/`CONFIG.ai` に寄せる。

### 🟢 セーブのスキーマバージョン未活用
`save-remote.js:30` で `schema:1` を書くが**ロード時に読まない**。バランス改変後の旧セーブが
黙って旧定義で動く/武器並び替えで `curW` がズレる。
→ 方向性: ロード時にバージョン判定＋マイグレーション関数。

---

## 優先度つき改善ロードマップ（提案）

1. **🔴 衝突の空間分割**（uniform grid）＋平方距離化 — 敵増加・ボス召喚に耐える基盤。
2. **🟠 JSON 手書きの撤廃**（サーバ）＋**入力検証/サイズ上限** — 破損・DoS の芽を断つ。
3. **🟠 combat.js / renderer.js の分割**と **FX レジストリ化** — 以降の追加コストを下げる。
4. **🟠 武器の CONFIG 化** ＋ **バランス定数の CONFIG 集約** — dev-editor の一貫性。
5. **🟡 セーブのスキーマ検証＋一時状態の隔離** — ロードの非等価/破損を解消。
6. **🟡 JS ゲームロジックの単体テスト** を CI に追加 — 回帰検知。
7. **🟡 マップのデータ化**（複数面）— リプレイ性。
8. **🟢 ロギング/エラー応答の整備・未使用依存の除去**。

> 総評: ゲームとしては機能し拡張も進んでいるが、(a) 衝突のスケーラビリティ、
> (b) サーバの JSON/入力堅牢性、(c) combat/renderer の肥大化 が、今後の規模拡大で
> 最初にボトルネック化する三大要因。まずこの3点に投資するのが費用対効果が高い。

---

## 対応状況（このブランチで実施）

| # | 項目 | 状態 | 概要 |
|---|---|---|---|
| 1 | 衝突の空間分割＋平方距離化 | ✅ | `systems/spatial.js`（一様グリッド）。ai 分離・弾/近接/ビーム/斬撃/爆発を近傍探索に。`dist2` 追加 |
| 2 | サーバ JSON 堅牢化＋入力検証/サイズ上限 | ✅ | `HttpJson`（上限付き読取・エスケープ復元）。名前長/timeMs範囲/本文サイズ/スロット名を検証、適切な 4xx |
| 3 | combat/renderer 分割＋FXレジストリ | ◑ | FX を `render/fx-draw.js` のレジストリ化（renderer の16分岐撤去）。combat/renderer の全面分割は段階対応（今回は近接ヒット統一・FX分離まで） |
| 4 | 武器の CONFIG 化＋バランス定数集約 | ✅ | `CONFIG.weapons`/`CONFIG.player`/`CONFIG.ai` に集約。state は CONFIG からコピー |
| 5 | セーブのスキーマ検証＋一時状態隔離 | ✅ | `SCHEMA_VERSION`＋`migrate`＋範囲クランプ。mob は `makeMobFromKey` で一時状態を作り直し（持ち越さない） |
| 5b| 固定タイムステップ | ✅ | `FIXED_DT`＋アキュムレータ。ヒットストップ/スローは timeScale 化。トンネリング抑止 |
| 6 | JS ロジックの単体テストを CI に追加 | ✅ | `src/test/js/`（physics/spatial/los/flowfield、計13）。CI で `node --test` |

> 残課題（次段）: combat.js / renderer.js の関数分割の継続、アイテムのデータ駆動化、
> マップの複数面化、認証の本実装、グラデーション等の描画キャッシュ。
