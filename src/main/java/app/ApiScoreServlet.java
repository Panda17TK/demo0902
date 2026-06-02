package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/api/score" })
public class ApiScoreServlet extends HttpServlet {
	private final ScoreDAO dao = DAOFactory.getScoreDAO();

	private static final int MAX_BODY = 8 * 1024;     // スコア送信の本文上限
	private static final int MAX_NAME = 32;            // 名前の最大文字数
	private static final long MAX_TIME_MS = 24L * 60 * 60 * 1000; // 24時間を上限

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json; charset=UTF-8");
		HttpSession sess = req.getSession(false);
		String uid = sess != null ? (String) sess.getAttribute("uid") : null;

		try (PrintWriter out = resp.getWriter()) {
			if (uid == null) {
				resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				out.print("{\"ok\":false,\"error\":\"not_logged_in\"}");
				return;
			}

			String body;
			try {
				body = HttpJson.readBody(req, MAX_BODY);
			} catch (IOException tooLarge) {
				resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
				out.print("{\"ok\":false,\"error\":\"too_large\"}");
				return;
			}

			String name = HttpJson.getString(body, "name", "Player");
			long timeMs = HttpJson.getLong(body, "timeMs", -1);

			// 入力検証
			name = sanitizeName(name);
			if (timeMs < 0 || timeMs > MAX_TIME_MS) {
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.print("{\"ok\":false,\"error\":\"invalid_timeMs\"}");
				return;
			}

			ScoreRecord rec = new ScoreRecord(uid, name, timeMs);
			rec.setCreatedAt(Instant.now());
			dao.save(rec);

			out.print("{\"ok\":true}");
		}
	}

	/** 制御文字を除去し、空なら既定名、長すぎれば切り詰める。 */
	static String sanitizeName(String s) {
		if (s == null) return "Player";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length() && sb.length() < MAX_NAME; i++) {
			char c = s.charAt(i);
			if (c >= 0x20 && c != 0x7f) sb.append(c);
		}
		String out = sb.toString().trim();
		return out.isEmpty() ? "Player" : out;
	}
}
