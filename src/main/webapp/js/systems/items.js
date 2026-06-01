import { clamp } from './physics.js';

export function updateItems(state, dt, bus, audio) {
	const p = state.player;
	for (let i = state.items.length - 1; i >= 0; i--) {
		const it = state.items[i];
		if (Math.abs(it.x - p.x) < (p.w / 2 + 10) && Math.abs(it.y - p.y) < (p.h / 2 + 10)) {
			if (it.type === 'key') { p.inv.key = true; bus.emit('ui:toast', '鍵を手に入れた'); bus.emit('sfx', 'pickup'); }
			if (it.type === 'ammo9') { const n = it.amt || 12; p.inv.ammo9 += n; bus.emit('ui:toast', `9mm +${n}`); bus.emit('sfx', 'pickup'); }
			if (it.type === 'ammo12') { const n = it.amt || 4; p.inv.ammo12 += n; bus.emit('ui:toast', `12g +${n}`); bus.emit('sfx', 'pickup'); }
			if (it.type === 'ammoBeam') { const n = it.amt || 1; p.inv.ammoBeam += n; bus.emit('ui:toast', `Beamセル +${n}`); bus.emit('sfx', 'pickup'); }
			if (it.type === 'ammoNade') { const n = it.amt || 1; p.inv.ammoNade += n; bus.emit('ui:toast', `Grenade +${n}`); bus.emit('sfx', 'pickup'); }
			if (it.type === 'med') { const h = it.heal || 25; p.hp = clamp(p.hp + h, 0, p.hpMax || 100); bus.emit('ui:toast', `体力 +${h}`); bus.emit('sfx', 'pickup'); }
			if (it.type === 'buffRange') { p.buffs.range = 2; p.buffs.tRange = 15; bus.emit('ui:toast', '近接範囲 ×2（15s）'); bus.emit('sfx', 'pickup'); }
			if (it.type === 'buffMelee') { p.buffs.dmg = 2; p.buffs.tDmg = 15; bus.emit('ui:toast', '近接火力 ×2（15s）'); bus.emit('sfx', 'pickup'); }
			if (it.type === 'buffSpeed') { p.buffs.speed = 2; p.buffs.tSpeed = 12; bus.emit('ui:toast', '移動速度 ×2（12s）'); bus.emit('sfx', 'pickup'); }
			if (it.type === 'crate') { onWeaponCrate(state, bus); }
			state.items.splice(i, 1);
		}
	}
}

function onWeaponCrate(state, bus) {
	// 全武器所持のため、クレートは弾薬補給（リサプライ）にする
	const inv = state.player.inv;
	inv.ammo9   = (inv.ammo9   | 0) + 40;
	inv.ammo12  = (inv.ammo12  | 0) + 8;
	inv.ammoBeam = (inv.ammoBeam | 0) + 2;
	inv.ammoNade = (inv.ammoNade | 0) + 1;
	bus.emit('ui:toast', '弾薬補給（9mm+40 / 12g+8 / Beam+2 / Nade+1）');
	bus.emit('sfx', 'pickup');
}
