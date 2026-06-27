// webapp/js/systems/combat.js
// プレイヤー側の戦闘オーケストレータ：タイマ/移動/照準/射撃トリガと、
// 近接(melee.js)・飛翔体(projectiles.js)の更新呼び出しをまとめる。
import { norm, clamp, moveAndCollide } from './physics.js';
import { TILE } from '../core/constants.js';
import { CONFIG } from '../core/config.js';
import { forNearby } from './spatial.js';
import { canPlaceAt } from './tiles.js';
import { spawnBeamFX, spawnMuzzleFX } from './fx.js';
import { rebuildFlowField } from './flowfield.js';
import { hasLineOfSight } from './los.js';
import { hurtMob } from './combat-core.js';
import { doMelee } from './melee.js';
import { updateBullets, updateGrenades, updateEnemyBullets, updateSlashes } from './projectiles.js';

// dev-editor で編集された CONFIG.weapons の攻撃ステータスを runtime 武器に同期する。
// mag/magSize/_autoRT 等のランタイム/構造フィールドは触らず、数値ステータスのみ反映。
const LIVE_STATS = ['dmg', 'fireRate', 'spread', 'pellets'];
function liveWeapon(rw) {
  const cfg = CONFIG.weapons && CONFIG.weapons.find((c) => c.id === rw.id);
  if (cfg) for (const k of LIVE_STATS) if (typeof cfg[k] === 'number') rw[k] = cfg[k];
  return rw;
}

// 自動射撃用：射程内かつ視線の通る最寄りの敵を返す
function nearestVisibleMob(state, p, maxR) {
  let best = null, bd = maxR * maxR;
  for (const m of state.mobs) {
    if (m.hp <= 0) continue;
    const dx = m.x - p.x, dy = m.y - p.y, d2 = dx * dx + dy * dy;
    if (d2 < bd && hasLineOfSight(state, p.x, p.y, m.x, m.y)) { bd = d2; best = m; }
  }
  return best;
}

export function switchWeapon(state, idx) {
  if (idx < 0) return;
  if (!state.player.weapons[idx]) return;
  state.player.curW = idx;
}

export function reload(state, bus) {
  const p = state.player;
  const w = p.weapons[p.curW];
  if (!w) return;

  // ビームは弾倉を持たず ammoBeam を直接消費するためリロード不要
  if (w.id === 'beam' || w.magSize == null) {
    if (bus && bus.emit) bus.emit('ui:toast', 'リロード不要');
    return;
  }

  // ← 「??」は使わずフォールバックで
  const magSize = (w.magSize != null) ? w.magSize : 0;
  const magNow  = (w.mag     != null) ? w.mag     : 0;
  const need = magSize - magNow;
  if (need <= 0) return;

  const pool = w.ammoType;
  const have = (state.player.inv && state.player.inv[pool]) ? state.player.inv[pool] : 0;
  const take = Math.min(need, have);

  if (take > 0) {
    w.mag += take;
    state.player.inv[pool] = have - take;
    if (bus && bus.emit) {
      bus.emit('ui:toast', (w.name || 'Weapon') + ' リロード (' + w.mag + '/' + magSize + ')');
      bus.emit('sfx', 'reload');
    }
  }
}

export function placeWallFront(state, bus) {
  const p = state.player;
  if (p.inv.blocks <= 0) {
    bus.emit('ui:toast', '資材がありません');
    return;
  }
  const tx = Math.floor((p.x + p.facing.x * 18) / TILE);
  const ty = Math.floor((p.y + p.facing.y * 18) / TILE);
  if (canPlaceAt(state, tx, ty)) {
    state.map[ty][tx] = '#';
    const wallHp = (p.mods && p.mods.wallHp) || 70; // 築城術アップグレードで頑丈に
    state.tileHP[ty][tx] = state.tileMaxHP[ty][tx] = wallHp;
    p.inv.blocks--;
    rebuildFlowField(state); // 壁ができたら経路を即再計算
    bus.emit('ui:toast', '壁を設置');
    bus.emit('sfx', 'build');
  } else {
    bus.emit('ui:toast', 'ここには設置できません');
  }
}

