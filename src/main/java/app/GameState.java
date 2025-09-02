package app;

public class GameState {
	private String uid;
	private int x;
	private int y;
	private int hp;

	public GameState(String uid, int x, int y, int hp) {
		this.uid = uid;
		this.x = x;
		this.y = y;
		this.hp = hp;
	}

	public String getUid() {
		return uid;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getHp() {
		return hp;
	}
}