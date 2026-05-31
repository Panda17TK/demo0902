import { clamp } from '../systems/physics.js';
import { TILE } from '../core/constants.js';
import { roundedRect, keyGlyph, boxGlyph, medGlyph, ringGlyph, swordGlyph, boltGlyph, crateGlyph } from './glyphs.js';

export function renderFrame(ctx, canvas, state) {
  const W = canvas.width, H = canvas.height;
  ctx.clearRect(0, 0, W, H);

  // カメラ：スムーズ追従(state.cam)があれば使用、無ければプレイヤー中心
  const cxw = state.cam ? state.cam.x : state.player.x;
  const cyw = state.cam ? state.cam.y : state.player.y;
  const baseCamX = clamp(cxw - W / 2, 0, state.dim.w * TILE - W);
  const baseCamY = clamp(cyw - H / 2, 0, state.dim.h * TILE - H);
  // 画面シェイク
  let sx = 0, sy = 0;
  if (state.shake && state.shake.t > 0) {
    sx = (Math.random() * 2 - 1) * state.shake.mag;
    sy = (Math.random() * 2 - 1) * state.shake.mag;
  }
  const camX = baseCamX + sx, camY = baseCamY + sy;

  ctx.save(); ctx.translate(-camX, -camY);

  // ===== タイル =====
  for (let y = 0; y < state.dim.h; y++) {
    for (let x = 0; x < state.dim.w; x++) {
      const c = state.map[y][x];
      const px = x * TILE, py = y * TILE;
      if (c === '#') {
        ctx.fillStyle = '#1b2735'; ctx.fillRect(px, py, TILE, TILE);
        ctx.fillStyle = '#0f1620'; ctx.fillRect(px + 2, py + 2, TILE - 4, TILE - 4);
        const hp = state.tileHP[y][x], mh = state.tileMaxHP[y][x];
        if (mh !== Infinity) {
          const r = clamp(1 - hp / mh, 0, 1);
          if (r > 0) {
            ctx.strokeStyle = `rgba(200,200,220,${0.25 + 0.5 * r})`;
            ctx.beginPath();
            ctx.moveTo(px + 4, py + 6); ctx.lineTo(px + TILE - 6, py + TILE - 8);
            ctx.moveTo(px + 6, py + TILE - 6); ctx.lineTo(px + TILE - 10, py + 10);
            ctx.stroke();
          }
        }
      } else if (c === 'D') {
        ctx.fillStyle = '#3b2a1a'; ctx.fillRect(px, py, TILE, TILE);
        ctx.strokeStyle = '#6b4c2b'; ctx.strokeRect(px + 6, py + 4, TILE - 12, TILE - 8);
      } else {
        ctx.fillStyle = '#131922'; ctx.fillRect(px, py, TILE, TILE);
        ctx.fillStyle = 'rgba(255,255,255,0.03)';
        ctx.fillRect(px + ((x + y) % 2), py + ((x * 7 + y * 13) % 3), 1, 1);
      }
    }
  }

  // ===== アイテム =====
  for (const it of state.items) {
    ctx.save(); ctx.translate(it.x, it.y);
    if (it.type === 'key')       { ctx.fillStyle = '#ffd16b'; keyGlyph(ctx); }
    if (it.type === 'ammo9')     { ctx.fillStyle = '#9ad0ff'; boxGlyph(ctx, '9'); }
    if (it.type === 'ammo12')    { ctx.fillStyle = '#c9a56b'; boxGlyph(ctx, '12'); }
    if (it.type === 'ammoBeam')  { ctx.fillStyle = '#a8ceff'; boxGlyph(ctx, 'B'); }
    if (it.type === 'ammoNade')  { ctx.fillStyle = '#ffa8a8'; boxGlyph(ctx, 'G'); }
    if (it.type === 'med')       { ctx.fillStyle = '#8fffc1'; medGlyph(ctx); }
    if (it.type === 'buffRange') { ringGlyph(ctx); }
    if (it.type === 'buffMelee') { swordGlyph(ctx); }
    if (it.type === 'buffSpeed') { boltGlyph(ctx); }
    if (it.type === 'crate')     { crateGlyph(ctx); }
    ctx.restore();
  }

  // ===== 弾体 =====
  ctx.fillStyle = '#d1e7ff'; for (const b of state.bullets)  { ctx.fillRect(b.x - 2, b.y - 2, 4, 4); }
  ctx.fillStyle = '#ffb3b3'; for (const b of state.ebullets) { ctx.fillRect(b.x - 2, b.y - 2, 4, 4); }
  ctx.fillStyle = '#ffcf99'; for (const g of state.grenades) { ctx.beginPath(); ctx.arc(g.x, g.y, 4, 0, Math.PI * 2); ctx.fill(); }

  // ===== 敵 =====
  for (const m of state.mobs) {
    ctx.save(); ctx.translate(m.x, m.y);
    const isElite = (m.tier === 'midboss' || m.tier === 'boss');
    // 被弾フラッシュ
    ctx.fillStyle = (m.hitFlash > 0) ? '#ffffff' : (m.color || '#b24a4a');
    roundedRect(ctx, -m.w / 2, -m.h / 2, m.w, m.h, isElite ? 6 : 4); ctx.fill();
    // エリートは縁取り＋狂乱中グロー
    if (isElite) {
      ctx.lineWidth = m.tier === 'boss' ? 3 : 2;
      ctx.strokeStyle = (m.enrageT > 0) ? '#ff5a5a' : '#ffe08a';
      roundedRect(ctx, -m.w / 2, -m.h / 2, m.w, m.h, 6); ctx.stroke();
      ctx.lineWidth = 1;
    }
    ctx.fillStyle = '#fff'; ctx.fillRect(-4, -3, 3, 3); ctx.fillRect(1, -3, 3, 3);
    // HPバー：エリートは常時、通常はダメージ時のみ
    const mh = m.maxhp || m.hp;
    if (isElite || m.hp < mh) {
      const bw = Math.max(m.w, isElite ? 40 : m.w), r = clamp(m.hp / mh, 0, 1);
      ctx.fillStyle = 'rgba(0,0,0,0.6)'; ctx.fillRect(-bw / 2, -m.h / 2 - 7, bw, isElite ? 4 : 3);
      ctx.fillStyle = r > 0.5 ? '#7fe08a' : (r > 0.25 ? '#e0d27f' : '#e08a7f');
      ctx.fillRect(-bw / 2, -m.h / 2 - 7, bw * r, isElite ? 4 : 3);
    }
    ctx.restore();
  }

  // ===== プレイヤー =====
  ctx.save(); ctx.translate(state.player.x, state.player.y);
  const hitFlash = state.player.iTime > 0 && (Math.floor(state.player.iTime * 20) % 2 === 0);
  ctx.fillStyle = hitFlash ? '#ff9aa2' : '#7ab0ff';
  roundedRect(ctx, -state.player.w / 2, -state.player.h / 2, state.player.w, state.player.h, 5); ctx.fill();
  ctx.strokeStyle = '#cfe5ff';
  ctx.beginPath(); ctx.moveTo(0, 0); ctx.lineTo(state.player.facing.x * 14, state.player.facing.y * 14); ctx.stroke();
  ctx.restore();

  // ===== FX =====
  for (const f of state.fx) {
    const a = 1 - f.t / f.life;
    if (a <= 0) continue;
    ctx.globalAlpha = a;

    if (f.type === 'slash') {
      ctx.save();
      ctx.translate(f.x, f.y);
      ctx.rotate(f.ang);
      const r0 = 10, r1 = 46;
      const grd = ctx.createRadialGradient(0, 0, r0, 0, 0, r1);
      grd.addColorStop(0, 'rgba(200,230,255,0.6)');
      grd.addColorStop(1, 'rgba(200,230,255,0.0)');
      ctx.fillStyle = grd;
      ctx.beginPath();
      ctx.moveTo(0, 0);
      ctx.arc(0, 0, r1, -Math.PI / 2, Math.PI / 2);
      ctx.closePath();
      ctx.fill();
      ctx.restore();
    }
    else if (f.type === 'eslash') {
      ctx.save();
      ctx.translate(f.x, f.y);
      ctx.rotate(f.ang);
      const r0 = 10, r1 = 44;
      const grd = ctx.createRadialGradient(0, 0, r0, 0, 0, r1);
      grd.addColorStop(0, 'rgba(255,150,150,0.70)');
      grd.addColorStop(1, 'rgba(255,120,120,0.00)');
      ctx.fillStyle = grd;
      ctx.beginPath();
      ctx.moveTo(0, 0);
      ctx.arc(0, 0, r1, -Math.PI / 2.4, Math.PI / 2.4);
      ctx.closePath();
      ctx.fill();
      ctx.restore();
    }
    else if (f.type === 'spark') {
      ctx.fillStyle = '#e6f3ff';
      ctx.fillRect(f.x - 1, f.y - 1, 2, 2);
    }
    else if (f.type === 'dust') {
      ctx.fillStyle = 'rgba(200,200,220,0.25)';
      const r = 2 + f.t * 10;
      ctx.beginPath();
      ctx.arc(f.x, f.y, r, 0, Math.PI * 2);
      ctx.fill();
    }
    else if (f.type === 'beam') {
      ctx.strokeStyle = 'rgba(220,240,255,0.9)';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(f.sx, f.sy);
      ctx.lineTo(f.ex, f.ey);
      ctx.stroke();
      ctx.lineWidth = 1;
    }
    else if (f.type === 'blast') {
      const r = f.r * (0.6 + 0.4 * a);
      const grd = ctx.createRadialGradient(f.x, f.y, 0, f.x, f.y, r);
      grd.addColorStop(0, 'rgba(255,230,150,0.6)');
      grd.addColorStop(1, 'rgba(255,120,80,0.0)');
      ctx.fillStyle = grd;
      ctx.beginPath();
      ctx.arc(f.x, f.y, r, 0, Math.PI * 2);
      ctx.fill();
    }
    else if (f.type === 'dmg') {
      ctx.font = 'bold 14px ui-sans-serif, system-ui, sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.lineWidth = 3;
      ctx.strokeStyle = 'rgba(0,0,0,0.8)';
      ctx.fillStyle = f.crit ? '#ffd166' : '#ffffff';
      ctx.strokeText(f.text, f.x, f.y);
      ctx.fillText(f.text, f.x, f.y);
    }

    // alpha 戻す（各FXごとに）
    ctx.globalAlpha = 1;
  }

  // ===== ライティング（ビネット）=====
  // プレイヤー周辺を明るく残し、周囲をうっすら暗く（雰囲気＋視線誘導）
  ctx.globalCompositeOperation = 'multiply';
  const lg = ctx.createRadialGradient(
    state.player.x, state.player.y, 60,
    state.player.x, state.player.y, 420
  );
  lg.addColorStop(0, 'rgba(255,255,255,1)');
  lg.addColorStop(1, 'rgba(0,0,0,0.55)');
  ctx.fillStyle = lg;
  ctx.fillRect(camX, camY, W, H);
  ctx.globalCompositeOperation = 'source-over';

  ctx.restore();
}
