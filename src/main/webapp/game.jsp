<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
	isELIgnored="false"%>
<!doctype html>
<html lang="ja">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>ARPG</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/game.css">
</head>
<body>
	<div id="wrap">
		<canvas id="game" width="1280" height="720"></canvas>
		<div id="hud"></div>
		<div id="help">
			<div style="margin-bottom: 6px">
				<span class="pill">操作</span>
			</div>
			<div>
				移動: <b>WASD / 矢印</b> 近接: <b>J</b> 射撃/投擲: <b>K</b> リロード: <b>R</b>
				武器切替: <b>1/2/3/4/5</b> ドア:<b>E</b><br /> <b>ダッシュ</b>: <b>Shift</b>（スタミナ消費、<b>速度×2</b>）
				<b>壁設置</b>: <b>F</b><br /> セーブ: <b>P</b> ロード: <b>L</b> ポーズ: <b>Esc</b>
			</div>
		</div>
		<div id="overlay" class="overlay hidden">
			<div class="panel">
				<h2>Game Over</h2>
				<div class="lines" id="result-lines"></div>
				<div class="form">
					<label>名前: <input type="text" id="player-name"
						maxlength="32" placeholder="Player"></label>
					<button id="btn-save-score">戦績を保存</button>
					<button id="btn-cancel">閉じる</button>
				</div>
				<div class="board" id="score-board"></div>
			</div>
			<div id="toast"></div>
		</div>
		<script>
    // サーバ側のコンテキストパスをJSへ
    window.CTX = '<%=request.getContextPath()%>
		';
	</script>
		<script type="module"
			src="${pageContext.request.contextPath}/js/main.js"></script>
</body>
</html>