export function updateCombat(state, dt, bus, input, audio) {
  const p = state.player;
  const mods = p.mods || { gunMul: 1, meleeMul: 1, fireMul: 1, moveMul: 1 };

  // 無敵時間の減衰（赤いままになるバグ防止）
  if (p.iTime > 0) { p.iTime -= dt; if (p.iTime < 0) p.iTime = 0; }
  // マズル/反動の減衰（描画用）
  if (p.muzzleT > 0) { p.muzzleT -= dt; if (p.muzzleT < 0) p.muzzleT = 0; }
  if (p.recoil > 0) { p.recoil -= dt * 28; if (p.recoil < 0) p.recoil = 0; }

  // バフ時間
  if (p.buffs.tRange > 0) { p.buffs.tRange -= dt; if (p.buffs.tRange <= 0) p.buffs.range = 1; }
  if (p.buffs.tDmg   > 0) { p.buffs.tDmg   -= dt; if (p.buffs.tDmg   <= 0) p.buffs.dmg   = 1; }
  if (p.buffs.tSpeed > 0) { p.buffs.tSpeed -= dt; if (p.buffs.tSpeed <= 0) p.buffs.speed = 1; }

  // 入力 → 移動/ダッシュ/向き（★ 速度1.2倍）
  // 移動ベクトル：タッチのアナログスティック(input.move)があれば倒し具合で速度可変、
  // 無ければキーボード(WASD/矢印)の8方向（常に最大速度）。
  let dirx = 0, diry = 0, speedScale = 0;
  if (input.move && input.move.active && (input.move.x || input.move.y)) {
    const mlen = Math.hypot(input.move.x, input.move.y);
    speedScale = Math.min(1, mlen);
    dirx = input.move.x / (mlen || 1);
    diry = input.move.y / (mlen || 1);
  } else {
    const ax = (input.pressed('a') || input.pressed('arrowleft') ? -1 : 0) + (input.pressed('d') || input.pressed('arrowright') ? 1 : 0);
    const ay = (input.pressed('w') || input.pressed('arrowup') ? -1 : 0) + (input.pressed('s') || input.pressed('arrowdown') ? 1 : 0);
    if (ax || ay) { const l = Math.hypot(ax, ay) || 1; dirx = ax / l; diry = ay / l; speedScale = 1; }
  }
  const moving = speedScale > 0;
  const dash = input.pressed('shift') && moving && p.sta > 0;
  p.isDashing = dash;

  const spd = p.baseSpeed * CONFIG.player.speedMul * p.buffs.speed * mods.moveMul * (dash ? CONFIG.player.dashMul : 1);
  let vx = dirx * spd * speedScale * dt, vy = diry * spd * speedScale * dt;
  if (moving) { p.facing.x = dirx; p.facing.y = diry; }

  // 照準の優先順位： 手動の右スティック ＞ 自動射撃のオート照準 ＞ 移動方向
  let autoFiring = false;
  const manualAim = !!(input.aim && input.aim.active && (input.aim.x || input.aim.y));
  if (manualAim) {
    const al = Math.hypot(input.aim.x, input.aim.y) || 1;
    p.facing.x = input.aim.x / al; p.facing.y = input.aim.y / al;
  } else if (input.autoFire) {
    const target = nearestVisibleMob(state, p, CONFIG.player.autoAimRange);
    if (target) {
      const dx = target.x - p.x, dy = target.y - p.y, l = Math.hypot(dx, dy) || 1;
      p.facing.x = dx / l; p.facing.y = dy / l;
      autoFiring = true; // ターゲットがいる時だけ自動発射
    }
  }
  p.sta = dash ? Math.max(0, p.sta - CONFIG.player.staDrain * dt) : Math.min(p.staMax, p.sta + CONFIG.player.staRegen * dt);

  p.vx *= Math.pow(0.001, dt); p.vy *= Math.pow(0.001, dt);
  vx += p.vx * dt; vy += p.vy * dt;
  moveAndCollide(state, p, vx, 0);
  moveAndCollide(state, p, 0, vy);

  // 近接（J）
  if (p.meleeCD > 0) p.meleeCD -= dt;
  if (input.pressed('j') && p.meleeCD <= 0) doMelee(state, bus);

  // 射撃（K）
  if (p.shootCD > 0) p.shootCD -= dt;
  let shotThisFrame = false;

  const curWeapon = p.weapons[p.curW];
  const firing = input.pressed('k') || autoFiring;
  if (firing && p.shootCD <= 0 && curWeapon) {
    // 攻撃ステータスは CONFIG.weapons から都度引く（dev-editor の編集をラン中に反映）。
    // mag/_autoRT 等のランタイム状態は runtime 側(curWeapon)に残す。
    const w = liveWeapon(curWeapon);

    if (w.id === 'beam') {
      // ビームは ammoBeam セルを1発ずつ消費
      if ((p.inv.ammoBeam || 0) <= 0) {
        bus.emit('ui:toast', 'Beam セル切れ');
      } else {
        p.shootCD = w.fireRate * mods.fireMul;
        p.inv.ammoBeam--;
        const beamDmg = w.dmg * mods.gunMul;
        const dir = norm(p.facing.x, p.facing.y), step = 6, maxL = 700;
        // まず壁までの到達点だけを ray-march で求める（タイル参照のみ・軽量）
        let x = p.x, y = p.y, ex = x, ey = y, reach = maxL;
        for (let t = 0; t < maxL; t += step) {
          x += dir.x * step; y += dir.y * step;
          const tx = Math.floor(x / TILE), ty = Math.floor(y / TILE);
          if (state.map[ty] && (state.map[ty][tx] === '#' || state.map[ty][tx] === 'D')) { reach = t; break; }
          ex = x; ey = y;
        }
        // 候補敵を1回だけ収集（線分の近傍）。各候補について、線分が AABB 内に居る
        // 区間長から「6px ステップ何回ぶん貫いたか」を解析的に求め、その回数だけ加害。
        // → 旧来の多段ヒット挙動を維持しつつ、グリッド照会(117回)を排除。
        for (const m of state.mobs) {
          if (m.hp <= 0) continue;
          // 線分 start..(reach) に対する mob 中心の射影 s と垂線距離
          const rx = m.x - p.x, ry = m.y - p.y;
          const s = rx * dir.x + ry * dir.y;            // 線分方向の位置
          if (s < -m.w / 2 || s > reach + m.w / 2) continue;
          const perp = Math.abs(rx * dir.y - ry * dir.x); // 線分からの垂線距離
          const half = Math.min(m.w, m.h) / 2;            // 軸並行AABBを帯で近似
          if (perp > half) continue;
          // AABB を貫く s 区間の長さ → 6px ステップ何回ぶんかを多段ヒットとして適用
          const enter = Math.max(0, s - half), exit = Math.min(reach, s + half);
          const ticks = Math.max(1, Math.floor((exit - enter) / step) + 1);
          for (let k = 0; k < ticks; k++) hurtMob(state, m, beamDmg, 0, 0, 0, { number: false, sparks: 2 });
        }
        spawnBeamFX(state, p.x, p.y, ex, ey);
        bus.emit('sfx', 'beam');
        shotThisFrame = true;
      }

    } else if (w.id === 'grenade') {
      if (w.mag <= 0) {
        bus.emit('ui:toast', '弾切れ - Rでリロード');
      } else {
        p.shootCD = w.fireRate * mods.fireMul; w.mag--;
        const dir = norm(p.facing.x, p.facing.y), sp = CONFIG.player.grenadeSpeed;
        state.grenades.push({ x: p.x + dir.x * 14, y: p.y + dir.y * 14, vx: dir.x * sp, vy: dir.y * sp, fuse: CONFIG.player.grenadeFuse });
        shotThisFrame = true;
      }

    } else {
      if (w.mag <= 0) {
        bus.emit('ui:toast', '弾切れ - Rでリロード');
      } else {
        p.shootCD = w.fireRate * mods.fireMul; w.mag--;
        const dir = norm(p.facing.x, p.facing.y), baseSpd = CONFIG.player.bulletSpeed, shots = w.pellets || 1;
        const bulletDmg = w.dmg * mods.gunMul;
        const aimAng = Math.atan2(dir.y, dir.x);
        for (let i = 0; i < shots; i++) {
          const ang = aimAng + (Math.random() - 0.5) * (w.spread || 0) * 2;
          const vx = Math.cos(ang) * baseSpd, vy = Math.sin(ang) * baseSpd;
          state.bullets.push({ x: p.x + Math.cos(ang) * 14, y: p.y + Math.sin(ang) * 14, vx, vy, life: 0.9, dmg: bulletDmg });
        }
        // マズルフラッシュ＋反動（描画用）。銃口位置は向きの先。
        spawnMuzzleFX(state, p.x + dir.x * 16, p.y + dir.y * 16, aimAng, w.id === 'shotgun' ? '#ffd08a' : '#fff1c0');
        p.muzzleT = 0.07; p.recoil = (w.id === 'shotgun') ? 4 : 2.5;
        bus.emit('sfx', w.id === 'mg' ? 'mg' : 'shot');
        shotThisFrame = true;
      }
    }
  }

  // ★ 自動リロード：射撃キーを離して一定時間（CONFIG.player.autoReloadDelay）経過、
  //   かつ弾が満タンでないとき予備弾から自動補充する。
  (function() {
    const w = p.weapons[p.curW];
    if (!w) return;

    const magSize = (w.magSize != null) ? w.magSize : null;
    if (w.id === 'beam' || magSize == null) { w._autoRT = 0; return; }

    if (w.mag < magSize) {
      if (shotThisFrame || input.pressed('k')) {
        w._autoRT = 0; // 射撃中はリセット
      } else {
        w._autoRT = (w._autoRT || 0) + dt;
        if (w._autoRT >= CONFIG.player.autoReloadDelay) {
          reload(state, bus);
          w._autoRT = 0;
        }
      }
    } else {
      w._autoRT = 0;
    }
  })();

  // 飛翔体・継続効果の更新（projectiles.js へ委譲）
  updateBullets(state, dt);
  updateGrenades(state, dt);
  updateEnemyBullets(state, dt);
  updateSlashes(state, dt);

  // 開発者モードのゴッドモード：HPを満タンに固定
  if (state.devGod) p.hp = p.hpMax || 100;
  p.hp = clamp(p.hp, 0, p.hpMax || 100);
}
