import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private GameUI gameUI;
    private GameClient client; 

    public MainFrame(GameClient client) {
        this.client = client;
        String myId = (client != null) ? client.getMyUserName() : "測試玩家";
        System.out.println("目前遊戲視窗載入中，玩家 ID: " + myId);

        setTitle("致命數字 Fatal Number - 遊戲進行中");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 視窗置中

        // 初始化遊戲主畫面
        // 將client傳入GameUI，這樣面板上的按鈕才能送出指令
        gameUI = new GameUI(client, myId); 
        this.getContentPane().add(gameUI);

        setVisible(true);
    }

    
    // 當GameClient收到伺服器的UPDATE訊息時，會呼叫此方法
    // UPDATE|currentNumber|direction|currentPlayer|playerData...
    public void updateUI(String message) {
        // 將字串傳遞給底層的 GameUI 進行解析與渲染
        gameUI.updateGameScreen(message);
    }


    // 當GameClient收到伺服器的WINNER訊息時，會呼叫此方法
    // WINNER|winnerId|deathHistory
    public void triggerWinnerDialog(String message) {
        // 拆解字串：WINNER|贏家ID|遺言清單
        String[] parts = message.split("\\|");
        if (parts.length < 3) return;

        String winnerId = parts[1];
        String deathHistory = parts[2];

        // 顯示結算視窗
        new WinnerDialog(this, winnerId, deathHistory, client).setVisible(true);
    }

    // 測試區
    
    public static void main(String[] args) {
        MainFrame frame = new MainFrame(null);
        
        Timer timer = new Timer(1500, e -> {
            /* 
             * UPDATE | 數字 | 方向 | 當前玩家 | 玩家清單 | 輪數
             * * 模擬場景：
             * - 目前數字：10 (快到13了，按鈕應該會變紅)
             * - 方向：CW (順時針)
             * - 當前玩家：測試玩家(我的回合，按鈕應啟用)
             * - 玩家清單：包含存活狀態與陷阱遮罩
             * - 輪數：3
             */
            String mockData = "UPDATE|10|CW|測試玩家|測試玩家:1:?;Player1:1:5;Player2:1:11;Player3:0:3|3";
            
            System.out.println(">>> 收到模擬數據 (含輪數): " + mockData);
            frame.updateUI(mockData);
        });
        
        timer.setRepeats(false);
        timer.start();
    }
    
    
    
    
}