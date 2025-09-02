export const TILE = 32;
export const DPR  = Math.max(1, Math.min(2.5, window.devicePixelRatio || 1));
export const MAX_DT = 0.05;

export const CTX = (typeof window !== 'undefined' && typeof window.CTX === 'string')
  ? window.CTX
  : '';