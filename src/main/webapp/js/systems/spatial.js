// webapp/js/systems/spatial.js
// 一様グリッド（spatial hash）による近傍探索。敵×敵の分離や弾×敵の判定を
// O(n^2)→近傍セルのみの走査に落とすための broad-phase。
//
// 使い方:
//   const grid = buildMobGrid(state, cell);   // 毎フレーム1回構築
//   forNearby(grid, x, y, radius, (m) => {...}); // 近傍の mob だけ列挙

import { CELL } from '../core/constants.js';

// グリッドを構築（mob を所属セルに振り分け）。cell はセル一辺(px)。
export function buildMobGrid(state, cell) {
  const c = cell || CELL;
  const map = new Map(); // key "cx,cy" -> mob[]
  const mobs = state.mobs;
  for (let i = 0; i < mobs.length; i++) {
    const m = mobs[i];
    if (m.hp <= 0) continue;
    const cx = Math.floor(m.x / c), cy = Math.floor(m.y / c);
    const key = cx + ',' + cy;
    let arr = map.get(key);
    if (!arr) { arr = []; map.set(key, arr); }
    arr.push(m);
  }
  return { cell: c, map };
}

// (x,y) 中心・半径 radius に重なりうるセルの mob を cb で列挙。
// 重複呼び出しはなし（各 mob はちょうど1セルに属する）。
export function forNearby(grid, x, y, radius, cb) {
  const c = grid.cell;
  const mincx = Math.floor((x - radius) / c), maxcx = Math.floor((x + radius) / c);
  const mincy = Math.floor((y - radius) / c), maxcy = Math.floor((y + radius) / c);
  for (let cy = mincy; cy <= maxcy; cy++) {
    for (let cx = mincx; cx <= maxcx; cx++) {
      const arr = grid.map.get(cx + ',' + cy);
      if (!arr) continue;
      for (let i = 0; i < arr.length; i++) cb(arr[i]);
    }
  }
}

// 近傍の中で最初に AABB/半径条件を満たす mob を返す（弾ヒット用）。
// 早期 return できるよう、cb は true を返すと打ち切る。
export function findNearby(grid, x, y, radius, test) {
  const c = grid.cell;
  const mincx = Math.floor((x - radius) / c), maxcx = Math.floor((x + radius) / c);
  const mincy = Math.floor((y - radius) / c), maxcy = Math.floor((y + radius) / c);
  for (let cy = mincy; cy <= maxcy; cy++) {
    for (let cx = mincx; cx <= maxcx; cx++) {
      const arr = grid.map.get(cx + ',' + cy);
      if (!arr) continue;
      for (let i = 0; i < arr.length; i++) {
        if (test(arr[i])) return arr[i];
      }
    }
  }
  return null;
}
