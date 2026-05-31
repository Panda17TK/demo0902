package app;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryGameSaveDAO implements GameSaveDAO {
  // uid -> slot -> GameSave
  private final Map<String, Map<String, GameSave>> map = new ConcurrentHashMap<>();

  @Override
  public void save(GameSave save) {
    map.computeIfAbsent(save.getUid(), k -> new ConcurrentHashMap<>())
       .put(save.getSlot(), save);
  }

  @Override
  public GameSave find(String uid, String slot) {
    Map<String, GameSave> m = map.get(uid);
    return (m != null) ? m.get(slot) : null;
  }

  @Override
  public List<GameSave> list(String uid) {
    Map<String, GameSave> m = map.get(uid);
    if (m == null) return Collections.emptyList();
    List<GameSave> out = new ArrayList<>(m.values());
    out.sort((a,b)-> {
      Instant ia = a.getUpdatedAt(), ib = b.getUpdatedAt();
      if (ia == null && ib == null) return 0;
      if (ia == null) return 1;
      if (ib == null) return -1;
      return ib.compareTo(ia); // 新しい順
    });
    return out;
  }

  @Override
  public void delete(String uid, String slot) {
    Map<String, GameSave> m = map.get(uid);
    if (m != null) m.remove(slot);
  }
}