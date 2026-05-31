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
		stats: { kills: 0, timeMs: 0, name: '', wave: 1 },

		// ウェーブ制（ローグライト）。spawner が更新する。
		//   phase: 'active'（殲滅中） | 'intermission'（強化カード選択中）
		//   toSpawn: この波で未出現の残数 / spawnCD: 次体出現までの間隔
		//   choices: インターミッション中に提示中の強化カード配列
		wave: { num: 1, phase: 'active', toSpawn: 0, spawnCD: 0.3, choices: null, interT: 0 },

		// 演出用
		cam: null,                 // スムーズ追従カメラ（loop で初期化）
		shake: { t: 0, mag: 0 },   // 画面シェイク
		hitstop: 0,                // ヒットストップ（被弾/爆発で一瞬スロー）

		player: {
			x: 0, y: 0, w: 22, h: 22,
			vx: 0, vy: 0,
			hp: 100, hpMax: 100, iTime: 0,
			baseSpeed: 110,
			facing: { x: 1, y: 0 },
			staMax: 100, sta: 100,
			buffs: { range: 1, dmg: 1, speed: 1, tRange: 0, tDmg: 0, tSpeed: 0 },
			// ラン中に恒久強化される倍率など（強化カード選択で更新）
			mods: { gunMul: 1, meleeMul: 1, fireMul: 1, moveMul: 1, healOnKill: 0, ammoMul: 1 },
			// 弾薬は有限。リロードはこの在庫から補充する（敵ドロップ/クレートで補給）
			inv: { blocks: 2, key: false, ammo9: 96, ammo12: 24, ammoBeam: 6, ammoNade: 3 },

			// 最初から全武器を所持（弾薬管理が攻略のカギ）
			weapons: [
				{ id: 'pistol',  name: 'Pistol',  dmg: 24, fireRate: 0.22, magSize: 12, mag: 12, spread: 0.05, pellets: 1, ammoType: 'ammo9'  },
				{ id: 'shotgun', name: 'Shotgun', dmg: 16, fireRate: 0.60, magSize: 6,  mag: 6,  spread: 0.25, pellets: 6, ammoType: 'ammo12' },
				{ id: 'mg',      name: 'MG',      dmg: 12, fireRate: 0.08, magSize: 40, mag: 40, spread: 0.12, pellets: 1, ammoType: 'ammo9'  },
				{ id: 'beam',    name: 'Beam',    dmg: 80, fireRate: 0.60, magSize: null, mag: 0, spread: 0, pellets: 1, ammoType: 'ammoBeam' },
				{ id: 'grenade', name: 'Grenade', dmg: 0,  fireRate: 0.90, magSize: 1,  mag: 1,  spread: 0,    pellets: 1, ammoType: 'ammoNade' },
			],
			curW: 0,

			meleeCD: 0,
			shootCD: 0,
			isDashing: false,
		},
	};

	return state;
}

/**
 * 既存の state オブジェクトを「その場で」初期状態に戻す。
 * HUD / overlay などが保持している state 参照を壊さないよう、
 * 新オブジェクトに差し替えず各プロパティを上書きする。プレイヤー名だけ引き継ぐ。
 * マップ構築・フローフィールド再計算は呼び出し側で行うこと。
 */
export function resetState(state) {
	const fresh = createInitialState();
	const keepName = (state.stats && typeof state.stats.name === 'string') ? state.stats.name : '';
	Object.keys(fresh).forEach((k) => { state[k] = fresh[k]; });
	state.stats.name = keepName;
	return state;
}