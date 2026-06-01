package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HttpJsonTest {

	private HttpServletRequest reqWith(String body) throws IOException {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		Mockito.when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
		return req;
	}

	@Test
	void getStringBasic() {
		assertEquals("Hero", HttpJson.getString("{\"name\":\"Hero\",\"timeMs\":10}", "name", "x"));
	}

	@Test
	void getStringUnescapes() {
		// \n と \" と \\ を含む名前を正しく復元
		String json = "{\"name\":\"a\\\"b\\\\c\\nd\"}";
		assertEquals("a\"b\\c\nd", HttpJson.getString(json, "name", "x"));
	}

	@Test
	void getStringMissingReturnsDefault() {
		assertEquals("def", HttpJson.getString("{\"timeMs\":1}", "name", "def"));
	}

	@Test
	void getLongBasic() {
		assertEquals(4242L, HttpJson.getLong("{\"timeMs\":4242}", "timeMs", -1));
	}

	@Test
	void getLongMissingReturnsDefault() {
		assertEquals(-1L, HttpJson.getLong("{\"name\":\"x\"}", "timeMs", -1));
	}

	@Test
	void readBodyWithinLimit() throws IOException {
		assertEquals("hello", HttpJson.readBody(reqWith("hello"), 1024));
	}

	@Test
	void readBodyTooLargeThrows() throws IOException {
		HttpServletRequest req = reqWith("0123456789ABCDEF");
		assertThrows(IOException.class, () -> HttpJson.readBody(req, 8));
	}
}
