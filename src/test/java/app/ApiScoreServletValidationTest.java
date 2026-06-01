package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class ApiScoreServletValidationTest {

	@BeforeAll
	static void useMemory() {
		System.setProperty("arpg.persistence", "memory");
	}

	private String post(String body, String uid, int[] statusOut) throws Exception {
		ApiScoreServlet servlet = new ApiScoreServlet();
		HttpServletRequest req = mock(HttpServletRequest.class);
		HttpSession sess = mock(HttpSession.class);
		when(req.getSession(false)).thenReturn(sess);
		when(sess.getAttribute("uid")).thenReturn(uid);
		when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
		HttpServletResponse resp = mock(HttpServletResponse.class);
		StringWriter sw = new StringWriter();
		when(resp.getWriter()).thenReturn(new PrintWriter(sw));
		// ステータスを捕捉
		org.mockito.Mockito.doAnswer(inv -> { statusOut[0] = inv.getArgument(0); return null; })
				.when(resp).setStatus(org.mockito.ArgumentMatchers.anyInt());
		servlet.doPost(req, resp);
		return sw.toString();
	}

	@Test
	void negativeTimeRejected() throws Exception {
		int[] st = { 200 };
		String out = post("{\"name\":\"X\",\"timeMs\":-5}", "u1", st);
		assertTrue(out.contains("invalid_timeMs"), out);
		assertEquals(400, st[0]);
	}

	@Test
	void hugeTimeRejected() throws Exception {
		int[] st = { 200 };
		String out = post("{\"name\":\"X\",\"timeMs\":999999999999}", "u1", st);
		assertTrue(out.contains("invalid_timeMs"), out);
	}

	@Test
	void validScoreAccepted() throws Exception {
		int[] st = { 200 };
		String out = post("{\"name\":\"Hero\",\"timeMs\":5000}", "u1", st);
		assertTrue(out.contains("\"ok\":true"), out);
	}

	@Test
	void nameSanitizedAndTruncated() {
		// 制御文字除去
		assertEquals("ab", ApiScoreServlet.sanitizeName("a\nb"));
		// 空→既定
		assertEquals("Player", ApiScoreServlet.sanitizeName("   "));
		// 長さ制限(32)
		String longName = "01234567890123456789012345678901234567890123456789";
		assertTrue(ApiScoreServlet.sanitizeName(longName).length() <= 32);
	}

	@Test
	void slotNameSanitized() {
		assertEquals("a_b_c", ApiGameStateV2Servlet.sanitizeSlot("a/b\\c"));
		assertEquals("slot1", ApiGameStateV2Servlet.sanitizeSlot(""));
		assertEquals("slot1", ApiGameStateV2Servlet.sanitizeSlot(null));
	}
}
