import { test } from 'node:test';
import assert from 'node:assert/strict';

const BASE = '../../main/webapp/js/';
const { MAPS, getMap } = await import(BASE + 'state/maps.js');

test('全マップの行幅が均一', () => {
  for (const m of MAPS) {
    const w = m.rows[0].length;
    for (const r of m.rows) assert.equal(r.length, w, `map ${m.id} の行幅不一致`);
  }
});

test('各マップに player スポーン(P)が1つ', () => {
  for (const m of MAPS) {
    const count = m.rows.join('').split('').filter((c) => c === 'P').length;
    assert.equal(count, 1, `map ${m.id} の P 数`);
  }
});

test('getMap は id 一致 / 既定で先頭', () => {
  assert.equal(getMap('corridors').id, 'corridors');
  assert.equal(getMap().id, MAPS[0].id);
  assert.equal(getMap('nonexistent').id, MAPS[0].id);
});

test('legend のマーカーは壁文字と衝突しない', () => {
  for (const m of MAPS) {
    for (const k of Object.keys(m.legend)) {
      assert.ok(k !== '#' && k !== 'D' && k !== '.', `map ${m.id} legend ${k} が予約文字`);
    }
  }
});

// プレイヤー(P)から全ての非壁マスへ到達できること。閉じ込め／隔離マップを弾く。
// '#' と 'D'(破壊不能壁) を壁として 4近傍 BFS する。
test('各マップは P から全床に到達できる（閉じ込め無し）', () => {
  const isWall = (c) => c === '#' || c === 'D';
  for (const m of MAPS) {
    const g = m.rows.map((r) => r.split(''));
    const H = g.length, W = g[0].length;
    let py = -1, px = -1, floors = 0;
    for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
      if (!isWall(g[y][x])) { floors++; if (g[y][x] === 'P') { py = y; px = x; } }
    }
    assert.ok(px >= 0, `map ${m.id} に P がない`);
    const seen = new Set([px + ',' + py]);
    const q = [[px, py]];
    while (q.length) {
      const [x, y] = q.shift();
      for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
        const nx = x + dx, ny = y + dy;
        if (nx < 0 || ny < 0 || nx >= W || ny >= H) continue;
        if (isWall(g[ny][nx])) continue;
        const k = nx + ',' + ny;
        if (!seen.has(k)) { seen.add(k); q.push([nx, ny]); }
      }
    }
    assert.equal(seen.size, floors, `map ${m.id} に到達不能な床がある (${seen.size}/${floors})`);
  }
});
