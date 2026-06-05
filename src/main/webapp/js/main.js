import { DPR, MAX_DT, FIXED_DT } from './core/constants.js';
import { loadConfig, CONFIG } from './core/config.js';
import { createEventBus } from './core/events.js';
import { createInput } from './core/input.js';
import { mountDevEditor } from './render/dev-editor.js';
import { createAudio } from './services/audio.js';
import { createStorage } from './services/storage.js';
import { createInitialState, resetState } from './state/state.js';
import { setupMap } from './state/map.js';
import { bindHotkeys } from './state/binds.js';
import { rebuildFlowField } from './systems/flowfield.js';
import { buildMobGrid } from './systems/spatial.js';
import { updateAI } from './systems/ai.js';
import { updateTiles } from './systems/tiles.js';
import { updateItems } from './systems/items.js';
import { updateSpawner, startWave } from './systems/spawner.js';
import { updateCombat, reload, placeWallFront } from './systems/combat.js';
import { updateFX } from './systems/fx.js';
import { isTouchDevice, createTouchControls } from './core/touch.js';
import { renderFrame } from './render/renderer.js';
import { mountGameOver } from './render/overlay.js';
import { mountHUD } from './render/hud.js';
import { mountUpgrades } from './render/upgrades.js';
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

  // --- 設定（開発者モード）読み込み：他システムが参照する前に ---
  loadConfig();

  // CONFIG.player の値をプレイヤー初期値へ反映
  function applyPlayerConfig(st) {
    const pc = CONFIG.player;
    st.player.baseSpeed = pc.baseSpeed;
    st.player.hpMax = pc.hpMax;
    st.player.hp = Math.min(st.player.hp, pc.hpMax);
    st.player.staMax = pc.staMax;
    st.player.sta = Math.min(st.player.sta, pc.staMax);
  }

  // --- state 初期化 & マップ ---
  const state = createInitialState();
  if (!state.stats) state.stats = { kills: 0, timeMs: 0, name: '' };
  applyPlayerConfig(state);
  state.runStart = performance.now();
  state.gameOver = false;

  setupMap(state);
  rebuildFlowField(state);
  startWave(state, 1);

  // --- HUD / Overlay ---
  mountHUD(hudEl, toastEl, state, bus);
  mountGameOver(document.getElementById('overlay'), bus, state);
  mountUpgrades(document.getElementById('upgrade-overlay'), bus, state);
  mountDevEditor(document.getElementById('dev-overlay'), bus, state);
  const devBtn = document.getElementById('dev-btn');
  if (devBtn) devBtn.addEventListener('click', () => bus.emit('dev:toggle'));

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
    applyPlayerConfig(state);
    setupMap(state);
    rebuildFlowField(state);
    startWave(state, 1);
    state.runStart = performance.now();
    state.gameOver = false;
    state.paused   = false;
    bus.emit('ui:toast', 'Restart');
  });

  // --- ループ ---
  let last = performance.now();
  let flowTimer = 0.0; // 団子対策の再計算タイマ
  let accumulator = 0; // 固定タイムステップの蓄積
  const MAX_STEPS = 5; // 1フレームでの最大ステップ数（スパイラル防止）

  // 固定ステップ1回ぶんのシミュレーション更新
  function stepSimulation(h) {
    // 補間用に「ステップ前」の位置を記録（描画は prev→cur を alpha で補間）
    const p = state.player;
    p.px0 = p.x; p.py0 = p.y;
    for (let i = 0; i < state.mobs.length; i++) {
      const m = state.mobs[i]; m.px0 = m.x; m.py0 = m.y;
    }
    // 近傍探索グリッドをステップ先頭で構築し、全システムで共有
    state.mobGrid = buildMobGrid(state);
    updateCombat(state, h, bus, input, audio);
    updateTiles(state, h, bus, audio);
    updateSpawner(state, h, bus, audio);
    updateAI(state, h, bus, audio);
    updateItems(state, h, bus, audio);
    updateFX(state, h, bus);

    // フローフィールド定期再計算
    flowTimer -= h;
    if (flowTimer <= 0) { rebuildFlowField(state); flowTimer = 0.35; }

    // HP=0 → 一度だけゲームオーバー
    if (!state.gameOver && state.player.hp <= 0) {
      state.gameOver = true;
      state.paused = true;
      const timeMs = Math.max(0, performance.now() - state.runStart);
      state.stats.timeMs = timeMs;
      bus.emit('game:over', { reason: 'death', timeMs });
    }
  }

  function loop(t) {
    try {
      fitCanvas();
      const dt = Math.min(MAX_DT, (t - last) / 1000);
      last = t;

      // 時間スケール（演出）：ヒットストップとスローモーションを掛け合わせる。
      // タイマ自体は実時間で減衰させ、演出が間延びしないようにする。
      let timeScale = 1;
      if (state.hitstop > 0) { state.hitstop -= dt; timeScale *= 0.06; }
      if (state.slowmo && state.slowmo.t > 0) {
        state.slowmo.t -= dt;
        timeScale *= state.slowmo.factor;
        if (state.slowmo.t <= 0) state.slowmo.t = 0;
      }
      // キルカム演出タイマ（描画用・実時間）
      if (state.killCam) { state.killCam.t += dt; if (state.killCam.t >= state.killCam.life) state.killCam = null; }

      if (!state.paused) {
        // ===== 固定タイムステップ蓄積（決定論・トンネリング抑止）=====
        accumulator += dt * timeScale;
        let steps = 0;
        while (accumulator >= FIXED_DT && steps < MAX_STEPS) {
          stepSimulation(FIXED_DT);
          accumulator -= FIXED_DT;
          steps++;
          if (state.gameOver) { accumulator = 0; break; }
        }
        // 過負荷時（steps 上限到達）は余剰を捨ててスパイラルを防ぐ
        if (steps >= MAX_STEPS) accumulator = 0;
      }
      // 補間係数（0..1）：直近ステップからの経過割合。描画位置を滑らかにする。
      state.alpha = state.paused ? 1 : Math.max(0, Math.min(1, accumulator / FIXED_DT));

      // カメラ（スムーズ追従＋向きの先読み）と画面シェイクの更新（実時間）
      const look = 36;
      let tgX = state.player.x + state.player.facing.x * look;
      let tgY = state.player.y + state.player.facing.y * look;
      // ボス撃破中はカメラを撃破地点へ寄せる（シネマティック）
      if (state.killCam) {
        const ph = 1 - state.killCam.t / state.killCam.life; // 1→0
        const w = Math.min(0.6, ph * 0.6);
        tgX = tgX * (1 - w) + state.killCam.x * w;
        tgY = tgY * (1 - w) + state.killCam.y * w;
      }
      if (!state.cam) state.cam = { x: tgX, y: tgY };
      const k = 1 - Math.pow(0.0001, dt);
      state.cam.x += (tgX - state.cam.x) * k;
      state.cam.y += (tgY - state.cam.y) * k;
      if (state.shake && state.shake.t > 0) {
        state.shake.t -= dt;
        if (state.shake.t <= 0) { state.shake.t = 0; state.shake.mag = 0; }
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
