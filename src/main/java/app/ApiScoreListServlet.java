package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/api/score/list" })
public class ApiScoreListServlet extends HttpServlet {
	private final ScoreDAO dao = DAOFactory.getScoreDAO();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		int limit = 10;
		try {
			String s = req.getParameter("limit");
			if (s != null)
				limit = Math.max(1, Math.min(100, Integer.parseInt(s)));
		} catch (Exception ignore) {
		}

		List<ScoreRecord> top = dao.listTop(limit);

		resp.setContentType("application/json; charset=UTF-8");
		try (PrintWriter out = resp.getWriter()) {
			out.print("{\"ok\":true,\"scores\":[");
			for (int i = 0; i < top.size(); i++) {
				ScoreRecord r = top.get(i);
				if (i > 0)
					out.print(',');
				out.print("{\"name\":\"");
				out.print(JsonUtil.escape(r.getName()));
				out.print("\",\"timeMs\":");
				out.print(r.getTimeMs());
				out.print(",\"createdAt\":\"");
				Instant t = r.getCreatedAt();
				out.print(t != null ? t.toString() : "");
				out.print("\"}");
			}
			out.print("]}");
		}
	}
}
