import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * 遊戲等待室面板：還原組員原始設計的 2x2 格狀排版與視覺風格。
 */
public class WaitingPanel extends JPanel {
    private MainFrame frame;
    private GameClient client;
    private JPanel playerGrid;
    private JButton btnReady;
    private boolean isReady = false;

    public WaitingPanel(MainFrame frame, GameClient client) {
        this.frame = frame;
        this.client = client;

        // 設定背景與佈局
        setBackground(new Color(15, 15, 15)); // 深黑色底
        setLayout(new BorderLayout());

        // 建立 2x2 玩家格網
        playerGrid = new JPanel(new GridLayout(2, 2, 25, 25));
        playerGrid.setOpaque(false);
        playerGrid.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        add(playerGrid, BorderLayout.CENTER);

        // 初始空位填充
        setupInitialSlots();

        // 準備按鈕邏輯
        btnReady = new JButton("準備");
        btnReady.setFont(new Font("微軟正黑體", Font.BOLD, 18));
        btnReady.setPreferredSize(new Dimension(150, 60));
        btnReady.setBackground(new Color(70, 70, 70));
        btnReady.setForeground(Color.WHITE);
        btnReady.setFocusPainted(false);

        btnReady.addActionListener(e -> {
            isReady = !isReady; // 切換狀態
            if (isReady) {
                client.sendMessage("READY");
                btnReady.setText("取消準備");
                btnReady.setBackground(new Color(100, 30, 30)); // 變紅色提醒取消
            } else {
                client.sendMessage("CANCEL_READY");
                btnReady.setText("準備");
                btnReady.setBackground(new Color(70, 70, 70));
            }
        });

        // 離開按鈕
        JButton btnLeave = new JButton("離開房間");
        btnLeave.addActionListener(e -> {
            client.sendMessage("LEAVE_ROOM");
        });

        // 底部按鈕面板
        JPanel southPanel = new JPanel();
        southPanel.setOpaque(false); // 保持透明背景
        southPanel.add(btnReady);
        southPanel.add(btnLeave);
        add(southPanel, BorderLayout.SOUTH);
    }

    /**
     * 初始化顯示 4 個空位。
     */
    private void setupInitialSlots() {
        playerGrid.removeAll();
        for (int i = 0; i < 4; i++) {
            playerGrid.add(new PlayerSlot("等待中...", false, false));
        }
    }

    /**
     * 更新面板上的房間顯示名稱。
     */
    public void setRoomName(String name) {
        // 由於 WaitingPanel 已在 MainFrame 內，這裡不設定標題，僅做邏輯保留
        System.out.println("進入房間: " + name);
    }

    /**
     * 接收伺服器資料並更新 4 個格子的玩家狀態。
     * 
     * @param statusData 格式: "Player1:READY;Player2:WAIT;"
     */
    public void updateStatus(String statusData) {
        if (statusData == null || statusData.isEmpty()) {
            return;
        }

        String[] players = statusData.split(";");

        SwingUtilities.invokeLater(() -> {
            playerGrid.removeAll(); // 清空舊格子

            for (String p : players) {
                if (p.isEmpty())
                    continue;
                String[] info = p.split(":");
                String name = info[0];
                boolean readyState = info[1].equals("READY");
                boolean isMe = name.equals(client.getMyUserName());

                playerGrid.add(new PlayerSlot(isMe ? name + " (You)" : name, readyState, isMe));
            }

            // 補足空位到 4 個
            for (int i = players.length; i < 4; i++) {
                playerGrid.add(new PlayerSlot("等待加入...", false, false));
            }

            playerGrid.revalidate();
            playerGrid.repaint();
        });
    }

    /**
     * 內部類別：單一玩家顯示區塊，還原原始視覺設定。
     */
    class PlayerSlot extends JPanel {
        public PlayerSlot(String name, boolean isReady, boolean isMe) {
            setLayout(new BorderLayout());
            Color themeColor = isMe ? new Color(46, 139, 87, 100) : new Color(30, 60, 90, 100);
            Color borderColor = isMe ? new Color(50, 255, 150) : new Color(100, 150, 255);

            setBackground(themeColor);
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(borderColor, 2, true),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            // 頭像佔位
            JLabel avatar = new JLabel("👤", JLabel.CENTER);
            avatar.setFont(new Font("Serif", Font.PLAIN, 50));
            avatar.setForeground(borderColor);
            add(avatar, BorderLayout.CENTER);

            // 文字資訊區
            JPanel infoPanel = new JPanel(new GridLayout(2, 1));
            infoPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(name, JLabel.CENTER);
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setFont(new Font("微軟正黑體", Font.BOLD, 14));

            JLabel statusLabel = new JLabel(isReady ? "● 已準備" : "○ 尚未準備", JLabel.CENTER);
            statusLabel.setForeground(isReady ? Color.GREEN : Color.LIGHT_GRAY);
            statusLabel.setFont(new Font("微軟正黑體", Font.PLAIN, 12));

            infoPanel.add(nameLabel);
            infoPanel.add(statusLabel);
            add(infoPanel, BorderLayout.SOUTH);
        }
    }
}