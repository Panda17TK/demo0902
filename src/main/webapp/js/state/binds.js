import { reload, placeWallFront, switchWeapon } from '../systems/combat.js';

export function bindHotkeys(state, bus, input, api){
  addEventListener('keydown', e=>{
    const k = e.key.toLowerCase();
    // ゲームオーバー中（オーバーレイ表示中）はホットキーを無効化し、
    // Escape による誤アンポーズを防ぐ。
    if(state.gameOver) return;
    if(k==='escape'){ state.paused = !state.paused; }
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