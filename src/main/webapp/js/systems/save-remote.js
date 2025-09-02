// webapp/js/systems/save-remote.js
import { makeZombie, makeSpitter } from './ai.js';
import { rebuildFlowField } from './flowfield.js';

// ---- CTX (contextPath) 自動検出 ----
var CTX = (function(){
  try{
    if (typeof window !== 'undefined' && window.CTX) return window.CTX;
    var m = (typeof location !== 'undefined' ? location.pathname : '').match(/^\/[^\/]+/);
    return m ? m[0] : '';
  }catch(_){ return ''; }
})();

// ---- 安全に JSON を読む（500でHTML返っても落ちない）----
function safeJson(res){
  return res.text().then(function(t){
    try{ return JSON.parse(t); }catch(_){
      try{ console.error('[safeJson] not JSON:', t.slice(0,256)); }catch(__){}
      return null;
    }
  });
}
function nowStr(ts){ try{ return new Date(ts).toLocaleString(); }catch(_){ return String(ts); } }

// ========== シリアライズ ==========
function serialize(state){
  var weapons = [];
  for (var i=0; i<state.player.weapons.length; i++){
    var w = state.player.weapons[i];
    weapons.push({ id:w.id, mag:w.mag });
  }
  var mobs = [];
  for (var j=0; j<state.mobs.length; j++){
    var m = state.mobs[j];
    mobs.push({ kind:m.kind, x:m.x, y:m.y, hp:m.hp, maxhp:m.maxhp });
  }
  return {
    schema: 1, // 将来のためのバージョン
    player: {
      x:state.player.x, y:state.player.y, hp:state.player.hp,
      inv:state.player.inv, curW:state.player.curW,
      weapons:weapons, buffs:state.player.buffs, sta:state.player.sta
    },
    map:state.map, tileHP:state.tileHP, tileMaxHP:state.tileMaxHP, items:state.items,
    mobs:mobs,
    spawner: state.timers ? { elapsed:(state.timers.elapsed||0), spawnTimer:(state.timers.spawn||5) } : null,
    dim: state.dim
  };
}

// ========== 復元（ここが肝） ==========

// d のサイズに合わせて配列を再構築
function reinitArraysToMatch(state, d){
  var H = (d && d.dim && d.dim.h) || (d && d.map ? d.map.length : 0);
  var W = (d && d.dim && d.dim.w) || (H>0 ? d.map[0].length : 0);
  if (!H || !W) throw new Error('bad map size');

  state.dim = { w: W, h: H };

  // map（ディープコピー）
  var newMap = new Array(H);
  for (var y=0; y<H; y++){
    var row = d.map[y];
    var cp = new Array(W);
    for (var x=0; x<W; x++) cp[x] = row[x];
    newMap[y] = cp;
  }
  state.map = newMap;

  // tileHP / tileMaxHP
  state.tileHP    = new Array(H);
  state.tileMaxHP = new Array(H);
  for (var y2=0; y2<H; y2++){
    state.tileHP[y2]    = new Array(W);
    state.tileMaxHP[y2] = new Array(W);
    for (var x2=0; x2<W; x2++){
      state.tileHP[y2][x2]    = d.tileHP[y2][x2];
      state.tileMaxHP[y2][x2] = d.tileMaxHP[y2][x2];
    }
  }
}

function applyToState(state, d){
  if (!d || !d.player || !d.map || !d.tileHP || !d.tileMaxHP){
    throw new Error('invalid save shape');
  }

  // 1) 配列系を d に合わせて組み直す
  reinitArraysToMatch(state, d);

  // 2) プレイヤー
  var pp = d.player;
  state.player.x   = (typeof pp.x  === 'number') ? pp.x  : state.player.x;
  state.player.y   = (typeof pp.y  === 'number') ? pp.y  : state.player.y;
  state.player.hp  = Math.max(0, Math.min(100, (typeof pp.hp === 'number') ? pp.hp : state.player.hp));
  state.player.inv = pp.inv || state.player.inv;
  state.player.curW = Math.max(0, Math.min(state.player.weapons.length-1, (pp.curW|0)));
  if (pp.buffs) state.player.buffs = pp.buffs;
  if (typeof pp.sta === 'number') state.player.sta = pp.sta;

  // 3) アイテム
  var items = Array.isArray(d.items) ? d.items : [];
  state.items.length = 0;
  for (var i=0; i<items.length; i++) state.items.push(Object.assign({}, items[i]));

  // 4) モブ（工場関数で復元し直すのが重要）
  var mobs = Array.isArray(d.mobs) ? d.mobs : [];
  state.mobs.length = 0;
  for (var j=0; j<mobs.length; j++){
    var m = mobs[j];
    var obj = (m.kind === 'spitter') ? makeSpitter(m.x, m.y) : makeZombie(m.x, m.y);
    if (typeof m.hp === 'number') obj.hp = m.hp;
    if (typeof m.maxhp === 'number') obj.maxhp = m.maxhp;
    state.mobs.push(obj);
  }

  // 5) 武器マガジン
  if (Array.isArray(pp.weapons)){
    for (var wi=0; wi<state.player.weapons.length; wi++){
      var sw = state.player.weapons[wi], mag = null;
      for (var vi=0; vi<pp.weapons.length; vi++){
        if (pp.weapons[vi].id === sw.id){ mag = pp.weapons[vi].mag; break; }
      }
      if (typeof mag === 'number'){
        var max = (sw.magSize == null ? Infinity : sw.magSize);
        sw.mag = Math.max(0, Math.min(max, mag));
      }
    }
  }

  // 6) 一時配列はクリア
  state.bullets.length  = 0;
  state.ebullets.length = 0;
  state.grenades.length = 0;
  state.fx.length       = 0;

  // 7) タイマーなど
  if (!state.timers) state.timers = {};
  if (d.spawner){
    state.timers.elapsed = d.spawner.elapsed || 0;
    state.timers.spawn   = d.spawner.spawnTimer || 5;
  }

  // 8) 経路場などの派生状態を再構築
  rebuildFlowField(state);

  // 9) 無敵時間などの一時値はリセット
  state.player.iTime = 0;

  return true;
}

