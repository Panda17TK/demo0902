package app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * データ永続化のルートディレクトリを解決する。
 * 優先順位: システムプロパティ arpg.data.dir > 環境変数 ARPG_DATA_DIR > ${user.home}/.arpg-demo0902
 * テストでは -Darpg.data.dir=... で任意のテンポラリへ向けられる。
 */
public final class AppPaths {
	private AppPaths() {
	}

	public static Path dataDir() {
		String prop = System.getProperty("arpg.data.dir");
		String env = System.getenv("ARPG_DATA_DIR");
		String base = (prop != null && !prop.isEmpty()) ? prop
				: (env != null && !env.isEmpty()) ? env
						: System.getProperty("user.home") + "/.arpg-demo0902";
		Path dir = Paths.get(base);
		try {
			Files.createDirectories(dir);
		} catch (Exception ignore) {
			// 生成失敗時は呼び出し側で例外になる
		}
		return dir;
	}
}
