import java.util.*;

public class GameState {
    public int currentNumber = 0;      // 目前喊到的數字 
    public int roundCount = 1;         // 紀錄目前是第幾輪操作
    public boolean isClockwise = true; // 目前方向 
    public int currentPlayerIdx = 0;   // 目前輪到的玩家索引
    public List<String> players = new ArrayList<>(); // 玩家 ID 清單 
    public Map<String, Boolean> playerAliveStatus = new HashMap<>(); // 存活狀態
    private Map<String, Integer> traps = new HashMap<>(); // 陷阱數字 
    public Map<String, Integer> returnLimit = new HashMap<>();
    public Map<String, Integer> passLimit = new HashMap<>();

    public GameState(List<String> playerIds) {
        this.players.addAll(playerIds);
        for (String id : playerIds) {
            playerAliveStatus.put(id, true);
            // 隨機分配 1-13 陷阱數字 
            traps.put(id, (int)(Math.random() * 13) + 1);
            returnLimit.put(id, 1); 
            passLimit.put(id, 1);   
        }
    }

    // 計算下一位存活玩家 
    public void nextTurn() {
        int step = isClockwise ? 1 : -1;
        int total = players.size();
        
        // 記錄移動前的位置
        int oldIdx = currentPlayerIdx;

        do {
            currentPlayerIdx = (currentPlayerIdx + step + total) % total;
        } while (!playerAliveStatus.get(players.get(currentPlayerIdx))); 

        // 判定 Round 增加的時機：
        // 當索引「繞回」或「經過」第 0 位玩家時，代表新的一輪開始
        if (isClockwise && currentPlayerIdx <= oldIdx) roundCount++;
        else if (!isClockwise && currentPlayerIdx >= oldIdx) roundCount++;

        System.out.println("目前是第 " + roundCount + " 輪");
    }

    public int getTrap(String id) { return traps.get(id); }

    public void setPlayerOut(String id) {
        playerAliveStatus.put(id, false);
        System.out.println("玩家 " + id + " 已出局！");
    }

    
    // 核心任務：序列化狀態為字串同步包 
    // 格式：UPDATE|CURRENT_NUM|DIRECTION|CURRENT_PLAYER|PLAYER_DATA
    public String serializeState(String targetPlayerId) {
        StringBuilder sb = new StringBuilder("UPDATE|");
        sb.append(currentNumber).append("|");
        sb.append(isClockwise ? "CW" : "CCW").append("|");
        sb.append(players.get(currentPlayerIdx)).append("|");

        for (int i = 0; i < players.size(); i++) {
            String id = players.get(i);
            sb.append(id).append(":");
            sb.append(playerAliveStatus.get(id) ? "1" : "0").append(":");
            
            // 隱私邏輯：若為接收者本人，隱藏陷阱數字 
            if (id.equals(targetPlayerId)) {
                sb.append("?");
            } else {
                sb.append(traps.get(id));
            }
            if (i < players.size() - 1) sb.append(";");
        }

        sb.append("|").append(roundCount);
        sb.append("|").append(returnLimit.getOrDefault(targetPlayerId, 0));
        sb.append("|").append(passLimit.getOrDefault(targetPlayerId, 0));
        return sb.toString();
    }

    public void resetGame() {
        this.currentNumber = 0;
        
        this.roundCount = 1;
        
        for (String playerId : players) {
            playerAliveStatus.put(playerId, true);
        }
        
        this.isClockwise = true;
        this.currentPlayerIdx = 0;

        System.out.println("遊戲已重置，輪數回到: " + roundCount);
    }
    

    public boolean useReturn(String id) {
        if (returnLimit.getOrDefault(id, 0) > 0) {
            returnLimit.put(id, 0); // 使用後歸零
            return true;
        }
        return false;
    }

    public boolean usePass(String id) {
        if (passLimit.getOrDefault(id, 0) > 0) {
            passLimit.put(id, 0); // 使用後歸零
            return true;
        }
        return false;
    }
}