# mobile-arena エクスポート（demobile への退避）

この `mobile-arena/` ディレクトリは、サーバ不要の静的PWA版サバイバルゲーム一式です
（`demo0902` のゲーム本体を移植し、スコア/セーブを localStorage 化したもの）。

本来は別リポジトリ `Panda17TK/demobile` で管理する想定ですが、本セッションの実行環境は
GitHub アクセスが `panda17tk/demo0902` のみに制限されており、エージェントから直接
`demobile` へ push できないため、**消失防止の退避先としてこのブランチに置いています。**

## demobile へ取り出す手順（あなたの環境で）

このブランチを取得し、`mobile-arena/` の中身だけを `demobile` に push します。

```bash
# 例: ワークツリーから mobile-arena だけ取り出して push
git clone https://github.com/Panda17TK/demo0902.git
cd demo0902
git checkout claude/mobile-arena-export-u36CG

# demobile を別ディレクトリへ用意して中身をコピー
cd ..
git clone https://github.com/Panda17TK/demobile.git
rsync -a --exclude='.git' demo0902/mobile-arena/ demobile/
cd demobile
git add -A && git commit -m "init: Wave Arena (static PWA) from demo0902 export"
git push -u origin main
```

その後、`demobile` の **Settings → Pages → Source =「GitHub Actions」** にすると
`mobile-arena/` 直下の `.github/workflows/pages.yml` 相当（コピー後はリポジトリ直下になる）
で `https://panda17tk.github.io/demobile/` に自動公開されます。

## 動作確認（ローカル）

```bash
cd mobile-arena
node --test test/*.test.mjs       # 21件パス
python3 -m http.server 8080       # → http://localhost:8080/
```

詳細は `mobile-arena/README.md` を参照。
