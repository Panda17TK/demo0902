# ARPG サバイバル — Android専用ネイティブ（Kotlin / libGDX）再設計・再実装 仕様書

- 状態: 確定（ブレインストーミング承認済み / 2026-06-28）
- 対象リポジトリ: `Panda17TK/demo0902`（同一リポジトリを置換）
- 旧実装の保全タグ: `legacy-web-v1.1.1`（置換前に付与）

---

## 0. 決定事項サマリ（このセッションで合意）

| # | 論点 | 決定 |
|---|---|---|
| 1 | 方向性 | **Kotlin でネイティブ全面書き換え**（WebView層を完全排除） |
| 2 | 描画基盤 | **libGDX**（OpenGL ES 2Dバッチ） |
| 3 | スコープ | **全機能フル移植**（開発者モード・約16種の攻撃・export/import・全演出を含む） |
| 4 | リポジトリ | **同一リポで旧実装を完全置換**（`legacy-web-v1.1.1` タグ後に撤去、履歴から参照） |
| 5 | 配信レベル | **APK優先・Play対応は後追い構造**（署名release APK を CI 生成、AAB/ストアは後続） |
| 6 | ECS | **Fleks**（Kotlin製ECS）採用 |
| 7 | 開発launcher | **lwjgl3 デスクトップを開発専用で同梱**（製品はAndroid専用） |
| 8 | 描画方式 | **起動時に手続き的ベクター描画をテクスチャへ焼き込み** → SpriteBatch |
| 9 | 音 | WebAudio合成SEを **短いOGGへ事前焼き** して再生 |
| 10 | フォント/i18n | **gdx-freetype + Noto Sans JP（サブセット）** + `I18NBundle`（ja既定） |
| 11 | 端末要件 | `minSdk 24 / targetSdk 35`、`sensorLandscape`、`VIBRATE` |

---

## 1. 目的 / 非目的

### 目的
- 既存の「見下ろし型ウェーブ制ローグライト・サバイバルシューター」を、**Android専用ネイティブゲームとして最適な形**に再実装する。
- 60fps安定・ネイティブ入力/触覚・最小APK・完全オフライン・Play対応可能な構造を得る。
- 既存の面白さ（戦闘・敵AI・ウェーブ・恒久強化・演出）と機能（開発者モード・データ駆動の敵/攻撃）を**忠実にフル移植**する。

### 非目的（v1）
- iOS / Web など Android 以外への配信（クロスプラットフォームの足場は残すが対象外）。
- オンライン機能（サーバ・クラウドランキング・マルチプレイ）。ランキングは**端末内ローカル**のみ。
- 課金・広告。
- 新規ゲームメカニクスの追加（フル移植が完了するまでは現状機能の再現に集中）。

---

## 2. 置換対象（現状の要約）

- ゲーム本体: Canvas 2D + 素のJS（ESモジュール約40ファイル、`core/state/systems/render/services`）。`state` 単一ソース＋イベントバスの ECS風構成。固定タイムステップ＋補間の決定論ループ。
- サーバ: Java 17 / Servlet / JSP / Tomcat 9。実機能は「セッションuid付与・スコアランキングAPI・スロットセーブAPI（ファイルJSON）」のみ。
- Android: Capacitor 6 で WebView ラップ。`assemble-www.mjs` が JSP→HTML 変換し `CTX=''`。APK単体はオフライン起動できるがスコア/セーブはサーバ前提で無効。

→ **Android専用化において Java/Tomcat サーバ層は不要**。面白さは全てクライアントJS側にあり、サーバはセーブ/スコアの保管庫にすぎない。本再設計はこのクライアントロジックを Kotlin/libGDX へ写経し、保管庫を端末内ストレージへ置換する。

---

## 3. ターゲット技術スタック

