// webapp/js/systems/combat.js
import { norm, clamp, moveAndCollide } from './physics.js';
import { TILE } from '../core/constants.js';
import { damageTile, canPlaceAt } from './tiles.js';
import { spawnSlashFX, spawnBeamFX, spawnBlastFX, spawnSparksFX } from './fx.js';

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

  // 無限マガジン
  if (w.infiniteMag) {
    if (w.magSize != null) {
      w.mag = w.magSize;
      if (bus && bus.emit) {
        bus.emit('ui:toast', (w.name || 'Weapon') + ' リロード (' + w.mag + '/' + w.magSize + ')');
        bus.emit('sfx', 'reload');
      }
    }
    return;
  }
  if (w.id === 'beam') {
    if (bus && bus.emit) bus.emit('ui:toast', 'Beamはリロード不要');
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
    state.tileHP[ty][tx] = state.tileMaxHP[ty][tx] = 70;
    p.inv.blocks--;
    bus.emit('ui:toast', '壁を設置');
    bus.emit('sfx', 'build');
  } else {
    bus.emit('ui:toast', 'ここには設置できません');
  }
}

export function updateCombat(state, dt, bus, input, audio) {
  const p = state.player;

  // 無敵時間の減衰（赤いままになるバグ防止）
  if (p.iTime > 0) { p.iTime -= dt; if (p.iTime < 0) p.iTime = 0; }

  // バフ時間
  if (p.buffs.tRange > 0) { p.buffs.tRange -= dt; if (p.buffs.tRange <= 0) p.buffs.range = 1; }
  if (p.buffs.tDmg   > 0) { p.buffs.tDmg   -= dt; if (p.buffs.tDmg   <= 0) p.buffs.dmg   = 1; }
  if (p.buffs.tSpeed > 0) { p.buffs.tSpeed -= dt; if (p.buffs.tSpeed <= 0) p.buffs.speed = 1; }

  // 入力 → 移動/ダッシュ/向き（★ 速度1.2倍）
  const ax = (input.pressed('a') || input.pressed('arrowleft') ? -1 : 0) + (input.pressed('d') || input.pressed('arrowright') ? 1 : 0);
  const ay = (input.pressed('w') || input.pressed('arrowup') ? -1 : 0) + (input.pressed('s') || input.pressed('arrowdown') ? 1 : 0);
  const moving = !!(ax || ay);
  const dash = input.pressed('shift') && moving && p.sta > 0;
  p.isDashing = dash;

  const spd = p.baseSpeed * 1.2 * p.buffs.speed * (dash ? 2 : 1); // ← ここで1.2倍
  let len = Math.hypot(ax, ay) || 1;
  let vx = (ax / len) * spd * dt, vy = (ay / len) * spd * dt;
  if (moving) { p.facing.x = ax / len; p.facing.y = ay / len; }
  p.sta = dash ? Math.max(0, p.sta - 35 * dt) : Math.min(p.staMax, p.sta + 22 * dt);

  p.vx *= Math.pow(0.001, dt); p.vy *= Math.pow(0.001, dt);
  vx += p.vx * dt; vy += p.vy * dt;
  moveAndCollide(state, p, vx, 0);
  moveAndCollide(state, p, 0, vy);

  // 近接（J）★ 射程1.5倍
  if (p.meleeCD > 0) p.meleeCD -= dt;
  if (input.pressed('j') && p.meleeCD <= 0) {
    p.meleeCD = 0.32;
    const baseReach = 34 * 1.5; // ← 1.5倍
    const reach = baseReach * p.buffs.range;
    const arc = Math.PI;
    const faceAng = Math.atan2(p.facing.y, p.facing.x);
    bus.emit('sfx', 'melee');
    spawnSlashFX(state, p.x, p.y, faceAng);

    // 即時ヒット（敵・壁）
    const meleeDmg = Math.round(22 * p.buffs.dmg);
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
      dmg: Math.round(8 * p.buffs.dmg)
    });
  }

  // 射撃（K）
  if (p.shootCD > 0) p.shootCD -= dt;
  let shotThisFrame = false;

  if (input.pressed('k') && p.shootCD <= 0) {
    const w = p.weapons[p.curW]; if (!w) return;

    if (w.id === 'beam') {
      // ★ infiniteAmmo なら在庫を減らさない。false のときのみ ammoBeam を消費
      const needsAmmo = !w.infiniteAmmo;
      if (needsAmmo && (p.inv.ammoBeam || 0) <= 0) {
        bus.emit('ui:toast', 'Beam セル切れ');
      } else {
        p.shootCD = w.fireRate;
        if (needsAmmo) p.inv.ammoBeam--;
        const dir = norm(p.facing.x, p.facing.y), step = 6, maxL = 700;
        let x = p.x, y = p.y, ex = x, ey = y;
        for (let t = 0; t < maxL; t += step) {
          x += dir.x * step; y += dir.y * step;
          const tx = Math.floor(x / TILE), ty = Math.floor(y / TILE);
          if (state.map[ty] && (state.map[ty][tx] === '#' || state.map[ty][tx] === 'D')) break;
          for (const m of state.mobs) {
            if (m.hp > 0 && Math.abs(m.x - x) < m.w / 2 && Math.abs(m.y - y) < m.h / 2) m.hp -= w.dmg;
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
        p.shootCD = w.fireRate; w.mag--;
        const dir = norm(p.facing.x, p.facing.y), sp = 280;
        state.grenades.push({ x: p.x + dir.x * 14, y: p.y + dir.y * 14, vx: dir.x * sp, vy: dir.y * sp, fuse: 1.0 });
        shotThisFrame = true;
      }

    } else {
      if (w.mag <= 0) {
        bus.emit('ui:toast', '弾切れ - Rでリロード');
      } else {
        p.shootCD = w.fireRate; w.mag--;
        const dir = norm(p.facing.x, p.facing.y), baseSpd = 360, shots = w.pellets || 1;
        for (let i = 0; i < shots; i++) {
          const ang = Math.atan2(dir.y, dir.x) + (Math.random() - 0.5) * (w.spread || 0) * 2;
          const vx = Math.cos(ang) * baseSpd, vy = Math.sin(ang) * baseSpd;
          state.bullets.push({ x: p.x + Math.cos(ang) * 14, y: p.y + Math.sin(ang) * 14, vx, vy, life: 0.9, dmg: w.dmg });
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
        w._autoRT = (w._autoRT || 0) + dt;
        if (w._autoRT >= 2.0) {
          if (w.infiniteMag) {
            w.mag = magSize;
            if (bus && bus.emit) {
              bus.emit('ui:toast', (w.name || 'Weapon') + ' 自動リロード (' + w.mag + '/' + magSize + ')');
              bus.emit('sfx', 'reload');
            }
          } else {
            reload(state, bus);
          }
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
        m.hp -= b.dmg;
        const n = norm(m.x - b.x, m.y - b.y); m.vx += n.x * 160; m.vy += n.y * 160;
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
    b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt;
    const tx = Math.floor(b.x / TILE), ty = Math.floor(b.y / TILE);
    if ((state.map[ty] && (state.map[ty][tx] === '#' || state.map[ty][tx] === 'D')) || b.life <= 0) {
      state.ebullets.splice(i, 1);
      continue;
    }
    const hitP = (Math.abs(state.player.x - b.x) < state.player.w / 2 + 3) && (Math.abs(state.player.y - b.y) < state.player.h / 2 + 3);
    if (hitP) {
      if (p.isDashing) {
        const n = norm(b.x - p.x, b.y - p.y);
        b.vx = n.x * 260; b.vy = n.y * 260; b.life = Math.min(b.life, 0.9);
        continue;
      } else {
        if (p.iTime <= 0) {
          p.hp -= b.dmg; p.iTime = 0.8;
          const n = norm(p.x - b.x, p.y - b.y);
          p.vx += n.x * 180; p.vy += n.y * 180;
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
          m.hp -= s.dmg;
          const n = norm(m.x - s.x, m.y - s.y);
          m.vx += n.x * 140; m.vy += n.y * 140;
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

  p.hp = clamp(p.hp, 0, 100);
}

function explode(state, x, y) {
  const r = 70;
  for (const m of state.mobs) {
    const dx = m.x - x, dy = m.y - y; const d = Math.hypot(dx, dy);
    if (d < r + Math.max(m.w, m.h) / 2) {
      const fall = 1 - d / r; const dmg = Math.round(110 * fall);
      if (dmg > 0) {
        m.hp -= dmg;
        const n = norm(dx, dy); m.vx += n.x * 280 * fall; m.vy += n.y * 280 * fall;
      }
    }
  }
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
