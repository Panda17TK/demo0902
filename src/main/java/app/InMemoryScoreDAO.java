package app;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryScoreDAO implements ScoreDAO {
  private final CopyOnWriteArrayList<ScoreRecord> store = new CopyOnWriteArrayList<>();
  private volatile long seq = 1;

  @Override
  public void save(ScoreRecord rec) {
    rec.setId(seq++);
    if (rec.getCreatedAt()==null) rec.setCreatedAt(Instant.now());
    store.add(rec);
  }

  @Override
  public List<ScoreRecord> listTop(int limit) {
    List<ScoreRecord> all = new ArrayList<>(store);
    all.sort(Comparator.comparingLong(ScoreRecord::getTimeMs).reversed()
        .thenComparing(ScoreRecord::getCreatedAt).reversed());
    if (all.size()>limit) return all.subList(0, limit);
    return all;
  }
}