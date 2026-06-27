import { makeMobFromKey, makeZombie, makeSpitter } from './enemies.js';
import { rebuildFlowField } from './flowfield.js';
import { promptModal, chooseModal } from '../core/modal.js';

// セーブフォーマットのバージョン。互換を壊す変更時にインクリメントし、
// migrate() で旧→新へ移行する。
var SCHEMA_VERSION = 2;

// 旧スキーマを現行に移行する（将来の変更に備えた拡張点）
function migrate(d) {
	var v = (d && typeof d.schema === 'number') ? d.schema : 1;
	// v1 → v2: weapons/mods/wave/stats を持たない可能性 → 既定で補完（applyToState 側で吸収）
	if (v < 2) { d.schema = 2; }
	return d;
}

// 数値を範囲にクランプして返す（NaN/型不正は既定値）
function clampNum(v, lo, hi, def) {
	if (typeof v !== 'number' || !isFinite(v)) return def;
	return Math.max(lo, Math.min(hi, v));
}

// CTX(コンテキストパス)を安全に決定
var CTX = (function() {
	try {
		if (typeof window !== 'undefined' && typeof window.CTX === 'string') return window.CTX;
		var p = (typeof location !== 'undefined' ? location.pathname : '');
		var m = p.match(/^\/[^\/]+/);
		return m ? m[0] : '';
	} catch (_) { return ''; }
})();

// ---- 安全に JSON を読む（HTMLエラーを握りつぶさない）----
function safeJson(res) {
	return res.text().then(function(t) {
		try { return JSON.parse(t); }
		catch (_) { try { console.error('[safeJson] not JSON:', t.slice(0, 256)); } catch (__) { } return null; }
	});
}
function nowStr(ts) { try { return new Date(ts).toLocaleString(); } catch (_) { return String(ts); } }

// ========== シリアライズ ==========
function serialize(state) {
	var weapons = state.player.weapons.map(function(w) { return { id: w.id, mag: w.mag }; });
	var mobs = state.mobs.map(function(m) {
		return { kind: m.kind, x: m.x, y: m.y, hp: m.hp, maxhp: m.maxhp, waveNum: m.waveNum };
	});
	return {
		schema: SCHEMA_VERSION,
		player: {
			x: state.player.x, y: state.player.y, hp: state.player.hp, hpMax: state.player.hpMax,
			inv: state.player.inv, curW: state.player.curW,
			weapons: weapons, buffs: state.player.buffs, mods: state.player.mods, sta: state.player.sta
		},
		wave: state.wave, stats: state.stats,
		map: state.map, tileHP: state.tileHP, tileMaxHP: state.tileMaxHP, items: state.items,
		mobs: mobs,
		spawner: state.timers ? { elapsed: (state.timers.elapsed || 0), spawnTimer: (state.timers.spawn || 5) } : null,
		dim: state.dim
	};
}

// ========== 復元ヘルパ ==========
function reinitArraysToMatch(state, d) {
	var H = (d && d.dim && d.dim.h) || (d && d.map ? d.map.length : 0);
	var W = (d && d.dim && d.dim.w) || (H > 0 ? d.map[0].length : 0);
	if (!H || !W) throw new Error('bad map size');

	state.dim = { w: W, h: H };

	// map
	var newMap = new Array(H);
	for (var y = 0; y < H; y++) {
		var row = d.map[y];
		var cp = new Array(W);
		for (var x = 0; x < W; x++) cp[x] = row[x];
		newMap[y] = cp;
	}
	state.map = newMap;

	// tileHP / tileMaxHP
	state.tileHP = new Array(H);
	state.tileMaxHP = new Array(H);
	for (var y2 = 0; y2 < H; y2++) {
		state.tileHP[y2] = new Array(W);
		state.tileMaxHP[y2] = new Array(W);
		for (var x2 = 0; x2 < W; x2++) {
			state.tileHP[y2][x2] = d.tileHP[y2][x2];
			state.tileMaxHP[y2][x2] = d.tileMaxHP[y2][x2];
		}
	}
}

