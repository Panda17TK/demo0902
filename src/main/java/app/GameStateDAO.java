package app;

public interface GameStateDAO {
	void save(GameState state);

	GameState find(String uid);

	void delete(String uid);
}