import { TILE } from '../core/constants.js';

// ★ 依存を断つ：ここに簡易ファクトリを置く
function makeZombie(x, y){
  return { kind:'zombie', x, y, w:22, h:22, hp:55, maxhp:55, baseSpeed:72, shootCD:0, vx:0, vy:0, meleeCD:0, bumpCD:0 };
}
function makeSpitter(x, y){
  return { kind:'spitter', x, y, w:22, h:22, hp:65, maxhp:65, baseSpeed:35, shootCD:0, vx:0, vy:0, meleeCD:0, bumpCD:0 };
}

const RAW = [
  "##############################",
  "#..P...........#...........X.#",
  "#............................#",
  "#....######.....A......#####.#",
  "#....#....#.............#....#",
  "#....#....#....M........#....#",
  "#....#....#.............#....#",
  "#....######....#######..#....#",
  "#.............W..............#",
  "#..A.....#......Z.........V..#",
  "#........#...................#",
  "#....T...#....S..............#",
  "#........#...................#",
  "#........#....Y..............#",
  "#........###########.........#",
  "#........#.........#.........#",
  "#........#....K....#....M....#",
  "#........#.........#.....W...#",
  "#..S.....#.............Z.....#",
  "##############################",
];

export function setupMap(state) {
  const H = RAW.length, W = RAW[0].length;

  // 基本配列
  state.map      = RAW.map(r => r.split(''));
  state.tileHP   = Array.from({ length: H }, () => Array(W).fill(Infinity));
  state.tileMaxHP= Array.from({ length: H }, () => Array(W).fill(Infinity));
  state.flow     = Array.from({ length: H }, () => Array(W).fill(Infinity));
  state.dim      = { w: W, h: H };

  // 念のため初期化（呼び出し側で作っている想定でも安全に）
  state.items    = state.items || [];
  state.mobs     = state.mobs  || [];

  for (let y = 0; y < H; y++){
    for (let x = 0; x < W; x++){
      const c  = state.map[y][x];
      const cx = (x + 0.5) * TILE, cy = (y + 0.5) * TILE;

      switch (c) {
        case 'P': state.player.x = cx; state.player.y = cy; state.map[y][x] = '.'; break;
        case 'Z': state.mobs.push(makeZombie(cx, cy));       state.map[y][x] = '.'; break;
        case 'T': state.mobs.push(makeSpitter(cx, cy));      state.map[y][x] = '.'; break;

        case 'K': state.items.push({ type:'key',      x:cx, y:cy });                          state.map[y][x]='.'; break;
        case 'A': state.items.push({ type:'ammo9',    x:cx, y:cy, amt:18 });                  state.map[y][x]='.'; break;
        case 'S': state.items.push({ type:'ammo12',   x:cx, y:cy, amt:5  });                  state.map[y][x]='.'; break;
        case 'M': state.items.push({ type:'med',      x:cx, y:cy, heal:25});                  state.map[y][x]='.'; break;
        case 'X': state.items.push({ type:'buffRange',x:cx, y:cy });                           state.map[y][x]='.'; break;
        case 'Y': state.items.push({ type:'buffMelee',x:cx, y:cy });                           state.map[y][x]='.'; break;
        case 'V': state.items.push({ type:'buffSpeed',x:cx, y:cy });                           state.map[y][x]='.'; break;
        case 'W': state.items.push({ type:'crate',    x:cx, y:cy });                           state.map[y][x]='.'; break;

        // '#', 'D' はそのまま、それ以外（未知文字）は通路 '.' にしておく
        default:
          if (c !== '#' && c !== 'D') state.map[y][x] = '.';
      }
    }
  }

  // 壁HP 初期化
  const isBorder = (tx, ty) => tx === 0 || ty === 0 || tx === W - 1 || ty === H - 1;
  for (let y = 0; y < H; y++){
    for (let x = 0; x < W; x++){
      if (state.map[y][x] === '#') {
        const v = isBorder(x, y) ? Infinity : 90;
        state.tileHP[y][x] = v;
        state.tileMaxHP[y][x] = v;
      }
    }
  }
}