package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InmemoryGameSaveDAO implements GameSaveDAO {
  // uid -> (slot -> GameSave)
  private final ConcurrentHashMap<String, ConcurrentHashMap<String, GameSave>> store = new ConcurrentHashMap<>();

  @Override
  public void save(String uid, String slot, String json){
    store.putIfAbsent(uid, new ConcurrentHashMap<String, GameSave>());
    ConcurrentHashMap<String, GameSave> bySlot = store.get(uid);
    bySlot.put(slot, new GameSave(uid, slot, json, System.currentTimeMillis()));
  }

  @Override
  public GameSave find(String uid, String slot){
    ConcurrentHashMap<String, GameSave> bySlot = store.get(uid);
    if(bySlot == null) return null;
    return bySlot.get(slot);
  }

  @Override
  public GameSave findLatest(String uid){
    ConcurrentHashMap<String, GameSave> bySlot = store.get(uid);
    if(bySlot == null || bySlot.isEmpty()) return null;
    GameSave best = null;
    for(GameSave gs : bySlot.values()){
      if(best == null || gs.getUpdatedAt() > best.getUpdatedAt()) best = gs;
    }
    return best;
  }

  @Override
  public List<GameSave> list(String uid){
    ConcurrentHashMap<String, GameSave> bySlot = store.get(uid);
    if(bySlot == null) return Collections.emptyList();
    ArrayList<GameSave> arr = new ArrayList<>(bySlot.values());
    arr.sort(Comparator.comparingLong(GameSave::getUpdatedAt).reversed());
    return arr;
  }

  @Override
  public void delete(String uid, String slot){
    ConcurrentHashMap<String, GameSave> bySlot = store.get(uid);
    if(bySlot != null) bySlot.remove(slot);
  }

  @Override
  public void deleteAll(String uid){
    store.remove(uid);
  }
}