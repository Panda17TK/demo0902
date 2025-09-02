package app;

import java.util.List;

public interface GameSaveDAO {
  void save(String uid, String slot, String json);
  GameSave find(String uid, String slot);          // 指定スロット
  GameSave findLatest(String uid);                 // 最新
  List<GameSave> list(String uid);                 // 一覧
  void delete(String uid, String slot);            // 指定
  void deleteAll(String uid);                      // 全削除
}