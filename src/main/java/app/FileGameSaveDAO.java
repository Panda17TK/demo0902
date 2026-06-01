package app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ゲームセーブを「1スロット=1ファイル」で保存するファイル DAO。
 * ファイル名は uid と slot を安全化して "<uid>__<slot>.save" とする。
 * updatedAt はファイルの最終更新時刻を用いる。
 */
public class FileGameSaveDAO implements GameSaveDAO {
	private final Path dir;
	private final Object lock = new Object();

	public FileGameSaveDAO() {
		this(AppPaths.dataDir().resolve("saves"));
	}

	public FileGameSaveDAO(Path dir) {
		this.dir = dir;
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// パス・トラバーサルや不正文字を防ぐためのサニタイズ
	static String safe(String s) {
		if (s == null) {
			return "_";
		}
		String out = s.replaceAll("[^A-Za-z0-9._-]", "_");
		return out.isEmpty() ? "_" : out;
	}

	private Path fileFor(String uid, String slot) {
		return dir.resolve(safe(uid) + "__" + safe(slot) + ".save");
	}

	@Override
	public void save(GameSave gs) {
		synchronized (lock) {
			Path target = fileFor(gs.getUid(), gs.getSlot());
			// テンポラリへ書いてから原子的にリネーム（半端書き込み/競合読み取りを防ぐ）
			Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
			try {
				Files.write(tmp, gs.getBlob());
				try {
					Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException atomicUnsupported) {
					// 一部FSでATOMIC_MOVE非対応 → 通常moveにフォールバック
					Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				try { Files.deleteIfExists(tmp); } catch (IOException ignore) { }
				throw new UncheckedIOException(e);
			}
		}
	}

	@Override
	public GameSave find(String uid, String slot) {
		synchronized (lock) {
			Path f = fileFor(uid, slot);
			if (!Files.exists(f)) {
				return null;
			}
			try {
				byte[] blob = Files.readAllBytes(f);
				GameSave gs = new GameSave(uid, slot, blob);
				gs.setUpdatedAt(Files.getLastModifiedTime(f).toInstant());
				return gs;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Override
	public List<GameSave> list(String uid) {
		List<GameSave> out = new ArrayList<>();
		synchronized (lock) {
			String prefix = safe(uid) + "__";
			if (!Files.exists(dir)) {
				return out;
			}
			try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
				List<Path> files = new ArrayList<>();
				stream.forEach(files::add);
				for (Path f : files) {
					String fn = f.getFileName().toString();
					if (!fn.startsWith(prefix) || !fn.endsWith(".save")) {
						continue;
					}
					String slot = fn.substring(prefix.length(), fn.length() - ".save".length());
					GameSave gs = new GameSave(uid, slot, null);
					try {
						gs.setUpdatedAt(Files.getLastModifiedTime(f).toInstant());
					} catch (IOException ignore) {
						gs.setUpdatedAt(Instant.EPOCH);
					}
					out.add(gs);
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		// 新しい順
		out.sort((a, b) -> {
			Instant ia = a.getUpdatedAt(), ib = b.getUpdatedAt();
			if (ia == null && ib == null) return 0;
			if (ia == null) return 1;
			if (ib == null) return -1;
			return ib.compareTo(ia);
		});
		return out;
	}

	@Override
	public void delete(String uid, String slot) {
		synchronized (lock) {
			try {
				Files.deleteIfExists(fileFor(uid, slot));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
}
