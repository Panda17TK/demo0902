package app;

public interface GameStateBlobDAO {
	void save(GameStateBlob blob);

	GameStateBlob find(String uid);

	void delete(String uid);
}