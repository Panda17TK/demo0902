// webapp/js/core/touch/stick.js
// 画面半分を占める動的ベースのバーチャルスティック。
//   - kind='move': 移動（アナログ：倒し具合 0..1 を速度スケールに）
//   - kind='aim' : 照準＋射撃（ドラッグ方向に向き、押下中は keys['k']=発射）
// 既存の input（keys / aim / move）へ書き込むだけでゲームロジックには触れない。

const BASE_R = 60; // 基準可動半径(px)。サイズ設定(cfg.scale)で拡縮する。

export function createStick(kind, { layer, input, cfg }) {
  const keys = input.keys;
  const aim  = input.aim;
  const move = input.move;
  const getR = () => BASE_R * cfg.scale;

  const z = document.createElement('div');
  z.className = 'tc-zone tc-zone-' + kind;
  const base = document.createElement('div'); base.className = 'tc-base';
  const knob = document.createElement('div'); knob.className = 'tc-knob';
  base.appendChild(knob);
  z.appendChild(base);

  let pid = null, cx = 0, cy = 0;

  function release() {
    pid = null;
    base.classList.remove('show');
    knob.style.transform = 'translate(-50%, -50%)';
    if (kind === 'move') { move.active = false; move.x = 0; move.y = 0; }
    else { aim.active = false; aim.x = 0; aim.y = 0; keys['k'] = false; }
  }

  function update(e) {
    const R = getR();
    const r = z.getBoundingClientRect();
    const dx = (e.clientX - r.left) - cx;
    const dy = (e.clientY - r.top) - cy;
    const len = Math.hypot(dx, dy);
    const cl = Math.min(len, R);
    const nx = len ? dx / len : 0;
    const ny = len ? dy / len : 0;
    knob.style.transform = `translate(calc(-50% + ${nx * cl}px), calc(-50% + ${ny * cl}px))`;
    const mag = cl / R;                 // 0..1（倒し具合）
    const ux = nx * mag, uy = ny * mag; // 成分 -1..1

    if (kind === 'move') {
      if (mag > 0.12) { move.active = true; move.x = ux; move.y = uy; }
      else { move.active = false; move.x = 0; move.y = 0; }
    } else {
      if (mag > 0.2) { aim.active = true; aim.x = nx; aim.y = ny; keys['k'] = true; }
      else { aim.active = false; keys['k'] = false; }
    }
  }

  z.addEventListener('pointerdown', (e) => {
    if (pid !== null) return;
    pid = e.pointerId;
    try { z.setPointerCapture(pid); } catch (_e) {}
    const r = z.getBoundingClientRect();
    cx = e.clientX - r.left; cy = e.clientY - r.top;
    base.style.left = cx + 'px'; base.style.top = cy + 'px'; base.classList.add('show');
    update(e);
    e.preventDefault();
  });
  z.addEventListener('pointermove', (e) => { if (e.pointerId === pid) { update(e); e.preventDefault(); } });
  z.addEventListener('pointerup',     (e) => { if (e.pointerId === pid) release(); });
  z.addEventListener('pointercancel', (e) => { if (e.pointerId === pid) release(); });

  layer.appendChild(z);
  return z;
}
