package app;

public class DAOFactory {
	private static final GameSaveDAO GAME_SAVE_DAO = new InMemoryGameSaveDAO(); // まずはメモリ
	private static final ScoreDAO SCORE_DAO = new InMemoryScoreDAO(); // 既存のスコアDAO

	public static GameSaveDAO getGameSaveDAO() {
		return GAME_SAVE_DAO;
	}

	public static ScoreDAO getScoreDAO() {
		return SCORE_DAO;
	}
}