| 領域 | 採用 | 理由 |
|---|---|---|
| 言語 | Kotlin | ネイティブ・null安全・コルーチン |
| エンジン | libGDX 1.13.x | OpenGL ES 2Dバッチ・入力・音声・アセット |
| ECS | Fleks | Kotlin製・既存 `state`+`systems` に直対応 |
| UI | scene2d.ui + VisUI | 開発者モードGUI・メニュー |
| フォント/i18n | gdx-freetype + Noto Sans JP（サブセット） + `I18NBundle` | 日本語グリフ生成（標準BitmapFontはASCIIのみ） |
| シリアライズ | kotlinx.serialization (JSON) | config/セーブ/スコア、export/import互換 |
| ビルド | Gradle（gdx-liftoff生成） | Maven/Capacitor撤去、APK-first・Play対応構造 |
| 開発launcher | lwjgl3（デスクトップ・開発専用） | PCで即起動・高速反復・旧版突合せ |

---

## 4. アーキテクチャ全体像

```
┌──────────────────────────────────────────────────────────────┐
│ Platform launchers                                           │
│   android/  AndroidLauncher (配布)   desktop/ Lwjgl3 (開発専用)│
└───────────────┬──────────────────────────────────────────────┘
                │ creates
┌───────────────▼──────────────────────────────────────────────┐
│ App : KtxGame  ── 画面遷移 + グローバルサービスDI             │
│   Boot → MainMenu → Game →(overlay) GameOver                  │
│   services: EventBus / Audio / Settings / ConfigStore /       │
│             SaveStore / ScoreStore / Haptics                  │
└───────────────┬──────────────────────────────────────────────┘
                │ GameScreen owns
┌───────────────▼───────────────┐   ┌──────────────────────────┐
│ Fleks World (シミュレーション) │   │ Render pipeline          │
│  components: Transform/Health/ │   │  SpriteFactory(起動時に  │
│   Mob/Player/Bullet/Item/Wall/ │──▶│   手続き描画→テクスチャ  │
│   Attack/Buffs/Mods/Tier ...   │   │   焼込み) → SpriteBatch   │
│  systems(固定順): Spatial→     │   │  + ShapeRenderer(状態     │
│   Combat→Tiles→Spawner→AI→     │   │   オーバーレイ/ベクター) │
│   Attacks→Items→FX→FlowField→  │   │  Parallax/Floor/HUD/FX   │
│   GameOver                     │   │  Camera+Viewport(解像度  │
│  singletons: Config/Maps/      │   │   非依存) Vignette shader│
│   FlowField/SpatialGrid/Wave/  │   └──────────────────────────┘
│   InputState/Cinematics/Stats  │
└────────────────────────────────┘
```

---

## 5. プロジェクト / モジュール構成（Gradle）

```
demo0902/                 ← 同一リポを置換（legacy-web-v1.1.1 タグ後）
├─ settings.gradle.kts    (core, android, desktop)
├─ core/                  …全ゲームロジック（プラットフォーム非依存）
│  └─ src/main/kotlin/io/github/panda17tk/arpg/
│     ├─ App.kt  screens/{Boot,MainMenu,Game,GameOver}Screen.kt
│     ├─ ecs/{components, systems, world}/
│     ├─ data/        … GameConfig, EnemyDef, AttackDef, WeaponDef, UpgradeDef, ConfigStore
│     ├─ attacks/     … AttackRegistry + 16種の挙動
│     ├─ map/         … MapData, Maps, MapSetup, Tiles
│     ├─ pathfinding/ … FlowField(BFS), LOS, SpatialGrid
│     ├─ render/      … GameRenderer, SpriteFactory, EnemySprites(焼込みレシピ),
│     │                 FXRenderer, Hud, Overlays, Parallax, GlyphAtlas
│     ├─ ui/          … DevEditor(VisUI), UpgradeCards, GameOver, TouchControls, Settings
│     ├─ input/       … InputRouter（keys/aim/move/autoFire の統一InputState）
│     ├─ services/    … EventBus, Audio, Settings, SaveStore, ScoreStore, Haptics(interface)
│     └─ core/        … Constants, Time, Rng(seedable), Vec2
├─ android/  AndroidLauncher, Manifest(横向き/VIBRATE), Haptics実装, 署名/アイコン
└─ desktop/  Lwjgl3Launcher（開発専用）
```

