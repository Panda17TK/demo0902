// js/systems/save-local.js
// localStorage 版のスロットセーブ（静的PWA：サーバ不要）。
// 公開APIは save-remote.js と互換（saveChooser/loadChooser）なので main.js は無改変。

import { makeMobFromKey, makeZombie } from './enemies.js';
import { rebuildFlowField } from './flowfield.js';

const SCHEMA_VERSION = 2;
const KEY_PREFIX = 'arena_save_';   // arena_save_<slot> = JSON
const KEY_INDEX = 'arena_save_index'; // スロット一覧 [{slot, updatedAt}]

function migrate(d) {
  const v = (d && typeof d.schema === 'number') ? d.schema : 1;
  if (v < 2) d.schema = 2;
  return d;
}
function clampNum(v, lo, hi, def) {
  if (typeof v !== 'number' || !isFinite(v)) return def;
  return Math.max(lo, Math.min(hi, v));
}

// ========== シリアライズ ==========
function serialize(state) {
  const weapons = state.player.weapons.map((w) => ({ id: w.id, mag: w.mag }));
  const mobs = state.mobs.map((m) => ({ kind: m.kind, x: m.x, y: m.y, hp: m.hp, maxhp: m.maxhp, waveNum: m.waveNum }));
  return {
    schema: SCHEMA_VERSION,
    player: {
      x: state.player.x, y: state.player.y, hp: state.player.hp, hpMax: state.player.hpMax,
      inv: state.player.inv, curW: state.player.curW,
      weapons, buffs: state.player.buffs, mods: state.player.mods, sta: state.player.sta,
    },
    wave: state.wave, stats: state.stats,
    map: state.map, tileHP: state.tileHP, tileMaxHP: state.tileMaxHP, items: state.items,
    mobs,
    spawner: state.timers ? { elapsed: state.timers.elapsed || 0, spawnTimer: state.timers.spawn || 5 } : null,
    dim: state.dim,
  };
}

// ========== 復元 ==========
function reinitArraysToMatch(state, d) {
  const H = (d && d.dim && d.dim.h) || (d && d.map ? d.map.length : 0);
  const W = (d && d.dim && d.dim.w) || (H > 0 ? d.map[0].length : 0);
  if (!H || !W) throw new Error('bad map size');
  state.dim = { w: W, h: H };
  state.map = d.map.map((row) => row.slice());
  state.tileHP = new Array(H); state.tileMaxHP = new Array(H);
  for (let y = 0; y < H; y++) {
    state.tileHP[y] = d.tileHP[y].slice();
    state.tileMaxHP[y] = d.tileMaxHP[y].slice();
  }
}

