# LP-E 着陸＋地表ステージ 設計ノート（未実装・要設計判断）

- 日付: 2026-06-30
- ステータス: 設計のみ（実装は次回）。LP-A〜LP-D（v2.13.0）まで完了済み。
- 位置づけ: Living Planets の最後の大ピース。これまでと違い**ワールドのモード切替**という大改修。

## なぜ別扱いか
これまでの SP/LP は既存システムへの加算的拡張で、ゲームを壊さずTDDできた。LP-E は **SPACE ⇄ SURFACE の世界切替**で、以下に触れる：世界の再構築、物理の分岐、描画の分岐、入力（着陸操作）、そして**状態の持ち越し設計判断**。リスクが高く、設計を決めてから着手すべき。

## 要設計判断（最重要）
**着陸/帰還で状態をどう持ち越すか。**
- 案A（持ち越す・推奨）: HP/スタミナ/弾/武器/アップグレード/wave を保持し、マップとmobだけ差し替え。`GameWorld` 再構築時にプレイヤー構成を引き継ぐ。シームレスだが実装やや重い。
- 案B（ローグライト・降下リセット無し）: 地表は別アリーナとして HP 等保持、宇宙に戻ると継続。最小なら案Aのサブセット。
- 案C（地表で完全リセット）: 不採用（着陸でHP全快/弾リセットは理不尽）。

→ 既定は **案A**（プレイヤーのコンポーネント群を退避→新ワールドへ移植）。

## アーキテクチャ案
- `WorldMode { SPACE, SURFACE }` ＋ 注入ホルダ `WorldState(mode, biome, originPlanetId)`。
- 純粋 `sim/Landing.kt`: `nearestLandable(px, py, planets, range): PlanetBody?`（着陸候補検出）。TDD。
- 着陸トリガ: 候補惑星に接近＋長押し（または専用ボタン）→ `LandingRequested`。
- 遷移: `GameScreen` が `WorldFactory.create(mode=SURFACE, biome=planet.biome, carry=playerSnapshot)` で世界を作り直す。地表マップは `SurfaceStages`（biome別アリーナ、`Stages` 流用）。
- 地表物理: `MovementSystem` が `WorldState.mode` を見て分岐。SURFACE では宇宙慣性を無効（`COAST_DECAY` を強く＝すぐ止まる）。ICE biome のみ滑る（低摩擦）、MAGMA は熱ダメージ（既存 Biomes 効果を流用）。
- 地表の敵: その惑星の biome 部族を spawn（LP-D の nature 部族＋今後の magma/ice/gas/lonely）。biome-gated spawn（現状は宇宙waveに混在）。
- 帰還: 地表の脱出地点 or 専用ボタンで `WorldFactory.create(mode=SPACE, carry=...)` に戻す。
- 描画: SURFACE は地表背景（biome色の地面）、SPACE は既存の星空（WorldView 分岐）。

## フェーズ分割（LP-E 着手時）
- LP-E1: `WorldMode`/`WorldState` ＋ pure `Landing` ＋ 着陸候補HUDヒント（遷移なし）。
- LP-E2: 状態退避→新ワールド移植（案A）＋ SPACE⇄SURFACE 遷移（同一マップで往復、まず物理分岐のみ）。
- LP-E3: `SurfaceStages`（biome別アリーナ）＋ biome-gated 敵 spawn。
- LP-E4: 地表物理の biome 差（ice 滑り/magma 熱/nature 視界）。
- LP-E5: 帰還地点・往復ループ・バランス。

## テスト方針
純粋中心: `Landing.nearestLandable`（範囲内で最寄り/範囲外null）、surface 物理の damping 分岐、状態移植のスナップショット往復。遷移配線は World 統合＋ビルド。

## 完了条件
各フェーズ最小実装＋テスト＋`:core:test` 緑＋既存回帰なし＋1フェーズ1コミット。
