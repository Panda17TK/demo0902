export function spawnSlashFX(state, x, y, ang) {
	state.fx.push({ type: 'slash', x, y, ang, t: 0, life: 0.30 });
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