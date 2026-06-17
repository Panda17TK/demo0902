// webapp/js/systems/melee.js
// プレイヤーの近接攻撃（即時の扇ヒット＋壁破壊＋継続扇スラッシュの生成）。

import { TILE, MELEE_SWING } from '../core/constants.js';
import { CONFIG } from '../core/config.js';
import { norm } from './physics.js';
import { forNearby } from './spatial.js';
import { damageTile } from './tiles.js';
import { hurtMob } from './combat-core.js';
import { spawnSlashFX } from './fx.js';

export function doMelee(state, bus) {
  const p = state.player;
  const mods = p.mods || { meleeMul: 1, dmg: 1 };
  p.meleeCD = CONFIG.player.meleeCD;
  p.meleeT = MELEE_SWING;   // 刃の一閃アニメ用（描画）
  const reach = CONFIG.player.meleeReach * p.buffs.range;
  const arc = Math.PI;
  const faceAng = Math.atan2(p.facing.y, p.facing.x);
  bus.emit('sfx', 'melee');
  spawnSlashFX(state, p.x, p.y, faceAng);

  // 即時ヒット（敵）。近傍グリッドで対象を絞り、hurtMob で一元処理（回避/ガード反映）。
  const meleeDmg = Math.round(CONFIG.player.meleeDmg * p.buffs.dmg * mods.meleeMul);
  const maxReach = reach + 24;
  const meleeHit = (m) => {
    if (m.hp <= 0) return;
    const dx = m.x - p.x, dy = m.y - p.y; const d = Math.hypot(dx, dy);
    if (d < reach + Math.max(m.w, m.h) / 2) {
      const ang = Math.atan2(dy, dx) - faceAng;
      const a = Math.abs((ang + Math.PI * 3) % (Math.PI * 2) - Math.PI);
      if (a <= arc / 2) {
        const n = norm(dx, dy);
        hurtMob(state, m, meleeDmg, n.x, n.y, CONFIG.player.meleeKB, { sparks: 4 });
      }
    }
  };
  if (state.mobGrid) forNearby(state.mobGrid, p.x, p.y, maxReach, meleeHit);
  else for (const m of state.mobs) meleeHit(m);

  // 壁破壊（前方3x3）
  const ftx = Math.floor((p.x + p.facing.x * 22) / TILE);
  const fty = Math.floor((p.y + p.facing.y * 22) / TILE);
  for (let oy = -1; oy <= 1; oy++) {
    for (let ox = -1; ox <= 1; ox++) {
      const tx = ftx + ox, ty = fty + oy;
      if (tx < 0 || ty < 0 || tx >= state.dim.w || ty >= state.dim.h) continue;
      if (state.map[ty][tx] === '#') damageTile(state, tx, ty, meleeDmg);
    }
  }

  // 継続扇（弱）＆敵弾相殺は projectiles.updateSlashes が処理
  state.slashes.push({
    x: p.x, y: p.y, ang: faceAng,
    t: 0, life: 0.30, reach: reach, arc: arc,
    tickInt: 0.07, tick: 0,
    dmg: Math.round(CONFIG.player.meleeSlashDmg * p.buffs.dmg * mods.meleeMul)
  });
}
