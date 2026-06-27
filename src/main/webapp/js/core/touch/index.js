// webapp/js/core/touch/index.js
// スマホ／タブレット向け画面上タッチ操作（ツインスティック）の組み立て役。
// 既存の input（keys / aim / move / autoFire）へ書き込むだけなのでゲームロジックは変更不要。
//   - 左スティック: 移動（アナログ：倒し具合で速度可変）   … stick.js
//   - 右スティック: 照準＋射撃（ドラッグ方向に向き、押下中 K=発射）… stick.js
//   - ボタン     : 近接 / ダッシュ / リロード / 武器切替 / 壁 / ポーズ / 設定 … buttons.js
//   - 設定       : 左右入替・自動射撃・バイブ・透明度・サイズ（localStorage 永続化）… settings-panel.js
//
// 構成の出所:
//   detect.js … タッチ端末判定        haptics.js … 触覚フィードバック
//   stick.js  … バーチャルスティック   buttons.js … アクションボタン
//   visual.js … 見た目反映             settings-panel.js … 設定パネル

import { loadSettings } from '../settings.js';
import { createStick } from './stick.js';
import { createButton } from './buttons.js';
import { createSettingsPanel } from './settings-panel.js';
import { makeHaptic } from './haptics.js';
import { applyVisual } from './visual.js';

export { isTouchDevice } from './detect.js';

export function createTouchControls(root, input, api) {
  if (!root) return null;
  const keys = input.keys;
  const cfg = loadSettings();
  const haptic = makeHaptic(cfg);

  const layer = document.createElement('div');
  layer.className = 'touch-controls';

  // ===== スティック =====
  createStick('move', { layer, input, cfg });
  createStick('aim',  { layer, input, cfg });

  // ===== 設定パネル（ボタンから開くため先に生成）=====
  const settings = createSettingsPanel({ layer, cfg, input, api });

  // ===== ボタン =====
  createButton(layer, 'melee',  '近接', { hold: (on) => { keys['j'] = on; } }, haptic);
  createButton(layer, 'dash',   'DASH', { hold: (on) => { keys['shift'] = on; } }, haptic);
  createButton(layer, 'reload', 'R',    { tap: () => { if (api.reload) api.reload(); } }, haptic);
  createButton(layer, 'weapon', '武器', { tap: () => { if (api.cycleWeapon) api.cycleWeapon(); } }, haptic);
  createButton(layer, 'build',  '壁',   { tap: () => { if (api.build) api.build(); } }, haptic);
  createButton(layer, 'pause',  'II',   { tap: () => { if (api.pause) api.pause(); } }, haptic);
  createButton(layer, 'settings', '⚙', { tap: () => settings.toggle() }, haptic);

  // ===== 初期反映 =====
  applyVisual(layer, cfg, input);

  root.appendChild(layer);
  return layer;
}
