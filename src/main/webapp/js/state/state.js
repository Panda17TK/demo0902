import { WEAPONS } from './data.js';
/** @returns {import('./types.js').State} */
export function createInitialState() {
	const state = {
		dim: { w: 0, h: 0 },
		map: [],
		tileHP: [],
		tileMaxHP: [],
		flow: [],
		items: [],
		mobs: [],
		bullets: [],
		ebullets: [],
		grenades: [],
		fx: [],
		slashes: [],
		timers: { elapsed: 0, spawn: 5 },
		paused: false,
		runStart: 0,
		gameOver: false,
		stats: { kills: 0, timeMs: 0, name: '' },

		player: {
			x: 0, y: 0, w: 22, h: 22,
			vx: 0, vy: 0,
			hp: 100, iTime: 0,
			baseSpeed: 110,
			facing: { x: 1, y: 0 },
			staMax: 100, sta: 100,
			buffs: { range: 1, dmg: 1, speed: 1, tRange: 0, tDmg: 0, tSpeed: 0 },
			// いちおう在庫は置くけど、下の infiniteMag / infiniteAmmo により基本使わない
			inv: { blocks: 0, ammo9: 999, ammo12: 999, ammoBeam: 999, ammoNade: 999 },

			// ★ 最初から全武器を所持
			weapons: [
				// ピストル（無限マガジン）
				{ id: 'pistol', name: 'Pistol', dmg: 12, fireRate: 0.25, magSize: 12, mag: 12, spread: 0.05, pellets: 1, ammoType: 'ammo9', infiniteMag: true },
				// ショットガン（無限マガジン）
				{ id: 'shotgun', name: 'Shotgun', dmg: 7, fireRate: 0.60, magSize: 6, mag: 6, spread: 0.25, pellets: 6, ammoType: 'ammo12', infiniteMag: true },
				// マシンガン（無限マガジン）
				{ id: 'mg', name: 'MG', dmg: 7, fireRate: 0.08, magSize: 40, mag: 40, spread: 0.12, pellets: 1, ammoType: 'ammo9', infiniteMag: true },
				// ビーム（弾消費なし＝infiniteAmmo）
				{ id: 'beam', name: 'Beam', dmg: 6, fireRate: 0.05, magSize: null, mag: 0, spread: 0, pellets: 1, infiniteAmmo: true },
				// グレネード（無限マガジン）
				{ id: 'grenade', name: 'Grenade', dmg: 0, fireRate: 0.90, magSize: 3, mag: 3, spread: 0, pellets: 1, ammoType: 'ammoNade', infiniteMag: true },
			],
			curW: 0,

			meleeCD: 0,
			shootCD: 0,
			isDashing: false,
		},
	};

	return state;
}