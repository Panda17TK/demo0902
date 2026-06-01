// webapp/js/render/fx-draw.js
// FX の描画レジストリ。`type → draw(ctx, f, a)` で登録する。
// 新しい FX を追加するときは、systems/fx.js に spawn 関数を、ここに描画関数を足すだけ。
// （renderer 本体の if 連鎖を編集する必要がない）

import { roundedRect } from './glyphs.js';

export const FX_DRAW = {
  slash(ctx, f) {
    ctx.save(); ctx.translate(f.x, f.y); ctx.rotate(f.ang);
    const grd = ctx.createRadialGradient(0, 0, 10, 0, 0, 46);
    grd.addColorStop(0, 'rgba(200,230,255,0.6)');
    grd.addColorStop(1, 'rgba(200,230,255,0.0)');
    ctx.fillStyle = grd;
    ctx.beginPath(); ctx.moveTo(0, 0); ctx.arc(0, 0, 46, -Math.PI / 2, Math.PI / 2); ctx.closePath(); ctx.fill();
    ctx.restore();
  },
  eslash(ctx, f) {
    ctx.save(); ctx.translate(f.x, f.y); ctx.rotate(f.ang);
    const grd = ctx.createRadialGradient(0, 0, 10, 0, 0, 44);
    grd.addColorStop(0, 'rgba(255,150,150,0.70)');
    grd.addColorStop(1, 'rgba(255,120,120,0.00)');
    ctx.fillStyle = grd;
    ctx.beginPath(); ctx.moveTo(0, 0); ctx.arc(0, 0, 44, -Math.PI / 2.4, Math.PI / 2.4); ctx.closePath(); ctx.fill();
    ctx.restore();
  },
  spark(ctx, f) {
    ctx.fillStyle = '#e6f3ff';
    ctx.fillRect(f.x - 1, f.y - 1, 2, 2);
  },
  dust(ctx, f) {
    ctx.fillStyle = 'rgba(200,200,220,0.25)';
    ctx.beginPath(); ctx.arc(f.x, f.y, 2 + f.t * 10, 0, Math.PI * 2); ctx.fill();
  },
  beam(ctx, f) {
    ctx.strokeStyle = 'rgba(220,240,255,0.9)';
    ctx.lineWidth = 3;
    ctx.beginPath(); ctx.moveTo(f.sx, f.sy); ctx.lineTo(f.ex, f.ey); ctx.stroke();
    ctx.lineWidth = 1;
  },
  blast(ctx, f, a) {
    const r = f.r * (0.6 + 0.4 * a);
    const grd = ctx.createRadialGradient(f.x, f.y, 0, f.x, f.y, r);
    grd.addColorStop(0, 'rgba(255,230,150,0.6)');
    grd.addColorStop(1, 'rgba(255,120,80,0.0)');
    ctx.fillStyle = grd;
    ctx.beginPath(); ctx.arc(f.x, f.y, r, 0, Math.PI * 2); ctx.fill();
  },
  dmg(ctx, f) {
    ctx.font = 'bold 14px ui-sans-serif, system-ui, sans-serif';
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    ctx.lineWidth = 3; ctx.strokeStyle = 'rgba(0,0,0,0.8)';
    ctx.fillStyle = f.crit ? '#ffd166' : '#ffffff';
    ctx.strokeText(f.text, f.x, f.y); ctx.fillText(f.text, f.x, f.y);
  },
  afterimage(ctx, f, a) {
    ctx.globalAlpha = a * 0.5;
    ctx.fillStyle = f.color || '#cfe5ff';
    roundedRect(ctx, f.x - f.w / 2, f.y - f.h / 2, f.w, f.h, 4); ctx.fill();
  },
  dodge(ctx, f, a) {
    ctx.strokeStyle = `rgba(255,255,255,${a})`;
    ctx.lineWidth = 2;
    ctx.beginPath(); ctx.arc(f.x, f.y, 10 + (1 - a) * 18, 0, Math.PI * 2); ctx.stroke();
    ctx.lineWidth = 1;
  },
  muzzle(ctx, f) {
    ctx.save(); ctx.translate(f.x, f.y); ctx.rotate(f.ang || 0);
    ctx.fillStyle = f.color || '#fff1c0';
    ctx.beginPath();
    ctx.moveTo(14, 0); ctx.lineTo(3, 4); ctx.lineTo(5, 0); ctx.lineTo(3, -4); ctx.closePath(); ctx.fill();
    ctx.beginPath(); ctx.arc(0, 0, 4, 0, Math.PI * 2); ctx.fill();
    ctx.restore();
  },
  gib(ctx, f) {
    ctx.save(); ctx.translate(f.x, f.y); ctx.rotate(f.t * 14 + (f.s || 0));
    ctx.fillStyle = f.color || '#b24a4a';
    const s = f.s || 3;
    ctx.fillRect(-s / 2, -s / 2, s, s);
    ctx.restore();
  },
  splat(ctx, f, a) {
    ctx.fillStyle = f.color || '#b24a4a';
    ctx.globalAlpha = a * 0.4;
    ctx.beginPath(); ctx.arc(f.x, f.y, f.r * (0.5 + (1 - a) * 0.5), 0, Math.PI * 2); ctx.fill();
  },
  deathflash(ctx, f, a) {
    const r = f.r * (0.4 + (1 - a) * 0.6);
    const grd = ctx.createRadialGradient(f.x, f.y, 0, f.x, f.y, r);
    grd.addColorStop(0, `rgba(255,255,255,${a})`);
    grd.addColorStop(1, 'rgba(255,255,255,0)');
    ctx.fillStyle = grd;
    ctx.beginPath(); ctx.arc(f.x, f.y, r, 0, Math.PI * 2); ctx.fill();
  },
};
