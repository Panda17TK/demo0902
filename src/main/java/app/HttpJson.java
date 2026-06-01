package app;

import java.io.IOException;
import java.io.Reader;

import javax.servlet.http.HttpServletRequest;

/**
 * リクエストボディの安全な読み取りと、最小限の JSON スカラ抽出を提供する。
 * 外部ライブラリを増やさないため自前実装だが、サイズ上限と文字列エスケープ復元を
 * 正しく扱う点で従来の素朴な正規表現抽出より堅牢。
 */
public final class HttpJson {
	private HttpJson() {
	}

	/** ボディ読み取りの上限（バイト相当の文字数）。これを超えたら例外。 */
	public static final int DEFAULT_MAX = 64 * 1024;

	/** 本文を上限付きで読み取る。超過時は IOException。 */
	public static String readBody(HttpServletRequest req, int maxChars) throws IOException {
		StringBuilder sb = new StringBuilder(Math.min(maxChars, 4096));
		try (Reader r = req.getReader()) {
			char[] buf = new char[4096];
			int n;
			while ((n = r.read(buf)) != -1) {
				if (sb.length() + n > maxChars) {
					throw new IOException("request body too large");
				}
				sb.append(buf, 0, n);
			}
		}
		return sb.toString();
	}

	/**
	 * JSON 文字列から指定キーの文字列値を取り出す（エスケープを復元）。
	 * 見つからなければ def を返す。ネスト・配列は対象外（フラットな {"k":"v"} 想定）。
	 */
	public static String getString(String json, String key, String def) {
		if (json == null) return def;
		String pat = "\"" + key + "\"";
		int ki = json.indexOf(pat);
		if (ki < 0) return def;
		int i = ki + pat.length();
		// コロンまで
		while (i < json.length() && json.charAt(i) != ':') i++;
		i++;
		while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
		if (i >= json.length() || json.charAt(i) != '"') return def;
		i++; // 開始の "
		StringBuilder sb = new StringBuilder();
		while (i < json.length()) {
			char c = json.charAt(i++);
			if (c == '\\') {
				if (i >= json.length()) break;
				char e = json.charAt(i++);
				switch (e) {
					case '"': sb.append('"'); break;
					case '\\': sb.append('\\'); break;
					case '/': sb.append('/'); break;
					case 'b': sb.append('\b'); break;
					case 'f': sb.append('\f'); break;
					case 'n': sb.append('\n'); break;
					case 'r': sb.append('\r'); break;
					case 't': sb.append('\t'); break;
					case 'u':
						if (i + 4 <= json.length()) {
							try {
								sb.append((char) Integer.parseInt(json.substring(i, i + 4), 16));
							} catch (NumberFormatException ignore) {
							}
							i += 4;
						}
						break;
					default: sb.append(e);
				}
			} else if (c == '"') {
				return sb.toString();
			} else {
				sb.append(c);
			}
		}
		return def;
	}

	/** JSON から指定キーの long を取り出す。見つからない/不正なら def。 */
	public static long getLong(String json, String key, long def) {
		if (json == null) return def;
		String pat = "\"" + key + "\"";
		int ki = json.indexOf(pat);
		if (ki < 0) return def;
		int i = ki + pat.length();
		while (i < json.length() && json.charAt(i) != ':') i++;
		i++;
		while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
		int start = i;
		if (i < json.length() && (json.charAt(i) == '-' || json.charAt(i) == '+')) i++;
		while (i < json.length() && Character.isDigit(json.charAt(i))) i++;
		if (i == start) return def;
		try {
			return Long.parseLong(json.substring(start, i));
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
