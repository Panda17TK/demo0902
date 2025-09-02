import { SPAWN } from '../state/data.js';
import { hasLineOfSight } from './los.js';

export function updateSpawner(state, dt, bus, audio) {
	state.timers.elapsed += dt; state.timers.spawn -= dt;
	const cap = Math.min(SPAWN.maxCap, Math.floor(SPAWN.capBase + state.timers.elapsed / 28));
	const interval = Math.max(3, SPAWN.baseInterval - state.timers.elapsed * 0.02);
	if (state.timers.spawn <= 0) {
		state.timers.spawn = interval + (Math.random() * 1.5 - 0.75);
		if (state.mobs.length < cap) {
			const n = (state.mobs.length < cap - 4) ? 3 : 1;
			for (let i = 0; i < n; i++) spawnOne(state, bus);
		}
	}
}

function tileFree(state, tx, ty) {
	if (tx < 0 || ty < 0 || tx >= state.dim.w || ty >= state.dim.h) return false;
	if (state.map[ty][tx] !== '.') return false;
	const cx = (tx + 0.5) * 32, cy = (ty + 0.5) * 32; const box = { x: cx, y: cy, w: 32 * 0.9, h: 32 * 0.9 };
	if (Math.abs(box.x - state.player.x) < (box.w / 2 + state.player.w / 2) && Math.abs(box.y - state.player.y) < (box.h / 2 + state.player.h / 2)) return false;
	for (const m of state.mobs) { if (Math.abs(box.x - m.x) < (box.w / 2 + m.w / 2) && Math.abs(box.y - m.y) < (box.h / 2 + m.h / 2)) return false; }
	return true;
}

function spawnOne(state, bus) {
	for (let tries = 0; tries < 160; tries++) {
		const tx = 1 + Math.floor(Math.random() * (state.dim.w - 2));
		const ty = 1 + Math.floor(Math.random() * (state.dim.h - 2));
		if (!tileFree(state, tx, ty)) continue;
		const cx = (tx + 0.5) * 32, cy = (ty + 0.5) * 32;
		const dx = cx - state.player.x, dy = cy - state.player.y;
		if (dx * dx + dy * dy < (32 * 10) * (32 * 10)) continue;
		if (hasLineOfSight(state, state.player.x, state.player.y, cx, cy)) continue;
		const type = Math.random() < 0.22 ? 'spitter' : 'zombie';
		state.mobs.push(type === 'spitter' ? { kind: 'spitter', x: cx, y: cy, w: 22, h: 22, hp: 65, maxhp: 65, baseSpeed: 35, shootCD: 0, vx: 0, vy: 0, meleeCD: 0, bumpCD: 0 } : { kind: 'zombie', x: cx, y: cy, w: 22, h: 22, hp: 55, maxhp: 55, baseSpeed: 72, shootCD: 0, vx: 0, vy: 0, meleeCD: 0, bumpCD: 0 });
		bus.emit('sfx', 'spawn');
		return true;
	}
	return false;
}