function applyToState(state, d) {
	if (!d || !d.player || !d.map || !d.tileHP || !d.tileMaxHP) throw new Error('invalid save shape');

	// スキーマ移行（未知の新しいバージョンは拒否）
	migrate(d);
	if (d.schema > SCHEMA_VERSION) throw new Error('save schema too new: ' + d.schema);

	reinitArraysToMatch(state, d);

	var pp = d.player;
	// 値は範囲検証してから反映（改変/破損データへの耐性）
	state.player.hpMax = clampNum(pp.hpMax, 1, 100000, state.player.hpMax);
	state.player.x = clampNum(pp.x, -1e6, 1e6, state.player.x);
	state.player.y = clampNum(pp.y, -1e6, 1e6, state.player.y);
	state.player.hp = clampNum(pp.hp, 0, state.player.hpMax, state.player.hp);
	state.player.inv = (pp.inv && typeof pp.inv === 'object') ? pp.inv : state.player.inv;
	state.player.curW = Math.max(0, Math.min(state.player.weapons.length - 1, (pp.curW | 0)));
	if (pp.buffs && typeof pp.buffs === 'object') state.player.buffs = pp.buffs;
	if (pp.mods && typeof pp.mods === 'object') state.player.mods = pp.mods;
	state.player.sta = clampNum(pp.sta, 0, state.player.staMax || 100, state.player.sta);
	// ウェーブ／戦績の復元（型と範囲を検証）
	if (d.wave && typeof d.wave === 'object') {
		state.wave = d.wave;
		state.wave.num = clampNum(d.wave.num, 1, 100000, 1) | 0;
	}
	if (d.stats && typeof d.stats === 'object') state.stats = d.stats;

	// items
	state.items.length = 0;
	var items = Array.isArray(d.items) ? d.items : [];
	for (var i = 0; i < items.length; i++) state.items.push(Object.assign({}, items[i]));

	// mobs（工場関数で復元）
	state.mobs.length = 0;
	var mobs = Array.isArray(d.mobs) ? d.mobs : [];
	for (var j = 0; j < mobs.length; j++) {
		var m = mobs[j];
		// kind（zombie/spitter/boss等）からデータ駆動で復元。未知kindはzombie扱い。
		var obj = makeMobFromKey(state, m.kind, m.x, m.y, m.waveNum || state.wave && state.wave.num || 1)
			|| makeZombie(m.x, m.y);
		if (typeof m.hp === 'number') obj.hp = m.hp;
		if (typeof m.maxhp === 'number') obj.maxhp = m.maxhp;
		state.mobs.push(obj);
	}

	// 武器マガジン
	if (Array.isArray(pp.weapons)) {
		for (var wi = 0; wi < state.player.weapons.length; wi++) {
			var sw = state.player.weapons[wi];
			var found = null;
			for (var vi = 0; vi < pp.weapons.length; vi++) {
				if (pp.weapons[vi].id === sw.id) { found = pp.weapons[vi]; break; }
			}
			if (found && typeof found.mag === 'number') {
				var max = (sw.magSize == null ? Infinity : sw.magSize);
				sw.mag = Math.max(0, Math.min(max, found.mag));
			}
		}
	}

	// volatile
	state.bullets.length = 0;
	state.ebullets.length = 0;
	state.grenades.length = 0;
	state.fx.length = 0;

	// timers
	if (!state.timers) state.timers = {};
	if (d.spawner) {
		state.timers.elapsed = d.spawner.elapsed || 0;
		state.timers.spawn = d.spawner.spawnTimer || 5;
	}

	// 派生状態
	rebuildFlowField(state);
	state.player.iTime = 0;

	return true;
}

// ========== スロット API ==========
export function listSlots(bus) {
	if (!CTX) { if (bus && bus.emit) bus.emit('ui:toast', 'CTX not set'); return Promise.resolve([]); }
	return fetch(CTX + '/api/state2/list', { method: 'GET' })
		.then(function(res) { return res.ok ? safeJson(res) : null; })
		.then(function(json) { return (json && json.ok) ? (json.slots || []) : []; })
		.catch(function(e) { try { console.error('[listSlots]', e); } catch (_) { } if (bus && bus.emit) bus.emit('ui:toast', 'list failed'); return []; });
}

