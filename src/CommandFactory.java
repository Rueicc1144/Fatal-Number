public class CommandFactory {
    private DeathHistoryLogger logger; // 引用黃家柔的模組

    public CommandFactory(DeathHistoryLogger logger) {
        this.logger = logger;
    }

    // 解析字串並生成對應物件
    public GameCommand createCommand(String message) {
        try {
            // 在 Java 中，管道符號 | 是特殊字元，必須使用 \\| 進行分割
            String[] parts = message.split("\\|");
            
            if (!parts[0].equals("ACTION")) return null;

            String playerId = parts[1];
            String type = parts[2];

            switch (type) {
                case "CALL":
                    int value = Integer.parseInt(parts[3]);
                    // 建立喊數指令並帶入 Logger 進行紀錄
                    return new CallNumberCommand(playerId, value, logger);
                case "PASS":
                    return new CardCommand.PassCardCommand(playerId);
                case "RETURN":
                    return new CardCommand.ReturnCardCommand(playerId);
                default:
                    System.out.println("未知指令類型: " + type);
                    return null;
            }
        } catch (Exception e) {
            System.out.println("指令解析失敗: " + message);
            return null;
        }
    }
}