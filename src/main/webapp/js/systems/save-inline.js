export function save(state, storage, bus) {
	const data = {
		player: {
			x: state.player.x, y: state.player.y, hp: state.player.hp, inv: state.player.inv, curW: state.player.curW,
			weapons: state.player.weapons.map(w => ({ id: w.id, mag: w.mag })),
			buffs: state.player.buffs,
			sta: state.player.sta
		},
		map: state.map,
		tileHP: state.tileHP,
		tileMaxHP: state.tileMaxHP,
		items: state.items,
		mobs: state.mobs.map(m => ({ kind: m.kind, x: m.x, y: m.y, hp: m.hp, maxhp: m.maxhp })),
		spawner: { elapsed: state.timers.elapsed, spawnTimer: state.timers.spawn }
	};
	storage.save(data);
	bus.emit('ui:toast', 'saved');
}

export function load(state, storage, bus) {
	const raw = storage.load();
	if (!raw) { bus.emit('ui:toast', 'セーブがありません'); return; }
	try {
		const d = raw; const H = state.dim.h, W = state.dim.w;
		Object.assign(state.player, {
			x: d.player.x, y: d.player.y, hp: d.player.hp, inv: d.player.inv, curW: d.player.curW
		});
		if (d.player.buffs) state.player.buffs = d.player.buffs;
		if (typeof d.player.sta === 'number') state.player.sta = d.player.sta;

		state.map = d.map.map(row => row.slice());
		for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
			state.tileHP[y][x] = d.tileHP[y][x];
			state.tileMaxHP[y][x] = d.tileMaxHP[y][x];
		}

		state.items.length = 0;
		for (const it of d.items) {
			state.items.push(Object.assign({}, it));
		}

		state.mobs.length = 0;
		for (const m of d.mobs) {
			state.mobs.push(Object.assign({}, m));
		}

		for (const w of state.player.weapons) {
			const f = d.player.weapons.find(v => v.id === w.id);
			if (f) w.mag = f.mag;
		}

		if (d.spawner) {
			state.timers.elapsed = d.spawner.elapsed || 0;
			state.timers.spawn = d.spawner.spawnTimer || 5;
		}
		bus.emit('ui:toast', 'loaded');
	} catch (e) {
		console.error(e);
		bus.emit('ui:toast', 'ロード失敗');
	}
}
