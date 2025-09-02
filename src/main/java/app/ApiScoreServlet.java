package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/api/score" })
public class ApiScoreServlet extends HttpServlet {
  private final ScoreDAO dao = DAOFactory.getScoreDAO();

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    HttpSession sess = req.getSession(false);
    String uid = (sess != null) ? (String) sess.getAttribute("uid") : null;
    if (uid == null) {
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      resp.setContentType("application/json; charset=UTF-8");
      resp.getWriter().print("{\"ok\":false,\"error\":\"not_logged_in\"}");
      return;
    }

    // 文字コードは EncodingFilter で UTF-8 済み
    StringBuilder sb = new StringBuilder();
    try (BufferedReader br = req.getReader()) {
      String line;
      while ((line = br.readLine()) != null) sb.append(line);
    }
    String body = sb.toString();

    String name = matchString(body, "\"name\"\\s*:\\s*\"(.*?)\"", "Player");
    long timeMs = matchLong(body, "\"timeMs\"\\s*:\\s*(\\d+)", 0L);

    ScoreRecord rec = new ScoreRecord(uid, name, timeMs);
    rec.setCreatedAt(Instant.now());
    dao.save(rec);

    resp.setContentType("application/json; charset=UTF-8");
    try (PrintWriter out = resp.getWriter()) {
      out.print("{\"ok\":true}");
    }
  }

  // ====== tiny helpers (依存ライブラリなし) ======
  private static String matchString(String body, String regex, String def) {
    Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(body);
    if (m.find()) return unescapeJsonString(m.group(1));
    return def;
  }

  private static long matchLong(String body, String regex, long def) {
    Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(body);
    if (m.find()) {
      try { return Long.parseLong(m.group(1)); } catch (Exception ignored) {}
    }
    return def;
  }

  private static String unescapeJsonString(String s) {
    // 最低限定義（今回使う範囲）
    return s.replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t");
  }
}