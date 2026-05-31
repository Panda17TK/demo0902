package app;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * ApiScoreServlet の doPost をモック request/response で検証。
 * DAO はインメモリ実装に固定（-Darpg.persistence=memory 相当）。
 */
class ApiScoreServletTest {

	@BeforeAll
	static void useMemory() {
		System.setProperty("arpg.persistence", "memory");
	}

	private HttpServletRequest reqWithBody(String body, String uid) throws Exception {
		HttpServletRequest req = mock(HttpServletRequest.class);
		HttpSession sess = mock(HttpSession.class);
		when(req.getSession(false)).thenReturn(sess);
		when(sess.getAttribute("uid")).thenReturn(uid);
		when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
		return req;
	}

	private String invoke(String body, String uid) throws Exception {
		ApiScoreServlet servlet = new ApiScoreServlet();
		HttpServletRequest req = reqWithBody(body, uid);
		HttpServletResponse resp = mock(HttpServletResponse.class);
		StringWriter sw = new StringWriter();
		when(resp.getWriter()).thenReturn(new PrintWriter(sw));
		servlet.doPost(req, resp);
		return sw.toString();
	}

	@Test
	void rejectsWhenNotLoggedIn() throws Exception {
		String out = invoke("{\"name\":\"X\",\"timeMs\":1000}", null);
		assertTrue(out.contains("\"ok\":false"), out);
		assertTrue(out.contains("not_logged_in"), out);
	}

	@Test
	void acceptsValidScore() throws Exception {
		String out = invoke("{\"name\":\"Hero\",\"timeMs\":4242}", "demo-1");
		assertTrue(out.contains("\"ok\":true"), out);
	}

	@Test
	void savedScoreAppearsInList() throws Exception {
		invoke("{\"name\":\"ListMe\",\"timeMs\":99999}", "demo-2");
		// 同じインメモリ DAO を共有するのでリストにも現れる
		java.util.List<ScoreRecord> top = DAOFactory.getScoreDAO().listTop(10);
		assertTrue(top.stream().anyMatch(r -> r.getTimeMs() == 99999), "保存したスコアが listTop に現れる");
	}
}
