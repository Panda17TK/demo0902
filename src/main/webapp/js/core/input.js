export function createInput(win) {
  const keys = Object.create(null);
  win.addEventListener('keydown', e => { keys[e.key.toLowerCase()] = true; if (['arrowup','arrowdown','arrowleft','arrowright',' '].includes(e.key.toLowerCase())) e.preventDefault(); });
  win.addEventListener('keyup',   e => { keys[e.key.toLowerCase()] = false; });
  return { keys, pressed: k => !!keys[k.toLowerCase()] };
}