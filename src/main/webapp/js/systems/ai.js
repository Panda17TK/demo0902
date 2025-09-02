import { TILE } from '../core/constants.js';
import { norm, moveAndCollide, rectInter, clamp } from './physics.js';
import { spawnEnemySlashFX } from './fx.js';

// ====== Mob factories ======
export function makeZombie(x, y) {
  return {
    kind: 'zombie',
    x, y, w: 22, h: 22,
    hp: 55, maxhp: 55,
    baseSpeed: 72,
    shootCD: 0,
    vx: 0, vy: 0,
    meleeCD: 0, bumpCD: 0,
    faceX: 1, faceY: 0,
    prevX: x, prevY: y, stuckT: 0
  };
}
export function makeSpitter(x, y) {
  return {
    kind: 'spitter',
    x, y, w: 22, h: 22,
    hp: 65, maxhp: 65,
    baseSpeed: 35,
    shootCD: 0,
    vx: 0, vy: 0,
    meleeCD: 0, bumpCD: 0,
    faceX: 1, faceY: 0,
    prevX: x, prevY: y, stuckT: 0
  };
}

// ====== Helpers ======
function isSolid(state, tx, ty) {
  if (tx < 0 || ty < 0 || tx >= state.dim.w || ty >= state.dim.h) return true;
  const c = state.map[ty][tx];
  return c === '#' || c === 'D';
}
function hasLineOfSight(state, x0, y0, x1, y1) {
  let cx = Math.floor(x0 / TILE), cy = Math.floor(y0 / TILE);
  const tx = Math.floor(x1 / TILE), ty = Math.floor(y1 / TILE);
  const dx = Math.sign(tx - cx), dy = Math.sign(ty - cy);
  const nx = Math.abs(tx - cx), ny = Math.abs(ty - cy);
  let err = nx - ny, steps = 0;
  while (!(cx === tx && cy === ty)) {
    if (isSolid(state, cx, cy)) return false;
    const e2 = 2 * err;
    if (e2 > -ny) { err -= ny; cx += dx; }
    if (e2 < nx)  { err += nx; cy += dy; }
    if (++steps > 600) break;
  }
  return true;
}
function canMelee(m, p, dist) {
  const meleeRange = (m.kind === 'spitter' ? 18 : 24);
  if (dist >= meleeRange) return false;
  if (m.kind !== 'spitter') return true;
  const mv  = norm(m.faceX || 0, m.faceY || 0);
  const toP = norm(p.x - m.x, p.y - m.y);
  const dot = mv.x * toP.x + mv.y * toP.y;
  const ang = Math.acos(clamp(dot, -1, 1));
  return ang <= Math.PI / 4; // 正面90°
}

