# drift
## モジュール構成

- `core/` — ゲームロジック（プラットフォーム非依存・Kotlin + libGDX + Fleks ECS）
- `desktop/` — LWJGL3 ランチャー（**開発専用**：PCで高速反復）
- `android/` — 配布対象の Android アプリ（APK）

## 操作

| 操作 | キーボード（開発） | タッチ（Android） |
|------|----|----|
| 移動 | WASD / 矢印 | 左スティック |
| 射撃 | K（押しっぱなし） | FIRE |
| 近接 | J | ML |
| ダッシュ（向き方向・強加速・スタミナ大） | Shift | DASH ボタン |
| 流しダッシュ（スティック方向・微加速・スタミナ極小） | — | 左スティックを大きく倒す |
| リロード | R | RL |
| 壁設置 | F | WALL |
| 着陸 / 発進（惑星 ⇄ 宇宙） | L | **惑星かスキャンカードをタップ**／緑の着陸ボタン |
| 武器切替 | 1〜5 | WPN（順送り） |
| インベントリ（装備/持物/マップ/セーブ） | I | 持物ボタン |
| フルスロットル（OC スラスター装備時） | O（押しっぱなし） | 全開ボタン |
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

Kotlin 2.0.21 / libGDX 1.13 / Fleks (ECS) / kotlinx.serialization / Gradle 8.10 / AGP 8.7 / minSdk 24・targetSdk 35

## 実装状況

Phase 0〜9 完了（コアシム / ワールド / 戦闘 / config / 敵 / ウェーブ / 強化カード / ボス /
演出・ゲームオーバー / タッチ・音・ハプティクス / 永続化）。

残タスク（要バイナリアセット・要実機確認）:

- 録音された効果音アセット（現状は手続き生成のブリープ音）
