// webapp/js/core/touch/detect.js
// タッチ端末の判定。
//
// 旧実装は `ontouchstart`／`maxTouchPoints` を単純 OR していたため、タッチ対応の
// ノートPC（マウス併用・主ポインタは fine）でも常にバーチャルパッドが出てしまっていた。
// ここでは「主ポインタが粗い(coarse)」または「ホバー不可かつタッチ入力あり（=純タッチ端末）」
// を条件にして誤検出を抑える。
export function isTouchDevice() {
  try {
    const mm = (typeof matchMedia === 'function') ? matchMedia : null;
    const coarse   = mm ? mm('(pointer: coarse)').matches : false;
    const noHover  = mm ? mm('(hover: none)').matches : false;
    const hasTouch = ('ontouchstart' in window) || !!(navigator && navigator.maxTouchPoints > 0);
    return coarse || (noHover && hasTouch);
  } catch (_e) {
    return false;
  }
}
