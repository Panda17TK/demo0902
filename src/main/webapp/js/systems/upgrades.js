// webapp/js/systems/upgrades.js
// ウェーブ間に提示する恒久強化（ローグライト）。choices はUIに渡すため関数を含めない。

export const UPGRADES = [
  { id: 'hp',       name: '体力上限 +25',     desc: '最大HPを25増やしHPも回復', apply(s) { s.player.hpMax += 25; s.player.hp = Math.min(s.player.hpMax, s.player.hp + 25); } },
  { id: 'move',     name: '移動速度 +12%',     desc: '恒久的に移動が速くなる',   apply(s) { s.player.mods.moveMul *= 1.12; } },
  { id: 'gun',      name: '銃ダメージ +20%',   desc: '全銃器の与ダメージ上昇',   apply(s) { s.player.mods.gunMul *= 1.2; } },
  { id: 'melee',    name: '近接ダメージ +30%', desc: '近接の与ダメージ上昇',     apply(s) { s.player.mods.meleeMul *= 1.3; } },
  { id: 'fire',     name: '連射速度 +15%',     desc: '射撃間隔が短くなる',       apply(s) { s.player.mods.fireMul *= 0.87; } },
  { id: 'lifesteal',name: '撃破で回復 +4',     desc: '敵を倒すたびHP+4',         apply(s) { s.player.mods.healOnKill += 4; } },
  { id: 'ammo',     name: '弾薬補給',          desc: '予備弾を大量に補充',       apply(s) { const i = s.player.inv; i.ammo9 += 120; i.ammo12 += 24; i.ammoBeam += 6; i.ammoNade += 3; } },
  { id: 'magnet',   name: 'アイテム回収範囲+', desc: '落し物を遠くから拾える',   apply(s) { s.player.mods.magnet += 28; } },
  { id: 'stamina',  name: 'スタミナ +30',      desc: '最大スタミナ+30 して全快', apply(s) { s.player.staMax += 30; s.player.sta = s.player.staMax; } },
  { id: 'blocks',   name: '資材 +5',           desc: '壁の資材ストック+5',       apply(s) { s.player.inv.blocks += 5; } },
];

// UI へ渡す軽量な選択肢（重複なし）
export function rollChoices(n) {
  const pool = UPGRADES.slice();
  const out = [];
  for (let k = 0; k < n && pool.length; k++) {
    const i = Math.floor(Math.random() * pool.length);
    const u = pool.splice(i, 1)[0];
    out.push({ id: u.id, name: u.name, desc: u.desc });
  }
  return out;
}

export function applyUpgrade(state, id) {
  const u = UPGRADES.find(x => x.id === id);
  if (u) u.apply(state);
  return u;
}
