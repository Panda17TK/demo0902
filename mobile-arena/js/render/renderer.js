import { clamp } from '../systems/physics.js';
import { TILE } from '../core/constants.js';
import { CONFIG } from '../core/config.js';
import { roundedRect, keyGlyph, boxGlyph, medGlyph, ringGlyph, swordGlyph, boltGlyph, crateGlyph } from './glyphs.js';
import { drawEnemyBody } from './enemy-sprites.js';
import { FX_DRAW } from './fx-draw.js';
import { verticalLinear, radialQuant, radialAtOrigin } from './grad-cache.js';

// グリフ名 → 描画関数（CONFIG.items.glyph から引く）
const GLYPH_DRAW = {
  key:   (ctx, def) => { ctx.fillStyle = def.color; keyGlyph(ctx); },
  box:   (ctx, def) => { ctx.fillStyle = def.color; boxGlyph(ctx, def.label || ''); },
  med:   (ctx, def) => { ctx.fillStyle = def.color; medGlyph(ctx); },
  ring:  (ctx) => ringGlyph(ctx),
  sword: (ctx) => swordGlyph(ctx),
  bolt:  (ctx) => boltGlyph(ctx),
  crate: (ctx) => crateGlyph(ctx),
};

// #rrggbb / #rgb → rgba(r,g,b,alpha)
function hexToRgba(hex, alpha) {
  let c = String(hex).replace('#', '');
  if (c.length === 3) c = c.split('').map((x) => x + x).join('');
  const n = parseInt(c, 16);
  return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${alpha})`;
}

// 補間描画位置：直近ステップの px0/py0 から現在位置へ alpha で線形補間。
// px0 が無い（生成直後/初回）の場合は現在位置をそのまま使う。
function rx(ent, a) { return (ent.px0 == null) ? ent.x : ent.px0 + (ent.x - ent.px0) * a; }
function ry(ent, a) { return (ent.py0 == null) ? ent.y : ent.py0 + (ent.y - ent.py0) * a; }

// 背景パララックス：奥行きの異なる2層の塵をカメラに対しゆっくり逆スクロール。
// 決定論的に配置するのでチラつかない。スクリーン空間に描画。
function drawParallax(ctx, W, H, camX, camY) {
  // ベースの暗いグラデ（縦方向・サイズ依存なのでキャッシュ）
  ctx.fillStyle = verticalLinear(ctx, H, [[0, '#0a0d12'], [1, '#0d1119']], 'parallaxBg');
  ctx.fillRect(0, 0, W, H);

  const layers = [
    { factor: 0.15, count: 40, size: 1, alpha: 0.18, span: 900 },
    { factor: 0.35, count: 28, size: 2, alpha: 0.12, span: 700 },
  ];
  for (const L of layers) {
    const ox = -camX * L.factor, oy = -camY * L.factor;
    ctx.fillStyle = `rgba(160,190,230,${L.alpha})`;
    for (let i = 0; i < L.count; i++) {
      // 決定論的な疑似乱数で点を配置
      const hx = (i * 2654435761) >>> 0, hy = (i * 40503 + 12345) >>> 0;
      let xx = ((hx % L.span) + ox) % L.span; if (xx < 0) xx += L.span;
      let yy = ((hy % L.span) + oy) % L.span; if (yy < 0) yy += L.span;
      // span を画面サイズに折り返してタイル状に敷く
      for (let tx = -L.span; tx < W + L.span; tx += L.span) {
        for (let ty = -L.span; ty < H + L.span; ty += L.span) {
          ctx.fillRect(tx + xx, ty + yy, L.size, L.size);
        }
      }
    }
  }
}

export function renderFrame(ctx, canvas, state) {
  const W = canvas.width, H = canvas.height;
  ctx.clearRect(0, 0, W, H);
  const nowMs = performance.now();   // 1フレーム1回だけ取得して各所で使い回す
  const nowS = nowMs / 1000;

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

  // ===== 背景パララックス（スクリーン空間・カメラに対しゆっくり逆移動）=====
  drawParallax(ctx, W, H, camX, camY);

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
        // 床：市松の濃淡＋目地＋決定論的な汚れ／ひび
        const checker = ((x + y) & 1) === 0;
        ctx.fillStyle = checker ? '#141b25' : '#121823';
        ctx.fillRect(px, py, TILE, TILE);
        // 目地（タイル境界の陰影）
        ctx.fillStyle = 'rgba(0,0,0,0.22)';
        ctx.fillRect(px, py, TILE, 1);
        ctx.fillRect(px, py, 1, TILE);
        ctx.fillStyle = 'rgba(255,255,255,0.02)';
        ctx.fillRect(px + 1, py + TILE - 1, TILE - 1, 1);
        // 決定論的ハッシュで斑点／ひびを散らす（毎フレーム同じ＝チラつかない）
        const h = (x * 73856093 ^ y * 19349663) >>> 0;
        if (h % 7 === 0) {
          ctx.fillStyle = 'rgba(255,255,255,0.04)';
          ctx.fillRect(px + (h % TILE), py + ((h >> 3) % TILE), 2, 2);
        }
        if (h % 23 === 0) {
          ctx.strokeStyle = 'rgba(0,0,0,0.25)'; ctx.lineWidth = 1;
          ctx.beginPath();
          ctx.moveTo(px + (h % (TILE - 8)) + 2, py + 6);
          ctx.lineTo(px + (h % (TILE - 8)) + 9, py + TILE - 6);
          ctx.stroke();
        }
      }
    }
  }

  // ===== アイテム（CONFIG.items から色/グリフを引く）=====
  const tItem = nowS;
  for (const it of state.items) {
    const def = CONFIG.items[it.type];
    if (!def) continue;
    const phase = (it.x + it.y) * 0.05;
    const bobY = Math.sin(tItem * 2.5 + phase) * 2;            // ふわふわ上下
    const pulse = 0.5 + 0.5 * Math.sin(tItem * 3 + phase);
    // 接地影
    ctx.fillStyle = 'rgba(0,0,0,0.25)';
    ctx.beginPath(); ctx.ellipse(it.x, it.y + 8, 7, 2.5, 0, 0, Math.PI * 2); ctx.fill();

    ctx.save(); ctx.translate(it.x, it.y + bobY);
    // グロー：原点中心の色固定グラデをキャッシュし、明滅は globalAlpha で表現
    const gc = def.color || '#ffffff';
    const grd = radialQuant(ctx, 16, [[0, hexToRgba(gc, 0.36)], [1, hexToRgba(gc, 0)]], 'itemGlow|' + gc);
    ctx.globalAlpha = 0.5 + 0.5 * pulse;
    ctx.fillStyle = grd; ctx.beginPath(); ctx.arc(0, 0, 16, 0, Math.PI * 2); ctx.fill();
    ctx.globalAlpha = 1;
    const draw = GLYPH_DRAW[def.glyph];
    if (draw) draw(ctx, def);
    ctx.restore();
  }

  // ===== 弾体 =====
  // プレイヤー弾：進行方向へ伸びるトレイル＋白コア
  ctx.lineCap = 'round';
  for (const b of state.bullets) {
    const sp = Math.hypot(b.vx, b.vy) || 1;
    const tx = (b.vx / sp) * 8, ty = (b.vy / sp) * 8;
    ctx.strokeStyle = 'rgba(160,210,255,0.5)'; ctx.lineWidth = 3;
    ctx.beginPath(); ctx.moveTo(b.x - tx, b.y - ty); ctx.lineTo(b.x, b.y); ctx.stroke();
    ctx.fillStyle = '#eaf4ff'; ctx.beginPath(); ctx.arc(b.x, b.y, 2, 0, Math.PI * 2); ctx.fill();
  }
  // 敵弾：脈動する赤いグロー（mine は点滅）。グラデは原点中心でキャッシュ、明滅は globalAlpha。
  // nowMs は renderFrame 冒頭で取得済み
  for (const b of state.ebullets) {
    const pulse = b.mine ? (0.5 + 0.5 * Math.sin(nowMs / 80)) : 1;
    const r = b.mine ? 6 : 4;
    ctx.save(); ctx.translate(b.x, b.y);
    const grd = radialQuant(ctx, r + 2, [[0, 'rgba(255,170,170,0.9)'], [1, 'rgba(255,80,80,0)']], 'ebGlow');
    ctx.globalAlpha = pulse;
    ctx.fillStyle = grd; ctx.beginPath(); ctx.arc(0, 0, r + 2, 0, Math.PI * 2); ctx.fill();
    ctx.globalAlpha = 1;
    ctx.fillStyle = b.mine ? '#ff6b6b' : '#ffd0d0';
    ctx.beginPath(); ctx.arc(0, 0, 2, 0, Math.PI * 2); ctx.fill();
    ctx.restore();
  }
  ctx.lineWidth = 1;
  // グレネード：点滅する信管ランプ付き
  for (const g of state.grenades) {
    ctx.fillStyle = '#5b6b3a'; ctx.beginPath(); ctx.arc(g.x, g.y, 4, 0, Math.PI * 2); ctx.fill();
    const blink = (Math.floor(nowMs / 120) % 2 === 0);
    ctx.fillStyle = blink ? '#ff5a3a' : '#7a2a1a';
    ctx.beginPath(); ctx.arc(g.x, g.y - 1, 1.6, 0, Math.PI * 2); ctx.fill();
  }

  // ===== 敵 =====
  const tEnemy = nowS;
  const A = (typeof state.alpha === 'number') ? state.alpha : 1;
  for (const m of state.mobs) {
    ctx.save(); ctx.translate(rx(m, A), ry(m, A));
    const isElite = (m.tier === 'midboss' || m.tier === 'boss');

    // 接地影（立体感）
    ctx.fillStyle = 'rgba(0,0,0,0.28)';
    ctx.beginPath();
    ctx.ellipse(0, m.h / 2 - 1, m.w * 0.46, m.h * 0.18, 0, 0, Math.PI * 2);
    ctx.fill();

    // 基調色：被弾フラッシュ／回避点滅は白
    const blink = m.dodgeT > 0 && (Math.floor(m.dodgeT * 40) % 2 === 0);
    const base = (m.hitFlash > 0 || blink) ? '#ffffff' : (m.color || '#b24a4a');
    if (m.dodgeT > 0) ctx.globalAlpha = 0.55; // 回避中は半透明（当たり判定なしを示唆）

    // 本体（種類別の凝ったスプライト）
    drawEnemyBody(ctx, m, tEnemy, base);
    ctx.globalAlpha = 1;

    // 溜め近接のテレグラフ（黄→赤のリングが収縮）
    if (m._charge) {
      const cm = (m.def && m.def.attacks) ? m.def.attacks.find(x => x.type === 'charge_melee') : null;
      const prog = 1 - Math.max(0, m._charge.t) / ((cm && cm.windup) || 0.7);
      ctx.strokeStyle = `rgba(255,${Math.round(180 - 120 * prog)},80,0.9)`;
      ctx.lineWidth = 3;
      ctx.beginPath(); ctx.arc(0, 0, m.w * (1.4 - 0.5 * prog), 0, Math.PI * 2); ctx.stroke();
      ctx.lineWidth = 1;
    }

    // HPバー：エリートは常時、通常はダメージ時のみ
    const mh = m.maxhp || m.hp;
    if (isElite || m.hp < mh) {
      const bw = Math.max(m.w, isElite ? 40 : m.w), r = clamp(m.hp / mh, 0, 1);
      const by = -m.h / 2 - (isElite ? 12 : 8);
      ctx.fillStyle = 'rgba(0,0,0,0.6)'; ctx.fillRect(-bw / 2, by, bw, isElite ? 4 : 3);
      ctx.fillStyle = r > 0.5 ? '#7fe08a' : (r > 0.25 ? '#e0d27f' : '#e08a7f');
      ctx.fillRect(-bw / 2, by, bw * r, isElite ? 4 : 3);
      // エリート名ラベル
      if (isElite && m.def && m.def.name) {
        ctx.fillStyle = '#e7ecf3';
        ctx.font = 'bold 10px ui-sans-serif, system-ui, sans-serif';
        ctx.textAlign = 'center'; ctx.textBaseline = 'bottom';
        ctx.fillText(m.def.name, 0, by - 2);
      }
    }
    ctx.restore();
  }

  // ===== プレイヤー =====
  {
    const pl = state.player;
    const ang = Math.atan2(pl.facing.y, pl.facing.x);
    const recoil = pl.recoil || 0;
    // 補間位置＋反動で本体を後方へわずかにずらす
    const ix = rx(pl, A), iy = ry(pl, A);
    const px = ix - Math.cos(ang) * recoil, py = iy - Math.sin(ang) * recoil;

    // 接地影
    ctx.fillStyle = 'rgba(0,0,0,0.3)';
    ctx.beginPath(); ctx.ellipse(ix, iy + pl.h / 2 - 1, pl.w * 0.46, pl.h * 0.18, 0, 0, Math.PI * 2); ctx.fill();

    // ダッシュ中の残像
    if (pl.isDashing) {
      ctx.globalAlpha = 0.25; ctx.fillStyle = '#7ab0ff';
      roundedRect(ctx, px - pl.facing.x * 8 - pl.w / 2, py - pl.facing.y * 8 - pl.h / 2, pl.w, pl.h, 5); ctx.fill();
      ctx.globalAlpha = 1;
    }

    ctx.save(); ctx.translate(px, py);
    // 銃身（向きの先・反動で前後）
    ctx.save(); ctx.rotate(ang);
    ctx.fillStyle = '#2b3a50';
    ctx.fillRect(6 - recoil, -2.5, 12, 5);
    ctx.fillStyle = '#1a2738';
    ctx.fillRect(15 - recoil, -1.5, 3, 3);
    // マズルフラッシュ（発射直後）
    if (pl.muzzleT > 0) {
      ctx.fillStyle = '#fff1c0';
      ctx.beginPath();
      ctx.moveTo(24 - recoil, 0); ctx.lineTo(17 - recoil, 3.5); ctx.lineTo(19 - recoil, 0); ctx.lineTo(17 - recoil, -3.5);
      ctx.closePath(); ctx.fill();
    }
    ctx.restore();

    // 本体
    const hitFlash = pl.iTime > 0 && (Math.floor(pl.iTime * 20) % 2 === 0);
    ctx.fillStyle = hitFlash ? '#ff9aa2' : '#7ab0ff';
    roundedRect(ctx, -pl.w / 2, -pl.h / 2, pl.w, pl.h, 6); ctx.fill();
    // 腹のハイライト
    ctx.fillStyle = 'rgba(255,255,255,0.18)';
    roundedRect(ctx, -pl.w / 2 + 3, -pl.h / 2 + 3, pl.w - 6, pl.h * 0.45, 4); ctx.fill();
    // 目（向きに追従）
    const ex = pl.facing.x * 3, ey = pl.facing.y * 2;
    ctx.fillStyle = '#fff';
    ctx.beginPath(); ctx.arc(-4, -2, 2.6, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(4, -2, 2.6, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#16202e';
    ctx.beginPath(); ctx.arc(-4 + ex, -2 + ey, 1.5, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(4 + ex, -2 + ey, 1.5, 0, Math.PI * 2); ctx.fill();
    ctx.restore();
  }

  // ===== FX（レジストリで type → 描画関数を引く）=====
  for (const f of state.fx) {
    const a = 1 - f.t / f.life;
    if (a <= 0) continue;
    ctx.globalAlpha = a;
    const draw = FX_DRAW[f.type];
    if (draw) draw(ctx, f, a);
    ctx.globalAlpha = 1; // 各FXごとに戻す
  }

  // ===== ライティング（ビネット）=====
  // プレイヤー周辺を明るく残し、周囲をうっすら暗く（雰囲気＋視線誘導）。
  // 原点中心の固定グラデをキャッシュし、プレイヤー位置へ translate して使う。
  ctx.globalCompositeOperation = 'multiply';
  ctx.save();
  ctx.translate(state.player.x, state.player.y);
  ctx.fillStyle = radialAtOrigin(ctx, 60, 420,
    [[0, 'rgba(255,255,255,1)'], [1, 'rgba(0,0,0,0.55)']], 'vignette');
  ctx.fillRect(camX - state.player.x, camY - state.player.y, W, H);
  ctx.restore();
  ctx.globalCompositeOperation = 'source-over';

  ctx.restore();

  // ===== スクリーン空間オーバーレイ =====
  // 被弾方向インジケータ：画面中央から見たダメージ源の方向に赤い弧を出す
  if (state.dmgMarks && state.dmgMarks.length) {
    const cx = W / 2, cy = H / 2;
    const rad = Math.min(W, H) * 0.34;
    for (const dm of state.dmgMarks) {
      const a = 1 - dm.t / dm.life;
      if (a <= 0) continue;
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(dm.ang);
      ctx.globalAlpha = a * 0.8;
      // 弧（方向を示す扇）
      const grd = ctx.createRadialGradient(rad, 0, 0, rad, 0, 60);
      grd.addColorStop(0, 'rgba(255,60,60,0.9)');
      grd.addColorStop(1, 'rgba(255,60,60,0)');
      ctx.fillStyle = grd;
      ctx.beginPath();
      ctx.arc(0, 0, rad + 18, -0.35, 0.35);
      ctx.arc(0, 0, rad - 14, 0.35, -0.35, true);
      ctx.closePath(); ctx.fill();
      ctx.restore();
    }
    ctx.globalAlpha = 1;
  }

  // 低HP時の赤いビネット（パルス）
  const pl2 = state.player;
  const hpr = (pl2.hp) / (pl2.hpMax || 100);
  if (hpr > 0 && hpr < 0.3 && !state.gameOver) {
    const pulse = 0.25 + 0.2 * Math.sin(nowMs / 220);
    const vg = ctx.createRadialGradient(W / 2, H / 2, Math.min(W, H) * 0.3, W / 2, H / 2, Math.max(W, H) * 0.62);
    vg.addColorStop(0, 'rgba(180,0,0,0)');
    vg.addColorStop(1, `rgba(180,0,0,${pulse * (1 - hpr / 0.3)})`);
    ctx.fillStyle = vg; ctx.fillRect(0, 0, W, H);
  }

  // ボス撃破キルカム：レターボックス＋テキスト
  if (state.killCam) {
    const ph = state.killCam.t / state.killCam.life; // 0→1
    const ease = Math.sin(Math.min(1, ph) * Math.PI);  // 出てから引っ込む
    const bar = Math.round(H * 0.12 * ease);
    if (bar > 0) {
      ctx.fillStyle = 'rgba(0,0,0,0.8)';
      ctx.fillRect(0, 0, W, bar);
      ctx.fillRect(0, H - bar, W, bar);
    }
    if (state.killCam.boss && ease > 0.2) {
      ctx.globalAlpha = ease;
      ctx.fillStyle = '#ffd166';
      ctx.font = 'bold 34px ui-sans-serif, system-ui, sans-serif';
      ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      ctx.fillText('BOSS DOWN', W / 2, H / 2);
      ctx.globalAlpha = 1;
    }
  }
}
