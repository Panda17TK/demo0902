// webapp/js/core/touch/layout.js
// アクションボタンの自由配置（ドラッグで移動）と永続化。
//   applyLayout: 保存済み座標をボタンに反映（未保存なら CSS 既定位置）
//   makeLayoutEditor: 編集モードのドラッグ処理・保存・リセットを提供
//
// 保存形式: cfg.layout = { melee:{left,top}, dash:{left,top}, ... }（layer 左上基準のpx）

// class 一覧から 'tc-btn-<key>' の key を取り出す
function btnKey(el) {
  for (const c of el.classList) {
    if (c.startsWith('tc-btn-')) return c.slice('tc-btn-'.length);
  }
  return null;
}

export function applyLayout(layer, cfg) {
  const L = (cfg && cfg.layout) || {};
  layer.querySelectorAll('.tc-btn').forEach((b) => {
    const key = btnKey(b);
    const pos = key && L[key];
    if (pos && typeof pos.left === 'number' && typeof pos.top === 'number') {
      b.style.left = pos.left + 'px'; b.style.top = pos.top + 'px';
      b.style.right = 'auto'; b.style.bottom = 'auto';
    } else {
      // 既定（CSS）に戻す
      b.style.left = b.style.top = b.style.right = b.style.bottom = '';
    }
  });
}

export function makeLayoutEditor(layer, cfg, save) {
  function attachDrag(b) {
    let pid = null, ox = 0, oy = 0;
    b.addEventListener('pointerdown', (e) => {
      if (!layer.classList.contains('tc-edit')) return; // 通常時はボタン本来の動作
      pid = e.pointerId; try { b.setPointerCapture(pid); } catch (_e) {}
      const r = b.getBoundingClientRect();
      ox = e.clientX - r.left; oy = e.clientY - r.top;
      e.preventDefault(); e.stopPropagation();
    });
    b.addEventListener('pointermove', (e) => {
      if (e.pointerId !== pid) return;
      const lr = layer.getBoundingClientRect();
      let left = e.clientX - lr.left - ox;
      let top  = e.clientY - lr.top  - oy;
      left = Math.max(0, Math.min(lr.width  - b.offsetWidth,  left));
      top  = Math.max(0, Math.min(lr.height - b.offsetHeight, top));
      b.style.left = left + 'px'; b.style.top = top + 'px';
      b.style.right = 'auto'; b.style.bottom = 'auto';
      e.preventDefault();
    });
    const up = (e) => {
      if (e.pointerId !== pid) return;
      pid = null;
      const key = btnKey(b);
      if (!key) return;
      cfg.layout = cfg.layout || {};
      cfg.layout[key] = { left: parseFloat(b.style.left) || 0, top: parseFloat(b.style.top) || 0 };
      save();
    };
    b.addEventListener('pointerup', up);
    b.addEventListener('pointercancel', up);
  }

  layer.querySelectorAll('.tc-btn').forEach(attachDrag);

  return {
    setEdit(on) { layer.classList.toggle('tc-edit', !!on); },
    reset() { cfg.layout = {}; save(); applyLayout(layer, cfg); },
  };
}
