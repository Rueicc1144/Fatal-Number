import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * 遊戲身分驗證面板。
 * 負責處理使用者登入介面呈現與驗證請求發送。
 */
public class AuthPanel extends JPanel {
    private MainFrame frame;
    private GameClient client;
    private JTextField userField;
    private JPasswordField passField;
    private BufferedImage bgImage;

    // UI 視覺規範常數
    private static final Color ACCENT_WHITE = new Color(240, 240, 240);
    private static final Color BORDER_COLOR = new Color(255, 255, 255, 50);
    private static final Font TITLE_FONT_ZH = new Font("Microsoft JhengHei", Font.BOLD, 36);
    private static final Font TITLE_FONT_EN = new Font("Georgia", Font.ITALIC, 20);

    /**
     * 建構登入面板並初始化組件佈局。
     * * @param frame 主視窗控制器
     * 
     * @param client 網路通訊客戶端
     */
    public AuthPanel(MainFrame frame, GameClient client) {
        this.frame = frame;
        this.client = client;

        loadResources();
        setLayout(new GridBagLayout());

        initComponents();
    }

    /**
     * 載入介面所需的靜態資源。
     */
    private void loadResources() {
        try {
            // 執行環境：於 src 目錄下執行 java GameClient
            bgImage = ImageIO.read(new File("resources/background.jpg"));
        } catch (IOException e) {
            setBackground(Color.BLACK);
        }
    }

    /**
     * 初始化介面組件。
     */
    private void initComponents() {
        JPanel loginContainer = createLoginCard();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 100, 0);
        add(loginContainer, gbc);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    /**
     * 建立具備半透明效果的登入表單容器。
     * * @return 封裝後的登入卡片面板
     */
    private JPanel createLoginCard() {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(45, 60, 45, 60));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;

        // 系統標題
        JLabel lbZh = new JLabel("致命數字", JLabel.CENTER);
        lbZh.setFont(TITLE_FONT_ZH);
        lbZh.setForeground(ACCENT_WHITE);
        c.gridy = 0;
        card.add(lbZh, c);

        JLabel lbEn = new JLabel("Fatal Number", JLabel.CENTER);
        lbEn.setFont(TITLE_FONT_EN);
        lbEn.setForeground(new Color(160, 160, 160));
        c.gridy = 1;
        c.insets = new Insets(5, 0, 45, 0);
        card.add(lbEn, c);

        // 使用者輸入區
        userField = new JTextField(15);
        styleInput(userField);
        c.gridy = 2;
        c.insets = new Insets(0, 0, 15, 0);
        card.add(userField, c);

        passField = new JPasswordField(15);
        styleInput(passField);
        c.gridy = 3;
        c.insets = new Insets(0, 0, 35, 0);
        card.add(passField, c);

        // 互動按鈕
        JButton btnLogin = new JButton("ACCESS SYSTEM");
        styleButton(btnLogin);
        btnLogin.addActionListener(e -> {
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword()).trim();
            if (!user.isEmpty()) {
                client.sendMessage("LOGIN|" + user + "|" + pass);
            }
        });
        c.gridy = 4;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(btnLogin, c);

        return card;
    }

    /**
     * 套用統一的輸入框樣式。
     */
    private void styleInput(JTextField f) {
        f.setFont(new Font("Consolas", Font.PLAIN, 16));
        f.setForeground(Color.WHITE);
        f.setBackground(new Color(255, 255, 255, 25));
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 40)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        f.setOpaque(false);
    }

    /**
     * 套用按鈕組件樣式。
     */
    private void styleButton(JButton b) {
        b.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
        b.setForeground(Color.BLACK);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));

        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setBackground(new Color(210, 210, 210));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(Color.WHITE);
            }
        });
    }

    /**
     * 取得目前輸入的帳號資訊。
     * * @return 使用者帳號字串
     */
    public String getAccount() {
        return userField.getText().trim();
    }
}