export function save(state, _storage, bus, slot) {
	if (!CTX) { if (bus && bus.emit) bus.emit('ui:toast', 'CTX not set'); return Promise.resolve(); }
	var ask = slot ? Promise.resolve(slot) : promptModal('保存スロット名（例: slot1, A など）', 'slot1');
	return ask.then(function(s) {
		if (!s) { if (bus && bus.emit) bus.emit('ui:toast', 'save cancelled'); return; }
		var payload = serialize(state);
		return fetch(CTX + '/api/state2?slot=' + encodeURIComponent(s), {
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8' },
			body: JSON.stringify(payload)
		}).then(function(res) {
			if (!res.ok) { if (bus && bus.emit) bus.emit('ui:toast', 'remote save failed'); return; }
			if (bus && bus.emit) bus.emit('ui:toast', 'saved (' + s + ')');
		}).catch(function(e) {
			try { console.error('[save]', e); } catch (_) { }
			if (bus && bus.emit) bus.emit('ui:toast', 'remote save error');
		});
	});
}

export function load(state, _storage, bus, slot) {
	if (!CTX) { if (bus && bus.emit) bus.emit('ui:toast', 'CTX not set'); return Promise.resolve(); }
	var url = CTX + '/api/state2' + (slot ? ('?slot=' + encodeURIComponent(slot)) : '');
	return fetch(url, { method: 'GET' })
		.then(function(res) {
			if (!res.ok) { if (bus && bus.emit) bus.emit('ui:toast', 'remote load failed'); return null; }
			return safeJson(res);
		})
		.then(function(json) {
			if (!json) return;
			if (!json.ok || !json.exists) { if (bus && bus.emit) bus.emit('ui:toast', 'セーブがありません'); return; }
			try {
				applyToState(state, json.data);
				if (bus && bus.emit) bus.emit('ui:toast', 'loaded' + (slot ? (' (' + slot + ')') : ''));
			} catch (e) {
				try { console.error('[applyToState]', e, json.data); } catch (_) { }
				if (bus && bus.emit) bus.emit('ui:toast', 'broken save data');
			}
		})
		.catch(function(e) {
			try { console.error('[load]', e); } catch (_) { }
			if (bus && bus.emit) bus.emit('ui:toast', 'remote load error');
		});
}

export function saveChooser(state, _storage, bus) {
	return save(state, _storage, bus, null);
}

export function loadChooser(state, _storage, bus) {
	return listSlots(bus).then(function(slots) {
		if (!slots.length) { if (bus && bus.emit) bus.emit('ui:toast', 'セーブがありません'); return; }
		var items = [];
		for (var i = 0; i < slots.length; i++) {
			items.push(slots[i].slot + '  (' + nowStr(slots[i].updatedAt) + ')');
		}
		return chooseModal('ロードするスロットを選択', items).then(function(idx) {
			if (idx >= 0 && idx < slots.length) {
				return load(state, _storage, bus, slots[idx].slot);
			}
			if (bus && bus.emit) bus.emit('ui:toast', 'load cancelled');
		});
	});
}

export function wipe(_state, _storage, bus, slot) {
	if (!CTX) { if (bus && bus.emit) bus.emit('ui:toast', 'CTX not set'); return Promise.resolve(); }
	var url = CTX + '/api/state2' + (slot ? ('?slot=' + encodeURIComponent(slot)) : '');
	return fetch(url, { method: 'DELETE' })
		.then(function(res) {
			if (!res.ok) { if (bus && bus.emit) bus.emit('ui:toast', 'delete failed'); return; }
			if (bus && bus.emit) bus.emit('ui:toast', slot ? ('deleted (' + slot + ')') : 'deleted all');
		})
		.catch(function(e) {
			try { console.error('[wipe]', e); } catch (_) { }
			if (bus && bus.emit) bus.emit('ui:toast', 'remote delete error');
		});
}