設計原則: 多数の小さく凝集したファイル（1ファイル1責務、目安200–400行）。`core` はプラットフォーム非依存に保ち、Android固有（Vibrator・ファイルパス）は interface 越しに `android` で実装する。

---

## 6. 既存 → Kotlin/ECS 対応表（フル移植の写経マップ）

| 既存JS | Kotlin/Fleks 移植先 |
|---|---|
| `main.js`（ループ/DI） | `GameScreen` + `World` 構築 + 固定ステップ |
| `state/state.js`（単一state） | Fleks エンティティ群 + singleton（Config/Wave/Stats/Cinematics…） |
| `core/config.js`（CONFIG） | `data/GameConfig`（@Serializable）+ `ConfigStore` |
| `systems/combat,melee,projectiles,combat-core` | `CombatSystem / MeleeSystem / ProjectileSystem` |
| `systems/ai.js` + `attacks.js`（REGISTRY） | `AISystem` + `attacks/AttackRegistry`（16種） |
| `systems/spawner.js`（ウェーブ） | `SpawnerSystem` + `WaveState` |
| `systems/enemies.js`（mob factory/tier） | `ecs/world/MobFactory`（tier=2/5/10） |
| `systems/flowfield,los,spatial,tiles,items,fx` | `pathfinding/*` + `TileSystem/ItemSystem/FXSystem` |
| `state/map.js, maps.js`（ランダムステージ） | `map/Maps, MapSetup` |
| `state/upgrades.js` + `render/upgrades.js`（3択） | `data/UpgradeDef` + `ui/UpgradeCardsUI` |
| `render/renderer, enemy-sprites, fx-draw, hud, overlay, glyphs` | `render/*`（焼込み＋SpriteBatch/ShapeRenderer） |
| `render/dev-editor.js`（GUIエディタ） | `ui/DevEditorUI`（VisUI） |
| `core/input.js` + `core/touch/*` | `input/InputRouter` + `ui/TouchControls` |
| `services/audio.js`（WebAudio合成SE） | `services/Audio`（事前生成OGG） |
| `systems/save-remote.js`（サーバAPI） | `services/SaveStore`（端末内ファイル・スロット） |
| Java `ApiScore*`/`FileScoreDAO` | `services/ScoreStore`（端末内ローカル・ランキング） |

---

## 7. ゲームループ & 描画パイプライン

### 7.1 シミュレーション（決定論・既存挙動を維持）
- `render(delta)` で `dt = min(MAX_DT, delta)`。
- 時間スケール `timeScale = hitstop係数 × slowmo係数`。タイマ自体は**実時間**で減衰（演出が間延びしない）。
- `accumulator += dt * timeScale`。`FIXED_DT` 刻みで最大 `MAX_STEPS`（=5）回 `world.update()`。スパイラル防止に上限到達時は余剰を破棄。
- 描画は `alpha = accumulator / FIXED_DT` による **prev→cur 補間**。各エンティティに前ステップ座標（`px0/py0`相当）を保持。
- システム更新順（既存と一致）: SpatialGrid構築 → Combat → Tiles → Spawner → AI/Attacks → Items → FX → FlowField定期再計算（0.35s）→ GameOver判定。

### 7.2 カメラ / シネマティック（実時間更新）
- スムーズ追従＋向き先読み（look-ahead）、`killCam` 寄せ、`shake` 減衰。
- `Viewport`（解像度非依存）で多様なAndroid画面比に対応。HUD は別 `ScreenViewport` のステージ。

