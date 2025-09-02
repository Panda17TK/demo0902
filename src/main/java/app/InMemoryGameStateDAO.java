package app;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryGameStateDAO implements GameStateDAO {
	private final ConcurrentHashMap<String, GameState> store = new ConcurrentHashMap<>();

	@Override
	public void save(GameState state) {
		store.put(state.getUid(), state);
	}

	@Override
	public GameState find(String uid) {
		return store.get(uid);
	}

	@Override
	public void delete(String uid) {
		store.remove(uid);
	}
}