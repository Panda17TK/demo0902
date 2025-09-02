import { clamp } from './physics.js';

export function updateItems(state, dt, bus, audio){
  const p=state.player;
  for(let i=state.items.length-1;i>=0;i--){
    const it=state.items[i];
    if(Math.abs(it.x-p.x)<(p.w/2+10) && Math.abs(it.y-p.y)<(p.h/2+10)){
      if(it.type==='key'){ p.inv.key=true; bus.emit('ui:toast','鍵を手に入れた'); bus.emit('sfx','pickup'); }
      if(it.type==='ammo9'){ const n=it.amt||12; p.inv.ammo9+=n; bus.emit('ui:toast',`9mm +${n}`); bus.emit('sfx','pickup'); }
      if(it.type==='ammo12'){ const n=it.amt||4; p.inv.ammo12+=n; bus.emit('ui:toast',`12g +${n}`); bus.emit('sfx','pickup'); }
      if(it.type==='ammoBeam'){ const n=it.amt||1; p.inv.ammoBeam+=n; bus.emit('ui:toast',`Beamセル +${n}`); bus.emit('sfx','pickup'); }
      if(it.type==='ammoNade'){ const n=it.amt||1; p.inv.ammoNade+=n; bus.emit('ui:toast',`Grenade +${n}`); bus.emit('sfx','pickup'); }
      if(it.type==='med'){ const h=it.heal||25; p.hp=clamp(p.hp+h,0,100); bus.emit('ui:toast',`体力 +${h}`); bus.emit('sfx','pickup'); }
      if(it.type==='buffRange'){ p.buffs.range=2; p.buffs.tRange=15; bus.emit('ui:toast','近接範囲 ×2（15s）'); bus.emit('sfx','pickup'); }
      if(it.type==='buffMelee'){ p.buffs.dmg=2; p.buffs.tDmg=15; bus.emit('ui:toast','近接火力 ×2（15s）'); bus.emit('sfx','pickup'); }
      if(it.type==='buffSpeed'){ p.buffs.speed=2; p.buffs.tSpeed=12; bus.emit('ui:toast','移動速度 ×2（12s）'); bus.emit('sfx','pickup'); }
      if(it.type==='crate'){ onWeaponCrate(state, bus); }
      state.items.splice(i,1);
    }
  }
}

function onWeaponCrate(state, bus){
  const want=['beam','mg','grenade'];
  const owned=new Set(state.player.weapons.map(w=>w.id));
  const todo=want.filter(id=>!owned.has(id));
  if(todo.length){
    const pick=todo[Math.floor(Math.random()*todo.length)];
    if(pick==='beam'){ state.player.weapons.push({id:'beam',name:'Beam Rifle',dmg:90,mag:Infinity,magSize:Infinity,spread:0,fireRate:2.0,ammoType:'ammoBeam'}); state.player.inv.ammoBeam+=3; bus.emit('ui:toast','新武器: Beam Rifle ＋セル+3'); }
    else if(pick==='mg'){ state.player.weapons.push({id:'mg',name:'Machine Gun',dmg:12,pellets:1,mag:40,magSize:40,spread:0.18,fireRate:0.08,ammoType:'ammo9'}); state.player.inv.ammo9+=60; bus.emit('ui:toast','新武器: Machine Gun ＋9mm+60'); }
    else { state.player.weapons.push({id:'grenade',name:'Grenade',dmg:120,mag:1,magSize:1,spread:0,fireRate:0.9,ammoType:'ammoNade'}); state.player.inv.ammoNade+=3; bus.emit('ui:toast','新武器: Grenade ＋弾×3'); }
    bus.emit('sfx','pickup');
  }else{
    const w=state.player.weapons[Math.floor(Math.random()*state.player.weapons.length)];
    state.player.inv[w.ammoType]=(state.player.inv[w.ammoType]||0)+(w.magSize||1);
    bus.emit('ui:toast',`${w.name} の弾を1マガジン分補充`); bus.emit('sfx','pickup');
  }
}
