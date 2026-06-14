import { test } from 'node:test';
import assert from 'node:assert/strict';

const { shouldAutoPause } = await import('../js/services/native.js');
const { createUiState, pushOverlay } = await import('../js/core/ui-state.js');

function mkState(over) {
  return { gameOver: !!over, ui: createUiState() };
}

test('playing 中はバックグラウンドで自動ポーズする', () => {
  const s = mkState(false);
  assert.equal(shouldAutoPause(s), true);
});

test('gameover 中は自動ポーズしない', () => {
  const s = mkState(true);
  assert.equal(shouldAutoPause(s), false);
});

test('既に overlay 表示中（停止済み）は何もしない', () => {
  const s = mkState(false);
  pushOverlay(s.ui, 'settings');
  assert.equal(shouldAutoPause(s), false);
});

test('overlay を閉じて playing に戻れば再び自動ポーズ対象', () => {
  const s = mkState(false);
  pushOverlay(s.ui, 'pause');
  s.ui.overlayStack.length = 0; // 閉じた
  assert.equal(shouldAutoPause(s), true);
});

test('state 不正でも例外を投げず false', () => {
  assert.equal(shouldAutoPause(null), false);
  assert.equal(shouldAutoPause({}), true); // gameOver/stack 無し → playing 扱い
});
