import java.util.*;

public class DeathHistoryLogger {
    private Map<String, String> deathNotes = new HashMap<>();

    public void recordElimination(String playerId, int trapNumber, int currentCount) {
        String reason = "在第 " + currentCount + " 輪喊到 " + trapNumber + "，不幸踩中陷阱！";
        deathNotes.put(playerId, reason);
        System.out.println("[Logger] 紀錄出局: " + playerId + " -> " + reason);
    }

    public String getAllDeathReasons() {
        if (deathNotes.isEmpty()) return "無人出局";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : deathNotes.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
        }
        return sb.toString();
    }
}