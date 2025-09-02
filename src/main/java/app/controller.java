package app;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/controller" })
public class controller extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// 学習用に簡易 uid をセッションへ（本格ログインは後で）
		HttpSession session = request.getSession(true);
		if (session.getAttribute("uid") == null) {
			session.setAttribute("uid", "demo-" + session.getId());
		}

		request.getRequestDispatcher("/WEB-INF/jsp/game.jsp").forward(request, response);
	}
}