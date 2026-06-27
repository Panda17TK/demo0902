import { test } from 'node:test';
import assert from 'node:assert/strict';

// ブラウザ環境のスタブ（モジュールが window/localStorage を安全参照できるように）
if (typeof globalThis.window === 'undefined') globalThis.window = globalThis;
if (typeof globalThis.localStorage === 'undefined') {
  globalThis.localStorage = { getItem() { return null; }, setItem() {}, removeItem() {} };
}

const BASE = '../../main/webapp/js/';
const { createInitialState } = await import(BASE + 'state/state.js');
const { setupMap } = await import(BASE + 'state/map.js');
const { makeMobFromKey } = await import(BASE + 'systems/enemies.js');
const { doMelee } = await import(BASE + 'systems/melee.js');
const { CONFIG } = await import(BASE + 'core/config.js');

const bus = { emit() {} };

// スタミナ比を指定して近接1回ぶんの「即時ヒットダメージ」と継続斬りの有無を返す
function swing(staRatio) {
  const s = createInitialState();
  s.mapId = 'pillars';
  setupMap(s);
  const p = s.player;
  p.facing = { x: 1, y: 0 };
  p.staMax = 100; p.sta = staRatio * 100; p.meleeCD = 0;
  p.buffs = { range: 1, dmg: 1 };
  p.mods = { meleeMul: 1, dmg: 1 };
  const m = makeMobFromKey(s, 'zombie', p.x + 30, p.y, 1);
  m.hp = 100000; m.hpMax = 100000; if (m.dodge) m.dodge = null;
  s.mobs = [m]; s.mobGrid = null; s.slashes = []; s.fx = s.fx || [];
  const before = m.hp;
  doMelee(s, bus);
  return { dmg: before - m.hp, slashes: s.slashes.length };
}

const pc = CONFIG.player;
const full = Math.round(pc.meleeDmg);
const weak = Math.round(pc.meleeDmg * pc.meleeWeakMul);
const fist = Math.round(pc.fistDmg);

test('40%以上は剣の本来の威力', () => {
  assert.equal(swing(1.0).dmg, full);
  assert.equal(swing(0.40).dmg, full); // 40%ちょうどは「40%を切る」ではないので本来
});

test('20%超〜40%未満は剣の威力低下', () => {
  assert.equal(swing(0.39).dmg, weak);
  assert.equal(swing(0.30).dmg, weak);
  assert.equal(swing(0.21).dmg, weak);
});

test('20%以下は拳（最弱）', () => {
  assert.equal(swing(0.20).dmg, fist);
  assert.equal(swing(0.10).dmg, fist);
});

test('威力順は 拳 < 低下剣 < 本来剣（低下しても拳より大）', () => {
  assert.ok(fist < weak, `fist(${fist}) < weak(${weak})`);
  assert.ok(weak < full, `weak(${weak}) < full(${full})`);
});

test('拳のときは継続斬り（残像扇）を出さない', () => {
  assert.equal(swing(0.10).slashes, 0);
  assert.equal(swing(1.0).slashes, 1);
  assert.equal(swing(0.30).slashes, 1);
});
