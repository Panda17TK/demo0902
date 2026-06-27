// webapp/js/core/touch/haptics.js
// 触覚フィードバック（バイブ）。対応端末（主に Android Chrome）でのみ作動し、
// iOS Safari など navigator.vibrate 非対応では黙ってスキップする。
// cfg.haptics が false の場合も無効。

export function makeHaptic(cfg) {
  return function haptic(ms = 12) {
    try {
      if (cfg && cfg.haptics && navigator && typeof navigator.vibrate === 'function') {
        navigator.vibrate(ms);
      }
    } catch (_e) { /* 非対応端末は無視 */ }
  };
}
