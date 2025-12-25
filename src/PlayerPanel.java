import javax.swing.*;
import javax.swing.GrayFilter;
import java.awt.*;

public class PlayerPanel extends JPanel {
    private JLabel cardLabel;
    private JLabel idLabel;
    // 這個變數將會儲存「已經縮放過」的牌背圖片
    private ImageIcon scaledCardBackIcon;
    
    // 設定卡片顯示的目標寬度與高度
    private static final int CARD_WIDTH = 100;
    private static final int CARD_HEIGHT = 140;

    public PlayerPanel(String playerId) {
        this.setOpaque(false);
        this.setLayout(new BorderLayout(0, 5));
        this.setPreferredSize(new Dimension(120, 180));

        // 1. 讀取原始大圖
        ImageIcon originalBack = new ImageIcon("resources/cards_img/card_back.png");
        // 2. 進行縮放
        scaledCardBackIcon = scaleImage(originalBack, CARD_WIDTH, CARD_HEIGHT);
        
        // 3. 關鍵修正：這裡一定要用 scaledCardBackIcon (縮小版)，不能用 originalBack (原始大圖)
        cardLabel = new JLabel(scaledCardBackIcon);
        cardLabel.setHorizontalAlignment(JLabel.CENTER);

        idLabel = new JLabel(playerId, JLabel.CENTER);
        idLabel.setOpaque(true);
        idLabel.setBackground(new Color(45, 35, 60));
        idLabel.setForeground(Color.WHITE);
        idLabel.setFont(new Font("微軟正黑體", Font.BOLD, 14));

        add(cardLabel, BorderLayout.CENTER);
        add(idLabel, BorderLayout.SOUTH);
    }

    private ImageIcon scaleImage(ImageIcon icon, int w, int h) {
        Image img = icon.getImage();
        Image scaledImg = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImg);
    }

    public void updateStatus(boolean isAlive, String trapValue, boolean isMyTurn) {
        if (!isAlive) {
            if (cardLabel.getIcon() != null) {
                Image img = ((ImageIcon)cardLabel.getIcon()).getImage();
                Image grayImg = GrayFilter.createDisabledImage(img);
                cardLabel.setIcon(new ImageIcon(grayImg));
            }
            this.setOpaque(true);
            this.setBackground(Color.DARK_GRAY);
            idLabel.setBackground(Color.BLACK);
        } else {
            this.setOpaque(isMyTurn);
            this.setBackground(isMyTurn ? new Color(255, 255, 0, 100) : null);
            idLabel.setBackground(new Color(45, 35, 60));

            if (trapValue.equals("?")) {
                // 使用預先縮放好的牌背
                cardLabel.setIcon(scaledCardBackIcon);
            } else {
                // 讀取新的數字牌並進行縮放
                String fileName = getCardFileName(trapValue);
                ImageIcon originalCard = new ImageIcon("resources/cards_img/" + fileName);
                cardLabel.setIcon(scaleImage(originalCard, CARD_WIDTH, CARD_HEIGHT));
            }
        }
        
        this.revalidate();
        this.repaint();
    }

    private String getCardFileName(String value) {
        switch (value) {
            case "1": return "ace_of_spades.png";
            case "11": return "jack_of_spades.png";
            case "12": return "queen_of_spades.png";
            case "13": return "king_of_spades.png";
            default: return value + "_of_spades.png";
        }
    }
}