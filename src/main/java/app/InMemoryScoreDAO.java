package app;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class InMemoryScoreDAO implements ScoreDAO {
	private final CopyOnWriteArrayList<ScoreRecord> list = new CopyOnWriteArrayList<>();

	@Override
	public void save(ScoreRecord rec) {
		list.add(rec);
	}

	@Override
	public List<ScoreRecord> listTop(int limit) {
		// 生存時間が長いほど上位（降順）
		return list.stream()
				.sorted(Comparator.comparingLong(ScoreRecord::getTimeMs).reversed()
						.thenComparing(ScoreRecord::getCreatedAt))
				.limit(Math.max(1, limit))
				.collect(Collectors.toList());
	}
}