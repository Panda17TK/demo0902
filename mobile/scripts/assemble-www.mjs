// mobile/scripts/assemble-www.mjs
// src/main/webapp の静的アセットから Capacitor 用の webDir(mobile/www) を組み立てる。
// JSP(game.jsp)を素の index.html に変換し、CTX='' でサーバ非依存にする。
// （スコア/セーブのAPIは未接続時に黙ってスキップされる作りなので、ゲーム本体はオフライン動作する）

import { promises as fs } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, '..', '..');
const webapp = path.join(repoRoot, 'src', 'main', 'webapp');
const outDir = path.join(repoRoot, 'mobile', 'www');

async function rmrf(p) { await fs.rm(p, { recursive: true, force: true }); }

async function copyInto(srcRel) {
  const src = path.join(webapp, srcRel);
  const dst = path.join(outDir, srcRel);
  await fs.cp(src, dst, { recursive: true });
}

function jspToHtml(jsp) {
  let s = jsp;
  // JSP ページディレクティブを除去
  s = s.replace(/<%@[\s\S]*?%>/g, '');
  // コンテキストパス式 → 空（同梱アセットは原点直下）
  s = s.replace(/\$\{pageContext\.request\.contextPath\}/g, '');
  s = s.replace(/<%=\s*request\.getContextPath\(\)\s*%>/g, '');
  // Service Worker 登録ブロックは APK では不要（WebView 同梱のため）。丸ごと除去。
  s = s.replace(/<script>\s*if \('serviceWorker'[\s\S]*?<\/script>/m, '');
  // 念のため CTX を空に固定
  s = s.replace(/window\.CTX\s*=\s*'[^']*';/, "window.CTX = '';");
  // 先頭の空行を整理
  return s.replace(/^\s*\n/, '');
}

async function main() {
  await rmrf(outDir);
  await fs.mkdir(outDir, { recursive: true });

  // 静的アセット（サーバ専用の WEB-INF/META-INF・JSP・sw.js は含めない）
  for (const rel of ['js', 'css', 'icons', 'manifest.webmanifest']) {
    await copyInto(rel);
  }

  // index.html を game.jsp から生成
  const jsp = await fs.readFile(path.join(webapp, 'game.jsp'), 'utf8');
  await fs.writeFile(path.join(outDir, 'index.html'), jspToHtml(jsp), 'utf8');

  console.log('assembled www/ from', path.relative(repoRoot, webapp));
}

main().catch((e) => { console.error(e); process.exit(1); });
