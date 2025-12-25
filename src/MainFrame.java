import javax.swing.*;

public class MainFrame extends JFrame {
    private GameUI gameUI;

    public MainFrame() {
        // 基本視窗設定
        setTitle("致命數字 Fatal Number");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 視窗置中

        // 初始化遊戲主畫面
        gameUI = new GameUI();
        this.add(gameUI);

        setVisible(true);
    }

    // public static void main(String[] args) {
    //     // 使用 Swing 的事件分配執行緒啟動 UI
    //     SwingUtilities.invokeLater(() -> new MainFrame());
    // }

    // 硬編碼，模擬遊戲進行
    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        GameUI ui = (GameUI) frame.getContentPane().getComponent(0);
    
        // 模擬過三秒後，Server 傳來第一個狀態更新
        Timer timer = new Timer(3000, e -> {
            String mockData = "UPDATE|8|CW|Player_South|Player_South:1:?;Player1:1:8;Player2:1:5;Player3:1:3";
            ui.updateGameScreen(mockData);  
            System.out.println("模擬更新完成");
        });
        timer.setRepeats(false);
        timer.start();

        Timer winnerTimer = new Timer(5000, e -> {
            // 模擬結算字串格式
            String mockWinner = "WINNER|梁祐齊|Player1:踩到陷阱出局;Player2:點數過大出局;Player3:被迴轉牌害死";
            ui.showWinnerScreen(mockWinner);
            System.out.println("模擬遊戲結束：顯示獲勝畫面");
        });
        winnerTimer.setRepeats(false);
        winnerTimer.start();
    }
}
