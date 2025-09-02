// src/main/java/app/GameStateBlob.java
package app;

public class GameStateBlob {
    private final String uid;
    private final String payload;   // JSON文字列をそのまま保存
    private final long updatedAt;

    public GameStateBlob(String uid, String payload, long updatedAt) {
        this.uid = uid;
        this.payload = payload;
        this.updatedAt = updatedAt;
    }
    public String getUid() { return uid; }
    public String getPayload() { return payload; }
    public long getUpdatedAt() { return updatedAt; }
}
