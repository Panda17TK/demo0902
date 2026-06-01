package app;

/**
 * DAO 実装の差し替えポイント。
 * 既定はファイル永続化（DB不要・Tomcat再起動後もデータが残る）。
 * システムプロパティ arpg.persistence=memory でインメモリ実装に切替（テスト等）。
 */
public class DAOFactory {
	private static final GameSaveDAO GAME_SAVE_DAO;
	private static final ScoreDAO SCORE_DAO;

	static {
		String mode = System.getProperty("arpg.persistence", "file");
		if ("memory".equalsIgnoreCase(mode)) {
			GAME_SAVE_DAO = new InMemoryGameSaveDAO();
			SCORE_DAO = new InMemoryScoreDAO();
		} else {
			GAME_SAVE_DAO = new FileGameSaveDAO();
			SCORE_DAO = new FileScoreDAO();
		}
	}

	public static GameSaveDAO getGameSaveDAO() {
		return GAME_SAVE_DAO;
	}

	public static ScoreDAO getScoreDAO() {
		return SCORE_DAO;
	}
}
