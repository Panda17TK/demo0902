package app;

public class DAOFactory {
	// まずはメモリDAO。DB化するときはここを差し替えるだけ。
	private static final GameStateDAO INSTANCE = new InMemoryGameStateDAO();

	public static GameStateDAO getGameStateDAO() {
		return INSTANCE;
	}
}