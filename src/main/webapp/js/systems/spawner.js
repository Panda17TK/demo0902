import { hasLineOfSight } from './los.js';
import { makeZombie, makeSpitter } from './ai.js';
import { pickUpgradeChoices } from '../state/upgrades.js';

// ウェーブ制スポナー
//   active: その波の規定数を出し切り、敵を全滅させると intermission へ
//   intermission: 強化カードを提示（main 側のオーバーレイが選択を処理）→ 次の波

const FIRST_WAVE_DELAY = 3.0; // 開始直後の猶予
const INTERMISSION = 4.0;     // 波間の休止（カード未選択ならこの後自動継続）

// 波ごとの敵数とスピッター比率
function waveQuota(n) { return Math.min(40, 6 + (n - 1) * 3); }
function spitterRatio(n) { return Math.min(0.45, 0.15 + n * 0.03); }
// 波が進むほど敵を強化（HP・速度）
function hpScale(n) { return 1 + (n - 1) * 0.12; }
function speedScale(n) { return 1 + (n - 1) * 0.04; }

export function updateSpawner(state, dt, bus, audio) {
  const w = state.wave;
  state.timers.elapsed += dt;

  if (w.phase === 'active') {
    // 出現
    if (w.toSpawn > 0) {
      w.spawnCD -= dt;
      if (w.spawnCD <= 0 && countAlive(state) < liveCap(w.num)) {
        if (spawnOne(state, bus, w.num)) { w.toSpawn--; w.spawnCD = spawnInterval(w.num); }
        else w.spawnCD = 0.2;
      }
    }
    // 殲滅判定：未出現0かつ生存0 → インターミッションへ
    if (w.toSpawn <= 0 && countAlive(state) === 0 && state.timers.elapsed > FIRST_WAVE_DELAY) {
      enterIntermission(state, bus);
    }
  } else if (w.phase === 'intermission') {
    w.interT -= dt;
    // カードが選ばれなくても一定時間で次の波へ（選択時は main 側が startNextWave を呼ぶ）
    if (w.interT <= 0) startNextWave(state, bus);
  }
}

export function startWave(state, n) {
  const w = state.wave;
  w.num = n;
  w.phase = 'active';
  w.toSpawn = waveQuota(n);
  w.spawnCD = 0.4;
  w.choices = null;
  state.stats.wave = n;
}

export function startNextWave(state, bus) {
  startWave(state, state.wave.num + 1);
  if (bus && bus.emit) bus.emit('ui:toast', 'WAVE ' + state.wave.num);
}

function enterIntermission(state, bus) {
  const w = state.wave;
  w.phase = 'intermission';
  w.interT = INTERMISSION;
  w.choices = pickUpgradeChoices(3);
  if (bus && bus.emit) bus.emit('wave:intermission', { wave: w.num, choices: w.choices });
}

function countAlive(state) {
  let n = 0;
  for (const m of state.mobs) if (m.hp > 0) n++;
  return n;
}

// 画面上の同時存在数の上限（波が進むほど増える）
function liveCap(n) { return Math.min(28, 8 + n * 2); }
function spawnInterval(n) { return Math.max(0.25, 0.9 - n * 0.04); }

function tileFree(state, tx, ty) {
  if (tx < 0 || ty < 0 || tx >= state.dim.w || ty >= state.dim.h) return false;
  if (state.map[ty][tx] !== '.') return false;
  const cx = (tx + 0.5) * 32, cy = (ty + 0.5) * 32; const box = { x: cx, y: cy, w: 32 * 0.9, h: 32 * 0.9 };
  if (Math.abs(box.x - state.player.x) < (box.w / 2 + state.player.w / 2) && Math.abs(box.y - state.player.y) < (box.h / 2 + state.player.h / 2)) return false;
  for (const m of state.mobs) { if (Math.abs(box.x - m.x) < (box.w / 2 + m.w / 2) && Math.abs(box.y - m.y) < (box.h / 2 + m.h / 2)) return false; }
  return true;
}

function spawnOne(state, bus, waveNum) {
  for (let tries = 0; tries < 160; tries++) {
    const tx = 1 + Math.floor(Math.random() * (state.dim.w - 2));
    const ty = 1 + Math.floor(Math.random() * (state.dim.h - 2));
    if (!tileFree(state, tx, ty)) continue;
    const cx = (tx + 0.5) * 32, cy = (ty + 0.5) * 32;
    const dx = cx - state.player.x, dy = cy - state.player.y;
    if (dx * dx + dy * dy < (32 * 8) * (32 * 8)) continue;
    if (hasLineOfSight(state, state.player.x, state.player.y, cx, cy)) continue;
    const isSpitter = Math.random() < spitterRatio(waveNum);
    const mob = isSpitter ? makeSpitter(cx, cy) : makeZombie(cx, cy);
    // 波スケールを適用
    const hs = hpScale(waveNum);
    mob.hp = mob.maxhp = Math.round(mob.maxhp * hs);
    mob.baseSpeed = Math.round(mob.baseSpeed * speedScale(waveNum));
    state.mobs.push(mob);
    if (bus && bus.emit) bus.emit('sfx', 'spawn');
    return true;
  }
  return false;
}
