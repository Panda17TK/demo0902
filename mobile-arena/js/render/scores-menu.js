// js/render/scores-menu.js
// REQ-TITLE-1: スコア（ローカルランキング）overlay。top==='scores' のとき表示。
// データは overlay.js と同じ arena_scores（kv 経由）。読み取り専用。

import { getItem } from '../services/kv.js';

const KEY_SCORES = 'arena_scores';

function readScores() {
  try { return JSON.parse(getItem(KEY_SCORES) || '[]'); } catch (_e) { return []; }
}
function fmt(ms) {
  const s = Math.floor((ms || 0) / 1000);
  return Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0');
}
function esc(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

export function mountScoresMenu(rootEl, { uiCtl }) {
  if (!rootEl) return { render() {} };

  const el = document.createElement('div');
  el.className = 'overlay scores-overlay hidden';
  el.id = 'scores-overlay';
  el.innerHTML =
    '<div class="panel scores-panel" role="dialog" aria-label="スコア">' +
    '  <h2>ランキング（上位10件）</h2>' +
    '  <div class="scores-board"></div>' +
    '  <div class="form scores-actions"><button type="button" class="scores-close" aria-label="閉じる">閉じる</button></div>' +
    '</div>';
  rootEl.appendChild(el);

  const boardEl = el.querySelector('.scores-board');
  el.querySelector('.scores-close').addEventListener('click', () => uiCtl.closeTop());

  function renderBoard() {
    const list = readScores()
      .slice()
      .sort((a, b) => (b.wave - a.wave) || (b.timeMs - a.timeMs))
      .slice(0, 10);
    boardEl.innerHTML = list.length
      ? list.map((r, i) =>
          (i + 1) + '. ' + esc(r.name || '(no name)') +
          ' — WAVE ' + (r.wave | 0) + ' / ' + fmt(r.timeMs)).join('<br>')
      : '<i>まだ記録がありません</i>';
  }

  function render(ui) {
    const top = ui.overlayStack[ui.overlayStack.length - 1] || null;
    const show = top === 'scores';
    el.classList.toggle('hidden', !show);
    if (show) renderBoard();
  }

  return { render };
}
