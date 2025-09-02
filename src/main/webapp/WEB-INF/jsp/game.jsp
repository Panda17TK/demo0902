<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
	isELIgnored="true"%>
<!doctype html>
<html lang="ja">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>ARPG（ノックバック接触＆敵近接）</title>
<style>
html, body {
	height: 100%;
	margin: 0;
	background: #0b0e13;
	color: #e7ecf3;
	font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto,
		Helvetica, Arial, "Apple Color Emoji", "Segoe UI Emoji";
}

#wrap {
	position: relative;
	height: 100%;
}

canvas {
	position: absolute;
	inset: 0;
	width: 100%;
	height: 100%;
	display: block;
	image-rendering: pixelated;
}

#hud {
	position: absolute;
	left: 12px;
	top: 10px;
	font-size: 14px;
	line-height: 1.35;
	background: rgba(0, 0, 0, .35);
	padding: 8px 10px;
	border-radius: 10px;
	backdrop-filter: blur(4px);
}

#help {
	position: absolute;
	right: 12px;
	top: 10px;
	font-size: 13px;
	line-height: 1.4;
	opacity: .9;
	background: rgba(0, 0, 0, .35);
	padding: 8px 10px;
	border-radius: 10px;
	max-width: min(60ch, 60vw);
}

.pill {
	display: inline-block;
	padding: 0 8px;
	border-radius: 999px;
	background: #17202a;
	border: 1px solid #273142;
	margin-right: 6px;
	font-variant: all-small-caps;
	letter-spacing: .03em;
}

a {
	color: #9ad0ff;
}

#toast {
	position: absolute;
	left: 50%;
	bottom: 16px;
	transform: translateX(-50%);
	background: rgba(0, 0, 0, .5);
	padding: 8px 12px;
	border-radius: 10px;
	font-size: 14px;
	min-width: 200px;
	text-align: center;
	opacity: 0;
	transition: opacity .25s ease;
}

