package app;

import java.time.Instant;

public class ScoreRecord {
  private long id;
  private String uid;
  private String name;
  private long timeMs;
  private Instant createdAt;

  public ScoreRecord(){}

  public ScoreRecord(String uid, String name, long timeMs) {
    this.uid = uid;
    this.name = name;
    this.timeMs = timeMs;
    this.createdAt = Instant.now();
  }

  // getters/setters
  public long getId(){ return id; }
  public void setId(long id){ this.id = id; }
  public String getUid(){ return uid; }
  public void setUid(String uid){ this.uid = uid; }
  public String getName(){ return name; }
  public void setName(String name){ this.name = name; }
  public long getTimeMs(){ return timeMs; }
  public void setTimeMs(long timeMs){ this.timeMs = timeMs; }
  public Instant getCreatedAt(){ return createdAt; }
  public void setCreatedAt(Instant createdAt){ this.createdAt = createdAt; }
}