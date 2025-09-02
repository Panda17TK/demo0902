package app;

public class DAOFactory {
  private static final GameStateDAO STATE_DAO = new InMemoryGameStateDAO();
  private static final GameSaveDAO  SAVE_INSTANCE = new InmemoryGameSaveDAO(); // 新規

  // 環境変数があれば JDBC、それ以外はメモリ
  private static final ScoreDAO SCORE_DAO;
  static {
    String url  = System.getProperty("DB_URL");     // 例: jdbc:h2:~/game;MODE=MySQL;AUTO_SERVER=TRUE
    String user = System.getProperty("DB_USER");    // 例: sa
    String pass = System.getProperty("DB_PASS");    // 例: 空文字
    if (url!=null && !url.isEmpty()){
      SCORE_DAO = new JdbcScoreDAO(url, user, pass);
    }else{
      SCORE_DAO = new InMemoryScoreDAO();
    }
  }

  public static GameStateDAO getGameStateDAO() { return STATE_DAO; }
  public static ScoreDAO getScoreDAO() { return SCORE_DAO; }
  public static GameSaveDAO  getGameSaveDAO(){  return SAVE_INSTANCE; }
}