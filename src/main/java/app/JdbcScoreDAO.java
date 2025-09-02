package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JdbcScoreDAO implements ScoreDAO {
  private final String url;
  private final String user;
  private final String pass;

  public JdbcScoreDAO(String url, String user, String pass) {
    this.url = url; this.user = user; this.pass = pass;
    initTable();
  }

  private void initTable() {
    try (Connection c = DriverManager.getConnection(url, user, pass);
         Statement st = c.createStatement()) {
      st.executeUpdate("CREATE TABLE IF NOT EXISTS scores(" +
          "id IDENTITY PRIMARY KEY, " +
          "uid VARCHAR(128), " +
          "name VARCHAR(64), " +
          "time_ms BIGINT NOT NULL, " +
          "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
    } catch (SQLException e) { throw new RuntimeException(e); }
  }

  @Override
  public void save(ScoreRecord rec) {
    String sql = "INSERT INTO scores(uid,name,time_ms,created_at) VALUES (?,?,?,?)";
    try (Connection c = DriverManager.getConnection(url, user, pass);
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, rec.getUid());
      ps.setString(2, rec.getName());
      ps.setLong(3, rec.getTimeMs());
      ps.setTimestamp(4, Timestamp.from(rec.getCreatedAt()!=null?rec.getCreatedAt():Instant.now()));
      ps.executeUpdate();
    } catch (SQLException e) { throw new RuntimeException(e); }
  }

  @Override
  public List<ScoreRecord> listTop(int limit) {
    String sql = "SELECT id,uid,name,time_ms,created_at FROM scores ORDER BY time_ms DESC, created_at DESC LIMIT ?";
    List<ScoreRecord> out = new ArrayList<>();
    try (Connection c = DriverManager.getConnection(url, user, pass);
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, limit);
      try(ResultSet rs = ps.executeQuery()){
        while(rs.next()){
          ScoreRecord r = new ScoreRecord();
          r.setId(rs.getLong(1));
          r.setUid(rs.getString(2));
          r.setName(rs.getString(3));
          r.setTimeMs(rs.getLong(4));
          r.setCreatedAt(rs.getTimestamp(5).toInstant());
          out.add(r);
        }
      }
    } catch (SQLException e) { throw new RuntimeException(e); }
    return out;
  }
}