### 7.3 描画（手続き描画の保存 + バッチ高速化）
- **起動時**: `SpriteFactory` が各敵/プレイヤー/弾/アイテム/FX を `Pixmap`/`FrameBuffer` へ**一度だけ焼き込み**、`TextureAtlas`（`TextureRegion`）化。既存のベクターレシピ（パス/グラデ/シルエット）をそのまま使う。
- **毎フレーム**: clear → ワールドカメラ → パララックス（2層）→ 床タイル（チャンク/決定論ハッシュ斑点）→ アイテム → 敵（焼込みスプライト＋状態オーバーレイ）→ プレイヤー → 弾/敵弾/グレネード/斬撃（トレイル）→ FX（gib/splat/flash/muzzle）→ ライティング/ビネット → スクリーン空間（被弾方向アーク・レターボックス・HUD・トースト・各種オーバーレイ）。
- 状態オーバーレイ（回避点滅・溜めテレグラフ・HPバー）は `ShapeRenderer` で都度描画し、本体描画と分離（既存方針と一致）。

---

## 8. データモデル（kotlinx.serialization）

既存 `CONFIG` を `@Serializable` データクラスへ集約（dev-editorのライブ反映と相性◎）。

```kotlin
@Serializable data class GameConfig(
  val player: PlayerConfig, val waves: WaveConfig, val drops: DropConfig,
  val upgrades: List<UpgradeDef>, val weapons: List<WeaponDef>,
  val enemies: List<EnemyDef>)               // roster（zombie/spitter/stalker/brute/...）

@Serializable data class EnemyDef(
  val key: String, val tier: Int,            // 通常=2 / 中ボス=5 / ボス=10
  val hp: Float, val speed: Float, val passives: List<String>,  // 例: "dodge"
  val attacks: List<AttackDef>, val sprite: SpriteParams)

@Serializable data class AttackDef(val type: String, val cd: Float, val params: JsonObject)
```

- `type` は16種レジストリのキー: `melee / shot / lunge / burst / nova / summon / slam / charge / homing / heal / enrage / mine / barrage / guard / charge_melee / blink`。新攻撃は **Registry へ足すだけ**（既存方針踏襲）。
- 攻撃数の上限は `tier` で管理（通常=2 / 中ボス=5 / ボス=10）。中ボス/ボスは組み込みテンプレート（ブルート/ウォーロック/オーバーロード）。

---

## 9. 永続化（サーバ廃止 → 端末内のみ・完全オフライン）

| 既存 | ネイティブ置換 |
|---|---|
| `settings.js`(localStorage) | `SettingsStore` = libGDX `Preferences`（利き手/透明度/サイズ/自動射撃/音量） |
| `config.js`(localStorage)＋export/import | `ConfigStore` = 同梱既定JSON ＋ `Gdx.files.local("config.json")` 上書き ＋ export/import |
| サーバ・スロットセーブ | `SaveStore` = `saves/<slot>.json`（`SaveData` に `schemaVersion` ＋ `migrate()` ＋ clamp検証） |
| Java スコアAPI/DAO | `ScoreStore` = `scores.json`（ローカル上位N・到達WAVE/生存時間） |

- **uid/セッション不要**（単一ローカルプロフィール）。ネットワーク0。
- インポートJSON・セーブは既存同様に**範囲検証/クランプ**して改竄・破損耐性を維持（既存 `save-remote.js` の `clampNum`/`applyToState` 検証を写経）。
- セーブ schema: 既存 v2 を起点に Kotlin 側 schema を定義。`migrate()` で旧→新移行の拡張点を残す。

---

## 10. 開発者モード・フォント/i18n・入力・音・触覚

### 10.1 開発者モード（VisUI）
- `DevEditorUI`: `scene2d.ui + VisUI`。数値ライブ編集（player/waves/drops/upgrades/敵パラメータ：速度・攻撃頻度・攻撃種類）、敵ロスター編集、保存/リセット/export/import、デバッグ操作（ゴッドモード・WAVEジャンプ・敵スポーン・全敵消去）。表示中はポーズ（既存挙動）。
- ConfigStore を読み書きし**ライブ反映**。

### 10.2 日本語フォント / i18n
- `gdx-freetype` ＋ Noto Sans JP（同梱）。起動時に**全UI文字列を走査して必要グリフのみ生成**（アトラス肥大防止）。HUD/本文/見出しの2–3サイズ。
- 文言は `I18NBundle`（ja既定、en後続可）で外部化。欠落グリフのフォールバック処理を用意。

