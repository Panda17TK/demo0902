package app;

import java.util.List;

public interface ScoreDAO {
	void save(ScoreRecord rec);

	List<ScoreRecord> listTop(int limit);
}