// js/state/stages.js
// REQ-CONTENT-1 / STAGE-1: ステージ定義（データ駆動）。F5a では骨子（番号算出・メタ）まで。
// マップ実体・敵編成・AI挙動係数の本格定義は F5b/F5c で拡張する。
//
// 連続難易度ティア: 1 ラン中ウェーブは途切れず、STAGE_WAVES ごとに次ステージへ。

export const STAGE_WAVES = 5; // 1 ステージあたりのウェーブ数（暫定）

// 各ステージのメタ。mapId/enemyPool/gimmicks/behavior は F5b/F5c で充実させる。
export const STAGES = [
  { id: 1, name: '崩落区画',   mapId: 'arena' },
  { id: 2, name: '廃液プラント', mapId: 'arena' },
  { id: 3, name: '封鎖回廊',   mapId: 'arena' },
  { id: 4, name: '中枢コア',   mapId: 'arena' },
];

export const STAGE_MAX = STAGES.length;

// REQ-STAGE-1: ウェーブ番号 → ステージ番号（1..stageMax）。超過分は stageMax に丸める
// （エンドレスは別途モードで扱う）。純関数。
export function stageForWave(waveNum, stageWaves = STAGE_WAVES, stageMax = STAGE_MAX) {
  const w = Math.max(1, waveNum | 0);
  const sw = Math.max(1, stageWaves | 0);
  const s = Math.floor((w - 1) / sw) + 1;
  return Math.min(s, Math.max(1, stageMax | 0));
}

// ステージ番号 → 定義（範囲外は最終ステージにクランプ）。
export function stageDef(stage) {
  const i = Math.min(STAGES.length - 1, Math.max(0, (stage | 0) - 1));
  return STAGES[i];
}
