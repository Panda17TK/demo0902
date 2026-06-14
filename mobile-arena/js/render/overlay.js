// js/render/overlay.js（静的PWA / ネイティブ共通）
// ゲームオーバー画面＋ローカルランキング（kv 経由：Web=localStorage / ネイティブ=Preferencesミラー）。

import { getItem, setItem } from '../services/kv.js';

const KEY_SCORES = 'arena_scores';

function readScores() {
  try { return JSON.parse(getItem(KEY_SCORES) || '[]'); } catch (_e) { return []; }
}
function writeScores(list) {
  try { setItem(KEY_SCORES, JSON.stringify(list)); } catch (_e) {}
}

export function mountGameOver(overlayEl, bus, state) {
  if (!overlayEl || !bus || !state) return;

  const nameInput  = overlayEl.querySelector('#player-name');
  const saveBtn    = overlayEl.querySelector('#btn-save-score');
  const restartBtn = overlayEl.querySelector('#btn-restart');
  const linesEl    = overlayEl.querySelector('#result-lines');
  const boardEl    = overlayEl.querySelector('#score-board');

  function ensureStats() {
    if (!state.stats) state.stats = {};
    if (typeof state.stats.kills  !== 'number') state.stats.kills  = 0;
    if (typeof state.stats.timeMs !== 'number') state.stats.timeMs = 0;
    if (typeof state.stats.wave   !== 'number') state.stats.wave   = 1;
    if (typeof state.stats.name   !== 'string') state.stats.name   = '';
  }

  function fmt(ms) {
    const s = Math.floor(ms / 1000);
    return Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0');
  }
  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  // 到達WAVE優先→生存時間でソート、上位10件
  function topScores() {
    return readScores()
      .slice()
      .sort((a, b) => (b.wave - a.wave) || (b.timeMs - a.timeMs))
      .slice(0, 10);
  }

  function renderBoard() {
    if (!boardEl) return;
    const list = topScores();
    const head = '<div><b>ランキング（上位10件）</b></div>';
    const body = list.length
      ? list.map((r, i) => {
          const nm = escapeHtml(r.name || '(no name)');
          const wv = (r.wave | 0);
          const tm = (typeof r.timeMs === 'number') ? fmt(r.timeMs) : '0:00';
          return (i + 1) + '. ' + nm + ' — WAVE ' + wv + ' / ' + tm;
        }).join('<br>')
      : '<i>まだ記録がありません</i>';
    boardEl.innerHTML = head + body;
  }

  bus.on('game:over', (payload) => {
    ensureStats();
    const timeMs = (payload && typeof payload.timeMs === 'number') ? payload.timeMs : 0;
    state.stats.timeMs = timeMs;
    overlayEl.classList.remove('hidden');
    const kills = state.stats.kills | 0;
    const wave = state.stats.wave | 0;
    if (linesEl) linesEl.innerHTML = '到達WAVE: <b>' + wave + '</b>　生存時間: <b>' + fmt(timeMs) + '</b>　倒した数: <b>' + kills + '</b>';
    if (nameInput) nameInput.value = state.stats.name || '';
    renderBoard();
  });

  if (saveBtn) {
    saveBtn.addEventListener('click', () => {
      ensureStats();
      const name = ((nameInput && nameInput.value ? nameInput.value.trim() : '') || 'Player').slice(0, 24);
      state.stats.name = name;
      const list = readScores();
      list.push({ name, wave: state.stats.wave | 0, timeMs: state.stats.timeMs | 0, kills: state.stats.kills | 0, createdAt: Date.now() });
      // 肥大化防止：上位200件のみ保持
      writeScores(list.sort((a, b) => (b.wave - a.wave) || (b.timeMs - a.timeMs)).slice(0, 200));
      renderBoard();
      saveBtn.textContent = '保存しました ✓';
      setTimeout(() => { saveBtn.textContent = '戦績を保存'; }, 1500);
    });
  }

  if (restartBtn) {
    restartBtn.addEventListener('click', () => {
      overlayEl.classList.add('hidden');
      bus.emit('game:restart');
    });
  }
}
