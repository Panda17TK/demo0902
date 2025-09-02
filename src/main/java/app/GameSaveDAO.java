package app;

import java.util.List;

public interface GameSaveDAO {
	void save(GameSave save);

	GameSave find(String uid, String slot);

	List<GameSave> list(String uid);

	void delete(String uid, String slot);
}