import javax.swing.*;
import java.awt.*;

public class WinnerDialog extends JDialog {
    
    public WinnerDialog(String winnerId, String deathNotes) {
        // 設定為強制回應視窗 (Modal)，使用者必須關閉此視窗才能繼續操作主程式
        setModal(true);
        setTitle("遊戲結算");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 標題區：顯示獲勝者
        JLabel winnerLabel = new JLabel("恭喜獲勝者: " + winnerId, JLabel.CENTER);
        winnerLabel.setFont(new Font("微軟正黑體", Font.BOLD, 20));
        winnerLabel.setForeground(Color.RED);
        add(winnerLabel, BorderLayout.NORTH);

        // 內容區：顯示出局遺言 (使用 JTextArea 處理長文字)
        JTextArea notesArea = new JTextArea();
        notesArea.setText("--- 出局診斷報告 ---\n" + deathNotes.replace(";", "\n"));
        notesArea.setEditable(false);
        notesArea.setLineWrap(true);
        notesArea.setBackground(new Color(240, 240, 240));
        
        JScrollPane scrollPane = new JScrollPane(notesArea);
        add(scrollPane, BorderLayout.CENTER);

        // 按鈕區：關閉視窗
        JButton closeButton = new JButton("返回大廳");
        closeButton.addActionListener(e -> dispose());
        add(closeButton, BorderLayout.SOUTH);
    }
}