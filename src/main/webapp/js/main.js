// webapp/js/main.js
import { DPR, MAX_DT } from './core/constants.js';
import { createEventBus } from './core/events.js';
import { createInput } from './core/input.js';
import { createAudio } from './services/audio.js';
import { createStorage } from './services/storage.js';
import { createInitialState } from './state/state.js';
import { setupMap } from './state/map.js';
import { bindHotkeys } from './state/binds.js';
import { rebuildFlowField } from './systems/flowfield.js'; // ファイル名は実体に合わせて
import { updateAI } from './systems/ai.js';
import { updateTiles } from './systems/tiles.js';
import { updateItems } from './systems/items.js';
import { updateSpawner } from './systems/spawner.js';
import { updateCombat } from './systems/combat.js';
import { updateFX } from './systems/fx.js';
import { renderFrame } from './render/renderer.js';
import { mountHUD } from './render/hud.js';
import { save, load } from './systems/save-inline.js';

const canvas  = document.getElementById('game');
const hudEl   = document.getElementById('hud');
const toastEl = document.getElementById('toast');

if (!canvas) {
  console.error('[INIT] #game canvas が見つかりません');
} else {
  const ctx2d = canvas.getContext('2d');
  if (!ctx2d) {
    console.error('[INIT] 2Dコンテキストの取得に失敗');
  }

  // --- DPRリサイズ ---
  function fitCanvas() {
    const w = Math.floor(canvas.clientWidth  * DPR);
    const h = Math.floor(canvas.clientHeight * DPR);
    if (canvas.width !== w || canvas.height !== h) {
      canvas.width = w;
      canvas.height = h;
    }
  }
  try {
    new ResizeObserver(fitCanvas).observe(canvas);
  } catch (e) {
    // 古い環境用フォールバック
    addEventListener('resize', fitCanvas);
  }
  fitCanvas();

  // --- DI ---
  const bus     = createEventBus();
  const input   = createInput(window);
  const audio   = createAudio();
  const storage = createStorage('nobihaza_like_save');

  // --- bus: SE ---
  bus.on('sfx', function(name){
    try { audio.sfx(name); } catch (e) {}
  });

  // --- state 初期化 & マップ構築 ---
  const state = createInitialState();
  setupMap(state);
  rebuildFlowField(state);   // 初回のフローフィールド

  // --- HUD ---
  mountHUD(hudEl, toastEl, state, bus);

  // --- ホットキー（保存/読込など） ---
  bindHotkeys(state, bus, input, {
    save: function(){ save(state, storage, bus); },
    load: function(){ load(state, storage, bus); }
  });

  // --- エラーをトーストにも表示---
  addEventListener('error', function(ev){
    var msg = 'Error: ' + (ev && ev.message ? ev.message : 'unknown');
    bus.emit('ui:toast', msg);
  });
  addEventListener('unhandledrejection', function(ev){
    try { console.error('Promise rejection:', ev && ev.reason); } catch (e) {}
    bus.emit('ui:toast', 'Async Error');
  });

  // --- ループ ---
  let last = performance.now();
  let flowTimer = 0.0; // 定期的にフローフィールド再計算

  function loop(t){
    try {
      fitCanvas();
      const dt = Math.min(MAX_DT, (t - last) / 1000);
      last = t;

      if (!state.paused) {
        // 入力/戦闘
        updateCombat(state, dt, bus, input, audio);
        // マップ系
        updateTiles(state, dt, bus, audio);
        updateSpawner(state, dt, bus, audio);
        // AI
        updateAI(state, dt, bus, audio);
        // アイテム
        updateItems(state, dt, bus, audio);
        // エフェクト
        updateFX(state, dt, bus);

        // フローフィールド定期再計算（団子対策）
        flowTimer -= dt;
        if (flowTimer <= 0) {
          rebuildFlowField(state);
          flowTimer = 0.35;
        }
      }

      // 描画
      renderFrame(ctx2d, canvas, state);
    } catch (e) {
      try { console.error('[LOOP]', e); } catch (_) {}
      bus.emit('ui:toast', 'Runtime Error');
    }
    requestAnimationFrame(loop);
  }
  requestAnimationFrame(loop);
}
