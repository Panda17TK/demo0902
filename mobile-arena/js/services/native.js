// js/services/native.js
// Capacitor ネイティブ統合の集約。すべて「任意依存」：プラグインが無い Web/PWA では
// 完全に no-op（例外を投げない）。動的 import で読み込み、失敗は握りつぶす。
//
// F4a(REQ-NATIVE-4): 中断/復帰の自動ポーズ。
//   - Web: document の visibilitychange（タブ非表示）。
//   - Native: @capacitor/app の appStateChange（isActive=false）。
//   - 復帰時は自動再開しない（ポーズを積むだけ）。

let capAppPromise = null;

// @capacitor/app（App プラグイン）を 1 度だけ動的 import。非ネイティブ/不在は null。
function getCapApp() {
  if (capAppPromise) return capAppPromise;
  capAppPromise = (async () => {
    try {
      const core = await import('@capacitor/core').catch(() => null);
      if (!core || !core.Capacitor || !core.Capacitor.isNativePlatform || !core.Capacitor.isNativePlatform()) return null;
      const mod = await import('@capacitor/app').catch(() => null);
      return (mod && mod.App) ? mod.App : null;
    } catch (_e) { return null; }
  })();
  return capAppPromise;
}

// 同期のベストエフォート判定（UI 分岐用。確実な判定は getCapApp 経由）。
export function isNativePlatform() {
  try {
    const cap = (typeof window !== 'undefined') && window.Capacitor;
    return !!(cap && cap.isNativePlatform && cap.isNativePlatform());
  } catch (_e) { return false; }
}

// REQ-NATIVE-4 純ロジック：いま自動ポーズすべきか（playing 中のみ）。
export function shouldAutoPause(state) {
  if (!state || state.gameOver) return false;
  const stack = state.ui && state.ui.overlayStack;
  return !(stack && stack.length > 0); // overlay 表示中は既に停止しているので何もしない
}

// バックグラウンド化（Web=タブ非表示 / Native=非アクティブ）で handler を呼ぶ。
export function onAppBackground(handler) {
  if (typeof handler !== 'function') return;
  // Web/PWA（デスクトップ含む）
  try {
    if (typeof document !== 'undefined' && document.addEventListener) {
      document.addEventListener('visibilitychange', () => { if (document.hidden) handler(); });
    }
  } catch (_e) {}
  // Native（任意）
  getCapApp().then((App) => {
    if (!App || !App.addListener) return;
    try { App.addListener('appStateChange', (st) => { if (st && st.isActive === false) handler(); }); } catch (_e) {}
  }).catch(() => {});
}
