// webapp/js/core/touch/buttons.js
// アクションボタン（ホールド／タップ）。押下時に触覚フィードバックを発火する。
//   opts.hold(on): 押下中 true / 離して false（近接・ダッシュ）
//   opts.tap()   : 単発（リロード・武器切替・壁・ポーズ・設定）

export function createButton(layer, cls, label, opts, haptic) {
  const b = document.createElement('button');
  b.type = 'button';
  b.className = 'tc-btn tc-btn-' + cls;
  b.textContent = label;

  if (opts.hold) {
    let pid = null;
    b.addEventListener('pointerdown', (e) => {
      pid = e.pointerId; try { b.setPointerCapture(pid); } catch (_e) {}
      if (haptic) haptic(12);
      opts.hold(true); b.classList.add('active'); e.preventDefault();
    });
    const up = (e) => { if (e.pointerId === pid) { pid = null; opts.hold(false); b.classList.remove('active'); } };
    b.addEventListener('pointerup', up);
    b.addEventListener('pointercancel', up);
  } else {
    b.addEventListener('pointerdown', (e) => {
      if (haptic) haptic(10);
      opts.tap(); b.classList.add('active'); e.preventDefault();
    });
    const up = () => b.classList.remove('active');
    b.addEventListener('pointerup', up);
    b.addEventListener('pointercancel', up);
  }

  layer.appendChild(b);
  return b;
}
