package app;

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

  private static final int MAX_SAVE_BYTES = 512 * 1024; // セーブ blob の上限
  private static final int MAX_SLOT_LEN = 32;           // スロット名の最大長

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
    String slot = sanitizeSlot(param(req, "slot", "slot1"));
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

    resp.setContentType("application/json; charset=UTF-8");
    String slot = sanitizeSlot(param(req, "slot", "slot1"));

    String body;
    try {
      body = HttpJson.readBody(req, MAX_SAVE_BYTES);
    } catch (IOException tooLarge) {
      resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      resp.getWriter().print("{\"ok\":false,\"error\":\"too_large\"}");
      return;
    }
    if (body == null || body.isEmpty()) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.getWriter().print("{\"ok\":false,\"error\":\"empty_body\"}");
      return;
    }
    // 保存する blob が JSON オブジェクトの体裁か軽く検証（壊れたデータの混入を防ぐ）
    String trimmed = body.trim();
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.getWriter().print("{\"ok\":false,\"error\":\"not_json\"}");
      return;
    }

    GameSave gs = new GameSave(uid, slot, body.getBytes(StandardCharsets.UTF_8));
    gs.setUpdatedAt(Instant.now());
    dao.save(gs);

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
      dao.delete(uid, sanitizeSlot(slot));
    }

    resp.setContentType("application/json; charset=UTF-8");
    resp.getWriter().print("{\"ok\":true}");
  }

  private static String param(HttpServletRequest req, String name, String def) {
    String v = req.getParameter(name);
    return (v == null || v.isEmpty()) ? def : v;
  }

  /** スロット名を安全な文字に限定し長さ制限。空なら slot1。 */
  static String sanitizeSlot(String s) {
    if (s == null) return "slot1";
    String out = s.replaceAll("[^A-Za-z0-9._-]", "_");
    if (out.length() > MAX_SLOT_LEN) out = out.substring(0, MAX_SLOT_LEN);
    return out.isEmpty() ? "slot1" : out;
  }

  private static String escape(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}