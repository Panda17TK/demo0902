import { TILE } from '../core/constants.js';
import { rectInter } from './physics.js';
//import { rebuildFlowField } from './flow.js'; 

export function damageTile(state, tx, ty, dmg) {
	if (tx < 0 || ty < 0 || tx >= state.dim.w || ty >= state.dim.h) return false;
	if (state.map[ty][tx] !== '#') return false;
	if (state.tileHP[ty][tx] === Infinity) return false;
	state.tileHP[ty][tx] -= dmg;
	if (state.tileHP[ty][tx] <= 0) {
		clearWall(state, tx, ty);
		state.player.inv.blocks++;
		if (typeof rebuildFlowField === 'function') rebuildFlowField(state);
		return true;
	}
	return false;
}
export function clearWall(state, tx, ty) {
	state.map[ty][tx] = '.'; state.tileHP[ty][tx] = Infinity; state.tileMaxHP[ty][tx] = Infinity;
}
export function canPlaceAt(state, tx, ty) {
	if (tx < 0 || ty < 0 || tx >= state.dim.w || ty >= state.dim.h) return false;
	if (state.map[ty][tx] !== '.') return false;
	const cx = tx * TILE + TILE / 2, cy = ty * TILE + TILE / 2; const box = { x: cx, y: cy, w: TILE * 0.9, h: TILE * 0.9 };
	if (rectInter(box, state.player)) return false;
	for (const m of state.mobs) { if (rectInter(box, m)) return false; }
	return true;
}

export function updateTiles(state, dt, bus, audio) {
	// ・崩落アニメ
	// ・設置クールダウン
	// ・フローフィールド再構築の節流し など
}