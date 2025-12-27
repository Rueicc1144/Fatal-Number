public interface GameCommand {
    void execute(GameState state);
    String getPlayerId();
}
