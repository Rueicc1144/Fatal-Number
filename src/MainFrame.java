import javax.swing.*;
import java.awt.*;

/**
 * 核心視窗容器：管理所有遊戲階段的切換 (AUTH, LOBBY, WAITING, GAME)
 */
public class MainFrame extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContainer = new JPanel(cardLayout);
    private GameClient client;

    private AuthPanel authPanel;
    private LobbyPanel lobbyPanel;
    private WaitingPanel waitingPanel; // 新增：等待室面板
    private GameUI gameUI;

    public MainFrame(GameClient client) {
        this.client = client;

        setTitle("致命數字 Fatal Number");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 初始化所有基礎面板
        authPanel = new AuthPanel(this, client);
        lobbyPanel = new LobbyPanel(this, client);
        waitingPanel = new WaitingPanel(this, client); // 初始化等待室

        // 將面板註冊到 CardLayout 容器中
        mainContainer.add(authPanel, "AUTH");
        mainContainer.add(lobbyPanel, "LOBBY");
        mainContainer.add(waitingPanel, "WAITING"); // 註冊等待室

        add(mainContainer);
        showPanel("AUTH"); // 初始畫面設為登入

        setVisible(true);
    }

    /**
     * 切換顯示面板
     */
    public void showPanel(String name) {
        cardLayout.show(mainContainer, name);
    }

    /**
     * 當建立房間成功時，設定房間名稱並跳轉
     */
    public void setWaitingRoomName(String name) {
        waitingPanel.setRoomName(name);
        showPanel("WAITING");
    }

    /**
     * 更新等待室內的玩家準備狀態
     * 
     * @param data 格式通常為 "Player1:Ready;Player2:Waiting"
     */
    public void updateWaitingStatus(String data) {
        waitingPanel.updateStatus(data);
        showPanel("WAITING"); // 確保畫面切換到等待室
    }

    /**
     * 遊戲正式開始，載入遊戲主 UI
     */
    public void enterGame(String myId) {
        // 每次進入遊戲重新初始化 GameUI，確保數據乾淨
        gameUI = new GameUI(client, myId);
        mainContainer.add(gameUI, "GAME");
        showPanel("GAME");
    }

    /**
     * 處理遊戲進行中的 UI 更新訊息
     */
    public void updateUI(String message) {
        if (gameUI != null) {
            gameUI.updateGameScreen(message);
        }
    }

    /**
     * 更新大廳的房間列表
     */
    public void addRoom(String id, String name, int count) {
        lobbyPanel.addRoomToList(id, name, count);
    }

    /**
     * 顯示遊戲結束結算對話框
     */
    public void triggerWinnerDialog(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 3)
            return;

        // WinnerDialog 結束後會透過 client.backToLobby() 回到大廳面板
        new WinnerDialog(this, parts[1], parts[2], client).setVisible(true);
    }
}