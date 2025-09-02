package app;

import java.time.Instant;

public class ScoreRecord {
	private final String uid;
	private final String name;
	private final long timeMs;
	private Instant createdAt;

	public ScoreRecord(String uid, String name, long timeMs) {
		this.uid = uid;
		this.name = name;
		this.timeMs = timeMs;
		this.createdAt = Instant.now();
	}

	public String getUid() {
		return uid;
	}

	public String getName() {
		return name;
	}

	public long getTimeMs() {
		return timeMs;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant t) {
		this.createdAt = t;
	}
}
