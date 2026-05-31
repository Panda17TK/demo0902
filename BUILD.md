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

## Eclipse での利用

`.project` / `.classpath` / `.settings` は従来どおり Eclipse(WTP) 用に残してあります。
Eclipse からはこれまで通り「サーバーで実行」で起動できます。
Maven として扱いたい場合は *Import → Existing Maven Projects* で `pom.xml` を取り込みます。

> 補足: Eclipse のコンパイル出力 `build/` は自動生成物のため `.gitignore` 済みです。
> Maven の出力は `target/` です。

## メモ

- 外部依存は `javax.servlet`（Servlet API, `provided`）のみ。実行時は Tomcat が提供します。
- `WEB-INF/lib/mysql-connector-j-8.1.0.jar` は将来の DB 化用に同梱（現状 JDBC 未使用）。
- Java の `--release 17` 指定により、JDK 21 環境でも Tomcat 9(Java 17) 互換の bytecode を出力します。
