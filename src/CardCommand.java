public class CardCommand {
    public static class PassCardCommand implements GameCommand {
        private String playerId;
        public PassCardCommand(String playerId) { this.playerId = playerId; }

        @Override
        public void execute(GameState state) {
            if (state.usePass(playerId)) {
                System.out.println(playerId + " 使用了 PASS");
                state.nextTurn();
            } else {
                System.out.println(playerId + " 的 PASS 次數已用完！");
            }
        }
        @Override
        public String getPlayerId() { return playerId; }
    }

    public static class ReturnCardCommand implements GameCommand {
        private String playerId;
        public ReturnCardCommand(String playerId) { this.playerId = playerId; }

        @Override
        public void execute(GameState state) {
            if (state.useReturn(playerId)) {
                System.out.println(playerId + " 使用了 RETURN");
                state.isClockwise = !state.isClockwise;
                state.nextTurn();
            } else {
                System.out.println(playerId + " 的 RETURN 次數已用完！");
            }
        }
        @Override
        public String getPlayerId() { return playerId; }
    }
}