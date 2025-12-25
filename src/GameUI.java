import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class GameUI extends JPanel {
    private JLabel currentNumLabel;
    private Map<String, PlayerPanel> playerPanels = new HashMap<>();
    
    // 模擬測試使用的玩家 ID
    private String myId = "梁祐齊"; 

    public GameUI() {
        this.setLayout(new BorderLayout(20, 20));
        this.setBackground(new Color(25, 25, 25)); 

        setupNorthPlayers();
        setupCenterInfo();
        setupSouthControls();
    }

    /**
     * 上方玩家區實作
     */
    private void setupNorthPlayers() {
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        northPanel.setOpaque(false);
        
        // 建立預設玩家，確保 ID 與測試字串匹配
        String[] ids = {"Player1", "Player2", "Player3", "You"};
        for (String id : ids) {
            PlayerPanel p = new PlayerPanel(id);
            playerPanels.put(id, p);
            northPanel.add(p);
        }
        add(northPanel, BorderLayout.NORTH);
    }

    /**
     * 中間資訊區實作
     */
    private void setupCenterInfo() {
        JPanel centerArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 20));
        centerArea.setOpaque(false);

        JLabel timerLabel = new JLabel("0:15", JLabel.CENTER);
        timerLabel.setOpaque(true);
        timerLabel.setBackground(new Color(255, 182, 193));
        timerLabel.setPreferredSize(new Dimension(80, 40));

        currentNumLabel = new JLabel("0", JLabel.CENTER);
        currentNumLabel.setFont(new Font("Arial", Font.BOLD, 80));
        currentNumLabel.setForeground(Color.WHITE);

        // 牌堆圖片路徑修正與縮放
        ImageIcon originalDeck = new ImageIcon("src/resources/cards_img/card_back.png");
        Image scaledDeckImg = originalDeck.getImage().getScaledInstance(100, 140, Image.SCALE_SMOOTH);
        JLabel deckLabel = new JLabel(new ImageIcon(scaledDeckImg));

        centerArea.add(timerLabel);
        centerArea.add(currentNumLabel);
        centerArea.add(deckLabel);
        add(centerArea, BorderLayout.CENTER);
    }

    /**
     * 下方控制區實作 (整合監聽器功能)
     */
    private void setupSouthControls() {
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        southPanel.setOpaque(false);

        // 建立具備功能的按鈕
        southPanel.add(createActionButton("PASS", "PASS", "", new Color(80, 40, 40)));
        southPanel.add(createActionButton("1", "CALL", "1", new Color(30, 45, 70)));
        southPanel.add(createActionButton("2", "CALL", "2", new Color(30, 45, 70)));
        southPanel.add(createActionButton("3", "CALL", "3", new Color(30, 45, 70)));
        southPanel.add(createActionButton("RETURN", "RETURN", "", new Color(80, 40, 40)));

        add(southPanel, BorderLayout.SOUTH);
    }

    /**
     * 輔助方法：建立按鈕並綁定指令發送功能
     * 協定格式：ACTION|ID|TYPE|VALUE
     */
    private JButton createActionButton(String text, String type, String value, Color bg) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(115, 50));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 18));
        btn.setFocusPainted(false);

        // 點擊事件：產生指令字串並印出
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String cmd = String.format("ACTION|%s|%s|%s", myId, type, value);
                System.out.println("成功觸發按鈕！準備送出指令: " + cmd);
            }
        });

        return btn;
    }

    /**
     * 解析 UPDATE 訊息並更新畫面
     */
    public void updateGameScreen(String updateMsg) {
        String[] parts = updateMsg.split("\\|");
        if (parts.length < 5) return;

        currentNumLabel.setText(parts[1]);

        String currentPlayer = parts[3];
        String[] allPlayersData = parts[4].split(";");
        for (String data : allPlayersData) {
            String[] info = data.split(":");
            String id = info[0];
            boolean isAlive = info[1].equals("1");
            String trapValue = info[2];

            PlayerPanel panel = playerPanels.get(id);
            if (panel != null) {
                panel.updateStatus(isAlive, trapValue, id.equals(currentPlayer));
            }
        }
        
        this.revalidate();
        this.repaint();
    }

    /**
     * 顯示結算畫面
     */
    public void showWinnerScreen(String winnerInfo) {
        String[] parts = winnerInfo.split("\\|");
        if (parts.length < 3) return;
        
        String winnerId = parts[1];
        String deathNotes = parts[2];
        
        // 呼叫 WinnerDialog 進行渲染
        new WinnerDialog(winnerId, deathNotes).setVisible(true);
    }
}