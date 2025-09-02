package app;

import java.time.Instant;

public class GameSave {
	private final String uid;
	private final String slot;
	private final byte[] blob;
	private Instant updatedAt;

	public GameSave(String uid, String slot, byte[] blob) {
		this.uid = uid;
		this.slot = slot;
		this.blob = blob;
	}

	public String getUid() {
		return uid;
	}

	public String getSlot() {
		return slot;
	}

	public byte[] getBlob() {
		return blob;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant t) {
		this.updatedAt = t;
	}
}