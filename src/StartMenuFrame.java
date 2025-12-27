import javax.swing.*;
import java.awt.*;

public class StartMenuFrame extends JFrame {
    public StartMenuFrame() {
        setTitle("致命數字 (Fatal Number) - 入口");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 視窗居中

        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        JLabel titleLabel = new JLabel("致命數字", JLabel.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 40));
        
        JButton btnStart = new JButton("開始遊戲");
        JButton btnRules = new JButton("遊戲規則");

        // 點擊開始：開啟登入對話框
        btnStart.addActionListener(e -> {
            GameClient client = new GameClient("127.0.0.1", 8888);
            client.startApp(); 
            this.setVisible(false);
        });

        // 點擊規則：彈出訊息框
        btnRules.addActionListener(e -> JOptionPane.showMessageDialog(this, 
            "1. 輪流喊數字，最多連喊三個。\n2. 每個人都有一個隱藏陷阱數字。\n3. 喊到自己陷阱者出局！", 
            "遊戲規則", JOptionPane.INFORMATION_MESSAGE));

        panel.add(titleLabel);
        panel.add(btnStart);
        panel.add(btnRules);
        add(panel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StartMenuFrame().setVisible(true));
    }
}