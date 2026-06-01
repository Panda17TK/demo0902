export const TILE = 32;
export const DPR  = Math.max(1, Math.min(2.5, window.devicePixelRatio || 1));
export const MAX_DT = 0.05;
// 空間グリッドのセル一辺(px)。敵サイズ(~22-46px)より少し大きめにして
// 近傍探索の対象セル数を抑える。
export const CELL = 48;
// 固定タイムステップ（秒）。ロジックはこの刻みで進める（決定論・トンネリング抑止）。
export const FIXED_DT = 1 / 120;

export const CTX = (typeof window !== 'undefined' && typeof window.CTX === 'string')
  ? window.CTX
  : '';