### 10.3 入力
- `InputRouter` がデスクトップ鍵盤（WASD/J/K/R/1–5/Shift/F/E/P/L/Esc/`）と Android ツインスティックを **単一 `InputState{ keys, move(analog), aim, fire, autoFire, actions }`** に統合（既存 `input` 契約と同形 → systems 不変）。
- `TouchControls`: 画面上ツインスティック（アナログ移動・照準＋射撃）、ボタン（近接/ダッシュ/リロード/武器切替/壁設置/ポーズ/設定）。利き手入替/透明度/サイズ/自動射撃を `SettingsStore` から。自動射撃は最寄りのLOSが通る敵へオート照準。
- アプリ切替/非アクティブで自動ポーズ（既存の visibilitychange 相当 = Android lifecycle pause）。

### 10.4 音 / 触覚
- 音: WebAudio合成SEを**短いOGGへ事前焼き**して `Sound` 再生（低遅延・実装簡潔）。合成パラメータは既存 `audio.js` から移植して生成。
- 触覚（新規付加価値）: `Haptics` interface（core）＋ Android `Vibrator`/`VibratorManager` 実装（命中/爆発/ボス撃破）。デスクトップは no-op。設定で ON/OFF・強度。

---

## 11. ビルド / CI / 配信（APK優先・Play対応構造）

- `android`: `applicationId = io.github.panda17tk.arpg`、`minSdk 24 / targetSdk 35 / compileSdk 35`、`screenOrientation = sensorLandscape`、`android.permission.VIBRATE`、`versionCode` / `versionName` 管理。
- 署名: デバッグは debug キー。release はCIシークレットの keystore → `assembleRelease` で署名済みAPK。`bundleRelease`(AAB) への拡張余地を残す。
- CI刷新（GitHub Actions）: Maven→Gradle。PRは `:core:test` ＋ `:android:assembleDebug`、`v*` タグは署名済み `assembleRelease` をアーティファクト化（現状のAPK生成を踏襲）。Gradle キャッシュ。
- 撤去（**Phase 0 で作業ツリーから削除**、`legacy-web-v1.1.1` タグから参照可能）: `pom.xml` / `src/main/java`(Javaサーバ) / `src/main/webapp`(JS/JSP) / `mobile/`(Capacitor) / Eclipse(`.classpath/.project/.settings`) / MySQLコネクタ。`README / DESIGN / BUILD` を新スタックへ改訂。アイコンは `icon-512` からアダプティブ生成。

---

## 12. テスト戦略（ロジック80%目標）

- `core` はプラットフォーム非依存 → JVMユニット（JUnit5、必要に応じ libGDX `HeadlessApplication`）。
- 既存JSの純粋関数テスト（physics / spatial / los / flowfield）を **Kotlinへ移植**。
- 追加: セーブ schema migrate/clamp、config export/import 往復、攻撃Registry（個別CD）、spawner ウェーブスケール、**RNG決定論ハーネス**（seed固定でN固定ステップ → NaN無し・個数上限・例外で全停止しない）。
- 手動: デスクトップ launcher で高速プレイテスト＋旧JS版との挙動突合せ。実機で60fps（敵24体）・タッチ・触覚スモーク。

---

## 13. 移植フェーズ（ビルド順・各フェーズ出荷可能）

| Ph | 内容 | 完了条件（例） |
|---|---|---|
| 0 | **安全＆足場＆撤去**: `legacy-web-v1.1.1` タグ付与 → 旧 web/Java/Capacitor/Eclipse/Maven を作業ツリーから撤去し Gradle のクリーンなルートを確保（旧実装はタグ／別 `git worktree` から参照・比較）→ gdx-liftoff生成（core/android/desktop, Kotlin, Fleks, freetype, VisUI）→ CI雛形 | 空 GameScreen が実機/PCで起動、CIがAPKを生成 |
| 1 | **コアsim基盤**: Constants/Time/Rng/Vec2、Fleks world、固定ステップ＋補間、カメラ/Viewport、移動/ダッシュ、入力(PC先行) | プレイヤーが動く・ダッシュ・60fps |
| 2 | **ワールド**: Map/Maps/MapSetup/Tiles、flowfield/LOS/spatial、壁設置/破壊 | ランダムステージ生成・壁設置/破壊・経路 |
| 3 | **戦闘**: 武器/弾/近接/combat-core、有限弾薬、リロード | 5武器・近接扇形・弾薬経済・リロード |
| 4 | **データ層**: GameConfig＋ConfigStore（既定＋上書き）、武器/プレイヤーを config 駆動へ | configからプレイヤー/武器生成、永続化 |
| 5 | **敵/AI/攻撃**: MobFactory＋tier、AISystem、AttackRegistry 全16種、中ボス/ボス、回避/charge_melee/blink | 全敵種・全攻撃が既存挙動で動作 |
| 6 | **ウェーブ/ローグライト**: Spawner＋WaveState、3択強化＋mods適用、スコア(到達WAVE) | 波→殲滅→3択→次波、強化が累積 |
| 7 | **描画＆ジュース**: 焼込みパイプライン、敵/弾/FX、HUD、ダメージ数字/フラッシュ/HPバー、hitstop/shake/killCam/slowmo/被弾方向、パララックス/床/ビネット | 旧版と視覚的に同等の手応え |
| 8 | **UI/プラットフォーム**: ツインスティック＋設定、DevEditor(VisUI)、日本語フォント、音(OGG)、触覚、ポーズ/自動ポーズ | タッチ操作・dev編集・日本語表示・SE/触覚 |
| 9 | **永続化＆仕上げ**: Save/ScoreStore、export/import、署名 release APK の CI 確定、ドキュメント刷新（README/DESIGN/BUILD）、実機性能調整、タグ整理（旧コード撤去は Ph0 で完了済み） | セーブ/スコア/設定が端末内に永続、完全オフライン動作 |

各フェーズ: ロジック重視箇所は TDD。デスクトップで旧JS版と並べて忠実度を比較（リグレッション判定）。

---

## 14. リスク / 留意点

- **焼込み描画の見た目再現**（グラデ/シルエット）→ 既存のベクターレシピをそのまま焼き、legacyと並べて**視覚差分**で担保。
- **日本語アトラスのサイズ/品質** → グリフ部分集合＋サイズ限定。
- **音の同一性** → 既存合成パラメータからOGGを事前生成し、必要なら微調整。
- **工数**（dev-editor含むフル移植は大）→ フェーズ分割で各段階を出荷・検証可能に。
- **浮動小数の決定論** → 単一固定ステップ・プラットフォーム依存数学を避ける。`Rng` を seedable に。

---

## 15. 受け入れ基準（忠実度の定義）

- 操作・武器・敵AI・攻撃16種・ウェーブ/中ボス/ボス・3択強化・演出（hitstop/shake/killCam/被弾方向/死亡FX/パララックス）が**旧版と体感同等**。
- 開発者モード（ライブ編集・ロスター編集・保存/リセット/export/import・デバッグ操作）が機能。
- スコア（到達WAVE中心）とスロットセーブが**端末内**に永続し、再起動後も残る。完全オフライン。
- 60fps（敵24体）を実機で維持。例外で全停止しない（ループ try/catch・I/Oはトースト通知）。
- 署名済み release APK が CI で生成され、Android端末にサイドロードして起動できる。
- 旧 Java/JS/Capacitor 一式が撤去され、リポジトリが Kotlin/libGDX 専用になっている（旧実装は `legacy-web-v1.1.1` タグから参照可能）。

---

## 16. 次工程

本仕様の承認後、`writing-plans` スキルで Phase 0 から順に詳細な実装計画（タスク分解・ファイル単位の移植手順・テスト）を作成する。
