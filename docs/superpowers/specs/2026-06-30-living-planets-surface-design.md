# LP-E 着陸＋地表ステージ 設計ノート（縦切り実装済み・一部残課題）

- 日付: 2026-06-30（実装状況を追記）
- ステータス: LP-E 骨格＋惑星biome分離＋Living Planets Overdrive 縦切り（生態系・社会・会話・報酬・演出）まで実装済み。残りは帰還地点／地表施設／地表素材の per-block 分布。LP-A〜LP-D（v2.13.0）完了済み。
- 位置づけ: Living Planets の最後の大ピース。これまでと違い**ワールドのモード切替**という大改修。

## 実装状況（現状反映）
本ノートの「アーキテクチャ案」は概ねそのまま実コードに入っている。

実装済み:
- `WorldMode { SPACE, SURFACE }` ＋ 注入ホルダ `WorldState(mode, biome, landingCandidate)`。
- 純粋 `sim/Landing.nearestLandable(...)` ＋ `ecs/systems/LandingSystem`（SPACEで着陸候補をフラグ、HUDヒント表示）。
- `GameScreen` の SPACE⇄SURFACE 遷移（`L`キー）。状態持ち越しは**案A**：`PlayerCarry` でHP/スタミナ/武器/弾/アップグレード/wave を退避し新ワールドへ移植。
- `map/SurfaceStages.forBiome(...)`（惑星biome別アリーナ生成）と `WorldFactory` 配線。
- 地表物理分岐（`MovementSystem`）：SURFACE で宇宙慣性を無効（強い摩擦）、ICE惑星は滑る、MAGMA惑星は全面熱ダメージ。

惑星biome分離（本コミット）:
- 惑星タイプ用に `planet/PlanetBiome { NATURE, MAGMA, ICE, GAS, DEAD, LONELY }` を新設。`PlanetBody.biome` / `WorldState.biome` / `SurfaceStages.forBiome` / 着陸HUD表示・惑星色をこれに統一。
- 地表の素材・効果用の `map.Biome { ROCK, GRASS, SNOW, MAGMA }` はブロック地形用として責務分離（据え置き：magma焼け/snow減速/grass回復）。
- `SurfaceStages` は惑星biomeから地形傾向（密度・広さ・規模）を決める。LONELY=小さくsparse、GAS=開けた地形、MAGMA=岩多め 等。

Living Planets Overdrive 縦切り（生態系・社会・報酬・演出）:
- biome別敵セット（`GameConfig`）：MAGMA/ICE/GAS/DEAD/LONELY の住人を追加。`EnemyDef.biome` で惑星タイプに紐付け。
- biome-gated spawn：`SpawnerSystem` は宇宙waveを generic 敵（`biome == null`）のみに限定し、地表ではwaveを停止。地表の住人は純粋 `sim/SurfaceEcology` が着陸時に一度だけ配置（部族キャンプ／浮遊storm／廃墟／孤独イベント）、`WorldFactory` が空きfloorにスナップして spawn。
- 社会AI：`CreatureState` に Warn/Rally/Surrender/Ignore を追加。純粋 `CreatureAI.nextState` が警告→戦闘、ward/王が傷つくと Rally（逃走/命乞いに優先）、命乞い放置で降伏、非戦闘個体は被弾まで無視。`CreatureMind.provoked` で一度交戦すると警告をやめる。新signalは既定オフで旧敵は不変。
- 会話：`SpeechLines` を12 trigger に拡張（警告/煽り/逃走/命乞い/隠れ/休息/子・王の守護/Rally/王・孤独の遭遇/降伏）。吹き出しに半透明プレート。
- 報酬：王・精鋭が biome 素材（核/遺物）をドロップ。取得で小バフ（最大HP/弾火力/連射/移動/弾薬補充/ランダム）を既存 `Mods`/`Health`/`Ammo` で適用。
- 目標：`sim/SurfaceObjective` が地表HUDに探索目標（主を倒せ→残数→制圧→離陸）を表示。
- 演出：`GameScreen` が惑星をbiome別に描画（ハロー＋大陸/極冠/縞/クレーター/灯）。地表は惑星biome色で淡くトーン。

残課題:
- 帰還地点：地表の専用脱出ポイント（現状は目標達成後に `L` で離陸）。
- 地表施設：拠点・祭壇・建造物など biome 別オブジェクト。
- 地表素材の per-block 分布：`map.Biome`（grass/snow/magma）は現状positionalのまま；地表は惑星biome色の全面トーンで代替。

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
- LP-E1 ✅: `WorldMode`/`WorldState` ＋ pure `Landing` ＋ 着陸候補HUDヒント（遷移なし）。
- LP-E2 ✅: 状態退避→新ワールド移植（案A）＋ SPACE⇄SURFACE 遷移（物理分岐込み）。
- LP-E3 △: `SurfaceStages`（biome別アリーナ）は実装済み。biome-gated 敵 spawn は残課題。
- LP-E4 △: 地表物理の biome 差は ice 滑り/magma 熱を実装済み。nature 視界等は残課題。
- LP-E5 ⬜: 帰還地点・往復ループ・バランスは残課題。
- 惑星biome分離 ✅: `planet/PlanetBiome` を新設し `map.Biome`（地形素材）と責務分離。

## テスト方針
純粋中心: `Landing.nearestLandable`（範囲内で最寄り/範囲外null）、surface 物理の damping 分岐、状態移植のスナップショット往復。遷移配線は World 統合＋ビルド。

## 完了条件
各フェーズ最小実装＋テスト＋`:core:test` 緑＋既存回帰なし＋1フェーズ1コミット。
