export function createInput(win) {
  const keys = Object.create(null);
  win.addEventListener('keydown', e => { keys[e.key.toLowerCase()] = true; if (['arrowup','arrowdown','arrowleft','arrowright',' '].includes(e.key.toLowerCase())) e.preventDefault(); });
  win.addEventListener('keyup',   e => { keys[e.key.toLowerCase()] = false; });
  // aim: タッチの右スティック等による照準オーバーライド（active 時は移動方向より優先）
  const aim = { x: 0, y: 0, active: false };
  return { keys, pressed: k => !!keys[k.toLowerCase()], aim };
}