package app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * スコアを JSON Lines（1行=1レコード）で追記保存するファイル DAO。
 * 依存ライブラリを増やさないため最小の手書き JSON で読み書きする。
 */
public class FileScoreDAO implements ScoreDAO {
	private final Path file;
	private final Object lock = new Object();
	// メモリキャッシュ：初回に全行を読み、以降は save で追記しつつキャッシュも更新。
	// listTop の度にファイル全走査するのを避ける。
	private List<ScoreRecord> cache = null;

	private static final Pattern P_UID = Pattern.compile("\"uid\"\\s*:\\s*\"(.*?)\"");
	private static final Pattern P_NAME = Pattern.compile("\"name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
	private static final Pattern P_TIME = Pattern.compile("\"timeMs\"\\s*:\\s*(\\d+)");
	private static final Pattern P_CREATED = Pattern.compile("\"createdAt\"\\s*:\\s*\"(.*?)\"");

	public FileScoreDAO() {
		this(AppPaths.dataDir().resolve("scores.jsonl"));
	}

	public FileScoreDAO(Path file) {
		this.file = file;
	}

	@Override
	public void save(ScoreRecord rec) {
		String line = "{\"uid\":\"" + JsonUtil.escape(rec.getUid())
				+ "\",\"name\":\"" + JsonUtil.escape(rec.getName())
				+ "\",\"timeMs\":" + rec.getTimeMs()
				+ ",\"createdAt\":\"" + (rec.getCreatedAt() != null ? rec.getCreatedAt().toString() : "")
				+ "\"}\n";
		synchronized (lock) {
			try {
				Files.write(file, line.getBytes(StandardCharsets.UTF_8),
						StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			// キャッシュ済みなら追記分も反映（全再読みを避ける）
			if (cache != null) cache.add(rec);
		}
	}

	@Override
	public List<ScoreRecord> listTop(int limit) {
		List<ScoreRecord> all;
		synchronized (lock) {
			if (cache == null) cache = readAll();
			all = new ArrayList<>(cache);
		}
		return all.stream()
				.sorted(Comparator.comparingLong(ScoreRecord::getTimeMs).reversed()
						.thenComparing(ScoreRecord::getCreatedAt))
				.limit(Math.max(1, limit))
				.collect(Collectors.toList());
	}

	private List<ScoreRecord> readAll() {
		List<ScoreRecord> out = new ArrayList<>();
		synchronized (lock) {
			if (!Files.exists(file)) {
				return out;
			}
			List<String> lines;
			try {
				lines = Files.readAllLines(file, StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			for (String ln : lines) {
				ScoreRecord r = parse(ln);
				if (r != null) {
					out.add(r);
				}
			}
		}
		return out;
	}

	private static ScoreRecord parse(String line) {
		if (line == null || line.trim().isEmpty()) {
			return null;
		}
		Matcher mu = P_UID.matcher(line);
		Matcher mn = P_NAME.matcher(line);
		Matcher mt = P_TIME.matcher(line);
		if (!mt.find()) {
			return null;
		}
		String uid = mu.find() ? unescape(mu.group(1)) : "";
		String name = mn.find() ? unescape(mn.group(1)) : "Player";
		long timeMs;
		try {
			timeMs = Long.parseLong(mt.group(1));
		} catch (NumberFormatException e) {
			return null;
		}
		ScoreRecord rec = new ScoreRecord(uid, name, timeMs);
		Matcher mc = P_CREATED.matcher(line);
		if (mc.find() && !mc.group(1).isEmpty()) {
			try {
				rec.setCreatedAt(Instant.parse(mc.group(1)));
			} catch (Exception ignore) {
				// 解析できなければ現在時刻のまま
			}
		}
		return rec;
	}

	private static String unescape(String s) {
		return s.replace("\\\"", "\"").replace("\\\\", "\\");
	}
}
