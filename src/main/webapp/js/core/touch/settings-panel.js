// webapp/js/core/touch/settings-panel.js
// タッチ操作の設定パネル（左右入替・自動射撃・バイブ・透明度・サイズ・ズーム・配置編集）。
// 変更は即 applyVisual / api で反映し、saveSettings で localStorage に永続化する。
// 開いている間は誤操作防止のため自動ポーズする（元から停止中なら触らない）。

import { saveSettings } from '../settings.js';
import { CONFIG } from '../config.js';
import { applyVisual } from './visual.js';

export function createSettingsPanel({ layer, cfg, input, api, layoutEditor }) {
  const cam = CONFIG.camera || { minZoom: 0.6, maxZoom: 1.2 };

  const panel = document.createElement('div');
  panel.className = 'tc-settings hidden';
  panel.innerHTML =
    '<h3>操作設定</h3>' +
    '<label class="row"><span>左右入れ替え</span><input type="checkbox" data-k="swap"></label>' +
    '<label class="row"><span>自動射撃</span><input type="checkbox" data-k="autoFire"></label>' +
    '<label class="row"><span>バイブ</span><input type="checkbox" data-k="haptics"></label>' +
    '<label class="row"><span>透明度</span><input type="range" data-k="opacity" min="0.3" max="1" step="0.05"></label>' +
    '<label class="row"><span>サイズ</span><input type="range" data-k="scale" min="0.8" max="1.4" step="0.05"></label>' +
    '<label class="row"><span>視野（ズーム）</span><input type="range" data-k="zoom" min="' + cam.minZoom + '" max="' + cam.maxZoom + '" step="0.05"></label>' +
    '<label class="row"><span>ボタン配置を編集</span><input type="checkbox" data-k="edit"></label>' +
    '<button type="button" class="tc-reset-layout">配置をリセット</button>' +
    '<button type="button" class="tc-close">閉じる</button>';
  layer.appendChild(panel);

  const elSwap    = panel.querySelector('[data-k="swap"]');
  const elAuto    = panel.querySelector('[data-k="autoFire"]');
  const elHaptics = panel.querySelector('[data-k="haptics"]');
  const elOpacity = panel.querySelector('[data-k="opacity"]');
  const elScale   = panel.querySelector('[data-k="scale"]');
  const elZoom    = panel.querySelector('[data-k="zoom"]');
  const elEdit    = panel.querySelector('[data-k="edit"]');
  const elReset   = panel.querySelector('.tc-reset-layout');
  const elClose   = panel.querySelector('.tc-close');

  function applyZoom() { if (api.setZoom) api.setZoom(cfg.zoom); }

  function syncInputs() {
    elSwap.checked    = !!cfg.swap;
    elAuto.checked    = !!cfg.autoFire;
    elHaptics.checked = !!cfg.haptics;
    elOpacity.value   = String(cfg.opacity);
    elScale.value     = String(cfg.scale);
    elZoom.value      = String(cfg.zoom);
    elEdit.checked    = layer.classList.contains('tc-edit');
  }
  function onChange() { applyVisual(layer, cfg, input); saveSettings(cfg); }

  elSwap.addEventListener('change',    () => { cfg.swap     = elSwap.checked;    onChange(); });
  elAuto.addEventListener('change',    () => { cfg.autoFire = elAuto.checked;    onChange(); });
  elHaptics.addEventListener('change', () => { cfg.haptics  = elHaptics.checked; onChange(); });
  elOpacity.addEventListener('input',  () => { cfg.opacity  = parseFloat(elOpacity.value); onChange(); });
  elScale.addEventListener('input',    () => { cfg.scale    = parseFloat(elScale.value);   onChange(); });
  elZoom.addEventListener('input',     () => { cfg.zoom     = parseFloat(elZoom.value); applyZoom(); saveSettings(cfg); });
  elEdit.addEventListener('change',    () => { if (layoutEditor) layoutEditor.setEdit(elEdit.checked); });
  elReset.addEventListener('click',    () => { if (layoutEditor) layoutEditor.reset(); });

  let panelOpen = false, pausedByPanel = false;
  function toggle() {
    panelOpen = !panelOpen;
    panel.classList.toggle('hidden', !panelOpen);
    if (panelOpen) {
      syncInputs();
      // 設定中は誤操作防止のため自動ポーズ（元から停止中なら触らない）
      pausedByPanel = api.isPaused ? !api.isPaused() : false;
      if (pausedByPanel && api.setPaused) api.setPaused(true);
    } else {
      // パネルを閉じたら編集モードも解除
      if (layoutEditor) layoutEditor.setEdit(false);
      if (pausedByPanel && api.setPaused) { api.setPaused(false); pausedByPanel = false; }
    }
  }
  elClose.addEventListener('click', () => { if (panelOpen) toggle(); });

  // 起動時にズーム設定を反映
  applyZoom();
  syncInputs();
  return { toggle };
}