// ====== Main ======
export function updateAI(state, dt, bus/*, audio */) {
  const p = state.player;
  const mobs = state.mobs;

  for (let i = mobs.length - 1; i >= 0; i--) {
    const m = mobs[i];
	if (m.hp <= 0) {
	  state.stats.kills = (state.stats.kills|0) + 1;
	  mobs.splice(i, 1);
	  continue;
	}

    // 減衰・クールダウン
    m.vx *= Math.pow(0.02, dt);
    m.vy *= Math.pow(0.02, dt);
    if (m.meleeCD > 0) m.meleeCD -= dt;
    if (m.bumpCD  > 0) m.bumpCD  -= dt;
    if (m.shootCD > 0) m.shootCD -= dt;

    // スタック検出
    const moved = Math.hypot(m.x - (m.prevX || m.x), m.y - (m.prevY || m.y));
    if (moved < 0.2) m.stuckT = (m.stuckT || 0) + dt; else m.stuckT = 0;

    const slow = (m.hp <= (m.maxhp || m.hp) * 0.5) ? 0.5 : 1;
    const eff  = (m.baseSpeed || 60) * slow;

    // プレイヤーとの関係
    const dx = p.x - m.x, dy = p.y - m.y;
    const dist = Math.hypot(dx, dy);
    const seeRange = (m.kind === 'spitter' ? 320 : 240);
    const see = dist < seeRange && hasLineOfSight(state, m.x, m.y, p.x, p.y);

    // ===== 分離（simple separation）=====
    let sepX = 0, sepY = 0, sepN = 0;
    const sepRadius = 24;
    for (const o of mobs) {
      if (o === m) continue;
      const dx2 = m.x - o.x, dy2 = m.y - o.y;
      const d2 = Math.hypot(dx2, dy2);
      if (d2 > 0 && d2 < sepRadius) {
        const n = norm(dx2, dy2);
        const w = (sepRadius - d2) / sepRadius;
        sepX += n.x * w; sepY += n.y * w; sepN++;
      }
    }
    if (sepN) {
      const v = norm(sepX, sepY);
      m.vx += v.x * eff * 0.5;
      m.vy += v.y * eff * 0.5;
    }

    // ===== 目的ベクトル =====
    let moveX = 0, moveY = 0;

    if (m.kind === 'spitter' && see) {
      // 射撃（低頻度）
      if (m.shootCD <= 0) {
        m.shootCD = 1.2 / slow;
        const d = norm(dx, dy); const sp = 220;
        state.ebullets.push({ x: m.x, y: m.y, vx: d.x * sp, vy: d.y * sp, life: 1.6, dmg: 12 });
        if (bus && bus.emit) bus.emit('sfx', 'shot');
      }
      // 後退
      const away = norm(dx, dy);
      moveX += -away.x * eff * dt;
      moveY += -away.y * eff * dt;
      if (away.x || away.y) { m.faceX = -away.x; m.faceY = -away.y; }
    } else {
      // フローフィールド追従
      const tx = Math.floor(m.x / TILE), ty = Math.floor(m.y / TILE);
      let here = Infinity;
      if (state.flow[ty] && typeof state.flow[ty][tx] !== 'undefined') here = state.flow[ty][tx];

      const dirs = [[1, 0], [-1, 0], [0, 1], [0, -1]];
      let best = Infinity, dirx = 0, diry = 0;
      for (let k=0;k<4;k++) {
        const nx = tx + dirs[k][0], ny = ty + dirs[k][1];
        if (nx < 0 || ny < 0 || nx >= state.dim.w || ny >= state.dim.h) continue;
        if (isSolid(state, nx, ny)) continue;
        const v = state.flow[ny] ? state.flow[ny][nx] : Infinity;
        if (v < best) { best = v; dirx = dirs[k][0]; diry = dirs[k][1]; }
      }

      if (best < here && best < Infinity) {
        const v = norm(dirx, diry);
        moveX += v.x * eff * dt;
        moveY += v.y * eff * dt;
        if (v.x || v.y) { m.faceX = v.x; m.faceY = v.y; }
      } else {
        // 到達不能や同値 → 直追い or 遊走
        if (see) {
          const v = norm(dx, dy);
          moveX += v.x * eff * dt;
          moveY += v.y * eff * dt;
          if (v.x || v.y) { m.faceX = v.x; m.faceY = v.y; }
        } else {
          const a = Math.random() * Math.PI * 2;
          const walk = m.stuckT > 0.7 ? 0.6 : 0.25;
          moveX += Math.cos(a) * eff * walk * dt;
          moveY += Math.sin(a) * eff * walk * dt;
        }
      }
    }

    // スタック脱出キック
    if (m.stuckT > 1.0) {
      const a = Math.random() * Math.PI * 2;
      m.vx += Math.cos(a) * 80;
      m.vy += Math.sin(a) * 80;
      m.stuckT = 0;
    }

    // 速度合成＋移動
    moveAndCollide(state, m, moveX + m.vx * dt, 0);
    moveAndCollide(state, m, 0,        moveY + m.vy * dt);

    // 接触ノックバック
    if (rectInter(m, p) && (!m.bumpCD || m.bumpCD <= 0)) {
      const n = norm(p.x - m.x, p.y - m.y);
      if (p.isDashing) { m.vx -= n.x * 380; m.vy -= n.y * 380; }
      else             { p.vx += n.x * 260; p.vy += n.y * 260; m.vx -= n.x * 220; m.vy -= n.y * 220; }
      m.bumpCD = 0.28;
    }

    // 近接（★ここにエフェクトも統合）
    if (m.meleeCD <= 0 && canMelee(m, p, dist)) {
      const ang = Math.atan2(p.y - m.y, p.x - m.x);
      spawnEnemySlashFX(state, m.x, m.y, ang); // 見た目

      if (p.iTime <= 0) { // 当たり判定（無敵中は入らない）
        p.hp -= 10; p.iTime = 0.9;
        const n2 = norm(p.x - m.x, p.y - m.y);
        p.vx += n2.x * 240; p.vy += n2.y * 240;
        if (bus && bus.emit) bus.emit('sfx', 'hit');
      }
      const baseCD = (m.kind === 'spitter' ? 3.0 : 0.9);
      m.meleeCD = baseCD / slow;
    }

    // 次回のスタック検出用
    m.prevX = m.x; m.prevY = m.y;
  }
}