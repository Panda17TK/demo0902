// webapp/js/systems/combat.js
import { norm, clamp, moveAndCollide } from './physics.js';
import { TILE } from '../core/constants.js';
import { CONFIG } from '../core/config.js';
import { damageTile, canPlaceAt } from './tiles.js';
import { spawnSlashFX, spawnBeamFX, spawnBlastFX, spawnSparksFX, spawnDamageNumber, addShake, addHitstop } from './fx.js';
import { rebuildFlowField } from './flowfield.js';
import { hasLineOfSight } from './los.js';

// 敵にダメージ＋ノックバック＋被弾フラッシュ＋（任意で）ダメージ数字
// nx,ny: 与える側→敵への方向（正規化済み）。kb: ノックバック強度。opts.number: 数字表示。
function hurtMob(state, m, dmg, nx, ny, kb, opts) {
  opts = opts || {};
  m.hp -= dmg;
  if (kb) { m.vx += (nx || 0) * kb; m.vy += (ny || 0) * kb; }
  m.hitFlash = 0.12;
  spawnSparksFX(state, m.x, m.y, opts.sparks != null ? opts.sparks : 5);
  if (opts.number !== false) spawnDamageNumber(state, m.x, m.y, dmg, { crit: !!opts.crit });
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

// 扇形ヒット判定（近接の継続ヒット＆弾相殺用）
function pointInFan(px, py, s) {
  const dx = px - s.x, dy = py - s.y;
  const d = Math.hypot(dx, dy);
  if (d > s.reach) return false;
  const ang = Math.atan2(dy, dx);
  const a = Math.abs(((ang - s.ang) + Math.PI * 3) % (Math.PI * 2) - Math.PI);
  return a <= s.arc * 0.5;
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

  const spd = p.baseSpeed * 1.2 * p.buffs.speed * mods.moveMul * (dash ? CONFIG.player.dashMul : 1);
  let vx = dirx * spd * speedScale * dt, vy = diry * spd * speedScale * dt;
  if (moving) { p.facing.x = dirx; p.facing.y = diry; }

  // 照準の優先順位： 手動の右スティック ＞ 自動射撃のオート照準 ＞ 移動方向
  let autoFiring = false;
  const manualAim = !!(input.aim && input.aim.active && (input.aim.x || input.aim.y));
  if (manualAim) {
    const al = Math.hypot(input.aim.x, input.aim.y) || 1;
    p.facing.x = input.aim.x / al; p.facing.y = input.aim.y / al;
  } else if (input.autoFire) {
    const target = nearestVisibleMob(state, p, 480);
    if (target) {
      const dx = target.x - p.x, dy = target.y - p.y, l = Math.hypot(dx, dy) || 1;
      p.facing.x = dx / l; p.facing.y = dy / l;
      autoFiring = true; // ターゲットがいる時だけ自動発射
    }
  }
  p.sta = dash ? Math.max(0, p.sta - 35 * dt) : Math.min(p.staMax, p.sta + 22 * dt);

  p.vx *= Math.pow(0.001, dt); p.vy *= Math.pow(0.001, dt);
  vx += p.vx * dt; vy += p.vy * dt;
  moveAndCollide(state, p, vx, 0);
  moveAndCollide(state, p, 0, vy);

  // 近接（J）★ 射程1.5倍
  if (p.meleeCD > 0) p.meleeCD -= dt;
  if (input.pressed('j') && p.meleeCD <= 0) {
    p.meleeCD = 0.32;
    const reach = CONFIG.player.meleeReach * p.buffs.range;
    const arc = Math.PI;
    const faceAng = Math.atan2(p.facing.y, p.facing.x);
    bus.emit('sfx', 'melee');
    spawnSlashFX(state, p.x, p.y, faceAng);

    // 即時ヒット（敵・壁）
    const meleeDmg = Math.round(CONFIG.player.meleeDmg * p.buffs.dmg * mods.meleeMul);
    for (const m of state.mobs) {
      if (m.hp <= 0) continue;
      const dx = m.x - p.x, dy = m.y - p.y; const d = Math.hypot(dx, dy);
      if (d < reach + Math.max(m.w, m.h) / 2) {
        const ang = Math.atan2(dy, dx) - faceAng;
        const a = Math.abs((ang + Math.PI * 3) % (Math.PI * 2) - Math.PI);
        if (a <= arc / 2) {
          m.hp -= meleeDmg;
          const n = norm(dx, dy); m.vx += n.x * 240; m.vy += n.y * 240;
        }
      }
    }
    const ftx = Math.floor((p.x + p.facing.x * 22) / TILE);
    const fty = Math.floor((p.y + p.facing.y * 22) / TILE);
    for (let oy = -1; oy <= 1; oy++) {
      for (let ox = -1; ox <= 1; ox++) {
        const tx = ftx + ox, ty = fty + oy;
        if (tx < 0 || ty < 0 || tx >= state.dim.w || ty >= state.dim.h) continue;
        if (state.map[ty][tx] === '#') damageTile(state, tx, ty, meleeDmg);
      }
    }

    // 持続ヒット（弱）＆ 敵弾の相殺（reachは上の値を流用＝1.5倍）
    state.slashes.push({
      x: p.x, y: p.y, ang: faceAng,
      t: 0, life: 0.30, reach: reach, arc: arc,
      tickInt: 0.07, tick: 0,
      dmg: Math.round(8 * p.buffs.dmg * mods.meleeMul)
    });
  }

  // 射撃（K）
  if (p.shootCD > 0) p.shootCD -= dt;
  let shotThisFrame = false;

  const curWeapon = p.weapons[p.curW];
  const firing = input.pressed('k') || autoFiring;
  if (firing && p.shootCD <= 0 && curWeapon) {
    const w = curWeapon;

    if (w.id === 'beam') {
      // ビームは ammoBeam セルを1発ずつ消費
      if ((p.inv.ammoBeam || 0) <= 0) {
        bus.emit('ui:toast', 'Beam セル切れ');
      } else {
        p.shootCD = w.fireRate * mods.fireMul;
        p.inv.ammoBeam--;
        const beamDmg = w.dmg * mods.gunMul;
        const dir = norm(p.facing.x, p.facing.y), step = 6, maxL = 700;
        let x = p.x, y = p.y, ex = x, ey = y;
        for (let t = 0; t < maxL; t += step) {
          x += dir.x * step; y += dir.y * step;
          const tx = Math.floor(x / TILE), ty = Math.floor(y / TILE);
          if (state.map[ty] && (state.map[ty][tx] === '#' || state.map[ty][tx] === 'D')) break;
          for (const m of state.mobs) {
            if (m.hp > 0 && Math.abs(m.x - x) < m.w / 2 && Math.abs(m.y - y) < m.h / 2) {
              hurtMob(state, m, beamDmg, 0, 0, 0, { number: false, sparks: 2 }); // ビームは連続ヒットのため数字省略
            }
          }
          ex = x; ey = y;
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
        const dir = norm(p.facing.x, p.facing.y), sp = 280;
        state.grenades.push({ x: p.x + dir.x * 14, y: p.y + dir.y * 14, vx: dir.x * sp, vy: dir.y * sp, fuse: 1.0 });
        shotThisFrame = true;
      }

    } else {
      if (w.mag <= 0) {
        bus.emit('ui:toast', '弾切れ - Rでリロード');
      } else {
        p.shootCD = w.fireRate * mods.fireMul; w.mag--;
        const dir = norm(p.facing.x, p.facing.y), baseSpd = 360, shots = w.pellets || 1;
        const bulletDmg = w.dmg * mods.gunMul;
        for (let i = 0; i < shots; i++) {
          const ang = Math.atan2(dir.y, dir.x) + (Math.random() - 0.5) * (w.spread || 0) * 2;
          const vx = Math.cos(ang) * baseSpd, vy = Math.sin(ang) * baseSpd;
          state.bullets.push({ x: p.x + Math.cos(ang) * 14, y: p.y + Math.sin(ang) * 14, vx, vy, life: 0.9, dmg: bulletDmg });
        }
        bus.emit('sfx', w.id === 'mg' ? 'mg' : 'shot');
        shotThisFrame = true;
      }
    }
  }

  // ★ 自動リロード：射撃キーを離してから2秒経過、かつ弾が満タンでない
  (function() {
    const w = p.weapons[p.curW];
    if (!w) return;

    const magSize = (w.magSize != null) ? w.magSize : null;
    if (w.id === 'beam' || magSize == null) { w._autoRT = 0; return; }

    if (w.mag < magSize) {
      if (shotThisFrame || input.pressed('k')) {
        w._autoRT = 0; // 射撃中はリセット
      } else {
        // 射撃を止めて2秒経過したら、予備弾から自動リロード
        w._autoRT = (w._autoRT || 0) + dt;
        if (w._autoRT >= 2.0) {
          reload(state, bus);
          w._autoRT = 0;
        }
      }
    } else {
      w._autoRT = 0;
    }
  })();

  // プレイヤー弾
  for (let i = state.bullets.length - 1; i >= 0; i--) {
    const b = state.bullets[i];
    b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt;
    const tx = Math.floor(b.x / TILE), ty = Math.floor(b.y / TILE);
    if (state.map[ty] && (state.map[ty][tx] === '#' || state.map[ty][tx] === 'D')) {
      damageTile(state, tx, ty, b.dmg);
      state.bullets.splice(i, 1);
      continue;
    }
    let hit = false;
    for (const m of state.mobs) {
      if (m.hp > 0 && Math.abs(m.x - b.x) < m.w / 2 && Math.abs(m.y - b.y) < m.h / 2) {
        const n = norm(m.x - b.x, m.y - b.y);
        hurtMob(state, m, b.dmg, n.x, n.y, 160, { number: true });
        hit = true; break;
      }
    }
    if (hit || b.life <= 0) state.bullets.splice(i, 1);
  }

  // グレネード
  for (let i = state.grenades.length - 1; i >= 0; i--) {
    const g = state.grenades[i];
    g.x += g.vx * dt; g.y += g.vy * dt; g.fuse -= dt;
    const tx = Math.floor(g.x / TILE), ty = Math.floor(g.y / TILE);
    if ((state.map[ty] && (state.map[ty][tx] === '#' || state.map[ty][tx] === 'D')) || g.fuse <= 0) {
      explode(state, g.x, g.y);
      state.grenades.splice(i, 1);
      continue;
    }
  }

  // 敵弾
  for (let i = state.ebullets.length - 1; i >= 0; i--) {
    const b = state.ebullets[i];
    // ホーミング弾：プレイヤー方向へ少しずつ旋回
    if (b.homing) {
      const want = Math.atan2(p.y - b.y, p.x - b.x);
      const cur = Math.atan2(b.vy, b.vx);
      let diff = ((want - cur + Math.PI * 3) % (Math.PI * 2)) - Math.PI;
      const turn = Math.max(-b.homing * dt, Math.min(b.homing * dt, diff));
      const sp = Math.hypot(b.vx, b.vy) || 1;
      const na = cur + turn;
      b.vx = Math.cos(na) * sp; b.vy = Math.sin(na) * sp;
    }
    // 地雷：静止して起爆を待つ（移動なし）
    if (!b.mine) { b.x += b.vx * dt; b.y += b.vy * dt; }
    b.life -= dt;
    const tx = Math.floor(b.x / TILE), ty = Math.floor(b.y / TILE);
    if ((!b.mine && state.map[ty] && (state.map[ty][tx] === '#' || state.map[ty][tx] === 'D')) || b.life <= 0) {
      state.ebullets.splice(i, 1);
      continue;
    }
    const hitR = b.mine ? 14 : 3;
    const hitP = (Math.abs(state.player.x - b.x) < state.player.w / 2 + hitR) && (Math.abs(state.player.y - b.y) < state.player.h / 2 + hitR);
    if (hitP) {
      if (p.isDashing) {
        const n = norm(b.x - p.x, b.y - p.y);
        b.vx = n.x * 260; b.vy = n.y * 260; b.life = Math.min(b.life, 0.9);
        continue;
      } else {
        if (p.iTime <= 0) {
          p.hp -= b.dmg; p.iTime = CONFIG.player.iFrameBullet;
          const n = norm(p.x - b.x, p.y - b.y);
          p.vx += n.x * 180; p.vy += n.y * 180;
          addShake(state, 0.18, 6);
        }
        state.ebullets.splice(i, 1);
        continue;
      }
    }
  }

  // 近接扇形の持続処理（敵＆敵弾に適用）
  for (let i = state.slashes.length - 1; i >= 0; i--) {
    const s = state.slashes[i];
    s.t += dt; s.tick -= dt;
    if (s.tick <= 0) {
      s.tick = s.tickInt;
      // 敵に追撃
      for (const m of state.mobs) {
        if (m.hp > 0 && pointInFan(m.x, m.y, s)) {
          const n = norm(m.x - s.x, m.y - s.y);
          hurtMob(state, m, s.dmg, n.x, n.y, 140, { number: false, sparks: 2 }); // 継続ヒットは数字省略
        }
      }
      // 敵弾の相殺
      for (let j = state.ebullets.length - 1; j >= 0; j--) {
        const b = state.ebullets[j];
        if (pointInFan(b.x, b.y, s)) {
          state.ebullets.splice(j, 1);
          spawnSparksFX(state, b.x, b.y, 4);
        }
      }
    }
    if (s.t >= s.life) state.slashes.splice(i, 1);
  }

  // 開発者モードのゴッドモード：HPを満タンに固定
  if (state.devGod) p.hp = p.hpMax || 100;
  p.hp = clamp(p.hp, 0, p.hpMax || 100);
}

function explode(state, x, y) {
  const r = 70;
  for (const m of state.mobs) {
    const dx = m.x - x, dy = m.y - y; const d = Math.hypot(dx, dy);
    if (d < r + Math.max(m.w, m.h) / 2) {
      const fall = 1 - d / r; const dmg = Math.round(110 * fall);
      if (dmg > 0) {
        const n = norm(dx, dy);
        hurtMob(state, m, dmg, n.x, n.y, 280 * fall, { number: true, sparks: 8 });
      }
    }
  }
  addShake(state, 0.25, 11);
  addHitstop(state, 0.05);
  const dp = Math.hypot(state.player.x - x, state.player.y - y);
  if (dp < r * 0.7) {
    const fall = 1 - dp / (r * 0.7);
    state.player.hp -= Math.round(25 * fall);
    const n = norm(state.player.x - x, state.player.y - y);
    state.player.vx += n.x * 200 * fall; state.player.vy += n.y * 200 * fall;
  }

  const tx0 = Math.max(1, Math.floor((x - r) / TILE));
  const ty0 = Math.max(1, Math.floor((y - r) / TILE));
  const tx1 = Math.min(state.dim.w - 2, Math.floor((x + r) / TILE));
  const ty1 = Math.min(state.dim.h - 2, Math.floor((y + r) / TILE));

  for (let ty = ty0; ty <= ty1; ty++) {
    for (let tx = tx0; tx <= tx1; tx++) {
      const cx = tx * TILE + TILE / 2;
      const cy = ty * TILE + TILE / 2;
      const d = Math.hypot(cx - x, cy - y);
      if (d <= r && state.map[ty][tx] === '#') {
        damageTile(state, tx, ty, 120 * (1 - d / r));
      }
    }
  }
  spawnBlastFX(state, x, y, r);
}
