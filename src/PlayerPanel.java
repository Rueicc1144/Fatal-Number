import javax.swing.*;
import java.awt.*;
import java.io.File;

public class PlayerPanel extends JPanel {
    private JLabel cardLabel;
    private JLabel idLabel;
    private ImageIcon scaledCardBackIcon;
    
    private static final int CARD_WIDTH = 100;
    private static final int CARD_HEIGHT = 140;
    
    private final String IMG_PATH = "resources/cards_img/";
    
    public PlayerPanel(String playerId) {
        this.setOpaque(false);
        this.setLayout(new BorderLayout(0, 5));
        this.setPreferredSize(new Dimension(120, 180));

        this.scaledCardBackIcon = loadAndScale(IMG_PATH + "card_back.png");
                
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

    private ImageIcon loadAndScale(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("找不到檔案: " + path);
            return null;
        }

        ImageIcon icon = new ImageIcon(file.getAbsolutePath());
        Image img = icon.getImage();
        Image scaledImg = img.getScaledInstance(CARD_WIDTH, CARD_HEIGHT, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImg);
    }

    public void updateStatus(boolean isAlive, String trapValue, boolean isMyTurn) {
        if (!isAlive) {
            // 出局處理
            if (cardLabel.getIcon() != null) {
                Image img = ((ImageIcon)cardLabel.getIcon()).getImage();
                cardLabel.setIcon(new ImageIcon(GrayFilter.createDisabledImage(img)));
            }
            this.setOpaque(true);
            this.setBackground(new Color(50, 50, 50, 150));
            idLabel.setBackground(Color.BLACK);
        } else {
            // 存活且是該回合玩家：高亮背景
            this.setOpaque(isMyTurn);
            this.setBackground(isMyTurn ? new Color(255, 255, 0, 100) : null);
            idLabel.setBackground(new Color(45, 35, 60));

            if (trapValue.equals("?")) {
                cardLabel.setIcon(scaledCardBackIcon);
            } else {
                String fileName = getCardFileName(trapValue);
                ImageIcon faceCard = loadAndScale(IMG_PATH + fileName);
                if (faceCard != null) {
                    cardLabel.setIcon(faceCard);
                }
            }
        }
        
        this.revalidate();
        this.repaint();
    }

    private String getCardFileName(String value) {
        String name;
        switch (value) {
            case "1":  name = "ace";   break;
            case "11": name = "jack";  break;
            case "12": name = "queen"; break;
            case "13": name = "king";  break;
            default:   name = value;   break;
        }
        return name + "_of_spades.png";
    }
}