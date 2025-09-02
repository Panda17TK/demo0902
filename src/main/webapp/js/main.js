import { DPR, MAX_DT } from './core/constants.js';
import { createEventBus } from './core/events.js';
import { createInput } from './core/input.js';
import { createAudio } from './services/audio.js';
import { createStorage } from './services/storage.js';
import { createInitialState } from './state/state.js';
import { setupMap } from './state/map.js';
import { bindHotkeys } from './state/binds.js';
import { rebuildFlowField } from './systems/flowfield.js';
import { updateAI } from './systems/ai.js';
import { updateTiles } from './systems/tiles.js';
import { updateItems } from './systems/items.js';
import { updateSpawner } from './systems/spawner.js';
import { updateCombat } from './systems/combat.js';
import { updateFX } from './systems/fx.js';
import { renderFrame } from './render/renderer.js';
import { mountGameOver } from './render/overlay.js';
import { mountHUD } from './render/hud.js';
import { saveChooser, loadChooser } from './systems/save-remote.js';

const canvas  = document.getElementById('game');
const hudEl   = document.getElementById('hud');
const toastEl = document.getElementById('toast');

if (!canvas) {
  console.error('[INIT] #game canvas が見つかりません');
} else {
  const ctx2d = canvas.getContext('2d');
  if (!ctx2d) console.error('[INIT] 2Dコンテキストの取得に失敗');

  // DPR リサイズ
  function fitCanvas() {
    const w = Math.floor(canvas.clientWidth  * DPR);
    const h = Math.floor(canvas.clientHeight * DPR);
    if (canvas.width !== w || canvas.height !== h) {
      canvas.width  = w;
      canvas.height = h;
    }
  }
  try {
    new ResizeObserver(fitCanvas).observe(canvas);
  } catch (_e) {
    addEventListener('resize', fitCanvas);
  }
  fitCanvas();

  // DI
  const bus     = createEventBus();
  const input   = createInput(window);
  const audio   = createAudio();
  const storage = createStorage('nobihaza_like_save'); // 今は未使用（互換残し）

  // bus: SE
  bus.on('sfx', name => { try { audio.sfx(name); } catch(_e){} });

  // state 初期化 & マップ
  const state = createInitialState();
  // ラン開始時間（戦績用）
  state.runStart = performance.now();
  state.gameOver = false;

  setupMap(state);
  rebuildFlowField(state);

  // HUD
  mountHUD(hudEl, toastEl, state, bus);
  mountGameOver(document.getElementById('overlay'), bus, state);

  // ホットキー
  bindHotkeys(state, bus, input, {
    save: () => saveChooser(state, null, bus),
    load: () => loadChooser(state, null, bus),
  });

  // エラーをトースト
  addEventListener('error', ev => {
    const msg = 'Error: ' + (ev && ev.message ? ev.message : 'unknown');
    bus.emit('ui:toast', msg);
  });
  addEventListener('unhandledrejection', ev => {
    try { console.error('Promise rejection:', ev && ev.reason); } catch(_e){}
    bus.emit('ui:toast', 'Async Error');
  });

  // ゲームオーバー発火 → オーバーレイ/送信は別UIが受ける想定
  bus.on('game:over', ({ reason, timeMs }) => {
    bus.emit('ui:toast', 'Game Over');
    // ここで overlay を開く実装があるなら呼ぶ（例）
    // bus.emit('overlay:gameover', { timeMs });
  });

  // ループ
  let last = performance.now();
  let flowTimer = 0.0;

  function loop(t) {
    try {
      fitCanvas();
      const dt = Math.min(MAX_DT, (t - last) / 1000);
      last = t;

      if (!state.paused) {
        updateCombat(state, dt, bus, input, audio);
        updateTiles(state, dt, bus, audio);
        updateSpawner(state, dt, bus, audio);
        updateAI(state, dt, bus, audio);
        updateItems(state, dt, bus, audio);
        updateFX(state, dt, bus);

        // 団子対策：定期再計算
        flowTimer -= dt;
        if (flowTimer <= 0) {
          rebuildFlowField(state);
          flowTimer = 0.35;
        }
      }

      renderFrame(ctx2d, canvas, state);
    } catch (e) {
      try { console.error('[LOOP]', e); } catch (_e) {}
      bus.emit('ui:toast', 'Runtime Error');
    }
    requestAnimationFrame(loop);
  }
  requestAnimationFrame(loop);
}
