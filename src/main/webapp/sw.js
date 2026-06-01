/* ARPG サバイバル — Service Worker
 * アプリシェル（JS/CSS/アイコン）をキャッシュしてオフライン起動を可能にする。
 * API（/api/*, /controller）はキャッシュせず常にネットワークへ（スコア等は鮮度優先）。
 */
const CACHE = 'arpg-v5';

// 自身の URL からスコープ（コンテキストパス）を導出。例: /demo0902/sw.js → /demo0902/
const SCOPE = self.registration ? new URL(self.registration.scope).pathname
                                : self.location.pathname.replace(/sw\.js$/, '');

const ASSETS = [
  'css/game.css',
  'manifest.webmanifest',
  'icons/icon-192.png',
  'icons/icon-512.png',
  // JS モジュール（相対パスでスコープ起点）
  'js/main.js',
  'js/core/constants.js', 'js/core/config.js', 'js/core/events.js',
  'js/core/input.js', 'js/core/touch.js', 'js/core/settings.js',
  'js/services/audio.js', 'js/services/storage.js',
  'js/state/state.js', 'js/state/data.js', 'js/state/map.js',
  'js/state/binds.js', 'js/state/upgrades.js', 'js/state/types.js',
  'js/systems/physics.js', 'js/systems/los.js', 'js/systems/flowfield.js',
  'js/systems/spatial.js',
  'js/systems/tiles.js', 'js/systems/items.js', 'js/systems/spawner.js',
  'js/systems/combat.js', 'js/systems/fx.js', 'js/systems/ai.js',
  'js/systems/attacks.js', 'js/systems/enemies.js', 'js/systems/save-remote.js',
  'js/render/renderer.js', 'js/render/hud.js', 'js/render/overlay.js',
  'js/render/upgrades.js', 'js/render/dev-editor.js', 'js/render/glyphs.js',
  'js/render/enemy-sprites.js', 'js/render/fx-draw.js',
].map((p) => SCOPE + p);

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE).then((c) => c.addAll(ASSETS)).catch(() => {}).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);

  // API と動的ページはネットワーク優先（オフライン時のみキャッシュ）
  if (url.pathname.includes('/api/') || url.pathname.endsWith('/controller')) {
    e.respondWith(fetch(req).catch(() => caches.match(req)));
    return;
  }

  // 静的アセットは cache-first（無ければ取得してキャッシュ）
  e.respondWith(
    caches.match(req).then((hit) => hit || fetch(req).then((res) => {
      const copy = res.clone();
      caches.open(CACHE).then((c) => c.put(req, copy)).catch(() => {});
      return res;
    }).catch(() => hit))
  );
});
