// webapp/js/core/settings.js
// タッチ操作の設定（左右入替・透明度・サイズ・自動射撃）を localStorage に永続化する。

const KEY = 'arpg_touch_settings_v1';

export const DEFAULTS = {
  swap: false,      // 左右スティックの入れ替え（true: 照準=左 / 移動=右）
  opacity: 0.9,     // コントロールの不透明度 0.3..1
  scale: 1.0,       // コントロールのサイズ倍率 0.8..1.4
  autoFire: false,  // 自動射撃（最寄りの敵にオート照準して発射）
  haptics: true,    // ボタン操作時の触覚フィードバック（対応端末のみ）
  layout: {},       // ボタンの自由配置 { ボタンキー: {left, top} }（編集モードで保存）
};

export function loadSettings() {
  try {
    const raw = localStorage.getItem(KEY);
    const obj = raw ? JSON.parse(raw) : {};
    const s = Object.assign({}, DEFAULTS, obj);
    if (!s.layout || typeof s.layout !== 'object') s.layout = {}; // 壊れた保存への保険
    return s;
  } catch (_e) {
    return Object.assign({}, DEFAULTS, { layout: {} });
  }
}

export function saveSettings(s) {
  try { localStorage.setItem(KEY, JSON.stringify(s)); } catch (_e) {}
}
