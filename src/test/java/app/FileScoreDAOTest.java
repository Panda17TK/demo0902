package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileScoreDAOTest {

	@Test
	void savesAndSortsByTimeDesc(@TempDir Path tmp) {
		FileScoreDAO dao = new FileScoreDAO(tmp.resolve("scores.jsonl"));
		dao.save(new ScoreRecord("u1", "Alice", 5000));
		dao.save(new ScoreRecord("u2", "Bob", 12000));
		dao.save(new ScoreRecord("u3", "Carol", 8000));

		List<ScoreRecord> top = dao.listTop(10);
		assertEquals(3, top.size());
		assertEquals("Bob", top.get(0).getName());   // 12000 が先頭
		assertEquals("Carol", top.get(1).getName());  // 8000
		assertEquals("Alice", top.get(2).getName());  // 5000
	}

	@Test
	void limitIsRespected(@TempDir Path tmp) {
		FileScoreDAO dao = new FileScoreDAO(tmp.resolve("scores.jsonl"));
		for (int i = 0; i < 20; i++) {
			dao.save(new ScoreRecord("u", "P" + i, i * 1000));
		}
		assertEquals(5, dao.listTop(5).size());
	}

	@Test
	void persistsAcrossInstances(@TempDir Path tmp) {
		Path f = tmp.resolve("scores.jsonl");
		new FileScoreDAO(f).save(new ScoreRecord("u", "Persist", 4242));
		// 別インスタンス＝サーバ再起動相当
		List<ScoreRecord> top = new FileScoreDAO(f).listTop(10);
		assertEquals(1, top.size());
		assertEquals(4242, top.get(0).getTimeMs());
	}

	@Test
	void cacheReflectsSavesWithoutReread(@TempDir Path tmp) {
		FileScoreDAO dao = new FileScoreDAO(tmp.resolve("scores.jsonl"));
		// listTop でキャッシュを温める
		assertTrue(dao.listTop(10).isEmpty());
		// 以降の save がキャッシュへ反映される（全再読みなしでも見える）
		dao.save(new ScoreRecord("u", "A", 100));
		dao.save(new ScoreRecord("u", "B", 300));
		List<ScoreRecord> top = dao.listTop(10);
		assertEquals(2, top.size());
		assertEquals("B", top.get(0).getName());
	}

	@Test
	void escapesQuotesInName(@TempDir Path tmp) {
		FileScoreDAO dao = new FileScoreDAO(tmp.resolve("scores.jsonl"));
		dao.save(new ScoreRecord("u", "a\"b\\c", 100));
		List<ScoreRecord> top = dao.listTop(10);
		assertEquals(1, top.size());
		assertEquals("a\"b\\c", top.get(0).getName());
	}

	@Test
	void emptyWhenNoFile(@TempDir Path tmp) {
		FileScoreDAO dao = new FileScoreDAO(tmp.resolve("missing.jsonl"));
		assertTrue(dao.listTop(10).isEmpty());
	}
}
