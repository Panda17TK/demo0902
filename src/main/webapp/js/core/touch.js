// webapp/js/core/touch.js
// スマホ／タブレット向けの画面上タッチ操作（ツインスティック）。
// 既存の input（keys / aim / move / autoFire）に書き込むだけなので、ゲームロジックは変更不要。
//   - 左スティック: 移動（アナログ：倒し具合で速度可変）
//   - 右スティック: 照準＋射撃（ドラッグ方向に向き、押下中は K=発射）
//   - ボタン: 近接(J) / ダッシュ(Shift) / リロード / 武器切替 / 壁設置 / ポーズ / 設定
// 設定: 左右入替・透明度・サイズ・自動射撃（localStorage 永続化）

import { loadSettings, saveSettings } from './settings.js';

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
  const move = input.move;

  const cfg = loadSettings();

  const layer = document.createElement('div');
  layer.className = 'touch-controls';

  const BASE_R = 60; // 基準可動半径(px)。サイズ設定で拡縮
  const getR = () => BASE_R * cfg.scale;

  // ===== スティック（動的ベース）=====
  function makeZone(kind) {
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

  makeButton('melee',  '近接', { hold: (on) => { keys['j'] = on; } });
  makeButton('dash',   'DASH', { hold: (on) => { keys['shift'] = on; } });
  makeButton('reload', 'R',    { tap: () => { if (api.reload) api.reload(); } });
  makeButton('weapon', '武器', { tap: () => { if (api.cycleWeapon) api.cycleWeapon(); } });
  makeButton('build',  '壁',   { tap: () => { if (api.build) api.build(); } });
  makeButton('pause',  'II',   { tap: () => { if (api.pause) api.pause(); } });
  makeButton('settings', '⚙', { tap: () => togglePanel() });

  // ===== 設定パネル =====
  const panel = document.createElement('div');
  panel.className = 'tc-settings hidden';
  panel.innerHTML =
    '<h3>操作設定</h3>' +
    '<label class="row"><span>左右入れ替え</span><input type="checkbox" data-k="swap"></label>' +
    '<label class="row"><span>自動射撃</span><input type="checkbox" data-k="autoFire"></label>' +
    '<label class="row"><span>透明度</span><input type="range" data-k="opacity" min="0.3" max="1" step="0.05"></label>' +
    '<label class="row"><span>サイズ</span><input type="range" data-k="scale" min="0.8" max="1.4" step="0.05"></label>' +
    '<button type="button" class="tc-close">閉じる</button>';
  layer.appendChild(panel);

  const elSwap     = panel.querySelector('[data-k="swap"]');
  const elAuto     = panel.querySelector('[data-k="autoFire"]');
  const elOpacity  = panel.querySelector('[data-k="opacity"]');
  const elScale    = panel.querySelector('[data-k="scale"]');
  const elClose    = panel.querySelector('.tc-close');

  function apply() {
    layer.classList.toggle('tc-swap', !!cfg.swap);
    layer.style.setProperty('--tc-op', String(cfg.opacity));
    layer.style.setProperty('--tc-scale', String(cfg.scale));
    input.autoFire = !!cfg.autoFire;
  }
  function syncInputs() {
    elSwap.checked    = !!cfg.swap;
    elAuto.checked    = !!cfg.autoFire;
    elOpacity.value   = String(cfg.opacity);
    elScale.value     = String(cfg.scale);
  }
  function onChange() { apply(); saveSettings(cfg); }

  elSwap.addEventListener('change', () => { cfg.swap = elSwap.checked; onChange(); });
  elAuto.addEventListener('change', () => { cfg.autoFire = elAuto.checked; onChange(); });
  elOpacity.addEventListener('input', () => { cfg.opacity = parseFloat(elOpacity.value); onChange(); });
  elScale.addEventListener('input',   () => { cfg.scale   = parseFloat(elScale.value);   onChange(); });

  let panelOpen = false, pausedByPanel = false;
  function togglePanel() {
    panelOpen = !panelOpen;
    panel.classList.toggle('hidden', !panelOpen);
    if (panelOpen) {
      syncInputs();
      // 設定中は誤操作防止のため自動ポーズ（元から停止中なら触らない）
      pausedByPanel = api.isPaused ? !api.isPaused() : false;
      if (pausedByPanel && api.setPaused) api.setPaused(true);
    } else if (pausedByPanel && api.setPaused) {
      api.setPaused(false); pausedByPanel = false;
    }
  }
  elClose.addEventListener('click', () => { if (panelOpen) togglePanel(); });

  apply();
  syncInputs();

  root.appendChild(layer);
  return layer;
}
