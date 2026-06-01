package app;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileGameSaveDAOTest {

	private static byte[] bytes(String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	void saveFindRoundTrip(@TempDir Path tmp) {
		FileGameSaveDAO dao = new FileGameSaveDAO(tmp);
		dao.save(new GameSave("user1", "slot1", bytes("{\"hp\":100}")));
		GameSave got = dao.find("user1", "slot1");
		assertArrayEquals(bytes("{\"hp\":100}"), got.getBlob());
	}

	@Test
	void overwriteUpdatesBlob(@TempDir Path tmp) {
		FileGameSaveDAO dao = new FileGameSaveDAO(tmp);
		dao.save(new GameSave("u", "s", bytes("old")));
		dao.save(new GameSave("u", "s", bytes("new")));
		assertArrayEquals(bytes("new"), dao.find("u", "s").getBlob());
	}

	@Test
	void listReturnsOnlyThatUsersSlots(@TempDir Path tmp) {
		FileGameSaveDAO dao = new FileGameSaveDAO(tmp);
		dao.save(new GameSave("u1", "a", bytes("1")));
		dao.save(new GameSave("u1", "b", bytes("2")));
		dao.save(new GameSave("u2", "a", bytes("3")));
		List<GameSave> u1 = dao.list("u1");
		assertEquals(2, u1.size());
		assertEquals(1, dao.list("u2").size());
	}

	@Test
	void deleteRemovesSlot(@TempDir Path tmp) {
		FileGameSaveDAO dao = new FileGameSaveDAO(tmp);
		dao.save(new GameSave("u", "s", bytes("x")));
		dao.delete("u", "s");
		assertNull(dao.find("u", "s"));
	}

	@Test
	void persistsAcrossInstances(@TempDir Path tmp) {
		new FileGameSaveDAO(tmp).save(new GameSave("u", "s", bytes("keep")));
		assertArrayEquals(bytes("keep"), new FileGameSaveDAO(tmp).find("u", "s").getBlob());
	}

	@Test
	void sanitizesUnsafeSlotNames(@TempDir Path tmp) {
		FileGameSaveDAO dao = new FileGameSaveDAO(tmp);
		// パス・トラバーサルを試みても dir 外に出ない（find で読めること＝同じ安全名に正規化）
		dao.save(new GameSave("u", "../../evil", bytes("safe")));
		assertArrayEquals(bytes("safe"), dao.find("u", "../../evil").getBlob());
	}

	@Test
	void safeStripsPathChars() {
		// スラッシュは _ に置換（ドットは許可）。重要なのは区切り文字が消えること。
		assertEquals(".._.._etc_passwd", FileGameSaveDAO.safe("../../etc/passwd"));
		assertTrue(!FileGameSaveDAO.safe("a/b\\c").contains("/"));
		assertTrue(!FileGameSaveDAO.safe("a/b\\c").contains("\\"));
		assertEquals("_", FileGameSaveDAO.safe(""));
		assertEquals("_", FileGameSaveDAO.safe(null));
	}
}
