import javax.swing.*;
import java.awt.*;

public class WinnerDialog extends JDialog {
    private GameClient client;
    
    public WinnerDialog(JFrame owner, String winnerId, String deathNotes, GameClient client) {
        super(owner, "遊戲結算", true);
        this.client = client;

        setSize(450, 400);
        setLocationRelativeTo(owner); 
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(new Color(245,245,245));

        // 標題區：顯示獲勝者
        JLabel winnerLabel = new JLabel("恭喜獲勝者: " + winnerId, JLabel.CENTER);
        winnerLabel.setFont(new Font("微軟正黑體", Font.BOLD, 20));
        winnerLabel.setForeground(new Color(184, 134, 11)); // 深紅色
        winnerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        add(winnerLabel, BorderLayout.NORTH);

        // 內容區：顯示出局遺言
        JTextArea notesArea = new JTextArea();
        // 將分號換成換行，方便閱讀
        String formattedNotes = deathNotes.replace(";", "\n");
        notesArea.setText("【 戰鬥回顧 】\n" + formattedNotes);
        notesArea.setEditable(false);
        notesArea.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
        
        JScrollPane scrollPane = new JScrollPane(notesArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("出局診斷報告"));
        add(scrollPane, BorderLayout.CENTER);

        // 按鈕區
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);

        // 重新開始按鈕
        JButton restartButton = new JButton("重新開始");
        restartButton.setFont(new Font("微軟正黑體", Font.BOLD, 14));
        restartButton.setBackground(new Color(60, 179, 113)); // 綠色
        restartButton.setForeground(Color.WHITE);
        restartButton.addActionListener(e -> {
            if (client != null) {
                client.sendMessage("RESTART");
            }
            dispose();
        });

        // 返回大廳按鈕
        JButton lobbyButton = new JButton("返回大廳");
        lobbyButton.setFont(new Font("微軟正黑體", Font.BOLD, 14));
        lobbyButton.setBackground(new Color(70, 130, 180)); // 藍色
        lobbyButton.setForeground(Color.WHITE);
        lobbyButton.addActionListener(e -> {
            if (client != null) {
                client.sendMessage("LEAVE_ROOM");
                client.backToLobby(); 
            }
            dispose();
        });

        buttonPanel.add(restartButton);
        buttonPanel.add(lobbyButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
}