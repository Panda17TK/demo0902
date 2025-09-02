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
	private static final Pattern P_NAME = Pattern.compile("\"name\"\\s*:\\s*\"(.*?)\"");
	private static final Pattern P_TIMEMS = Pattern.compile("\"timeMs\"\\s*:\\s*(\\d+)");

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HttpSession sess = req.getSession(false);
		String uid = sess != null ? (String) sess.getAttribute("uid") : null;

		resp.setContentType("application/json; charset=UTF-8");
		try (PrintWriter out = resp.getWriter()) {
			if (uid == null) {
				out.print("{\"ok\":false,\"error\":\"not_logged_in\"}");
				return;
			}

			StringBuilder sb = new StringBuilder(1024);
			try (BufferedReader br = req.getReader()) {
				String line;
				while ((line = br.readLine()) != null)
					sb.append(line);
			}
			String body = sb.toString();

			String name = "Player";
			long timeMs = 0L;

			Matcher m1 = P_NAME.matcher(body);
			if (m1.find())
				name = m1.group(1);
			Matcher m2 = P_TIMEMS.matcher(body);
			if (m2.find()) {
				try {
					timeMs = Long.parseLong(m2.group(1));
				} catch (NumberFormatException ignore) {
				}
			}

			ScoreRecord rec = new ScoreRecord(uid, name, timeMs);
			rec.setCreatedAt(Instant.now());
			dao.save(rec);

			out.print("{\"ok\":true}");
		}
	}
}
