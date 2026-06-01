// webapp/js/render/enemy-sprites.js
// 敵キャラの「凝った」描画。種類ごとに固有のシルエット・目・装飾・歩行アニメを描く。
// 既存の状態オーバーレイ（回避点滅・溜めテレグラフ・HPバー）は renderer 側が担当し、
// ここでは「ボディ本体」だけを担う。原点は敵の中心、向きは m.faceX/faceY。

import { roundedRect } from './glyphs.js';

// 色ユーティリティ：#rrggbb を増減して陰影色を作る
function shade(hex, amt) {
  let c = hex.replace('#', '');
  if (c.length === 3) c = c.split('').map((x) => x + x).join('');
  const n = parseInt(c, 16);
  let r = (n >> 16) + amt, g = ((n >> 8) & 0xff) + amt, b = (n & 0xff) + amt;
  r = Math.max(0, Math.min(255, r)); g = Math.max(0, Math.min(255, g)); b = Math.max(0, Math.min(255, b));
  return `rgb(${r},${g},${b})`;
}

// 向きに応じた目のオフセット（左右と上下を少しだけ寄せる）
function eyeShift(m) {
  const fx = m.faceX || 1, fy = m.faceY || 0;
  const l = Math.hypot(fx, fy) || 1;
  return { x: (fx / l) * 3, y: (fy / l) * 2 };
}

