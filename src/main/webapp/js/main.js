import { DPR, MAX_DT } from './core/constants.js';
import { createEventBus } from './core/events.js';
import { createInput } from './core/input.js';
import { createAudio } from './services/audio.js';
import { createStorage } from './services/storage.js';
import { createInitialState, resetState } from './state/state.js';
import { setupMap } from './state/map.js';
import { bindHotkeys } from './state/binds.js';
import { rebuildFlowField } from './systems/flowfield.js';
import { updateAI } from './systems/ai.js';
import { updateTiles } from './systems/tiles.js';
import { updateItems } from './systems/items.js';
import { updateSpawner } from './systems/spawner.js';
import { updateCombat, reload, placeWallFront } from './systems/combat.js';
import { updateFX } from './systems/fx.js';
import { isTouchDevice, createTouchControls } from './core/touch.js';
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

  // --- DPR リサイズ ---
  function fitCanvas() {
    const w = Math.floor(canvas.clientWidth * DPR);
    const h = Math.floor(canvas.clientHeight * DPR);
    if (canvas.width !== w || canvas.height !== h) {
      canvas.width = w;
      canvas.height = h;
    }
  }
  try {
    new ResizeObserver(fitCanvas).observe(canvas);
  } catch (_e) {
    addEventListener('resize', fitCanvas);
  }
  fitCanvas();

  // --- DI ---
  const bus     = createEventBus();
  const input   = createInput(window);
  const audio   = createAudio();
  const storage = createStorage('nobihaza_like_save'); // 互換用（今は未使用）

  // --- bus: SE ---
  bus.on('sfx', (name) => { try { audio.sfx(name); } catch (_e) {} });

  // --- state 初期化 & マップ ---
  const state = createInitialState();
  if (!state.stats) state.stats = { kills: 0, timeMs: 0, name: '' };
  state.runStart = performance.now();
  state.gameOver = false;

  setupMap(state);
  rebuildFlowField(state);

  // --- HUD / Overlay ---
  mountHUD(hudEl, toastEl, state, bus);
  mountGameOver(document.getElementById('overlay'), bus, state);

  // --- ホットキー（保存/読込） ---
  bindHotkeys(state, bus, input, {
    save: () => saveChooser(state, null, bus),
    load: () => loadChooser(state, null, bus),
  });

  // --- タッチ操作（スマホ/タブレット）---
  if (isTouchDevice()) {
    document.body.classList.add('touch');
    createTouchControls(document.getElementById('wrap'), input, {
      reload: () => reload(state, bus),
      build:  () => placeWallFront(state, bus),
      cycleWeapon: () => {
        const ws = state.player.weapons;
        if (!ws.length) return;
        state.player.curW = (state.player.curW + 1) % ws.length;
        bus.emit('ui:toast', '武器: ' + (ws[state.player.curW].name || ''));
      },
      pause: () => { if (!state.gameOver) state.paused = !state.paused; },
      isPaused: () => state.paused,
      setPaused: (b) => { if (!state.gameOver) state.paused = !!b; },
    });
  }

  // --- 初回のユーザー操作で AudioContext を resume（モバイル対策）---
  const resumeAudio = () => {
    try { audio.resume && audio.resume(); } catch (_e) {}
    window.removeEventListener('pointerdown', resumeAudio);
    window.removeEventListener('keydown', resumeAudio);
  };
  window.addEventListener('pointerdown', resumeAudio);
  window.addEventListener('keydown', resumeAudio);

  // --- エラーをトーストにも表示 ---
  addEventListener('error', (ev) => {
    const msg = 'Error: ' + (ev && ev.message ? ev.message : 'unknown');
    bus.emit('ui:toast', msg);
  });
  addEventListener('unhandledrejection', (ev) => {
    try { console.error('Promise rejection:', ev && ev.reason); } catch (_e) {}
    bus.emit('ui:toast', 'Async Error');
  });

  // 任意：ゲームオーバーイベント受信時にトースト
  bus.on('game:over', ({ reason, timeMs }) => {
    bus.emit('ui:toast', 'Game Over');
  });

  // リスタート：state をその場で初期化し、マップ／フローフィールドを作り直す
  bus.on('game:restart', () => {
    resetState(state);
    setupMap(state);
    rebuildFlowField(state);
    state.runStart = performance.now();
    state.gameOver = false;
    state.paused   = false;
    bus.emit('ui:toast', 'Restart');
  });

  // --- ループ ---
  let last = performance.now();
  let flowTimer = 0.0; // 団子対策の再計算タイマ

  function loop(t) {
    try {
      fitCanvas();
      const dt = Math.min(MAX_DT, (t - last) / 1000);
      last = t;

      if (!state.paused) {
        // 更新
        updateCombat(state, dt, bus, input, audio);
        updateTiles(state, dt, bus, audio);
        updateSpawner(state, dt, bus, audio);
        updateAI(state, dt, bus, audio);
        updateItems(state, dt, bus, audio);
        updateFX(state, dt, bus);

        // フローフィールド定期再計算
        flowTimer -= dt;
        if (flowTimer <= 0) {
          rebuildFlowField(state);
          flowTimer = 0.35;
        }

        // ★ HP=0 → 一度だけゲームオーバー
        if (!state.gameOver && state.player.hp <= 0) {
          state.gameOver = true;
          state.paused   = true;
          const timeMs = Math.max(0, performance.now() - state.runStart);
          state.stats.timeMs = timeMs;
          bus.emit('game:over', { reason: 'death', timeMs });
        }
      }

      // 描画
      renderFrame(ctx2d, canvas, state);
    } catch (e) {
      try { console.error('[LOOP]', e); } catch (_e) {}
      bus.emit('ui:toast', 'Runtime Error');
    }
    requestAnimationFrame(loop);
  }
  requestAnimationFrame(loop);
}
