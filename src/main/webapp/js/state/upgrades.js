// webapp/js/state/upgrades.js
// ウェーブ間に提示する恒久強化カード（ラン中ずっと持続）。
// apply(state) で player.mods 等を更新する。

export const UPGRADES = [
  {
    id: 'gun_dmg', name: '火力強化', desc: '射撃ダメージ +25%',
    apply(s) { s.player.mods.gunMul *= 1.25; },
  },
  {
    id: 'fire_rate', name: '連射強化', desc: '発射間隔 -15%',
    apply(s) { s.player.mods.fireMul *= 0.85; },
  },
  {
    id: 'melee', name: '近接強化', desc: '近接ダメージ +35%',
    apply(s) { s.player.mods.meleeMul *= 1.35; },
  },
  {
    id: 'max_hp', name: '頑強', desc: '最大HP +25・全回復',
    apply(s) { s.player.hpMax += 25; s.player.hp = s.player.hpMax; },
  },
  {
    id: 'speed', name: '俊足', desc: '移動速度 +12%',
    apply(s) { s.player.mods.moveMul *= 1.12; },
  },
  {
    id: 'ammo', name: '弾薬調達', desc: '弾薬ドロップ +50%・即時補給',
    apply(s) {
      s.player.mods.ammoMul *= 1.5;
      const inv = s.player.inv;
      inv.ammo9 += 40; inv.ammo12 += 8; inv.ammoBeam += 2; inv.ammoNade += 1;
    },
  },
  {
    id: 'lifesteal', name: '吸血', desc: '撃破ごとに HP +2',
    apply(s) { s.player.mods.healOnKill += 2; },
  },
  {
    id: 'engineer', name: '築城術', desc: '資材 +4・壁が頑丈に',
    apply(s) { s.player.inv.blocks += 4; s.player.mods.wallHp = (s.player.mods.wallHp || 70) + 40; },
  },
];

// ランダムに n 枚（重複なし）選んで返す
export function pickUpgradeChoices(n) {
  const pool = UPGRADES.slice();
  const out = [];
  for (let i = 0; i < n && pool.length; i++) {
    const k = Math.floor(Math.random() * pool.length);
    out.push(pool.splice(k, 1)[0]);
  }
  return out;
}

export function applyUpgrade(state, id) {
  const u = UPGRADES.find((x) => x.id === id);
  if (u) u.apply(state);
  return u;
}