function drawEyes(ctx, m, spread, ry, opts) {
  opts = opts || {};
  const e = eyeShift(m);
  const r = opts.r || 2.4;
  // 白目
  if (!opts.noWhite) {
    ctx.fillStyle = '#fff';
    ctx.beginPath(); ctx.arc(-spread, ry, r + 1, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(spread, ry, r + 1, 0, Math.PI * 2); ctx.fill();
  }
  // 瞳（向きに追従）
  ctx.fillStyle = opts.pupil || '#1a1f29';
  ctx.beginPath(); ctx.arc(-spread + e.x, ry + e.y, r, 0, Math.PI * 2); ctx.fill();
  ctx.beginPath(); ctx.arc(spread + e.x, ry + e.y, r, 0, Math.PI * 2); ctx.fill();
}

// 歩行の上下バウンド量（移動中だけ弾む）
function bob(m, time) {
  const px = (typeof m.prevX === 'number') ? m.prevX : m.x;
  const py = (typeof m.prevY === 'number') ? m.prevY : m.y;
  const moving = Math.hypot(m.x - px, m.y - py) > 0.05 || (m.vx * m.vx + m.vy * m.vy) > 4;
  const amp = moving ? 1.6 : 0.6;
  return Math.sin(time * 9 + (m.animSeed || 0)) * amp;
}

// ====== 種類別スプライト ======
// 各関数は (ctx, m, time, base) を受け、本体を描く。base は被弾/回避で上書きされた基調色。

function spriteZombie(ctx, m, time, base) {
  const by = bob(m, time);
  const w = m.w, h = m.h;
  ctx.save(); ctx.translate(0, by);
  // 体（やや潰れた角丸＋ギザ肩）
  ctx.fillStyle = base;
  roundedRect(ctx, -w / 2, -h / 2, w, h, 5); ctx.fill();
  // 腹のハイライト
  ctx.fillStyle = shade(m.color, 18);
  roundedRect(ctx, -w / 2 + 3, -h / 2 + 3, w - 6, h * 0.5, 4); ctx.fill();
  // 揺れる腕（前のめり）
  const sway = Math.sin(time * 9 + (m.animSeed || 0)) * 2;
  ctx.fillStyle = shade(m.color, -22);
  ctx.fillRect(-w / 2 - 2, -2 + sway, 4, 9);
  ctx.fillRect(w / 2 - 2, -2 - sway, 4, 9);
  // 目（落ちくぼんだ赤目）
  drawEyes(ctx, m, 4, -2, { r: 2.2, pupil: '#ff5a5a' });
  // 口（ジグザグ）
  ctx.strokeStyle = shade(m.color, -30); ctx.lineWidth = 1.2;
  ctx.beginPath();
  ctx.moveTo(-4, 4); ctx.lineTo(-2, 6); ctx.lineTo(0, 4); ctx.lineTo(2, 6); ctx.lineTo(4, 4);
  ctx.stroke(); ctx.lineWidth = 1;
  ctx.restore();
}

function spriteSpitter(ctx, m, time, base) {
  const w = m.w, h = m.h;
  const pulse = 1 + Math.sin(time * 6 + (m.animSeed || 0)) * 0.06; // ぷるぷる
  ctx.save(); ctx.scale(pulse, 2 - pulse);
  // 体（丸い粘体）
  ctx.fillStyle = base;
  ctx.beginPath(); ctx.ellipse(0, 0, w / 2, h / 2, 0, 0, Math.PI * 2); ctx.fill();
  // 体内のドット（毒胞子）
  ctx.fillStyle = shade(m.color, 30);
  for (let i = 0; i < 4; i++) {
    const a = (i / 4) * Math.PI * 2 + time + (m.animSeed || 0);
    ctx.beginPath(); ctx.arc(Math.cos(a) * 4, Math.sin(a) * 3, 1.4, 0, Math.PI * 2); ctx.fill();
  }
  ctx.restore();
  // 口（前方の発射口）：向きに合わせて配置
  const e = eyeShift(m);
  ctx.fillStyle = shade(m.color, -34);
  ctx.beginPath(); ctx.arc(e.x * 2.2, e.y * 2.2 + 1, 3, 0, Math.PI * 2); ctx.fill();
  // 一つ目
  drawEyes(ctx, m, 0, -3, { r: 3, pupil: '#0a0f14', noWhite: false });
}

function spriteStalker(ctx, m, time, base) {
  const w = m.w, h = m.h;
  // 体（菱形＝俊敏さ）。向きに合わせて少し回転。
  const ang = Math.atan2(m.faceY || 0, m.faceX || 1);
  ctx.save(); ctx.rotate(ang);
  ctx.fillStyle = base;
  ctx.beginPath();
  ctx.moveTo(w * 0.6, 0); ctx.lineTo(0, h * 0.42); ctx.lineTo(-w * 0.5, 0); ctx.lineTo(0, -h * 0.42);
  ctx.closePath(); ctx.fill();
  // 背中のスリット模様
  ctx.strokeStyle = shade(m.color, 36); ctx.lineWidth = 1.4;
  ctx.beginPath(); ctx.moveTo(-w * 0.3, 0); ctx.lineTo(w * 0.4, 0); ctx.stroke();
  ctx.lineWidth = 1;
  ctx.restore();
  // 鋭い眼（一文字）：前方に2つ
  const e = eyeShift(m);
  ctx.fillStyle = '#d6c2ff';
  ctx.save(); ctx.translate(e.x, e.y); ctx.rotate(ang);
  ctx.fillRect(1, -3, 5, 1.6);
  ctx.fillRect(1, 1.4, 5, 1.6);
  ctx.restore();
}

function spriteElite(ctx, m, time, base) {
  // 中ボス／ボス共通の重厚な見た目（甲殻＋複眼＋ボスは王冠）
  const w = m.w, h = m.h;
  const breathe = Math.sin(time * 3 + (m.animSeed || 0)) * 1.2;
  ctx.save(); ctx.translate(0, breathe * 0.3);
  // 胴体（甲殻）
  ctx.fillStyle = base;
  roundedRect(ctx, -w / 2, -h / 2, w, h, 8); ctx.fill();
  // 装甲プレート
  ctx.fillStyle = shade(m.color, -26);
  roundedRect(ctx, -w / 2, -h / 2, w, h * 0.32, 8); ctx.fill();
  ctx.fillStyle = shade(m.color, 20);
  roundedRect(ctx, -w / 2 + 4, h * 0.06, w - 8, h * 0.34, 5); ctx.fill();
  // トゲ（肩）
  ctx.fillStyle = shade(m.color, -40);
  for (const sx of [-1, 1]) {
    ctx.beginPath();
    ctx.moveTo(sx * (w / 2 - 2), -h / 2 + 4);
    ctx.lineTo(sx * (w / 2 + 6), -h / 2 - 2);
    ctx.lineTo(sx * (w / 2 - 2), -h / 2 + 12);
    ctx.closePath(); ctx.fill();
  }
  // 複眼（4つの光る目）
  const e = eyeShift(m);
  const eyeColor = (m.enrageT > 0) ? '#ff5a5a' : '#ffd166';
  ctx.fillStyle = eyeColor;
  for (const ex of [-5, 5]) for (const ey of [-3, 1]) {
    ctx.beginPath(); ctx.arc(ex + e.x * 0.6, ey + e.y * 0.6, 1.7, 0, Math.PI * 2); ctx.fill();
  }
  // ボスは王冠
  if (m.tier === 'boss') {
    ctx.fillStyle = '#ffd166';
    const cy = -h / 2 - 4;
    ctx.beginPath();
    ctx.moveTo(-8, cy + 6); ctx.lineTo(-8, cy);
    ctx.lineTo(-4, cy + 4); ctx.lineTo(0, cy - 2);
    ctx.lineTo(4, cy + 4); ctx.lineTo(8, cy);
    ctx.lineTo(8, cy + 6);
    ctx.closePath(); ctx.fill();
  }
  ctx.restore();
}

const SPRITES = {
  zombie: spriteZombie,
  spitter: spriteSpitter,
  stalker: spriteStalker,
};

// 敵本体を描く（renderer から呼ぶ）。base は被弾/回避で決まる基調色。
export function drawEnemyBody(ctx, m, time, base) {
  if (m.tier === 'midboss' || m.tier === 'boss') { spriteElite(ctx, m, time, base); return; }
  const fn = SPRITES[m.kind];
  if (fn) { fn(ctx, m, time, base); return; }
  // フォールバック（未知の敵）：角丸＋目
  ctx.fillStyle = base;
  roundedRect(ctx, -m.w / 2, -m.h / 2, m.w, m.h, 4); ctx.fill();
  drawEyes(ctx, m, 4, -3, {});
}