#toast.show {
	opacity: 1;
}
</style>
</head>
<body>
	<div id="wrap">
		<canvas id="game" width="1280" height="720"></canvas>
		<div id="hud"></div>
		<div id="help">
			<div style="margin-bottom: 6px">
				<span class="pill">操作</span>
			</div>
			<div>
				移動: <b>WASD / 矢印</b> 近接: <b>J</b> 射撃/投擲: <b>K</b> リロード: <b>R</b>
				武器切替: <b>1/2/3/4/5</b> ドア:<b>E</b><br /> <b>ダッシュ</b>: <b>Shift</b>（スタミナ消費、<b>速度×2</b>）
				<b>壁設置</b>: <b>F</b><br /> セーブ: <b>P</b> ロード: <b>L</b> ポーズ: <b>Esc</b>
			</div>
			<div style="margin-top: 8px">
				<span class="pill">更新</span> 接触は<b>ノーダメ</b>で<b>相互ノックバック</b>／敵は<b>近接攻撃</b>（スピッターは<b>超低頻度</b>＆<b>正面90°のみ</b>）／主人公の近接は<b>多段ヒットと残像</b>で<b>スピッターより広範囲</b>／<b>近接で敵弾を相殺</b>可能。
			</div>
		</div>
		<div id="toast"></div>
	</div>

	<!-- JSP/EL を使わず scriptlet でコンテキストパスを注入（例: /demo0902） -->
	<script>const CTX = '<%=request.getContextPath()%>';</script>

	<script>
  (function(){
    "use strict";

    // ===== 基本設定 =====
    const TILE = 32;
    const DPR = Math.max(1, Math.min(2.5, window.devicePixelRatio || 1));
    const canvas = document.getElementById('game');
    const ctx = canvas.getContext('2d');
    const hud = document.getElementById('hud');
    const toast = document.getElementById('toast');

    // --- 簡易SFX ---
    const AC = (window.AudioContext || window.webkitAudioContext) ? new (window.AudioContext || window.webkitAudioContext)() : null;
    function sfx(type){
      if(!AC) return;
      const t = AC.currentTime;
      const o = AC.createOscillator();
      const g = AC.createGain();
      o.connect(g); g.connect(AC.destination);
      if(type==='shot'){o.type='square'; o.frequency.setValueAtTime(880,t); g.gain.setValueAtTime(0.2,t); g.gain.exponentialRampToValueAtTime(0.0001,t+0.08);}
      if(type==='mg'){o.type='square'; o.frequency.setValueAtTime(660,t); g.gain.setValueAtTime(0.18,t); g.gain.exponentialRampToValueAtTime(0.0001,t+0.04);}
      if(type==='beam'){o.type='triangle'; o.frequency.setValueAtTime(1200,t); g.gain.setValueAtTime(0.23,t); g.gain.exponentialRampToValueAtTime(0.0001,t+0.18);}
      if(type==='boom'){o.type='sawtooth'; o.frequency.setValueAtTime(120,t); g.gain.setValueAtTime(0.4,t); g.gain.exponentialRampToValueAtTime(0.0001,t+0.35);}
      if(type==='reload'){o.type='triangle'; o.frequency.setValueAtTime(420,t); g.gain.setValueAtTime(0.15,t); g.gain.exponentialRampToValueAtTime(0.0001,t+0.12);}
      if(type==='melee'){o.type='sawtooth'; o.frequency.setValueAtTime(220,t); g.gain.setValueAtTime(0.25,t); g.gain.exponentialRampToValueAtTime(0.0001,t+0.06);}
      if(type==='hit'){o.type='square'; o.frequency.setValueAtTime(110,t); g.gain.setValueAtTime(0.3,t); g.gain.exponentialRampToValueAtTime(0.0001,t+0.2);}
      if(type==='pickup'){o.type='triangle'; o.frequency.setValueAtTime(660,t); g.gain.setValueAtTime(0.12,t); g.gain.exponentialRampToValueAtTime(0.0001,t+0.1);}
      if(type==='door'){o.type='square'; o.frequency.setValueAtTime(300,t); g.gain.setValueAtTime(0.15,t); g.gain.exponentialRampToValueAtTime(0.0001,t+0.12);}
      if(type==='build'){o.type='triangle'; o.frequency.setValueAtTime(180,t); g.gain.setValueAtTime(0.2,t); g.gain.exponentialRampToValueAtTime(0.0001,t+0.15);}
      if(type==='break'){o.type='square'; o.frequency.setValueAtTime(140,t); g.gain.setValueAtTime(0.28,t); g.gain.exponentialRampToValueAtTime(0.0001,t+0.18);}
      if(type==='spawn'){o.type='sine'; o.frequency.setValueAtTime(260,t); g.gain.setValueAtTime(0.18,t); g.gain.exponentialRampToValueAtTime(0.0001,t+0.22);}
      o.start(t); o.stop(t+0.25);
    }

    // --- ランタイムエラートースト ---
    addEventListener('error', ev => {
      try{ console.error(ev.error || ev.message); }catch(_){}
      showToast('Error: ' + (ev.message || 'unknown'));
    });
    addEventListener('unhandledrejection', ev => {
      try{ console.error('Promise rejection:', ev.reason); }catch(_){}
      showToast('Async Error');
    });
    function showToast(msg){
      if(!toast) return;
      toast.textContent = msg;
      toast.classList.add('show');
      setTimeout(()=>toast.classList.remove('show'), 3000);
    }

    // --- リサイズ(DPR) ---
    function fitCanvas(){
      const w = canvas.clientWidth * DPR;
      const h = canvas.clientHeight * DPR;
      if(canvas.width!==w || canvas.height!==h){ canvas.width=w; canvas.height=h; }
    }
    new ResizeObserver(fitCanvas).observe(canvas);
    fitCanvas();

    // ===== マップ =====
    const MAP_RAW = [
      "##############################",
      "#..P...........#...........X.#",
      "#............................#",
      "#....######.....A......#####.#",
      "#....#....#.............#....#",
      "#....#....#....M........#....#",
      "#....#....#.............#....#",
      "#....######....#######..#....#",
      "#.............W..............#",
      "#..A.....#......Z.........V..#",
      "#........#...................#",
      "#....T...#....S..............#",
      "#........#...................#",
      "#........#....Y..............#",
      "#........###########.........#",
      "#........#.........#.........#",
      "#........#....K....#....M....#",
      "#........#.........#.....W...#",
      "#..S.....#.............Z.....#",
      "##############################",
    ];
    const MAP_H = MAP_RAW.length, MAP_W = MAP_RAW[0].length;

    // ===== エンティティ =====
    const items = [];
    const mobs  = [];
    const fx    = [];

    let player = {
      x:0,y:0,w:20,h:20, baseSpeed:120, hp:100, iTime:0, vx:0, vy:0,
      facing:{x:1,y:0}, meleeCD:0, shootCD:0,
      buffs:{range:1,dmg:1,speed:1,tRange:0,tDmg:0,tSpeed:0},
      staMax:100, sta:100,
      inv:{key:false, ammo9:36, ammo12:4, ammoBeam:2, ammoNade:0, blocks:2},
      weapons:[
        {id:'pistol',  name:'Pistol',  dmg:28, pellets:1, mag:6, magSize:6, spread:0.04, fireRate:0.22, ammoType:'ammo9'},
        {id:'shotgun', name:'Shotgun', dmg:18, pellets:6, mag:2, magSize:2, spread:0.25, fireRate:0.6,  ammoType:'ammo12'},
      ],
      curW:0
    };

    // マップ構築（P/Z/T/アイテム配置）
    let map = MAP_RAW.map((row,y)=>{
      const arr = row.split('');
      for(let x=0;x<arr.length;x++){
        const c = arr[x];
        const cx = (x+0.5)*TILE, cy = (y+0.5)*TILE;
        if(c==='P'){ player.x=cx; player.y=cy; arr[x]='.'; }
        else if(c==='Z'){ mobs.push(makeZombie(cx,cy)); arr[x]='.'; }
        else if(c==='T'){ mobs.push(makeSpitter(cx,cy)); arr[x]='.'; }
        else if(c==='K'){ items.push({type:'key',x:cx,y:cy}); arr[x]='.'; }
        else if(c==='A'){ items.push({type:'ammo9',x:cx,y:cy,amt:18}); arr[x]='.'; }
        else if(c==='S'){ items.push({type:'ammo12',x:cx,y:cy,amt:5}); arr[x]='.'; }
        else if(c==='M'){ items.push({type:'med',x:cx,y:cy,heal:25}); arr[x]='.'; }
        else if(c==='X'){ items.push({type:'buffRange',x:cx,y:cy}); arr[x]='.'; }
        else if(c==='Y'){ items.push({type:'buffMelee',x:cx,y:cy}); arr[x]='.'; }
        else if(c==='V'){ items.push({type:'buffSpeed',x:cx,y:cy}); arr[x]='.'; }
        else if(c==='W'){ items.push({type:'crate',x:cx,y:cy}); arr[x]='.'; }
      }
      return arr;
    });

    // 壁HP
    const tileHP    = Array.from({length:MAP_H},()=>Array(MAP_W).fill(Infinity));
    const tileMaxHP = Array.from({length:MAP_H},()=>Array(MAP_W).fill(Infinity));
    function isBorder(tx,ty){ return tx===0||ty===0||tx===MAP_W-1||ty===MAP_H-1; }
    function setWall(tx,ty,hp){ map[ty][tx]='#'; tileHP[ty][tx]=hp; tileMaxHP[ty][tx]=hp; }
    function clearWall(tx,ty){ map[ty][tx]='.'; tileHP[ty][tx]=Infinity; tileMaxHP[ty][tx]=Infinity; }
    for(let y=0;y<MAP_H;y++) for(let x=0;x<MAP_W;x++){
      if(map[y][x]==='#'){ if(isBorder(x,y)){ tileHP[y][x]=tileMaxHP[y][x]=Infinity; } else { tileHP[y][x]=tileMaxHP[y][x]=90; } }
    }

    function makeZombie(x,y){ return {kind:'zombie',x,y,w:22,h:22,hp:55,maxhp:55,baseSpeed:72,shootCD:0,vx:0,vy:0,meleeCD:0,bumpCD:0}; }
    function makeSpitter(x,y){ return {kind:'spitter',x,y,w:22,h:22,hp:65,maxhp:65,baseSpeed:35,shootCD:0,vx:0,vy:0,meleeCD:0,bumpCD:0}; }

    // ===== 入力 =====
    const keys = Object.create(null);
    addEventListener('keydown', e=>{
      if(["ArrowUp","ArrowDown","ArrowLeft","ArrowRight"," "].includes(e.key)) e.preventDefault();
      keys[e.key.toLowerCase()] = true;
    });
    addEventListener('keyup', e=>{ keys[e.key.toLowerCase()] = false; });

    // ===== 便利関数 =====
    const clamp = (v,a,b)=>Math.max(a,Math.min(b,v));
    function rectInter(a,b){ return Math.abs(a.x-b.x)<(a.w+b.w)/2 && Math.abs(a.y-b.y)<(a.h+b.h)/2; }
    function solidAt(tx,ty){ if(tx<0||ty<0||tx>=MAP_W||ty>=MAP_H) return '#'; return map[ty][tx]; }
    function isSolidChar(c){ return c==='#' || c==='D'; }
    function forTiles(aabb,cb){
      const x0=Math.floor((aabb.x-aabb.w/2)/TILE), x1=Math.floor((aabb.x+aabb.w/2)/TILE);
      const y0=Math.floor((aabb.y-aabb.h/2)/TILE), y1=Math.floor((aabb.y+aabb.h/2)/TILE);
      for(let ty=y0;ty<=y1;ty++) for(let tx=x0;tx<=x1;tx++){ cb(tx,ty,solidAt(tx,ty)); }
    }
    function moveAndCollide(ent,dx,dy){
      ent.x+=dx; let hitX=false;
      forTiles(ent,(tx,ty,c)=>{
        if(isSolidChar(c)){
          const left=tx*TILE, right=left+TILE;
          if(ent.x-ent.w/2<right && ent.x+ent.w/2>left && ent.y+ent.h/2>ty*TILE && ent.y-ent.h/2<(ty+1)*TILE){
            if(dx>0) ent.x=left-ent.w/2, hitX=true; else if(dx<0) ent.x=right+ent.w/2, hitX=true;
          }
        }
      });
      ent.y+=dy; let hitY=false;
      forTiles(ent,(tx,ty,c)=>{
        if(isSolidChar(c)){
          const top=ty*TILE, bottom=top+TILE;
          if(ent.x+ent.w/2>tx*TILE && ent.x-ent.w/2<(tx+1)*TILE && ent.y-ent.h/2<bottom && ent.y+ent.h/2>top){
            if(dy>0) ent.y=top-ent.h/2, hitY=true; else if(dy<0) ent.y=bottom+ent.h/2, hitY=true;
          }
        }
      });
      return {hitX,hitY};
    }
    function norm(x,y){ const l=Math.hypot(x,y)||1; return {x:x/l,y:y/l}; }

    // ===== 弾など =====
    const bullets=[], ebullets=[], grenades=[], slashes=[];
    function pointInFan(px,py,s){
      const dx=px-s.x, dy=py-s.y; const d=Math.hypot(dx,dy);
      if(d>s.reach) return false;
      const ang=Math.atan2(dy,dx);
      const a=Math.abs(((ang-s.ang)+Math.PI*3)%(Math.PI*2)-Math.PI);
      return a<=s.arc*0.5;
    }

    // ===== 画面揺れ =====
    let shakeTime=0, shakeMag=0;
    function addShake(t,m){ shakeTime=Math.max(shakeTime,t); shakeMag=Math.max(shakeMag,m); }

    // ===== 距離場(BFS) =====
    let distGrid = Array.from({length:MAP_H},()=>Array(MAP_W).fill(Infinity));
    let bfsTimer=0;
    function rebuildFlowField(){
      for(let y=0;y<MAP_H;y++) for(let x=0;x<MAP_W;x++) distGrid[y][x]=Infinity;
      const q=[]; const sx=Math.floor(player.x/TILE), sy=Math.floor(player.y/TILE);
      if(sx<0||sy<0||sx>=MAP_W||sy>=MAP_H) return;
      distGrid[sy][sx]=0; q.push([sx,sy]);
      while(q.length){
        const [cx,cy]=q.shift(); const d=distGrid[cy][cx];
        const nb=[[1,0],[-1,0],[0,1],[0,-1]];
        for(const [dx,dy] of nb){
          const nx=cx+dx, ny=cy+dy;
          if(nx<0||ny<0||nx>=MAP_W||ny>=MAP_H) continue;
          const c=solidAt(nx,ny); if(isSolidChar(c)) continue;
          if(distGrid[ny][nx]>d+1){ distGrid[ny][nx]=d+1; q.push([nx,ny]); }
        }
      }
    }

    // ===== タイルダメ/設置 =====
    function damageTile(tx,ty,dmg){
      if(tx<0||ty<0||tx>=MAP_W||ty>=MAP_H) return false;
      if(map[ty][tx]!=='#') return false;
      if(tileHP[ty][tx]===Infinity) return false;
      tileHP[ty][tx]-=dmg;
      spawnSparks(tx*TILE+TILE/2, ty*TILE+TILE/2, 4);
      addShake(0.08,3); sfx('break');
      if(tileHP[ty][tx]<=0){ clearWall(tx,ty); player.inv.blocks++; flash('資材 +1'); rebuildFlowField(); return true; }
      return false;
    }
    function canPlaceAt(tx,ty){
      if(tx<0||ty<0||tx>=MAP_W||ty>=MAP_H) return false;
      if(map[ty][tx]!=='.') return false;
      const cx=tx*TILE+TILE/2, cy=ty*TILE+TILE/2; const box={x:cx,y:cy,w:TILE*0.9,h:TILE*0.9};
      if(rectInter(box,player)) return false;
      for(const m of mobs){ if(rectInter(box,m)) return false; }
      return true;
    }
    function placeWallFront(){
      if(player.inv.blocks<=0){ flash('資材がありません'); return; }
      const tx=Math.floor((player.x+player.facing.x*18)/TILE), ty=Math.floor((player.y+player.facing.y*18)/TILE);
      if(canPlaceAt(tx,ty)){ setWall(tx,ty,70); player.inv.blocks--; sfx('build'); addShake(0.06,2); spawnDust(tx*TILE+TILE/2, ty*TILE+TILE/2, 8); rebuildFlowField(); flash('壁を設置'); }
      else { flash('ここには設置できません'); }
    }

    // ===== スポーン =====
    let elapsed=0, spawnTimer=5; const SPAWN_BASE=7, MOB_CAP_BASE=8;
    function updateSpawner(dt){
      elapsed+=dt; spawnTimer-=dt;
      const cap=Math.min(24, Math.floor(MOB_CAP_BASE + elapsed/28));
      const interval=Math.max(3, SPAWN_BASE - elapsed*0.02);
      if(spawnTimer<=0){
        spawnTimer=interval + (Math.random()*1.5 - 0.75);
        if(mobs.length<cap){
          const n=(mobs.length<cap-4)?3:1;
          for(let i=0;i<n;i++) spawnOne();
        }
      }
    }
    function tileFree(tx,ty){
      if(tx<0||ty<0||tx>=MAP_W||ty>=MAP_H) return false;
      if(map[ty][tx]!=='.') return false;
      const cx=(tx+0.5)*TILE, cy=(ty+0.5)*TILE; const box={x:cx,y:cy,w:TILE*0.9,h:TILE*0.9};
      if(rectInter(box,player)) return false;
      for(const m of mobs){ if(rectInter(box,m)) return false; }
      return true;
    }
    function pickSpawnTile(){
      for(let tries=0;tries<160;tries++){
        const tx=1+Math.floor(Math.random()*(MAP_W-2));
        const ty=1+Math.floor(Math.random()*(MAP_H-2));
        if(!tileFree(tx,ty)) continue;
        const cx=(tx+0.5)*TILE, cy=(ty+0.5)*TILE;
        const dx=cx-player.x, dy=cy-player.y;
        if(dx*dx+dy*dy<(TILE*10)*(TILE*10)) continue;
        if(hasLineOfSight(player.x,player.y,cx,cy)) continue;
        return {tx,ty};
      }
      return null;
    }
    function spawnOne(){
      const t=pickSpawnTile(); if(!t) return false;
      const x=(t.tx+0.5)*TILE, y=(t.ty+0.5)*TILE;
      const type=Math.random()<0.22?'spitter':'zombie';
      mobs.push(type==='spitter'?makeSpitter(x,y):makeZombie(x,y));
      spawnDust(x,y,10); sfx('spawn'); addShake(0.05,1.5);
      return true;
    }

    // ===== ドロップ =====
    function dropLoot(x,y){
      if(Math.random()<0.75){ const amt=12+Math.floor(Math.random()*13); items.push({type:'ammo9',x,y,amt}); }
      else { const amt=2+Math.floor(Math.random()*5); items.push({type:'ammo12',x,y,amt}); }
      if(Math.random()<0.25){ const extra=6+Math.floor(Math.random()*9); items.push({type:'ammo9',x:x+Math.random()*8-4,y:y+Math.random()*8-4,amt:extra});}
      if(Math.random()<0.30){ items.push({type:'med',x,y,heal:25});}
      if(Math.random()<0.07){ items.push({type:'buffSpeed',x:x+Math.random()*6-3,y:y+Math.random()*6-3});}
      if(Math.random()<0.06){ items.push({type:'buffMelee',x:x+Math.random()*6-3,y:y+Math.random()*6-3});}
      if(Math.random()<0.05){ items.push({type:'buffRange',x:x+Math.random()*6-3,y:y+Math.random()*6-3});}
      if(Math.random()<0.06){ items.push({type:'crate',x:x+Math.random()*10-5,y:y+Math.random()*10-5});}
      if(Math.random()<0.06){ items.push({type:'ammoNade',x:x+Math.random()*6-3,y:y+Math.random()*6-3,amt:1});}
      if(Math.random()<0.08){ items.push({type:'ammoBeam',x:x+Math.random()*6-3,y:y+Math.random()*6-3,amt:1});}
    }

    // ===== FX =====
    function spawnSlash(x,y,ang){
      fx.push({type:'slash',x,y,ang,t:0,life:0.30});
      slashes.push({
        x,y,ang,t:0,life:0.30,
        reach:34*player.buffs.range, arc:Math.PI,
        tickInt:0.07, tickLeft:0.0,
        dmg:Math.round(8*player.buffs.dmg)
      });
    }
    function spawnSparks(x,y,n){
      for(let i=0;i<n;i++){
        const a=Math.random()*Math.PI*2, sp=60+Math.random()*120;
        fx.push({type:'spark',x,y,vx:Math.cos(a)*sp,vy:Math.sin(a)*sp,t:0,life:0.25});
      }
    }
    function spawnDust(x,y,n){
      for(let i=0;i<n;i++){
        const a=Math.random()*Math.PI*2, sp=20+Math.random()*40;
        fx.push({type:'dust',x,y,vx:Math.cos(a)*sp,vy:Math.sin(a)*sp,t:0,life:0.35});
      }
    }
    function spawnBeam(sx,sy,ex,ey){ fx.push({type:'beam',sx,sy,ex,ey,t:0,life:0.10}); }
    function spawnBlast(x,y,r){ fx.push({type:'blast',x,y,r,t:0,life:0.22}); }

    // ===== 更新 =====
    let paused=false;
    addEventListener('keydown', e=>{
      if(e.key==='Escape') paused=!paused;
      const k=e.key.toLowerCase();
      if(k==='p'){ saveGame(); flash('saved'); }
      if(k==='l'){ loadGame(); flash('loaded'); }
      if(k==='1'){ player.curW=0; }
      if(k==='2'){ player.curW=1; }
      if(k==='3'){ const i=player.weapons.findIndex(w=>w.id==='beam'); if(i>=0) player.curW=i; }
      if(k==='4'){ const i=player.weapons.findIndex(w=>w.id==='mg'); if(i>=0) player.curW=i; }
      if(k==='5'){ const i=player.weapons.findIndex(w=>w.id==='grenade'); if(i>=0) player.curW=i; }
      if(k==='r'){ reload(); }
      if(k==='f'){ placeWallFront(); }
    });

    function reload(){
      const w=player.weapons[player.curW]; if(!w) return;
      if(w.id==='beam'){ flash('Beamはリロード不要'); return; }
      const need=w.magSize-w.mag; if(need<=0) return;
      const pool=w.ammoType; const take=Math.min(need, player.inv[pool]||0);
      if(take>0){ w.mag+=take; player.inv[pool]-=take; flash(`${w.name} リロード (${w.mag}/${w.magSize})`); sfx('reload'); }
    }

    function updateBuffs(dt){
      const b=player.buffs;
      if(b.tRange>0){ b.tRange-=dt; if(b.tRange<=0) b.range=1; }
      if(b.tDmg>0){ b.tDmg-=dt; if(b.tDmg<=0) b.dmg=1; }
      if(b.tSpeed>0){ b.tSpeed-=dt; if(b.tSpeed<=0) b.speed=1; }
    }

    function update(dt){
      if(paused) return;
      updateBuffs(dt);

      // --- 移動/ダッシュ ---
      const ax=(keys['a']||keys['arrowleft']?-1:0)+(keys['d']||keys['arrowright']?1:0);
      const ay=(keys['w']||keys['arrowup']?-1:0)+(keys['s']||keys['arrowdown']?1:0);
      const moving=!!(ax||ay);
      const dash = keys['shift'] && moving && player.sta>0;
      player.isDashing = dash;

      const spd = player.baseSpeed * player.buffs.speed * (dash?2:1);
      let len=Math.hypot(ax,ay)||1;
      let vx=(ax/len)*spd*dt, vy=(ay/len)*spd*dt;
      if(moving){ player.facing.x=ax/len; player.facing.y=ay/len; }
      if(dash) player.sta=Math.max(0, player.sta-35*dt); else player.sta=Math.min(player.staMax, player.sta+22*dt);

      player.vx *= Math.pow(0.001, dt);
      player.vy *= Math.pow(0.001, dt);
      vx += player.vx*dt; vy += player.vy*dt;
      moveAndCollide(player, vx, 0); moveAndCollide(player, 0, vy);

      // --- 近接 ---
      if(player.meleeCD>0) player.meleeCD-=dt;
      if(keys['j'] && player.meleeCD<=0){
        player.meleeCD=0.32;
        const baseReach=34, reach=baseReach*player.buffs.range, arc=Math.PI;
        const faceAng=Math.atan2(player.facing.y, player.facing.x);
        sfx('melee'); addShake(0.08,3); spawnSlash(player.x,player.y,faceAng);
        const meleeDmg=Math.round(22*player.buffs.dmg);
        // 対モブ
        for(const m of mobs){
          if(m.hp<=0) continue;
          const dx=m.x-player.x, dy=m.y-player.y; const d=Math.hypot(dx,dy);
          if(d<reach+Math.max(m.w,m.h)/2){
            const ang=Math.atan2(dy,dx)-faceAng;
            const a=Math.abs((ang+Math.PI*3)%(Math.PI*2)-Math.PI);
            if(a<=arc/2){ m.hp-=meleeDmg; const n=norm(dx,dy); m.vx+=n.x*240; m.vy+=n.y*240; spawnSparks(m.x,m.y,6); }
          }
        }
        // 対タイル
        const ftx=Math.floor((player.x+player.facing.x*22)/TILE), fty=Math.floor((player.y+player.facing.y*22)/TILE);
        for(let oy=-1;oy<=1;oy++) for(let ox=-1;ox<=1;ox++){
          const tx=ftx+ox, ty=fty+oy;
          if(tx<0||ty<0||tx>=MAP_W||ty>=MAP_H) continue;
          if(map[ty][tx]==='#') damageTile(tx,ty,meleeDmg);
        }
      }

      // --- 射撃/投擲 ---
      if(player.shootCD>0) player.shootCD-=dt;
      if(keys['k'] && player.shootCD<=0){
        const w=player.weapons[player.curW];
        if(!w){ /* no weapon */ }
        else if(w.id==='beam'){
          if((player.inv.ammoBeam||0)<=0){ flash('Beam セル切れ'); }
          else{
            player.shootCD=w.fireRate; player.inv.ammoBeam--;
            const dir=norm(player.facing.x,player.facing.y); const step=6, maxL=700;
            let x=player.x, y=player.y, ex=x, ey=y; const hit=new Set();
            for(let t=0;t<maxL;t+=step){
              x+=dir.x*step; y+=dir.y*step;
              const tx=Math.floor(x/TILE), ty=Math.floor(y/TILE);
              if(isSolidChar(solidAt(tx,ty))) break;
              for(const m of mobs){
                if(m.hp>0 && !hit.has(m) && Math.abs(m.x-x)<m.w/2 && Math.abs(m.y-y)<m.h/2){
                  m.hp-=w.dmg; hit.add(m); spawnSparks(m.x,m.y,8);
                }
              }
              ex=x; ey=y;
            }
            spawnBeam(player.x,player.y,ex,ey); sfx('beam'); addShake(0.12,8);
          }
        } else if(w.id==='grenade'){
          if(w.mag<=0){ flash('弾切れ - Rでリロード'); }
          else{
            player.shootCD=w.fireRate; w.mag--;
            const dir=norm(player.facing.x,player.facing.y); const sp=280;
            grenades.push({x:player.x+dir.x*14,y:player.y+dir.y*14,vx:dir.x*sp,vy:dir.y*sp,fuse:1.0});
            addShake(0.06,3);
          }
        } else {
          if(w.mag<=0){ flash('弾切れ - Rでリロード'); }
          else{
            player.shootCD=w.fireRate; w.mag--;
            const dir=norm(player.facing.x,player.facing.y); const baseSpd=360; const shots=w.pellets||1;
            for(let i=0;i<shots;i++){
              const ang=Math.atan2(dir.y,dir.x)+(Math.random()-0.5)*(w.spread||0)*2;
              const vx=Math.cos(ang)*baseSpd, vy=Math.sin(ang)*baseSpd;
              bullets.push({x:player.x+Math.cos(ang)*14,y:player.y+Math.sin(ang)*14,vx,vy,life:0.9,dmg:w.dmg});
            }
            sfx(w.id==='mg'?'mg':'shot');
            addShake(w.id==='shotgun'?0.18:0.06, w.id==='shotgun'?7:(w.id==='mg'?3:4));
          }
        }
      }

      // --- 弾更新（プレイヤー） ---
      for(let i=bullets.length-1;i>=0;i--){
        const b=bullets[i];
        b.x+=b.vx*dt; b.y+=b.vy*dt; b.life-=dt;
        const tx=Math.floor(b.x/TILE), ty=Math.floor(b.y/TILE);
        if(isSolidChar(solidAt(tx,ty))){ damageTile(tx,ty,b.dmg); bullets.splice(i,1); continue; }
        let hit=false;
        for(const m of mobs){
          if(m.hp>0 && Math.abs(m.x-b.x)<m.w/2 && Math.abs(m.y-b.y)<m.h/2){
            m.hp-=b.dmg; const n=norm(m.x-b.x,m.y-b.y); m.vx+=n.x*160; m.vy+=n.y*160; spawnSparks(m.x,m.y,5); hit=true; break;
          }
        }
        if(hit || b.life<=0) bullets.splice(i,1);
      }

      // --- グレネード ---
      for(let i=grenades.length-1;i>=0;i--){
        const g=grenades[i];
        g.x+=g.vx*dt; g.y+=g.vy*dt; g.fuse-=dt;
        const tx=Math.floor(g.x/TILE), ty=Math.floor(g.y/TILE);
        if(isSolidChar(solidAt(tx,ty)) || g.fuse<=0){ explode(g.x,g.y); grenades.splice(i,1); continue; }
      }

      // スポーン
      updateSpawner(dt);

      // --- 敵AI ---
      bfsTimer-=dt; if(bfsTimer<=0){ rebuildFlowField(); bfsTimer=0.35; }
      for(let i=mobs.length-1;i>=0;i--){
        const m=mobs[i];
        if(m.hp<=0){ dropLoot(m.x,m.y); mobs.splice(i,1); continue; }

        m.vx*=Math.pow(0.02,dt); m.vy*=Math.pow(0.02,dt);
        if(m.meleeCD>0) m.meleeCD-=dt;
        if(m.bumpCD>0)  m.bumpCD-=dt;

        const slow=(m.hp<=m.maxhp*0.5)?0.5:1;
        const effSpeed=m.baseSpeed*slow;

        const dx=player.x-m.x, dy=player.y-m.y; const d=Math.hypot(dx,dy);
        const see = d<(m.kind==='spitter'?320:240) && hasLineOfSight(m.x,m.y,player.x,player.y);
        let vx=0, vy=0;

        if(m.kind==='spitter'){
          if(see){
            if(m.shootCD>0) m.shootCD-=dt;
            else{
              m.shootCD=1.2/slow;
              const dir=norm(dx,dy); const sp=220;
              ebullets.push({x:m.x,y:m.y,vx:dir.x*sp,vy:dir.y*sp,life:1.6,dmg:12});
              sfx('shot');
            }
            const desired=-1; const v=norm(dx,dy); vx=v.x*desired*effSpeed*dt; vy=v.y*desired*effSpeed*dt;
          }else{
            const tx=Math.floor(m.x/TILE), ty=Math.floor(m.y/TILE);
            const dirs=[[1,0],[-1,0],[0,1],[0,-1]]; let best={d:Infinity,dir:[0,0]};
            for(const [ex,ey] of dirs){
              const nx=tx+ex, ny=ty+ey; if(nx<0||ny<0||nx>=MAP_W||ny>=MAP_H) continue;
              const dd=distGrid[ny][nx]; if(dd<best.d && !isSolidChar(solidAt(nx,ny))) best={d:dd,dir:[ex,ey]};
            }
            if(best.d<Infinity){ const v=norm(best.dir[0],best.dir[1]); vx=v.x*effSpeed*dt; vy=v.y*effSpeed*dt; }
          }
        }else{
          if(see){ const v=norm(dx,dy); vx=v.x*effSpeed*dt; vy=v.y*effSpeed*dt; }
          else{
            const tx=Math.floor(m.x/TILE), ty=Math.floor(m.y/TILE);
            const dirs=[[1,0],[-1,0],[0,1],[0,-1]]; let best={d:Infinity,dir:[0,0]};
            for(const [ex,ey] of dirs){
              const nx=tx+ex, ny=ty+ey; if(nx<0||ny<0||nx>=MAP_W||ny>=MAP_H) continue;
              const dd=distGrid[ny][nx]; if(dd<best.d && !isSolidChar(solidAt(nx,ny))) best={d:dd,dir:[ex,ey]};
            }
            if(best.d<Infinity){ const v=norm(best.dir[0],best.dir[1]); vx=v.x*effSpeed*dt; vy=v.y*effSpeed*dt; }
          }
        }

        vx+=m.vx*dt; vy+=m.vy*dt;
        const col=moveAndCollide(m,vx,vy);
        if(col.hitX||col.hitY){ m.x+=(Math.random()-.5)*12; m.y+=(Math.random()-.5)*12; }

        // 接触：ダメージなし、ノックバックのみ（ダッシュ中は敵だけ弾く）
        const touching=rectInter(m,player);
        if(touching && (m.bumpCD||0)<=0){
          const n=norm(player.x-m.x, player.y-m.y);
          if(player.isDashing){
            m.vx-=n.x*380; m.vy-=n.y*380;
            spawnSparks((m.x+player.x)/2,(m.y+player.y)/2,6); addShake(0.06,4);
          }else{
            player.vx+=n.x*260; player.vy+=n.y*260;
            m.vx-=n.x*220; m.vy-=n.y*220;
          }
          m.bumpCD=0.28;
        }

        // 近接（スピッターは正面90°・低頻度）
        const meleeRange=(m.kind==='spitter'?18:24);
        const baseCD=(m.kind==='spitter'?3.0:0.9);
        let arcOK=true;
        if(m.kind==='spitter'){
          const mv=norm(vx,vy); const toP=norm(dx,dy);
          const dot=mv.x*toP.x + mv.y*toP.y;
          const ang=Math.acos(clamp(dot,-1,1));
          arcOK = ang <= Math.PI/4;
        }
        if(m.meleeCD<=0 && d<meleeRange && arcOK){
          if(player.iTime<=0){
            player.hp-=10; player.iTime=0.9; sfx('hit'); addShake(0.18,6);
            const n2=norm(player.x-m.x, player.y-m.y); player.vx+=n2.x*240; player.vy+=n2.y*240;
          }
          m.meleeCD = baseCD/slow;
        }
      }

      // 敵弾
      for(let i=ebullets.length-1;i>=0;i--){
        const b=ebullets[i];
        b.x+=b.vx*dt; b.y+=b.vy*dt; b.life-=dt;
        const tx=Math.floor(b.x/TILE), ty=Math.floor(b.y/TILE);
        if(isSolidChar(solidAt(tx,ty)) || b.life<=0){ ebullets.splice(i,1); continue; }

        // 反射弾 → 敵ヒット
        if(b.reflected){
          let hitEnemy=false;
          for(const m of mobs){
            if(m.hp>0 && Math.abs(m.x-b.x)<m.w/2 && Math.abs(m.y-b.y)<m.h/2){
              m.hp-=b.dmg; const n=norm(m.x-b.x,m.y-b.y); m.vx+=n.x*160; m.vy+=n.y*160; spawnSparks(m.x,m.y,5); hitEnemy=true; break;
            }
          }
          if(hitEnemy){ ebullets.splice(i,1); continue; }
        }

        // プレイヤー判定（ダッシュ中は反射）
        const hitP = (Math.abs(player.x-b.x) < player.w/2+3) && (Math.abs(player.y-b.y) < player.h/2+3);
        if(hitP){
          if(player.isDashing){
            const n=norm(b.x-player.x, b.y-player.y);
            b.vx=n.x*260; b.vy=n.y*260; b.reflected=true; b.life=Math.min(b.life,0.9); spawnSparks(b.x,b.y,4);
            continue;
          }else{
            if(player.iTime<=0){
              player.hp-=b.dmg; player.iTime=0.8; sfx('hit'); addShake(0.2,7);
              const n=norm(player.x-b.x, player.y-b.y); player.vx+=n.x*180; player.vy+=n.y*180;
            }
            ebullets.splice(i,1); continue;
          }
        }
      }

      // アイテム取得
      for(let i=items.length-1;i>=0;i--){
        const it=items[i];
        if(Math.abs(it.x-player.x)<(player.w/2+10) && Math.abs(it.y-player.y)<(player.h/2+10)){
          if(it.type==='key'){ player.inv.key=true; flash('鍵を手に入れた'); sfx('pickup'); }
          if(it.type==='ammo9'){ const n=it.amt||12; player.inv.ammo9+=n; flash(`9mm +${n}`); sfx('pickup'); }
          if(it.type==='ammo12'){ const n=it.amt||4; player.inv.ammo12+=n; flash(`12g +${n}`); sfx('pickup'); }
          if(it.type==='ammoBeam'){ const n=it.amt||1; player.inv.ammoBeam+=n; flash(`Beamセル +${n}`); sfx('pickup'); }
          if(it.type==='ammoNade'){ const n=it.amt||1; player.inv.ammoNade+=n; flash(`Grenade +${n}`); sfx('pickup'); }
          if(it.type==='med'){ const h=it.heal||25; player.hp=clamp(player.hp+h,0,100); flash(`体力 +${h}`); sfx('pickup'); }
          if(it.type==='buffRange'){ player.buffs.range=2; player.buffs.tRange=15; flash('近接範囲 ×2（15s）'); sfx('pickup'); }
          if(it.type==='buffMelee'){ player.buffs.dmg=2; player.buffs.tDmg=15; flash('近接火力 ×2（15s）'); sfx('pickup'); }
          if(it.type==='buffSpeed'){ player.buffs.speed=2; player.buffs.tSpeed=12; flash('移動速度 ×2（12s）'); sfx('pickup'); }
          if(it.type==='crate'){ onWeaponCrate(); }
          items.splice(i,1);
        }
      }

      // ドア
      if(keys['e']){
        const tx=Math.floor((player.x+player.facing.x*18)/TILE), ty=Math.floor((player.y+player.facing.y*18)/TILE);
        if(solidAt(tx,ty)==='D'){
          if(player.inv.key){ map[ty][tx]='.'; flash('ドアを開けた'); sfx('door'); rebuildFlowField(); }
          else { flash('鍵が必要だ'); }
        }
      }

      // FX更新
      for(let i=fx.length-1;i>=0;i--){
        const f=fx[i];
        f.t+=dt; if(f.t>=f.life){ fx.splice(i,1); continue; }
        if(typeof f.x==='number') f.x+=(f.vx||0)*dt;
        if(typeof f.y==='number') f.y+=(f.vy||0)*dt;
      }

      // スラッシュ持続（多段＆弾相殺）
      for(let i=slashes.length-1;i>=0;i--){
        const s=slashes[i]; s.t+=dt; s.tickLeft-=dt;
        if(s.tickLeft<=0){
          s.tickLeft=s.tickInt;
          for(const m of mobs){
            if(m.hp>0 && pointInFan(m.x,m.y,s)){
              m.hp-=s.dmg; const n=norm(m.x-s.x,m.y-s.y); m.vx+=n.x*140; m.vy+=n.y*140; spawnSparks(m.x,m.y,3);
            }
          }
          for(let j=ebullets.length-1;j>=0;j--){
            const b=ebullets[j]; if(pointInFan(b.x,b.y,s)){ ebullets.splice(j,1); spawnSparks(b.x,b.y,4); }
          }
        }
        if(s.t>=s.life) slashes.splice(i,1);
      }

      // 死亡/揺れ
      player.hp=clamp(player.hp,0,100);
      if(shakeTime>0){ shakeTime-=dt; if(shakeTime<0) shakeTime=0; }
    }

    function onWeaponCrate(){
      const want=['beam','mg','grenade'];
      const owned=new Set(player.weapons.map(w=>w.id));
      const todo=want.filter(id=>!owned.has(id));
      if(todo.length){
        const pick=todo[Math.floor(Math.random()*todo.length)];
        if(pick==='beam'){
          const beam={id:'beam', name:'Beam Rifle', dmg:90, mag:Infinity, magSize:Infinity, spread:0, fireRate:2.0, ammoType:'ammoBeam'};
          player.weapons.push(beam); player.inv.ammoBeam+=3; flash('新武器: Beam Rifle ＋セル+3'); sfx('pickup');
        }else if(pick==='mg'){
          const mg={id:'mg', name:'Machine Gun', dmg:12, pellets:1, mag:40, magSize:40, spread:0.18, fireRate:0.08, ammoType:'ammo9'};
          player.weapons.push(mg); player.inv.ammo9+=60; flash('新武器: Machine Gun ＋9mm+60'); sfx('pickup');
        }else{
          const gr={id:'grenade', name:'Grenade', dmg:120, mag:1, magSize:1, spread:0, fireRate:0.9, ammoType:'ammoNade'};
          player.weapons.push(gr); player.inv.ammoNade+=3; gr.mag=1; flash('新武器: Grenade ＋弾×3'); sfx('pickup');
        }
      }else{
        const w=player.weapons[Math.floor(Math.random()*player.weapons.length)];
        player.inv[w.ammoType]=(player.inv[w.ammoType]||0)+(w.magSize||1);
        flash(`${w.name} の弾を1マガジン分補充`); sfx('pickup');
      }
    }

    function explode(x,y){
      const r=70; spawnBlast(x,y,r); spawnDust(x,y,16); sfx('boom'); addShake(0.28,12);
      for(const m of mobs){
        const dx=m.x-x, dy=m.y-y; const d=Math.hypot(dx,dy);
        if(d<r+Math.max(m.w,m.h)/2){ const fall=1-d/r; const dmg=Math.round(110*fall);
          if(dmg>0){ m.hp-=dmg; const n=norm(dx,dy); m.vx+=n.x*280*fall; m.vy+=n.y*280*fall; spawnSparks(m.x,m.y,8); }
        }
      }
      const dp=Math.hypot(player.x-x, player.y-y);
      if(dp<r*0.7){ const fall=1-dp/(r*0.7); player.hp-=Math.round(25*fall); const n=norm(player.x-x, player.y-y); player.vx+=n.x*200*fall; player.vy+=n.y*200*fall; }
      const tx0=Math.max(1,Math.floor((x-r)/TILE)), ty0=Math.max(1,Math.floor((y-r)/TILE));
      const tx1=Math.min(MAP_W-2,Math.floor((x+r)/TILE)), ty1=Math.min(MAP_H-2,Math.floor((y+r)/TILE));
      for(let ty=ty0;ty<=ty1;ty++) for(let tx=tx0;tx<=tx1;tx++){
        const cx=tx*TILE+TILE/2, cy=ty*TILE+TILE/2; const d=Math.hypot(cx-x, cy-y);
        if(d<=r && map[ty][tx]==='#'){ damageTile(tx,ty, 120*(1-d/r)); }
      }
    }

    function hasLineOfSight(x0,y0,x1,y1){
      let cx=Math.floor(x0/TILE), cy=Math.floor(y0/TILE);
      const tx=Math.floor(x1/TILE), ty=Math.floor(y1/TILE);
      const dx=Math.sign(tx-cx), dy=Math.sign(ty-cy);
      const nx=Math.abs(tx-cx), ny=Math.abs(ty-cy);
      let err=nx-ny;
      while(!(cx===tx && cy===ty)){
        if(isSolidChar(solidAt(cx,cy))) return false;
        const e2=2*err;
        if(e2>-ny){ err-=ny; cx+=dx; }
        if(e2< nx){ err+=nx; cy+=dy; }
        if(nx+ny>600) break;
      }
      return true;
    }

    // ===== 描画 =====
    function roundedRect(x,y,w,h,r){
      const rr=Math.min(r,w/2,h/2);
      ctx.beginPath();
      ctx.moveTo(x+rr,y); ctx.lineTo(x+w-rr,y); ctx.quadraticCurveTo(x+w,y,x+w,y+rr);
      ctx.lineTo(x+w,y+h-rr); ctx.quadraticCurveTo(x+w,y+h,x+w-rr,y+h);
      ctx.lineTo(x+rr,y+h); ctx.quadraticCurveTo(x,y+h,x,y+h-rr);
      ctx.lineTo(x,y+rr); ctx.quadraticCurveTo(x,y,x+rr,y); ctx.closePath();
    }
    function keyGlyph(){ roundedRect(-7,-3,14,6,2); ctx.fill(); ctx.fillRect(2,-1,8,2); }
    function boxGlyph(label){ roundedRect(-9,-6,18,12,2); ctx.fill(); ctx.fillStyle='#131922'; ctx.fillRect(-7,-1,14,2); ctx.fillStyle='#111'; ctx.font=`${10*DPR}px sans-serif`; ctx.textAlign='center'; ctx.textBaseline='middle'; ctx.fillText(label,0,-4); }
    function medGlyph(){ roundedRect(-7,-5,14,10,2); ctx.fill(); ctx.fillStyle='#131922'; ctx.fillRect(-1,-4,2,8); ctx.fillRect(-4,-1,8,2); }
    function ringGlyph(){ ctx.strokeStyle='#9ecbff'; ctx.lineWidth=2; ctx.beginPath(); ctx.arc(0,0,8,0,Math.PI*2); ctx.stroke(); ctx.lineWidth=1; }
    function swordGlyph(){ ctx.fillStyle='#ff9aa2'; ctx.fillRect(-1,-6,2,12); ctx.fillRect(-4,-1,8,2); }
    function boltGlyph(){ ctx.fillStyle='#ffe08a'; ctx.beginPath(); ctx.moveTo(-3,-6); ctx.lineTo(1,-2); ctx.lineTo(-1,2); ctx.lineTo(3,6); ctx.lineTo(-1,2); ctx.lineTo(1,-2); ctx.closePath(); ctx.fill(); }
    function crateGlyph(){ ctx.fillStyle='#b48a5a'; roundedRect(-8,-6,16,12,2); ctx.fill(); ctx.fillStyle='#2a2016'; ctx.fillRect(-6,-1,12,2); }

    function draw(){
      const W=canvas.width, H=canvas.height; ctx.clearRect(0,0,W,H);
      const baseCamX=clamp(player.x - W/2, 0, MAP_W*TILE - W);
      const baseCamY=clamp(player.y - H/2, 0, MAP_H*TILE - H);
      const sx=shakeTime>0?(Math.random()*2-1)*shakeMag:0;
      const sy=shakeTime>0?(Math.random()*2-1)*shakeMag:0;
      const camX=baseCamX+sx, camY=baseCamY+sy;
      ctx.save(); ctx.translate(-camX,-camY);

      // タイル
      for(let y=0;y<MAP_H;y++){
        for(let x=0;x<MAP_W;x++){
          const c=map[y][x]; const px=x*TILE, py=y*TILE;
          if(c==='#'){
            ctx.fillStyle='#1b2735'; ctx.fillRect(px,py,TILE,TILE);
            ctx.fillStyle='#0f1620'; ctx.fillRect(px+2,py+2,TILE-4,TILE-4);
            const hp=tileHP[y][x], mh=tileMaxHP[y][x];
            if(mh!==Infinity){
              const r=clamp(1 - hp/mh, 0, 1);
              if(r>0){ ctx.strokeStyle=`rgba(200,200,220,${0.25+0.5*r})`; ctx.beginPath();
                ctx.moveTo(px+4,py+6); ctx.lineTo(px+TILE-6, py+TILE-8);
                ctx.moveTo(px+6,py+TILE-6); ctx.lineTo(px+TILE-10, py+10); ctx.stroke();
              }
            }
          }else if(c==='D'){
            ctx.fillStyle='#3b2a1a'; ctx.fillRect(px,py,TILE,TILE);
            ctx.strokeStyle='#6b4c2b'; ctx.strokeRect(px+6,py+4, TILE-12, TILE-8);
          }else{
            ctx.fillStyle='#131922'; ctx.fillRect(px,py,TILE,TILE);
            ctx.fillStyle='rgba(255,255,255,0.03)';
            ctx.fillRect(px+((x+y)%2), py+((x*7+y*13)%3),1,1);
          }
        }
      }

      // アイテム
      for(const it of items){
        ctx.save(); ctx.translate(it.x,it.y);
        if(it.type==='key'){ ctx.fillStyle='#ffd16b'; keyGlyph(); }
        if(it.type==='ammo9'){ ctx.fillStyle='#9ad0ff'; boxGlyph('9'); }
        if(it.type==='ammo12'){ ctx.fillStyle='#c9a56b'; boxGlyph('12'); }
        if(it.type==='ammoBeam'){ ctx.fillStyle='#a8ceff'; boxGlyph('B'); }
        if(it.type==='ammoNade'){ ctx.fillStyle='#ffa8a8'; boxGlyph('G'); }
        if(it.type==='med'){ ctx.fillStyle='#8fffc1'; medGlyph(); }
        if(it.type==='buffRange'){ ringGlyph(); }
        if(it.type==='buffMelee'){ swordGlyph(); }
        if(it.type==='buffSpeed'){ boltGlyph(); }
        if(it.type==='crate'){ crateGlyph(); }
        ctx.restore();
      }

      // 弾
      ctx.fillStyle='#d1e7ff'; for(const b of bullets){ ctx.fillRect(b.x-2,b.y-2,4,4); }
      // 敵弾
      ctx.fillStyle='#ffb3b3'; for(const b of ebullets){ ctx.fillRect(b.x-2,b.y-2,4,4); }
      // グレネード
      ctx.fillStyle='#ffcf99'; for(const g of grenades){ ctx.beginPath(); ctx.arc(g.x,g.y,4,0,Math.PI*2); ctx.fill(); }

      // 敵
      for(const m of mobs){
        ctx.save(); ctx.translate(m.x,m.y);
        ctx.fillStyle = (m.kind==='spitter') ? '#3aa06f' : '#b24a4a';
        roundedRect(-m.w/2,-m.h/2,m.w,m.h,4); ctx.fill();
        ctx.fillStyle='#fff'; ctx.fillRect(-4,-3,3,3); ctx.fillRect(1,-3,3,3);
        ctx.restore();
      }

      // プレイヤー
      ctx.save(); ctx.translate(player.x,player.y);
      const hitFlash = player.iTime>0 && (Math.floor(player.iTime*20)%2===0);
      ctx.fillStyle = hitFlash ? '#ff9aa2' : '#7ab0ff';
      roundedRect(-player.w/2,-player.h/2,player.w,player.h,5); ctx.fill();
      ctx.strokeStyle='#cfe5ff'; ctx.beginPath(); ctx.moveTo(0,0); ctx.lineTo(player.facing.x*14, player.facing.y*14); ctx.stroke();
      ctx.restore();

      // FX
      for(const f of fx){
        const a=1 - f.t/f.life; ctx.globalAlpha=a;
        if(f.type==='slash'){
          ctx.save(); ctx.translate(f.x,f.y); ctx.rotate(f.ang);
          const r0=10, r1=46;
          const grd=ctx.createRadialGradient(0,0,r0, 0,0,r1);
          grd.addColorStop(0,'rgba(200,230,255,0.6)');
          grd.addColorStop(1,'rgba(200,230,255,0.0)');
          ctx.fillStyle=grd; ctx.beginPath(); ctx.moveTo(0,0); ctx.arc(0,0,r1,-Math.PI/2,Math.PI/2); ctx.closePath(); ctx.fill();
          ctx.restore();
        }
        if(f.type==='spark'){ ctx.fillStyle='#e6f3ff'; ctx.fillRect(f.x-1,f.y-1,2,2); }
        if(f.type==='dust'){ ctx.fillStyle='rgba(200,200,220,0.25)'; ctx.beginPath(); ctx.arc(f.x,f.y,2+f.t*10,0,Math.PI*2); ctx.fill(); }
        if(f.type==='beam'){ ctx.strokeStyle='rgba(220,240,255,0.9)'; ctx.lineWidth=3; ctx.beginPath(); ctx.moveTo(f.sx,f.sy); ctx.lineTo(f.ex,f.ey); ctx.stroke(); ctx.lineWidth=1; }
        if(f.type==='blast'){ const r=f.r*(0.6+0.4*(1-a)); const grd=ctx.createRadialGradient(f.x,f.y,0,f.x,f.y,r); grd.addColorStop(0,'rgba(255,230,150,0.6)'); grd.addColorStop(1,'rgba(255,120,80,0.0)'); ctx.fillStyle=grd; ctx.beginPath(); ctx.arc(f.x,f.y,r,0,Math.PI*2); ctx.fill(); }
        ctx.globalAlpha=1;
      }

      // ライティング
      ctx.globalCompositeOperation='multiply';
      const g=ctx.createRadialGradient(player.x,player.y,24, player.x,player.y,360);
      g.addColorStop(0,'rgba(255,255,255,0.0)'); g.addColorStop(1,'rgba(0,0,0,0.90)');
      ctx.fillStyle=g; ctx.fillRect(camX,camY,W,H);
      ctx.globalCompositeOperation='source-over';

      ctx.restore();

      // HUD（JSのテンプレは EL 無効化によりそのまま使える）
      const w = player.weapons[player.curW] || {name:'(none)',mag:0,magSize:0,id:'none'};
      const next = Math.max(0, spawnTimer).toFixed(1);
      const magTxt = w.id==='beam' ? '∞' : `${w.mag}/${w.magSize}`;
      hud.innerHTML =
        `HP: ${player.hp}　Sta: ${Math.round(player.sta)}%　Key: ${player.inv.key ? '✓' : '—'}　資材: ${player.inv.blocks}　Zombies: ${mobs.length}　NextSpawn: ${next}s<br>`+
        `武器: <b>${w.name}</b>　Mag: <b>${magTxt}</b>　Reserve: 9mm=${player.inv.ammo9} / 12g=${player.inv.ammo12} / BeamCell=${player.inv.ammoBeam} / Nade=${player.inv.ammoNade}<br>`+
        `Buff: 範囲×${player.buffs.range} (${player.buffs.tRange>0 ? player.buffs.tRange.toFixed(0)+'s' : ''})　`+
        `近接×${player.buffs.dmg} (${player.buffs.tDmg>0 ? player.buffs.tDmg.toFixed(0)+'s' : ''})　`+
        `速度×${player.buffs.speed} (${player.buffs.tSpeed>0 ? player.buffs.tSpeed.toFixed(0)+'s' : ''})`;
    }

    // ===== セーブ/ロード =====
    function saveGame(){
      const data={
        player:{x:player.x,y:player.y,hp:player.hp,inv:player.inv,curW:player.curW,
          weapons:player.weapons.map(w=>({id:w.id,mag:w.mag})), buffs:player.buffs, sta:player.sta},
        map, tileHP, tileMaxHP, items,
        mobs: mobs.map(m=>({kind:m.kind,x:m.x,y:m.y,hp:m.hp,maxhp:m.maxhp})),
        spawner:{elapsed, spawnTimer}
      };
      localStorage.setItem('nobihaza_like_save', JSON.stringify(data));
    }
    function loadGame(){
      const raw=localStorage.getItem('nobihaza_like_save');
      if(!raw){ flash('セーブがありません'); return; }
      try{
        const data=JSON.parse(raw);
        player.x=data.player.x; player.y=data.player.y; player.hp=data.player.hp;
        player.inv=data.player.inv; player.curW=data.player.curW;
        if(data.player.buffs) player.buffs=data.player.buffs;
        if(typeof data.player.sta==='number') player.sta=data.player.sta;
        map = data.map.map(row=>row.slice());
        for(let y=0;y<MAP_H;y++) for(let x=0;x<MAP_W;x++){ tileHP[y][x]=data.tileHP[y][x]; tileMaxHP[y][x]=data.tileMaxHP[y][x]; }
        items.length=0; for(const it of data.items) items.push({...it});
        mobs.length=0; for(const m of data.mobs){ const obj=(m.kind==='spitter'?makeSpitter(m.x,m.y):makeZombie(m.x,m.y)); obj.hp=m.hp; obj.maxhp=m.maxhp||obj.maxhp; mobs.push(obj); }
        for(const w of player.weapons){ const f=data.player.weapons.find(v=>v.id===w.id); if(f) w.mag=f.mag; }
        if(data.spawner){ elapsed=data.spawner.elapsed||0; spawnTimer=data.spawner.spawnTimer||5; }
        rebuildFlowField();
      }catch(e){ console.error(e); flash('ロード失敗'); }
    }

    // ===== UI =====
    let toastTimer=null;
    function flash(msg){
      toast.textContent=msg; toast.classList.add('show');
      if(toastTimer) clearTimeout(toastTimer);
      toastTimer=setTimeout(()=>toast.classList.remove('show'), 1200);
    }

    // ===== ループ =====
    let last=performance.now();
    function loop(t){
      fitCanvas();
      const dt=Math.min(0.05,(t-last)/1000); last=t;
      update(dt); draw(); requestAnimationFrame(loop);
    }
    requestAnimationFrame(loop);

  })();
  </script>
</body>
</html>
