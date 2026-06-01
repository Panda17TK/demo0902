export function spawnSlashFX(state, x, y, ang) {
	state.fx.push({ type: 'slash', x, y, ang, t: 0, life: 0.30 });
}

// ダメージ数字ポップ
export function spawnDamageNumber(state, x, y, dmg, opts) {
	opts = opts || {};
	state.fx.push({
		type: 'dmg', x: x + (Math.random() * 10 - 5), y: y - 6,
		vx: (Math.random() * 30 - 15), vy: -42,
		t: 0, life: 0.7, text: String(Math.max(1, Math.round(dmg))),
		crit: !!opts.crit,
	});
}

// 画面シェイク（既存の値より強い時だけ更新）
export function addShake(state, t, mag) {
	if (!state.shake) state.shake = { t: 0, mag: 0 };
	state.shake.t = Math.max(state.shake.t, t);
	state.shake.mag = Math.max(state.shake.mag, mag);
}

// ヒットストップ（一瞬のスロー）
export function addHitstop(state, t) {
	state.hitstop = Math.max(state.hitstop || 0, t);
}

// 残像（縮地などの高速移動の軌跡）
export function spawnAfterimageFX(state, x, y, w, h, color) {
	state.fx.push({ type: 'afterimage', x, y, w, h, color: color || '#cfe5ff', t: 0, life: 0.25 });
}

// 回避成功の白フラッシュ＋リング
export function spawnDodgeFX(state, x, y) {
	state.fx.push({ type: 'dodge', x, y, t: 0, life: 0.22 });
}


export function spawnSparksFX(state, x, y, n = 6) {
	for (let i = 0; i < n; i++) {
		const a = Math.random() * Math.PI * 2;
		const sp = 60 + Math.random() * 120;
		state.fx.push({ type: 'spark', x, y, vx: Math.cos(a) * sp, vy: Math.sin(a) * sp, t: 0, life: 0.25 });
	}
}

export function spawnEnemySlashFX(state, x, y, ang) {
	state.fx.push({ type: 'eslash', x, y, ang, t: 0, life: 0.26 });
}

export function spawnDustFX(state, x, y, n = 8) {
	for (let i = 0; i < n; i++) {
		const a = Math.random() * Math.PI * 2;
		const sp = 20 + Math.random() * 40;
		state.fx.push({ type: 'dust', x, y, vx: Math.cos(a) * sp, vy: Math.sin(a) * sp, t: 0, life: 0.35 });
	}
}

export function spawnBeamFX(state, sx, sy, ex, ey) {
	state.fx.push({ type: 'beam', sx, sy, ex, ey, t: 0, life: 0.10 });
}

export function spawnBlastFX(state, x, y, r) {
	state.fx.push({ type: 'blast', x, y, r, t: 0, life: 0.22 });
}

export function updateFX(state, dt/*, bus */) {
	const arr = state.fx;
	for (let i = arr.length - 1; i >= 0; i--) {
		const f = arr[i];
		f.t += dt;
		if (typeof f.x === 'number') f.x += (f.vx || 0) * dt;
		if (typeof f.y === 'number') f.y += (f.vy || 0) * dt;
		if (f.t >= f.life) arr.splice(i, 1);
	}
}