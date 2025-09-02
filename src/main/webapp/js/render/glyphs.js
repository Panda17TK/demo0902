export function roundedRect(ctx,x,y,w,h,r){
  const rr=Math.min(r,w/2,h/2);
  ctx.beginPath();
  ctx.moveTo(x+rr,y); ctx.lineTo(x+w-rr,y); ctx.quadraticCurveTo(x+w,y,x+w,y+rr);
  ctx.lineTo(x+w,y+h-rr); ctx.quadraticCurveTo(x+w,y+h,x+w-rr,y+h);
  ctx.lineTo(x+rr,y+h); ctx.quadraticCurveTo(x,y+h,x,y+h-rr);
  ctx.lineTo(x,y+rr); ctx.quadraticCurveTo(x,y,x+rr,y); ctx.closePath();
}
export function keyGlyph(ctx){ roundedRect(ctx,-7,-3,14,6,2); ctx.fill(); ctx.fillRect(2,-1,8,2); }
export function boxGlyph(ctx,label){ roundedRect(ctx,-9,-6,18,12,2); ctx.fill(); ctx.fillStyle='#131922'; ctx.fillRect(-7,-1,14,2); ctx.fillStyle='#111'; ctx.font=`10px sans-serif`; ctx.textAlign='center'; ctx.textBaseline='middle'; ctx.fillText(label,0,-4); }
export function medGlyph(ctx){ roundedRect(ctx,-7,-5,14,10,2); ctx.fill(); ctx.fillStyle='#131922'; ctx.fillRect(-1,-4,2,8); ctx.fillRect(-4,-1,8,2); }
export function ringGlyph(ctx){ ctx.strokeStyle='#9ecbff'; ctx.lineWidth=2; ctx.beginPath(); ctx.arc(0,0,8,0,Math.PI*2); ctx.stroke(); ctx.lineWidth=1; }
export function swordGlyph(ctx){ ctx.fillStyle='#ff9aa2'; ctx.fillRect(-1,-6,2,12); ctx.fillRect(-4,-1,8,2); }
export function boltGlyph(ctx){ ctx.fillStyle='#ffe08a'; ctx.beginPath(); ctx.moveTo(-3,-6); ctx.lineTo(1,-2); ctx.lineTo(-1,2); ctx.lineTo(3,6); ctx.lineTo(-1,2); ctx.lineTo(1,-2); ctx.closePath(); ctx.fill(); }
export function crateGlyph(ctx){ ctx.fillStyle='#b48a5a'; roundedRect(ctx,-8,-6,16,12,2); ctx.fill(); ctx.fillStyle='#2a2016'; ctx.fillRect(-6,-1,12,2); }
