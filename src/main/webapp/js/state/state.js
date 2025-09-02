import { WEAPONS } from './data.js';
/** @returns {import('./types.js').State} */
export function createInitialState() {
	return {
		map: null, // setupMap で埋める
		tileHP: null, tileMaxHP: null,
		player: {
			x: 0, y: 0, w: 20, h: 20, baseSpeed: 120, hp: 100, iTime: 0, vx: 0, vy: 0,
			facing: { x: 1, y: 0 }, meleeCD: 0, shootCD: 0,
			buffs: { range: 1, dmg: 1, speed: 1, tRange: 0, tDmg: 0, tSpeed: 0 },
			staMax: 100, sta: 100,
			inv: { key: false, ammo9: 36, ammo12: 4, ammoBeam: 2, ammoNade: 0, blocks: 2 },
			weapons: [structuredClone(WEAPONS.pistol), structuredClone(WEAPONS.shotgun)],
			curW: 0
		},
		mobs: [], items: [], fx: [],
		bullets: [], ebullets: [], grenades: [], fx: [], slashes: [],
		flow: null,
		timers: { elapsed: 0, spawn: 5, bfs: 0 },
		shake: { t: 0, mag: 0 },
		paused: false,
		stats: {
			startWallMs: Date.now(),
			timeMs: 0,
			kills: 0,
			name: '' // 入力されたら保存
		},
		gameOver: false,
	};
}
