# ARPG サバイバル (Android / Kotlin + libGDX)

見下ろし型・ウェーブ制ローグライトのサバイバルシューターを、**Android専用ネイティブゲーム**として
Kotlin + libGDX で再実装するプロジェクト。

> 旧 Web 版（Java/Servlet + Canvas/JS + Capacitor）は git タグ `legacy-web-v1.1.1` に保存されています。
> 設計の経緯は [docs/superpowers/specs/2026-06-28-android-native-redesign-design.md](docs/superpowers/specs/2026-06-28-android-native-redesign-design.md) を参照。

## モジュール構成

- `core/` — ゲームロジック（プラットフォーム非依存・Kotlin + libGDX + Fleks）
- `desktop/` — LWJGL3 ランチャー（**開発専用**：PCで高速反復）
- `android/` — 配布対象の Android アプリ（APK）

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

Kotlin 2.0 / libGDX 1.13 / Fleks (ECS) / Gradle 8.10 / AGP 8.7 / minSdk 24・targetSdk 35
