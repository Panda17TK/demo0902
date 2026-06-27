// webapp/js/core/touch/visual.js
// タッチ UI の見た目（左右入替・不透明度・サイズ）と自動射撃トグルを反映する。
// 設定変更時とマウント時の両方から呼ばれる単一の出所。

export function applyVisual(layer, cfg, input) {
  layer.classList.toggle('tc-swap', !!cfg.swap);
  layer.style.setProperty('--tc-op', String(cfg.opacity));
  layer.style.setProperty('--tc-scale', String(cfg.scale));
  input.autoFire = !!cfg.autoFire;
}
