# Living Planets 実装計画（物理基盤の後継）

> 元構想: ユーザー提供の「Living Planets Expansion」ドキュメント。本計画はその**物理基盤（Space Physics Overhaul / v2.12.0 済）を踏まえた再スコープ版**。合言葉「惑星はステージではない。生き物の社会である。」

**Goal:** 敵を単なる攻撃オブジェクトでなく**生き物**として扱う。怯え・逃走・命乞い・隠れ・休息・守護を持ち、高知能個体は話す。惑星に着陸して地表の生態へ降りる。

**Architecture:** 既存ECSをその場で拡張。判断ロジックは純粋モジュール（`sim/`）でTDD。`EnemyDef` に生態パラメータを足し（legacy-safe デフォルトで既存6体の挙動は不変）、`CreatureMind` を全mobに付与してデータ駆動化。

**前提（物理刷新で完了済）:** 惑星（`PlanetBody`/`PlanetField`）・重力・ダッシュ脱出・クラッシュ・`gravityResponse`。

## フェーズ（依存順）

- **LP-A 生き物AI（核心）**: `EnemyDef` 生態フィールド＋`CreatureMind`＋`sim/CreatureAI.nextState`（純粋・TDD）。状態 Hostile/Flee/Beg/Hide/Rest。AISystem が状態で移動方向と攻撃可否を切替（Flee=逃走・無攻撃、Beg=停止・無攻撃、Hide=遮蔽へ→Rest、Rest=回復・接近で解除）。既存敵は bravery=1 で不変、数体に低 bravery+canBeg / 高 intelligence+canHideAndRest を付与。
- **LP-B 会話・吹き出し**: `Speech` component＋`sim/SpeechLines`（trigger→台詞、データ駆動）＋AISystem/描画で頭上に短文（命乞い・警告・逃走）。canSpeak な高知能個体のみ。
- **LP-C 部族・家族・守護**: `EnemyDef` に role（child/elder/king/guardian）＋`ProtectionTarget`。子・長老・王の近くで守護者が Protect（恐怖より守護優先・奮起）。王は周囲 morale aura。`sim` で純粋判定。
- **LP-D biome 敵セット**: nature/magma/ice/gas/lonely の最小敵を `GameConfig` に追加（既存 AttackSpec の組合せ＋生態パラメータ差）。
- **LP-E 着陸＋地表ステージ**: `WorldMode` SPACE/SURFACE、着陸トリガ、`SurfaceStages`、地表物理（慣性無効、ice 滑り/magma 熱）。最大の基盤投資。

## 完了条件（各フェーズ共通）
最小実装＋テスト＋`:core:test` 緑＋既存回帰なし＋1フェーズ1コミット。可能なら `:android:assembleDebug`。

## テスト方針
純粋モジュール中心（`CreatureAI`/`SpeechLines`/守護判定/地表物理）。状態遷移・台詞選択・守護発火を単体で。配線は World 統合テスト＋ビルドで担保。
