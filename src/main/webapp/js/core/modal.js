// webapp/js/core/modal.js
// 自前の軽量モーダル（window.prompt の置き換え）。タッチでも扱いやすく、
// ゲームのレンダリングループをブロックしない（prompt/alert は同期ブロックするため不可）。
// CSS に依存しないようインラインスタイルで自己完結する。

function makeOverlay() {
  const ov = document.createElement('div');
  ov.style.cssText =
    'position:fixed;inset:0;z-index:1000;display:flex;align-items:center;justify-content:center;' +
    'background:rgba(0,0,0,.55);padding:16px;';
  const panel = document.createElement('div');
  panel.style.cssText =
    'background:#101622;border:1px solid #273142;border-radius:12px;padding:16px;' +
    'min-width:240px;max-width:min(420px,92vw);max-height:80vh;overflow:auto;color:#e7ecf3;' +
    'font-size:15px;box-shadow:0 10px 40px rgba(0,0,0,.5);';
  ov.appendChild(panel);
  return { ov, panel };
}

function btn(label, kind) {
  const b = document.createElement('button');
  b.type = 'button';
  b.textContent = label;
  const accent = kind === 'primary' ? '#1e2a3a' : '#171f2b';
  b.style.cssText =
    'margin-top:10px;width:100%;padding:10px;border-radius:8px;color:#e7ecf3;' +
    'border:1px solid #2b3a50;background:' + accent + ';font-size:15px;cursor:pointer;';
  return b;
}

// テキスト入力モーダル。OK で入力値、キャンセル/背景タップで null を解決。
export function promptModal(message, defaultVal) {
  return new Promise((resolve) => {
    const { ov, panel } = makeOverlay();
    const h = document.createElement('div'); h.textContent = message; h.style.marginBottom = '10px';
    const input = document.createElement('input');
    input.type = 'text';
    input.value = defaultVal == null ? '' : String(defaultVal);
    input.style.cssText = 'width:100%;padding:10px;border-radius:8px;border:1px solid #2b3a50;background:#0c121b;color:#e7ecf3;font-size:15px;box-sizing:border-box;';
    const ok = btn('OK', 'primary');
    const cancel = btn('キャンセル');
    panel.append(h, input, ok, cancel);

    function close(val) { try { document.body.removeChild(ov); } catch (_e) {} resolve(val); }
    ok.addEventListener('click', () => close(input.value));
    cancel.addEventListener('click', () => close(null));
    ov.addEventListener('click', (e) => { if (e.target === ov) close(null); });
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') close(input.value);
      else if (e.key === 'Escape') close(null);
    });

    document.body.appendChild(ov);
    try { input.focus(); input.select(); } catch (_e) {}
  });
}

// 選択モーダル。items（文字列配列）から1つ選び、その index を解決。キャンセルで -1。
export function chooseModal(message, items) {
  return new Promise((resolve) => {
    const { ov, panel } = makeOverlay();
    const h = document.createElement('div'); h.textContent = message; h.style.marginBottom = '10px';
    panel.appendChild(h);

    function close(val) { try { document.body.removeChild(ov); } catch (_e) {} resolve(val); }
    (items || []).forEach((label, i) => {
      const b = btn(label);
      b.addEventListener('click', () => close(i));
      panel.appendChild(b);
    });
    const cancel = btn('キャンセル');
    cancel.addEventListener('click', () => close(-1));
    panel.appendChild(cancel);
    ov.addEventListener('click', (e) => { if (e.target === ov) close(-1); });

    document.body.appendChild(ov);
  });
}
