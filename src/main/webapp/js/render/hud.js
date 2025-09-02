export function mountHUD(hud, toast, state, bus){
  function renderHUD(){
    const w=state.player.weapons[state.player.curW] || {name:'(none)',mag:0,magSize:0,id:'none'};
    const next=Math.max(0, state.timers.spawn).toFixed(1);
	const magTxt = (w.id==='beam' || w.infiniteMag) ? '∞' : `${w.mag}/${w.magSize}`;
    
    hud.innerHTML =
      `HP: ${state.player.hp}　Sta: ${Math.round(state.player.sta)}%　Key: ${state.player.inv.key?'✓':'—'}　資材: ${state.player.inv.blocks}　Zombies: ${state.mobs.length}　NextSpawn: ${next}s<br>`+
      `武器: <b>${w.name}</b>　Mag: <b>${magTxt}</b>　Reserve: 9mm=${state.player.inv.ammo9} / 12g=${state.player.inv.ammo12} / BeamCell=${state.player.inv.ammoBeam} / Nade=${state.player.inv.ammoNade}<br>`+
      `Buff: 範囲×${state.player.buffs.range} (${state.player.buffs.tRange>0?state.player.buffs.tRange.toFixed(0)+'s':''})　`+
      `近接×${state.player.buffs.dmg} (${state.player.buffs.tDmg>0?state.player.buffs.tDmg.toFixed(0)+'s':''})　`+
      `速度×${state.player.buffs.speed} (${state.player.buffs.tSpeed>0?state.player.buffs.tSpeed.toFixed(0)+'s':''})`;
  }
  setInterval(renderHUD, 100);

  bus.on('ui:toast', msg=>{
    toast.textContent=msg; toast.classList.add('show');
    clearTimeout(toast._t); toast._t=setTimeout(()=>toast.classList.remove('show'), 1200);
  });
}
