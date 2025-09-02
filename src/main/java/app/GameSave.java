package app;

public class GameSave {
  private final String uid;
  private final String slot;   // 例: "slot1", "A" など
  private final String json;   // クライアントそのままのJSON
  private final long updatedAt;

  public GameSave(String uid, String slot, String json, long updatedAt){
    this.uid = uid; this.slot = slot; this.json = json; this.updatedAt = updatedAt;
  }
  public String getUid(){ return uid; }
  public String getSlot(){ return slot; }
  public String getJson(){ return json; }
  public long getUpdatedAt(){ return updatedAt; }
}