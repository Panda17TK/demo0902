package app;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/api/state" })
public class ApiGameStateServlet extends HttpServlet {

	private GameStateDAO dao = DAOFactory.getGameStateDAO();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String uid = (String) req.getSession().getAttribute("uid");
		resp.setContentType("application/json; charset=UTF-8");

		GameState st = dao.find(uid);
		try (PrintWriter out = resp.getWriter()) {
			if (st == null) {
				out.print("{\"ok\":true,\"exists\":false}");
			} else {
				out.printf("{\"ok\":true,\"exists\":true,\"x\":%d,\"y\":%d,\"hp\":%d}",
						st.getX(), st.getY(), st.getHp());
			}
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String uid = (String) req.getSession().getAttribute("uid");
		if (uid == null) {
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			resp.setContentType("application/json; charset=UTF-8");
			resp.getWriter().print("{\"ok\":false,\"error\":\"not_logged_in\"}");
			return;
		}
		int x = parseInt(req.getParameter("x"), 100);
		int y = parseInt(req.getParameter("y"), 100);
		int hp = parseInt(req.getParameter("hp"), 100);

		GameState st = new GameState(uid, x, y, hp);
		dao.save(st);

		resp.setContentType("application/json; charset=UTF-8");
		resp.getWriter().print("{\"ok\":true}");
	}

	private int parseInt(String s, int def) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return def;
		}
	}
}