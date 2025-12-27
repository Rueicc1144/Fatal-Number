import javax.swing.*;
import java.awt.*;

public class AuthDialog extends JDialog {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private GameClient client;

    private JTextField loginUserField = new JTextField();
    private JPasswordField loginPassField = new JPasswordField();
    
    private JTextField regUserField = new JTextField();
    private JPasswordField regPassField = new JPasswordField();

    public AuthDialog(GameClient client) {
        this.client = client;
        setTitle("身分驗證");
        setModal(true);
        setSize(350, 250);
        setLocationRelativeTo(null);

        // 建立登入面板
        JPanel loginPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        loginPanel.setBorder(BorderFactory.createTitledBorder("登入系統"));
        loginPanel.add(new JLabel("帳號:")); loginPanel.add(loginUserField);
        loginPanel.add(new JLabel("密碼:")); loginPanel.add(loginPassField);
        
        JButton btnLogin = new JButton("登入");
        btnLogin.addActionListener(e -> handleLogin()); // 💡 執行登入邏輯
        
        JButton btnGoReg = new JButton("前往註冊");
        btnGoReg.addActionListener(e -> cardLayout.show(mainPanel, "REGISTER"));
        
        loginPanel.add(btnLogin);
        loginPanel.add(btnGoReg);

        // 建立註冊面板
        JPanel registerPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        registerPanel.setBorder(BorderFactory.createTitledBorder("帳號註冊"));
        registerPanel.add(new JLabel("帳號:")); registerPanel.add(regUserField);
        registerPanel.add(new JLabel("密碼:")); registerPanel.add(regPassField);
        
        JButton btnRegSubmit = new JButton("提交註冊");
        btnRegSubmit.addActionListener(e -> handleRegister()); // 💡 執行註冊邏輯
        
        JButton btnBackLogin = new JButton("返回登入");
        btnBackLogin.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        
        registerPanel.add(btnRegSubmit);
        registerPanel.add(btnBackLogin);

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(registerPanel, "REGISTER");
        add(mainPanel);
    }

    public String getLoginAccount() {
        return loginUserField.getText().trim();
    }

    private void handleLogin() {
        String user = loginUserField.getText().trim();
        String pass = new String(loginPassField.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請填寫帳號與密碼");
            return;
        }


        // 透過陳姿吟的 GameClient 發送協定
        client.sendMessage("LOGIN|" + user + "|" + pass);
    }

    private void handleRegister() {
        String user = regUserField.getText().trim();
        String pass = new String(regPassField.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請填寫完整註冊資訊");
            return;
        }

        // 透過陳姿吟的 GameClient 發送協定
        client.sendMessage("REGISTER|" + user + "|" + pass);
    }
    
    public void switchToLoginCard() {
        cardLayout.show(mainPanel, "LOGIN");
        regUserField.setText("");
        regPassField.setText("");
    }
}