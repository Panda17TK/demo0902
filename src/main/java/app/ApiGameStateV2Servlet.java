package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/api/state2", "/api/state2/list" })
public class ApiGameStateV2Servlet extends HttpServlet {
  private final GameSaveDAO dao = DAOFactory.getGameSaveDAO();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    HttpSession sess = req.getSession(false);
    String uid = (sess != null) ? (String) sess.getAttribute("uid") : null;
    resp.setContentType("application/json; charset=UTF-8");

    if (uid == null) {
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      resp.getWriter().print("{\"ok\":false,\"error\":\"not_logged_in\"}");
      return;
    }

    String servletPath = req.getServletPath();
    if (servletPath.endsWith("/list")) {
      // スロット一覧
      List<GameSave> list = dao.list(uid);
      StringBuilder sb = new StringBuilder();
      sb.append("{\"ok\":true,\"slots\":[");
      for (int i = 0; i < list.size(); i++) {
        GameSave gs = list.get(i);
        if (i > 0) sb.append(',');
        sb.append("{\"slot\":\"")
          .append(escape(gs.getSlot()))
          .append("\",\"updatedAt\":\"")
          .append(gs.getUpdatedAt() != null ? gs.getUpdatedAt().toString() : "")
          .append("\"}");
      }
      sb.append("]}");
      resp.getWriter().print(sb.toString());
      return;
    }

    // 単一スロットの取得
    String slot = param(req, "slot", "slot1");
    GameSave gs = dao.find(uid, slot);
    try (PrintWriter out = resp.getWriter()) {
      if (gs == null || gs.getBlob() == null) {
        out.print("{\"ok\":true,\"exists\":false}");
      } else {
        // blob はクライアントから送られた JSON 生文字列（UTF-8）
        String json = new String(gs.getBlob(), StandardCharsets.UTF_8);
        out.print("{\"ok\":true,\"exists\":true,\"data\":");
        out.print(json);
        out.print("}");
      }
    }
  }

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

    String slot = param(req, "slot", "slot1");
    String body = readBody(req);
    if (body == null || body.isEmpty()) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.setContentType("application/json; charset=UTF-8");
      resp.getWriter().print("{\"ok\":false,\"error\":\"empty_body\"}");
      return;
    }

    GameSave gs = new GameSave(uid, slot, body.getBytes(StandardCharsets.UTF_8));
    gs.setUpdatedAt(Instant.now());
    dao.save(gs);

    resp.setContentType("application/json; charset=UTF-8");
    resp.getWriter().print("{\"ok\":true}");
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    HttpSession sess = req.getSession(false);
    String uid = (sess != null) ? (String) sess.getAttribute("uid") : null;
    if (uid == null) {
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      resp.setContentType("application/json; charset=UTF-8");
      resp.getWriter().print("{\"ok\":false,\"error\":\"not_logged_in\"}");
      return;
    }

    String slot = req.getParameter("slot");
    if (slot == null || slot.isEmpty()) {
      // 全削除
      List<GameSave> list = new ArrayList<>(dao.list(uid));
      for (GameSave gs : list) dao.delete(uid, gs.getSlot());
    } else {
      dao.delete(uid, slot);
    }

    resp.setContentType("application/json; charset=UTF-8");
    resp.getWriter().print("{\"ok\":true}");
  }

  private static String param(HttpServletRequest req, String name, String def) {
    String v = req.getParameter(name);
    return (v == null || v.isEmpty()) ? def : v;
  }

  private static String readBody(HttpServletRequest req) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader br = req.getReader()) {
      String line;
      while ((line = br.readLine()) != null) sb.append(line);
    }
    return sb.toString();
  }

  private static String escape(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}