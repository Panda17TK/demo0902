// webapp/js/core/touch.js
// スマホ／タブレット向けの画面上タッチ操作（ツインスティック）。
// 既存の input（keys / aim）に書き込むだけなので、ゲームロジックは変更不要。
//   - 左スティック: 移動（WASD 相当の8方向）
//   - 右スティック: 照準＋射撃（ドラッグ方向に向き、押している間 K=発射）
//   - ボタン: 近接(J) / ダッシュ(Shift) / リロード / 武器切替 / 壁設置 / ポーズ

export function isTouchDevice() {
  try {
    return (typeof matchMedia === 'function' && matchMedia('(pointer: coarse)').matches)
      || ('ontouchstart' in window)
      || (navigator && navigator.maxTouchPoints > 0);
  } catch (_e) { return false; }
}

export function createTouchControls(root, input, api) {
  if (!root) return null;
  const keys = input.keys;
  const aim  = input.aim;

  const layer = document.createElement('div');
  layer.className = 'touch-controls';

  // ===== スティック（動的ベース）=====
  function makeZone(kind) {
    const z = document.createElement('div');
    z.className = 'tc-zone tc-zone-' + kind;
    const base = document.createElement('div'); base.className = 'tc-base';
    const knob = document.createElement('div'); knob.className = 'tc-knob';
    base.appendChild(knob);
    z.appendChild(base);

    const R = 60; // 可動半径(px)
    let pid = null, cx = 0, cy = 0;

    function release() {
      pid = null;
      base.style.opacity = '0';
      knob.style.transform = 'translate(-50%, -50%)';
      if (kind === 'move') {
        keys['w'] = keys['a'] = keys['s'] = keys['d'] = false;
      } else {
        aim.active = false; aim.x = 0; aim.y = 0; keys['k'] = false;
      }
    }

    function update(e) {
      const r = z.getBoundingClientRect();
      const dx = (e.clientX - r.left) - cx;
      const dy = (e.clientY - r.top) - cy;
      const len = Math.hypot(dx, dy);
      const cl = Math.min(len, R);
      const nx = len ? dx / len : 0;
      const ny = len ? dy / len : 0;
      knob.style.transform = `translate(calc(-50% + ${nx * cl}px), calc(-50% + ${ny * cl}px))`;
      const ux = nx * (cl / R), uy = ny * (cl / R); // -1..1

      if (kind === 'move') {
        const dz = 0.3;
        keys['w'] = uy < -dz; keys['s'] = uy > dz;
        keys['a'] = ux < -dz; keys['d'] = ux > dz;
      } else {
        const dz = 0.2;
        if (Math.hypot(ux, uy) > dz) { aim.active = true; aim.x = nx; aim.y = ny; keys['k'] = true; }
        else { aim.active = false; keys['k'] = false; }
      }
    }

    z.addEventListener('pointerdown', (e) => {
      if (pid !== null) return;
      pid = e.pointerId;
      try { z.setPointerCapture(pid); } catch (_e) {}
      const r = z.getBoundingClientRect();
      cx = e.clientX - r.left; cy = e.clientY - r.top;
      base.style.left = cx + 'px'; base.style.top = cy + 'px'; base.style.opacity = '1';
      update(e);
      e.preventDefault();
    });
    z.addEventListener('pointermove', (e) => { if (e.pointerId === pid) { update(e); e.preventDefault(); } });
    z.addEventListener('pointerup',     (e) => { if (e.pointerId === pid) release(); });
    z.addEventListener('pointercancel', (e) => { if (e.pointerId === pid) release(); });

    layer.appendChild(z);
    return z;
  }

  makeZone('move');
  makeZone('aim');

  // ===== ボタン =====
  function makeButton(cls, label, opts) {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = 'tc-btn tc-btn-' + cls;
    b.textContent = label;
    if (opts.hold) {
      let pid = null;
      b.addEventListener('pointerdown', (e) => {
        pid = e.pointerId; try { b.setPointerCapture(pid); } catch (_e) {}
        opts.hold(true); b.classList.add('active'); e.preventDefault();
      });
      const up = (e) => { if (e.pointerId === pid) { pid = null; opts.hold(false); b.classList.remove('active'); } };
      b.addEventListener('pointerup', up);
      b.addEventListener('pointercancel', up);
    } else {
      b.addEventListener('pointerdown', (e) => { opts.tap(); b.classList.add('active'); e.preventDefault(); });
      const up = () => b.classList.remove('active');
      b.addEventListener('pointerup', up);
      b.addEventListener('pointercancel', up);
    }
    layer.appendChild(b);
    return b;
  }

  makeButton('melee',  '近接',     { hold: (on) => { keys['j'] = on; } });
  makeButton('dash',   'DASH',     { hold: (on) => { keys['shift'] = on; } });
  makeButton('reload', 'R',        { tap: () => { if (api.reload) api.reload(); } });
  makeButton('weapon', '武器',     { tap: () => { if (api.cycleWeapon) api.cycleWeapon(); } });
  makeButton('build',  '壁',       { tap: () => { if (api.build) api.build(); } });
  makeButton('pause',  'II',       { tap: () => { if (api.pause) api.pause(); } });

  root.appendChild(layer);
  return layer;
}
