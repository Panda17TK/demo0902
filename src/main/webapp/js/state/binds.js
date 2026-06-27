import { reload, placeWallFront, switchWeapon } from '../systems/combat.js';
import { CONFIG } from '../core/config.js';

// カメラズームを段階調整（- で引き＝視界広く / =（+）で寄り）。CONFIG.camera の範囲にクランプ。
function adjustZoom(state, delta){
  const cam = CONFIG.camera || { zoom: 1, minZoom: 0.6, maxZoom: 1.2 };
  const cur = state.camZoom != null ? state.camZoom : cam.zoom;
  state.camZoom = Math.max(cam.minZoom, Math.min(cam.maxZoom, Math.round((cur + delta) * 100) / 100));
}

export function bindHotkeys(state, bus, input, api){
  addEventListener('keydown', e=>{
    const k = e.key.toLowerCase();
    // ゲームオーバー中（オーバーレイ表示中）はホットキーを無効化し、
    // Escape による誤アンポーズを防ぐ。
    if(state.gameOver) return;
    if(k==='escape'){ state.paused = !state.paused; }
    if(k==='-'){ adjustZoom(state, -0.1); }            // 視界を広げる（引き）
    if(k==='=' || k==='+'){ adjustZoom(state, +0.1); } // 視界を狭める（寄り）
    if(k==='p'){ api.save(); }
    if(k==='l'){ api.load(); }
    if(k==='1') switchWeapon(state, 0);
    if(k==='2') switchWeapon(state, 1);
    if(k==='3') switchWeapon(state, 2);
    if(k==='4') switchWeapon(state, 3);
    if(k==='5') switchWeapon(state, 4);
    if(k==='r') reload(state, bus);
    if(k==='f') placeWallFront(state, bus);
  });
}