import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GameUI extends JPanel {
    private JLabel currentNumLabel; 
    private JLabel roundLabel;
    private JLabel timerLabel;

    private JPanel northPanel;
    private JPanel southPanel;
    private javax.swing.Timer countdownTimer;
    JButton pass;
    JButton btn1;
    JButton btn2;
    JButton btn3;
    JButton ret;
    private Map<String, PlayerPanel> playerPanels = new HashMap<>();
    private GameClient client; 
    private String myId;
    private int remainingSeconds = 15;
    
    public GameUI(GameClient client, String myId) {
        this.client = client;
        this.myId = myId;
        
        this.setLayout(new BorderLayout(20, 20));
        this.setBackground(new Color(25, 25, 25)); 

        northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        northPanel.setOpaque(false);
        add(northPanel, BorderLayout.NORTH);

        setupCenterInfo();
        setupSouthControls();

        countdownTimer = new javax.swing.Timer(1000, e -> {
            remainingSeconds--;
            if (remainingSeconds >= 0) {
                timerLabel.setText("0:" + String.format("%02d", remainingSeconds));
                
                // 💡 倒數 5 秒內變紅色警告
                if (remainingSeconds <= 5) {
                    timerLabel.setForeground(Color.RED);
                } else {
                    timerLabel.setForeground(Color.BLACK);
                }
            } else {
                // 時間到了，停止計時
                countdownTimer.stop();
                timerLabel.setText("0:00");
            }
        });
    }

    /**
     * 中間資訊區實作
     */
    private void setupCenterInfo() {
        JPanel centerArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 20));
        centerArea.setOpaque(false);

        timerLabel = new JLabel("0:15", JLabel.CENTER);
        timerLabel.setOpaque(true);
        timerLabel.setBackground(new Color(255, 182, 193));
        timerLabel.setPreferredSize(new Dimension(80, 40));

        currentNumLabel = new JLabel("999999", JLabel.CENTER);
        currentNumLabel.setFont(new Font("Arial", Font.BOLD, 80));
        currentNumLabel.setForeground(Color.WHITE);

        roundLabel = new JLabel("Round: 1", JLabel.CENTER);
        roundLabel.setForeground(Color.YELLOW);
        roundLabel.setFont(new Font("Arial", Font.BOLD, 20));
        

        // 牌堆圖片路徑修正與縮放
        ImageIcon originalDeck = new ImageIcon("src/resources/cards_img/card_back.png");
        Image scaledDeckImg = originalDeck.getImage().getScaledInstance(100, 140, Image.SCALE_SMOOTH);
        JLabel deckLabel = new JLabel(new ImageIcon(scaledDeckImg));

        centerArea.add(timerLabel);
        centerArea.add(currentNumLabel);
        centerArea.add(deckLabel);
        centerArea.add(roundLabel);
        add(centerArea, BorderLayout.CENTER);
    }

    /**
     * 下方控制區實作 (整合監聽器功能)
     */
    private void setupSouthControls() {
        southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        southPanel.setOpaque(false);
        pass = createActionButton("PASS", "PASS", "0", new Color(80, 40, 40));
        btn1 = createActionButton("1", "CALL", "1", new Color(30, 45, 70));
        btn2 = createActionButton("2", "CALL", "2", new Color(30, 45, 70));
        btn3 = createActionButton("3", "CALL", "3", new Color(30, 45, 70));
        ret = createActionButton("RETURN", "RETURN", "0", new Color(80, 40, 40));
        // 建立具備功能的按鈕
        southPanel.add(pass);
        southPanel.add(btn1);
        southPanel.add(btn2);
        southPanel.add(btn3);
        southPanel.add(ret);

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

        btn.addActionListener(e -> {
            // 💡 這裡改成陳姿吟的 sendMessage
            String cmd = String.format("ACTION|%s|%s|%s", myId, type, value);
            if (client != null) {
                client.sendMessage(cmd);
            }
            System.out.println("指令已送出: " + cmd);
        });

        return btn;
    }

    /**
     * 解析來自伺服器的 UPDATE 訊息並更新遊戲畫面
     * 格式：UPDATE|目前數字|移動方向|當前玩家ID|玩家狀態清單|輪數|return次數|pass次數
     */
    public void updateGameScreen(String updateMsg) {
        // parts[0]: UPDATE
        // parts[1]: currentNum
        // parts[2]: CW/CCW
        // parts[3]: currentPlayer
        // parts[4]: playersData
        // parts[5]: roundCount
        // parts[6]: returnCount 
        // parts[7]: passCount   
        String[] parts = updateMsg.split("\\|");
        
        if (parts.length < 6) return;

        // 更新中間大數字顯示
        int currentNum = Integer.parseInt(parts[1]); 
        currentNumLabel.setText(String.valueOf(currentNum));    

        // 取得回合相關資訊
        String currentPlayerId = parts[3]; 
        String[] allPlayersData = parts[4].split(";");
        String roundValue = parts[5];

        // 處理玩家本人的控制權限
        boolean isMyTurn = myId.equals(currentPlayerId);
        
        // 先設定按鈕是否可點擊 (如果是我的回合才開啟)
        setControlsEnabled(isMyTurn);

        if (isMyTurn) {
            updateButtonStates(currentNum);
        } else {
            currentNumLabel.setForeground(currentNum >= 11 ? Color.RED : Color.WHITE);
        }

        // 處理輪數顯示
        if (roundLabel != null) {
            roundLabel.setText("Round: " + roundValue);
        }

        // 第一次收到訊息時，初始化所有玩家的面板 (頭像與卡片)
        if (playerPanels.isEmpty()) {
            setupDynamicPanels(allPlayersData);
        }

        // 更新所有玩家的即時狀態 (存活、陷阱數、是否亮邊框)
        for (String data : allPlayersData) {
            String[] info = data.split(":");
            if (info.length < 3) continue;

            String id = info[0];
            boolean isAlive = info[1].equals("1");
            String trapValue = info[2]; // 會拿到"?"(自己的)或數字(別人的)

            PlayerPanel panel = playerPanels.get(id);
            if (panel != null) {
                boolean highlight = id.equals(currentPlayerId);
                panel.updateStatus(isAlive, trapValue, highlight);
            }
        }

        if (parts.length >= 8) {
            int returnLeft = Integer.parseInt(parts[6]);
            int passLeft = Integer.parseInt(parts[7]);

            ret.setEnabled(isMyTurn && returnLeft > 0);
            pass.setEnabled(isMyTurn && passLeft > 0);

            ret.setText(returnLeft == 0 ? "Gone" : "RETURN");
            pass.setText(passLeft == 0 ? "Gone" : "PASS");
        }

        if (countdownTimer != null) {
            countdownTimer.stop();      
            remainingSeconds = 15;    
            timerLabel.setText("0:15"); 
            timerLabel.setForeground(Color.BLACK); 
            countdownTimer.start();    
        }

        if (isMyTurn) {
            timerLabel.setBackground(new Color(255, 100, 100)); // 輪到我，底色變亮紅
        } else {
            timerLabel.setBackground(new Color(182, 255, 193)); // 別人的回合，底色變淡綠
        }
    }

    private void updateButtonStates(int currentNum) {
        btn1.setEnabled(currentNum + 1 <= 13);
        btn2.setEnabled(currentNum + 2 <= 13);
        btn3.setEnabled(currentNum + 3 <= 13);
        if (currentNum >= 11) {
            currentNumLabel.setForeground(Color.RED);
        }else{
        currentNumLabel.setForeground(Color.WHITE);
    }
    }

    private void setupDynamicPanels(String[] allPlayersData) {
        northPanel.removeAll(); 
        playerPanels.clear();

        for (String data : allPlayersData) {
            String[] info = data.split(":");
            String id = info[0]; // 原始ID
            
            // 決定顯示名稱
            String displayName = id.equals(myId) ? id + " (我)" : id;
            
            // 建立並存儲面板
            PlayerPanel p = new PlayerPanel(displayName);
            playerPanels.put(id, p);
            northPanel.add(p);
        }
        northPanel.revalidate();
        northPanel.repaint();
    }

    private void setControlsEnabled(boolean enabled) {
        // 遍歷下方控制區的所有按鈕並啟用/禁用
        for (Component c : southPanel.getComponents()) {
            if (c instanceof JButton) {
                c.setEnabled(enabled);
            }
        }
    }

    
}