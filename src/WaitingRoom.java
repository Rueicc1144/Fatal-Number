import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
public class WaitingRoom extends JFrame {
    private GameClient client;
    private JPanel playerGrid;
    private JButton btnReady;
    JButton btnLeave;
    private boolean isReady = false; 

    public WaitingRoom(GameClient client) {
        this.client = client;
        setTitle("Dead Number - Waiting Room");
        setSize(650, 500);
        getContentPane().setBackground(new Color(15, 15, 15)); // 深黑色底
        setLayout(new BorderLayout());

        playerGrid = new JPanel(new GridLayout(2, 2, 25, 25));
        playerGrid.setOpaque(false);
        playerGrid.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        add(playerGrid, BorderLayout.CENTER);
        
        setupInitialSlots();

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

        btnLeave = new JButton("離開房間");
        btnLeave.addActionListener(e -> {
            client.sendMessage("LEAVE_ROOM");
        });

        JPanel southPanel = new JPanel();
        southPanel.add(btnReady);
        southPanel.add(btnLeave);
        add(southPanel, BorderLayout.SOUTH);
    }

    private void setupInitialSlots() {
        playerGrid.removeAll();
        for (int i = 0; i < 4; i++) {
            playerGrid.add(new PlayerSlot("等待中...", false, false));
        }
    }

    public void setRoomName(String name) {
        setTitle("房間: " + name + " - Waiting Room");
    }

    public void updateStatus(String playersData) {
        // playersData格式範例: "A:READY;B:WAIT;"

        if (playersData == null || playersData.isEmpty()) {
            System.out.println("收到空的玩家狀態。");
            return;
        }

        
        String[] players = playersData.split(";");

        SwingUtilities.invokeLater(() -> {
            playerGrid.removeAll(); // 先清空舊的格子
            
            for (String p : players) {
                if (p.isEmpty()) continue;
                String[] info = p.split(":");
                String name = info[0];
                boolean isReady = info[1].equals("READY");
                boolean isMe = name.equals(client.getMyUserName());
                
                playerGrid.add(new PlayerSlot(isMe ? name + " (You)" : name, isReady, isMe));
            }
            
            // 補足空位到4個
            for (int i = players.length; i < 4; i++) {
                playerGrid.add(new PlayerSlot("等待加入...", false, false));
            }
            
            playerGrid.revalidate();
            playerGrid.repaint();
        });
    }

    class PlayerSlot extends JPanel {
        public PlayerSlot(String name, boolean isReady, boolean isMe) {
            setLayout(new BorderLayout());
            Color themeColor = isMe ? new Color(46, 139, 87, 100) : new Color(30, 60, 90, 100);
            Color borderColor = isMe ? new Color(50, 255, 150) : new Color(100, 150, 255);

            setBackground(themeColor);
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderColor, 2, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            // 中間放頭像佔位
            JLabel avatar = new JLabel("👤", JLabel.CENTER);
            avatar.setFont(new Font("Serif", Font.PLAIN, 50));
            avatar.setForeground(borderColor);
            add(avatar, BorderLayout.CENTER);

            // 下方文字區
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