// ========== スロット API ==========
export function listSlots(bus){
  return fetch(CTX + '/api/state2/list', { method:'GET' })
    .then(function(res){ return res.ok ? safeJson(res) : null; })
    .then(function(json){ if (!json || !json.ok) return []; return json.slots || []; })
    .catch(function(e){ try{ console.error('[listSlots]', e); }catch(_){}
      bus && bus.emit && bus.emit('ui:toast','list failed'); return []; });
}

export function save(state, _storage, bus, slot){
  try{
    var s = slot || window.prompt('保存スロット名（例: slot1, A など）','slot1');
    if (!s){ bus.emit('ui:toast','save cancelled'); return Promise.resolve(); }
    var payload = serialize(state);
    return fetch(CTX + '/api/state2?slot=' + encodeURIComponent(s), {
      method:'POST',
      headers:{ 'Content-Type':'application/json; charset=UTF-8' },
      body: JSON.stringify(payload)
    }).then(function(res){
      if (!res.ok){ bus.emit('ui:toast','remote save failed'); return; }
      bus.emit('ui:toast','saved ('+s+')');
    }).catch(function(e){
      try{ console.error('[save]', e); }catch(_){}
      bus.emit('ui:toast','remote save error');
    });
  }catch(e){
    try{ console.error('[save-sync]', e); }catch(_){}
    bus.emit('ui:toast','remote save error'); return Promise.resolve();
  }
}

export function load(state, _storage, bus, slot){
  var url = CTX + '/api/state2' + (slot ? ('?slot='+encodeURIComponent(slot)) : '');
  return fetch(url, { method:'GET' })
    .then(function(res){
      if (!res.ok){ bus.emit('ui:toast','remote load failed'); return null; }
      return safeJson(res);
    })
    .then(function(json){
      if (!json) return;
      if (!json.ok || !json.exists){ bus.emit('ui:toast','セーブがありません'); return; }
      var d = json.data;
      try{
        applyToState(state, d);
        bus.emit('ui:toast','loaded' + (slot?(' ('+slot+')'):''));
      }catch(e){
        try{ console.error('[applyToState]', e, d); }catch(_){}
        bus.emit('ui:toast','broken save data');
      }
    })
    .catch(function(e){
      try{ console.error('[load]', e); }catch(_){}
      bus.emit('ui:toast','remote load error');
    });
}

// 簡易選択UI
export function saveChooser(state, _storage, bus){
  return save(state, _storage, bus, null);
}
export function loadChooser(state, _storage, bus){
  return listSlots(bus).then(function(slots){
    if (!slots.length){ bus.emit('ui:toast','セーブがありません'); return; }
    var lines = [];
    for (var i=0; i<slots.length; i++){
      lines.push((i+1)+': '+slots[i].slot+' ('+nowStr(slots[i].updatedAt)+')');
    }
    var ans = window.prompt('ロードする番号を入力:\n'+lines.join('\n'), '1');
    var idx = ans ? (parseInt(ans,10)-1) : -1;
    if (idx>=0 && idx<slots.length){
      return load(state, _storage, bus, slots[idx].slot);
    }else{
      bus.emit('ui:toast','load cancelled');
    }
  });
}

export function wipe(_state, _storage, bus, slot){
  var url = CTX + '/api/state2' + (slot ? ('?slot='+encodeURIComponent(slot)) : '');
  return fetch(url, { method:'DELETE' })
    .then(function(res){
      if (!res.ok){ bus.emit('ui:toast','delete failed'); return; }
      bus.emit('ui:toast', slot?('deleted ('+slot+')'):'deleted all');
    })
    .catch(function(e){
      try{ console.error('[wipe]', e); }catch(_){}
      bus.emit('ui:toast','remote delete error');
    });
}
