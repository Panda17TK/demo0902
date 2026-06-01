var CTX = (function(){
  try{
    if (typeof window !== 'undefined' && typeof window.CTX === 'string') return window.CTX;
    var p = (typeof location !== 'undefined' ? location.pathname : '');
    var m = p.match(/^\/[^\/]+/);
    return m ? m[0] : '';
  }catch(_){ return ''; }
})();

export function mountGameOver(overlayEl, bus, state) {
  if (!overlayEl || !bus || !state) return;

  var nameInput  = overlayEl.querySelector('#player-name');
  var saveBtn    = overlayEl.querySelector('#btn-save-score');
  var restartBtn = overlayEl.querySelector('#btn-restart');
  var linesEl    = overlayEl.querySelector('#result-lines');
  var boardEl    = overlayEl.querySelector('#score-board');

  function ensureStats() {
    if (!state.stats) state.stats = {};
    if (typeof state.stats.kills  !== 'number') state.stats.kills  = 0;
    if (typeof state.stats.timeMs !== 'number') state.stats.timeMs = 0;
    if (typeof state.stats.name   !== 'string') state.stats.name   = '';
  }

  function fmt(ms) {
    var s = Math.floor(ms / 1000);
    var m = Math.floor(s / 60);
    var r = s % 60;
    return m + ':' + String(r).padStart(2, '0');
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function renderBoard(list) {
    if (!boardEl) return;
    var head = '<div><b>ランキング（上位10件）</b></div>';
    var body = (list || []).map(function (r, i) {
      var nm = escapeHtml((r && r.name) ? r.name : '(no name)');
      var tm = (r && typeof r.timeMs === 'number') ? fmt(r.timeMs) : '0:00';
      var dt = (r && r.createdAt) ? escapeHtml(new Date(r.createdAt).toLocaleString()) : '';
      return (i + 1) + '. ' + nm + ' - ' + tm + ' / ' + dt;
    }).join('<br>');
    boardEl.innerHTML = head + body;
  }

  function fetchBoard() {
    if (!CTX) { // 未設定なら空で表示
      renderBoard([]);
      return Promise.resolve();
    }
    return fetch(CTX + '/api/score/list?limit=10', { method: 'GET' })
      .then(function (res) {
        if (!res.ok) throw new Error('bad status');
        return res.json();
      })
      .then(function (json) {
        var arr = (json && json.ok && json.scores) ? json.scores : [];
        renderBoard(arr);
      })
      .catch(function (e) {
        if (boardEl) boardEl.textContent = 'ランキング取得に失敗しました';
        try { console.error(e); } catch (_) {}
      });
  }

  // --- ゲームオーバーで開く ---
  bus.on('game:over', function (payload) {
    ensureStats();
    var timeMs = (payload && typeof payload.timeMs === 'number') ? payload.timeMs : 0;
    state.stats.timeMs = timeMs;

    overlayEl.classList.remove('hidden');

    var kills = state.stats.kills | 0;
    var wave = state.stats.wave | 0;
    if (linesEl) {
      linesEl.innerHTML = '到達WAVE: <b>' + wave + '</b>　生存時間: <b>' + fmt(timeMs) + '</b>　倒した数: <b>' + kills + '</b>';
    }
    if (nameInput) nameInput.value = state.stats.name || '';

    fetchBoard();
  });

  // --- Save ボタン ---
  if (saveBtn) {
    saveBtn.addEventListener('click', function () {
      ensureStats();
      var name = (nameInput && nameInput.value ? nameInput.value.trim() : '') || 'Player';
      state.stats.name = name;
      var t = state.stats.timeMs | 0;

      if (!CTX) {
        alert('サーバのコンテキストが未設定です');
        return;
      }

      fetch(CTX + '/api/score', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=UTF-8' },
        body: JSON.stringify({ name: name, timeMs: t })
      })
        .then(function (res) {
          if (!res.ok) throw new Error('bad status');
          return res.json();
        })
        .then(function (json) {
          if (!json || !json.ok) throw new Error('ng');
          return fetchBoard();
        })
        .then(function () {
          alert('戦績を保存しました');
        })
        .catch(function (e) {
          try { console.error(e); } catch (_) {}
          alert('戦績の保存に失敗しました');
        });
    });
  }

  // --- Restart ボタン ---
  if (restartBtn) {
    restartBtn.addEventListener('click', function () {
      overlayEl.classList.add('hidden');
      bus.emit('game:restart');
    });
  }
}