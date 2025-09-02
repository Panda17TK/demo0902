package app;

import java.io.IOException;
import java.io.PrintWriter;
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
    try{
      String l = req.getParameter("limit");
      if (l!=null) limit = Math.max(1, Math.min(100, Integer.parseInt(l)));
    }catch(Exception ignored){}

    List<ScoreRecord> list = dao.listTop(limit);

    resp.setContentType("application/json; charset=UTF-8");
    try(PrintWriter out = resp.getWriter()){
      out.print("{\"ok\":true,\"scores\":[");
      for (int i=0;i<list.size();i++){
        ScoreRecord r = list.get(i);
        if (i>0) out.print(",");
        out.printf("{\"name\":%s,\"timeMs\":%d,\"createdAt\":%d}",
            toJson(r.getName()), r.getTimeMs(), r.getCreatedAt().toEpochMilli());
      }
      out.print("]}");
    }
  }

  private String toJson(String s){
    if (s==null) return "null";
    return "\"" + s.replace("\\","\\\\").replace("\"","\\\"") + "\"";
  }
}