public class CallNumberCommand implements GameCommand {
    private String playerId;
    private int count;
    private DeathHistoryLogger logger;

    public CallNumberCommand(String playerId, int count, DeathHistoryLogger logger) {
        this.playerId = playerId;
        this.count = count;
        this.logger = logger;
    }

    @Override
    public void execute(GameState state) {
        // 檢查機制：如果目前數字 + 喊的點數 > 13，則禁止操作
        if (state.currentNumber + count > 13) {
            return;
        }

        // 執行累加與判定
        for(int i = 0; i < count; i++){
            state.currentNumber++;
            
            // 判定是否踩雷
            if (state.currentNumber == state.getTrap(playerId)) {
                logger.recordElimination(playerId, state.currentNumber, state.roundCount);
                state.setPlayerOut(playerId);
                
                state.currentNumber = 0; 
                state.nextTurn();
                return; 
            }
        }

        // 達標歸零機制：如果剛好喊到 13，將數字重置為 0
        if(state.currentNumber == 13){
            state.currentNumber = 0;
        }

        // 正常換下一位
        state.nextTurn(); 
    }

    @Override
    public String getPlayerId() { return playerId; }
}