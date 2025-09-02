package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

public class FileGameStateBlobDAO implements GameStateBlobDAO {
    private final Path baseDir;
    // 簡易キャッシュ
    private final ConcurrentHashMap<String, GameStateBlob> cache = new ConcurrentHashMap<>();

    public FileGameStateBlobDAO() {
        // ユーザホーム配下に保存（環境に合わせて変えてOK）
        String home = System.getProperty("user.home", ".");
        this.baseDir = Paths.get(home, ".arpg-saves");
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) { /* ignore */ }
    }

    private Path fileOf(String uid) {
        return baseDir.resolve(uid + ".json");
    }

    @Override
    public void save(GameStateBlob blob) {
        cache.put(blob.getUid(), blob);
        Path f = fileOf(blob.getUid());
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(f, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), StandardCharsets.UTF_8)) {
            // 1行目にUNIXタイム、2行目以降にJSON本体
            w.write(Long.toString(blob.getUpdatedAt()));
            w.write('\n');
            w.write(blob.getPayload() == null ? "null" : blob.getPayload());
        } catch (IOException e) {
            // 実運用ならログ
        }
    }

    @Override
    public GameStateBlob find(String uid) {
        GameStateBlob c = cache.get(uid);
        if (c != null) return c;
        Path f = fileOf(uid);
        if (!Files.exists(f)) return null;
        try (BufferedReader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            String line1 = r.readLine();
            long ts;
            try { ts = Long.parseLong(line1 == null ? "0" : line1.trim()); } catch (NumberFormatException e) { ts = 0L; }
            StringBuilder sb = new StringBuilder();
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                if (!first) sb.append('\n');
                sb.append(line);
                first = false;
            }
            GameStateBlob blob = new GameStateBlob(uid, sb.toString(), ts);
            cache.put(uid, blob);
            return blob;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void delete(String uid) {
        cache.remove(uid);
        try { Files.deleteIfExists(fileOf(uid)); } catch (IOException e) { /* ignore */ }
    }
}