function applyToState(state, d) {
  if (!d || !d.player || !d.map || !d.tileHP || !d.tileMaxHP) throw new Error('invalid save shape');
  migrate(d);
  if (d.schema > SCHEMA_VERSION) throw new Error('save schema too new: ' + d.schema);
  reinitArraysToMatch(state, d);

  const pp = d.player;
  state.player.hpMax = clampNum(pp.hpMax, 1, 100000, state.player.hpMax);
  state.player.x = clampNum(pp.x, -1e6, 1e6, state.player.x);
  state.player.y = clampNum(pp.y, -1e6, 1e6, state.player.y);
  state.player.hp = clampNum(pp.hp, 0, state.player.hpMax, state.player.hp);
  state.player.inv = (pp.inv && typeof pp.inv === 'object') ? pp.inv : state.player.inv;
  state.player.curW = Math.max(0, Math.min(state.player.weapons.length - 1, (pp.curW | 0)));
  if (pp.buffs && typeof pp.buffs === 'object') state.player.buffs = pp.buffs;
  if (pp.mods && typeof pp.mods === 'object') state.player.mods = pp.mods;
  state.player.sta = clampNum(pp.sta, 0, state.player.staMax || 100, state.player.sta);
  if (d.wave && typeof d.wave === 'object') { state.wave = d.wave; state.wave.num = clampNum(d.wave.num, 1, 100000, 1) | 0; }
  if (d.stats && typeof d.stats === 'object') state.stats = d.stats;

  state.items.length = 0;
  (Array.isArray(d.items) ? d.items : []).forEach((it) => state.items.push(Object.assign({}, it)));

  state.mobs.length = 0;
  (Array.isArray(d.mobs) ? d.mobs : []).forEach((m) => {
    const obj = makeMobFromKey(state, m.kind, m.x, m.y, m.waveNum || (state.wave && state.wave.num) || 1) || makeZombie(m.x, m.y);
    if (typeof m.hp === 'number') obj.hp = m.hp;
    if (typeof m.maxhp === 'number') obj.maxhp = m.maxhp;
    state.mobs.push(obj);
  });

  if (Array.isArray(pp.weapons)) {
    for (const sw of state.player.weapons) {
      const found = pp.weapons.find((v) => v.id === sw.id);
      if (found && typeof found.mag === 'number') {
        const max = (sw.magSize == null ? Infinity : sw.magSize);
        sw.mag = Math.max(0, Math.min(max, found.mag));
      }
    }
  }

  state.bullets.length = 0; state.ebullets.length = 0; state.grenades.length = 0; state.fx.length = 0;
  if (!state.timers) state.timers = {};
  if (d.spawner) { state.timers.elapsed = d.spawner.elapsed || 0; state.timers.spawn = d.spawner.spawnTimer || 5; }
  rebuildFlowField(state);
  state.player.iTime = 0;
  return true;
}

// ========== localStorage スロット ==========
function readIndex() {
  try { return JSON.parse(localStorage.getItem(KEY_INDEX) || '[]'); } catch (_e) { return []; }
}
function writeIndex(list) {
  try { localStorage.setItem(KEY_INDEX, JSON.stringify(list)); } catch (_e) {}
}
function upsertIndex(slot) {
  const list = readIndex().filter((e) => e.slot !== slot);
  list.unshift({ slot, updatedAt: Date.now() });
  writeIndex(list);
}

export function listSlots() {
  return Promise.resolve(readIndex());
}

export function save(state, _storage, bus, slot) {
  try {
    const s = slot || window.prompt('保存スロット名（例: slot1, A など）', 'slot1');
    if (!s) { bus && bus.emit('ui:toast', 'save cancelled'); return Promise.resolve(); }
    localStorage.setItem(KEY_PREFIX + s, JSON.stringify(serialize(state)));
    upsertIndex(s);
    bus && bus.emit('ui:toast', 'saved (' + s + ')');
  } catch (e) {
    try { console.error('[save]', e); } catch (_) {}
    bus && bus.emit('ui:toast', 'save error');
  }
  return Promise.resolve();
}

export function load(state, _storage, bus, slot) {
  try {
    const raw = localStorage.getItem(KEY_PREFIX + slot);
    if (!raw) { bus && bus.emit('ui:toast', 'セーブがありません'); return Promise.resolve(); }
    applyToState(state, JSON.parse(raw));
    bus && bus.emit('ui:toast', 'loaded (' + slot + ')');
  } catch (e) {
    try { console.error('[load]', e); } catch (_) {}
    bus && bus.emit('ui:toast', 'broken save data');
  }
  return Promise.resolve();
}

export function saveChooser(state, _storage, bus) {
  return save(state, _storage, bus, null);
}

export function loadChooser(state, _storage, bus) {
  return listSlots().then((slots) => {
    if (!slots.length) { bus && bus.emit('ui:toast', 'セーブがありません'); return; }
    const lines = slots.map((e, i) => (i + 1) + ': ' + e.slot + ' (' + new Date(e.updatedAt).toLocaleString() + ')');
    const ans = window.prompt('ロードする番号を入力:\n' + lines.join('\n'), '1');
    const idx = ans ? (parseInt(ans, 10) - 1) : -1;
    if (idx >= 0 && idx < slots.length) return load(state, _storage, bus, slots[idx].slot);
    bus && bus.emit('ui:toast', 'load cancelled');
  });
}
