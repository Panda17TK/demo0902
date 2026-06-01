# ビルド / 実行ガイド

このプロジェクトは **Eclipse Dynamic Web Project**（Tomcat 9 / Servlet 4.0(javax) / Java 17）ですが、
`pom.xml` を備えており **CLI（Maven）からもビルド・実行**できます。
ディレクトリ構成は Maven 標準（`src/main/java`, `src/main/webapp`）です。

## 前提

- JDK 17 以上（Java 21 でも `--release 17` でビルド可能）
- Maven 3.9 以上
- 初回の依存解決と `cargo:run` での Tomcat 取得にはインターネット接続が必要

## CLI コマンド

### WAR をビルド
```bash
mvn -B -ntp clean package
```
→ `target/demo0902.war` が生成されます。これを任意の Tomcat 9 の `webapps/` に置けば
`http://<host>:8080/demo0902/` で動作します（`welcome-file = /controller`）。

### CLI から Tomcat 9 を起動して実行（推奨：手早い動作確認）
```bash
mvn -B -ntp clean package cargo:run
```
→ Cargo プラグインが Tomcat 9.0.95 を自動ダウンロード・起動し、WAR をデプロイします。
ブラウザで **http://localhost:8080/demo0902/** を開くとゲームが起動します（Ctrl+C で停止）。

### コンパイルのみ
```bash
mvn -B -ntp compile
```

### テスト実行
```bash
# Java（JUnit 5）：DAO・サーブレット・JSON ユーティリティ
mvn -B -ntp test

# JS（Node 組み込みテストランナー）：physics / spatial / los / flowfield 等の純粋関数
node --test "src/test/js/*.test.mjs"
```
CI（GitHub Actions）では JS→Java の順で両方を実行します。

## 永続化（ファイル/JSON）

ランキングとセーブはファイルに保存され、Tomcat 再起動後も残ります（DB 不要）。

- 保存先（優先順）: `-Darpg.data.dir=...` ＞ 環境変数 `ARPG_DATA_DIR` ＞ `~/.arpg-demo0902`
- スコア: `scores.jsonl`（JSON Lines・追記） / セーブ: `saves/<uid>__<slot>.save`
- インメモリに切替: `-Darpg.persistence=memory`（揮発・テスト向け）

例（保存先を指定して起動）:
```bash
mvn -B -ntp clean package cargo:run -Darpg.data.dir=/var/tmp/arpg
```

## PWA（インストール／オフライン）

`manifest.webmanifest` と `sw.js`（Service Worker）を同梱。HTTPS もしくは `localhost`
でアクセスするとアプリシェルがキャッシュされ、オフライン起動やホーム画面への追加が可能です。
（スコア等の API はネットワーク優先のため、オンライン時のみ更新されます。）

## Eclipse での利用

`.project` / `.classpath` / `.settings` は従来どおり Eclipse(WTP) 用に残してあります。
Eclipse からはこれまで通り「サーバーで実行」で起動できます。
Maven として扱いたい場合は *Import → Existing Maven Projects* で `pom.xml` を取り込みます。

> 補足: Eclipse のコンパイル出力 `build/` は自動生成物のため `.gitignore` 済みです。
> Maven の出力は `target/` です。

## メモ

- 外部依存は `javax.servlet`（Servlet API, `provided`）のみ。実行時は Tomcat が提供します。
- DB は使用していない（永続化はファイル/JSON）。DB 化する場合は `DAOFactory` に
  JDBC 実装を足し、コネクタ jar を `WEB-INF/lib/` に置く。
- Java の `--release 17` 指定により、JDK 21 環境でも Tomcat 9(Java 17) 互換の bytecode を出力します。
