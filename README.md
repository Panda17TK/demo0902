# ARPG サバイバル (Android / Kotlin + libGDX)

見下ろし型・ウェーブ制ローグライトのサバイバルシューターを、**Android専用ネイティブゲーム**として
Kotlin + libGDX で再実装するプロジェクト。

> 旧 Web 版（Java/Servlet + Canvas/JS + Capacitor）は git タグ `legacy-web-v1.1.1` に保存されています。
> 現行ブランチには旧コードは含まれません（Phase 0 でクリーンに再スキャフォールド済み）。
> 設計の経緯は [docs/superpowers/specs/2026-06-28-android-native-redesign-design.md](docs/superpowers/specs/2026-06-28-android-native-redesign-design.md) を参照。

## モジュール構成

- `core/` — ゲームロジック（プラットフォーム非依存・Kotlin + libGDX + Fleks ECS）
- `desktop/` — LWJGL3 ランチャー（**開発専用**：PCで高速反復）
- `android/` — 配布対象の Android アプリ（APK）

## ゲーム内容

- 5 種の武器（ピストル / ショットガン / マシンガン / ビーム / グレネード）と近接・ダッシュ・設置壁
- 通常敵 3 種（ゾンビ / スピッター / ストーカー）＋ 中ボス 2 種・ボス 1 種（全 11 種の攻撃ハンドラ）
- エスカレートするウェーブ（5 の倍数で中ボス、10 の倍数でボス）とスコア
- ウェーブ間の 3 択強化カード（恒久強化・ラン中持続）
- 撃破/被弾の演出（破片・画面シェイク）とゲームオーバー＋リスタート
- タッチ操作（左：移動スティック / 右：アクションボタン）・効果音・ハプティクス
- ハイスコア永続化（最高ウェーブ / 撃破数）

## 操作

| 操作 | キーボード（開発） | タッチ（Android） |
|------|----|----|
| 移動 | WASD / 矢印 | 左スティック |
| 射撃 | K（押しっぱなし） | FIRE |
| 近接 | J | ML |
| ダッシュ | Shift | DASH |
| リロード | R | RL |
| 壁設置 | F | WALL |
| 武器切替 | 1〜5 | WPN（順送り） |
| 強化カード選択 | 1 / 2 / 3 | （数字キー） |
| リスタート | R（ゲームオーバー時） | — |

## 開発

```bash
# 前提: JDK 17 以上（JDK 21 で動作確認済み）, Android SDK (local.properties に sdk.dir)

# デスクトップで起動（開発用）
./gradlew :desktop:run

# ユニットテスト
./gradlew :core:test

# デバッグ APK をビルド
./gradlew :android:assembleDebug   # → android/build/outputs/apk/debug/
```

## 技術スタック

Kotlin 2.1 / libGDX 1.13 / Fleks (ECS) / kotlinx.serialization / Gradle 8.10 / AGP 8.7 / minSdk 24・targetSdk 35

## 実装状況

Phase 0〜9 完了（コアシム / ワールド / 戦闘 / config / 敵 / ウェーブ / 強化カード / ボス /
演出・ゲームオーバー / タッチ・音・ハプティクス / 永続化）。

残タスク（要バイナリアセット・要実機確認）:

- 日本語フォント描画（`gdx-freetype` + `.ttf` 同梱が必要。現状 UI は ASCII ラベル）
- 録音された効果音アセット（現状は手続き生成のブリープ音）
