// webapp/js/core/config.js
// ゲーム全パラメータの単一の出所（Single Source of Truth）。
// 開発者モードのエディタはこの CONFIG を編集し、localStorage に永続化する。
//
// 重要: 各システムは「分割代入で値をコピー」せず、参照経由（CONFIG.xxx.yyy）で
//        読むこと。そうすればエディタの変更が次フレームから即座に反映される。

const STORAGE_KEY = 'arpg_config_v1';

// ===== 既定値（変更不可のテンプレート） =====
export const DEFAULT_CONFIG = {
  player: {
    baseSpeed: 110,
    hpMax: 100,
    staMax: 100,
    meleeDmg: 22,        // 近接の基礎ダメージ（mods.meleeMul と乗算）
    meleeReach: 51,      // 近接の射程(px)。元 34*1.5
    dashMul: 2,          // ダッシュ時の速度倍率
    iFrameMelee: 0.9,    // 被弾後の無敵時間(s)：近接
    iFrameBullet: 0.8,   // 被弾後の無敵時間(s)：弾
  },

  waves: {
    firstWaveDelay: 3.0,    // 開始直後の猶予(s)
    intermission: 4.0,      // 波間の休止(s)。カード未選択時の自動継続まで
    baseQuota: 6,           // 第1波の出現数
    quotaPerWave: 3,        // 波ごとの増加
    maxQuota: 40,
    hpScalePerWave: 0.12,   // 敵HPの波スケール（+12%/波）
    speedScalePerWave: 0.04,// 敵速度の波スケール
    liveCapBase: 8,         // 同時存在数の基準
    liveCapPerWave: 2,
    maxLiveCap: 28,
    spawnIntervalBase: 0.9, // 1体あたり出現間隔の基準
    spawnIntervalPerWave: 0.04,
    minSpawnInterval: 0.25,
    midBossEvery: 5,        // この波ごとに中ボスを1体追加
    bossEvery: 10,          // この波ごとにボスを1体追加
  },

  drops: {
    ammoMulBase: 1,         // 基礎の弾薬ドロップ倍率（mods.ammoMul と乗算）
    ammo9Chance: 0.55,
    ammo12Chance: 0.17,     // ammo9 を外した後の追加確率帯
    ammoBeamChance: 0.10,
    ammoNadeChance: 0.08,
    medChance: 0.18,
    buffSpeedChance: 0.05,
    buffMeleeChance: 0.04,
    crateChance: 0.04,
    // ボス/中ボス撃破時の追加ドロップ
    bossCrateBonus: 3,
  },

  // 恒久強化カードの効き幅（エディタで調整可能）
  upgrades: {
    gunMul: 1.25,    // 火力強化（乗算）
    fireMul: 0.85,   // 連射強化（発射間隔の乗算＝小さいほど速い）
    meleeMul: 1.35,  // 近接強化
    maxHpAdd: 25,    // 頑強（加算）
    moveMul: 1.12,   // 俊足
    ammoMul: 1.5,    // 弾薬調達
    lifestealAdd: 2, // 吸血（撃破ごとのHP回復量に加算）
    wallHpAdd: 40,   // 築城術（設置壁HPに加算）
    blocksAdd: 4,    // 築城術（資材）
  },

  // 敵アーキタイプ。tier: normal(攻撃2) / midboss(5) / boss(10)
  // attacks: 攻撃タイプ定義の配列（systems/attacks.js が解釈）。
  enemies: {
    zombie: {
      name: 'ゾンビ', tier: 'normal', color: '#b24a4a',
      hp: 55, speed: 72, w: 22, h: 22, seeRange: 240, contactKB: 220,
      attacks: [
        { type: 'melee', cd: 0.9, dmg: 10, range: 12, arc: 360 },
        { type: 'lunge', cd: 3.5, range: 90, power: 360 },
      ],
    },
    spitter: {
      name: 'スピッター', tier: 'normal', color: '#3aa06f',
      hp: 65, speed: 35, w: 22, h: 22, seeRange: 320, contactKB: 220,
      attacks: [
        { type: 'melee', cd: 3.0, dmg: 10, range: 9, arc: 90 },
        { type: 'shot', cd: 1.2, dmg: 12, speed: 220, kite: true },
      ],
    },
    // ストーカー：縮地で間合いを詰め、溜め近接で刺す。たまに回避する。
    stalker: {
      name: 'ストーカー', tier: 'normal', color: '#9a6ad0',
      hp: 60, speed: 64, w: 22, h: 22, seeRange: 340, contactKB: 200,
      dodge: { chance: 0.18, duration: 0.15, cd: 2.0 }, // 回避：18%・0.15s無敵・CD2s
      attacks: [
        { type: 'blink', cd: 3.0, maxTiles: 5, dur: 0.1, minDist: 70, standoff: 28 },
        { type: 'charge_melee', cd: 2.4, range: 40, reach: 30, windup: 0.6, dmg: 18, kb: 320 },
      ],
    },
  },
};

// ===== 実行時 CONFIG（これを各システムが参照） =====
export const CONFIG = deepClone(DEFAULT_CONFIG);

// ----- ユーティリティ -----
function deepClone(o) { return JSON.parse(JSON.stringify(o)); }

// src の値を dst に「既存キーのみ」マージ（壊れた保存データで未知キーが増えるのを防ぐ）
function mergeInto(dst, src) {
  if (!src || typeof src !== 'object') return;
  for (const k of Object.keys(dst)) {
    if (!(k in src)) continue;
    if (dst[k] && typeof dst[k] === 'object' && !Array.isArray(dst[k])) {
      mergeInto(dst[k], src[k]);
    } else {
      dst[k] = src[k];
    }
  }
}

export function loadConfig() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) mergeInto(CONFIG, JSON.parse(raw));
  } catch (_e) { /* 壊れた保存は無視して既定値 */ }
  return CONFIG;
}

export function saveConfig() {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(CONFIG)); } catch (_e) {}
}

export function resetConfig() {
  mergeInto(CONFIG, deepClone(DEFAULT_CONFIG));
  // DEFAULT に存在するキーで CONFIG 側が変質している場合に備え、丸ごと置換もしておく
  const fresh = deepClone(DEFAULT_CONFIG);
  for (const k of Object.keys(CONFIG)) delete CONFIG[k];
  Object.assign(CONFIG, fresh);
  saveConfig();
  return CONFIG;
}

export function exportConfig() { return JSON.stringify(CONFIG, null, 2); }

export function importConfig(json) {
  const obj = (typeof json === 'string') ? JSON.parse(json) : json;
  mergeInto(CONFIG, obj);
  saveConfig();
  return CONFIG;
}
