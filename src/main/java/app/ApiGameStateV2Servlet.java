package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/api/state2", "/api/state2/*" })
public class ApiGameStateV2Servlet extends HttpServlet {

  private final GameSaveDAO dao = DAOFactory.getGameSaveDAO();

  private String uid(HttpServletRequest req){
    HttpSession s = req.getSession(true);
    Object u = s.getAttribute("uid");
    if(u == null){
      // 学習用: なければ擬似UIDを入れる（本番は認証で）
      String gen = "demo-" + s.getId();
      s.setAttribute("uid", gen);
      return gen;
    }
    return String.valueOf(u);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    String u = uid(req);
    String path = req.getPathInfo(); // 例: "/list" or null
    String slot = req.getParameter("slot"); // 指定があれば

    resp.setContentType("application/json; charset=UTF-8");
    PrintWriter out = resp.getWriter();

    if ("/list".equals(path)){
      List<GameSave> list = dao.list(u);
      out.print("{\"ok\":true,\"slots\":[");
      for (int i=0;i<list.size();i++){
        GameSave gs = list.get(i);
        if(i>0) out.print(",");
        out.printf("{\"slot\":%s,\"updatedAt\":%d}",
          toJsonString(gs.getSlot()), gs.getUpdatedAt());
      }
      out.print("]}");
      return;
    }

    GameSave gs;
    if (slot != null && !slot.isEmpty()){
      gs = dao.find(u, slot);
    }else{
      gs = dao.findLatest(u);
    }

    if (gs == null){
      out.print("{\"ok\":true,\"exists\":false}");
    }else{
      out.print("{\"ok\":true,\"exists\":true,\"data\":");
      out.print(gs.getJson()); // 保存したJSONをそのまま返す（data にオブジェクトとして）
      out.print("}");
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    String u = uid(req);
    String slot = req.getParameter("slot");
    if (slot == null || slot.isEmpty()) slot = "slot1";

    String body = readAll(req.getReader()); // JSON本文をそのまま取得
    if (body == null || body.isEmpty()){
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.setContentType("application/json; charset=UTF-8");
      resp.getWriter().print("{\"ok\":false,\"error\":\"empty_body\"}");
      return;
    }
    dao.save(u, slot, body);
    resp.setContentType("application/json; charset=UTF-8");
    resp.getWriter().print("{\"ok\":true}");
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    String u = uid(req);
    String slot = req.getParameter("slot");
    if (slot == null || slot.isEmpty()){
      dao.deleteAll(u);
    }else{
      dao.delete(u, slot);
    }
    resp.setContentType("application/json; charset=UTF-8");
    resp.getWriter().print("{\"ok\":true}");
  }

  private static String readAll(BufferedReader r) throws IOException {
    StringBuilder sb = new StringBuilder();
    String line;
    while((line = r.readLine()) != null) sb.append(line);
    return sb.toString();
  }

  // JSON 文字列エスケープ（ダブルクォート囲み用）
  private static String toJsonString(String s){
    if (s == null) return "null";
    return "\"" + s.replace("\\","\\\\").replace("\"","\\\"") + "\"";
  }
}