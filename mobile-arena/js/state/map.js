import { TILE } from '../core/constants.js';
// 初期スポーンも波中スポーンと同じデータ駆動経路（makeMobFromKey）に統一。
import { makeMobFromKey } from '../systems/enemies.js';
import { getMap } from './maps.js';

export function setupMap(state) {
  const def = getMap(state.mapId);
  const rows = def.rows;
  const H = rows.length, W = rows[0].length;
  const legend = def.legend || {};
  const wallHp = def.wallHp || 90;

  state.map      = rows.map(r => r.split(''));
  state.tileHP   = Array.from({ length: H }, () => Array(W).fill(Infinity));
  state.tileMaxHP= Array.from({ length: H }, () => Array(W).fill(Infinity));
  state.flow     = Array.from({ length: H }, () => Array(W).fill(Infinity));
  state.dim      = { w: W, h: H };
  state.items    = state.items || [];
  state.mobs     = state.mobs  || [];

  for (let y = 0; y < H; y++) {
    for (let x = 0; x < W; x++) {
      const c = state.map[y][x];
      if (c === '#' || c === 'D') continue;
      const cx = (x + 0.5) * TILE, cy = (y + 0.5) * TILE;
      const mark = legend[c];
      if (mark) {
        if (mark.kind === 'player') { state.player.x = cx; state.player.y = cy; }
        else if (mark.kind === 'enemy') {
          const mob = makeMobFromKey(state, mark.enemy, cx, cy, 1);
          if (mob) state.mobs.push(mob);
        } else if (mark.kind === 'item') {
          const it = { type: mark.item, x: cx, y: cy };
          if (mark.amt != null) it.amt = mark.amt;
          if (mark.heal != null) it.heal = mark.heal;
          state.items.push(it);
        }
      }
      // マーカー/未知文字は床にする
      state.map[y][x] = '.';
    }
  }

  // 壁HP 初期化（境界は不壊、内部の '#' は破壊可能）
  const isBorder = (tx, ty) => tx === 0 || ty === 0 || tx === W - 1 || ty === H - 1;
  for (let y = 0; y < H; y++) {
    for (let x = 0; x < W; x++) {
      if (state.map[y][x] === '#') {
        const v = isBorder(x, y) ? Infinity : wallHp;
        state.tileHP[y][x] = v;
        state.tileMaxHP[y][x] = v;
      }
    }
  }
}
