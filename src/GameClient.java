import java.io.*;
import java.net.*;
import javax.swing.*;

public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String serverIP;
    private String myUserName;
    private int port;

    private AuthDialog authDialog;   // 登入/註冊視窗
    private LobbyFrame lobbyFrame;   // 大廳視窗
    private MainFrame mainFrame;     // 主遊戲視窗(戰鬥畫面)
    private WaitingRoom waitingRoom; // 等候室

    public GameClient(String serverIP, int port) {
        this.serverIP = serverIP;
        this.port = port;
    }

    public void startApp() {
        connect();
        authDialog = new AuthDialog(this); 
        authDialog.setVisible(true);
    }

    private void connect() {
        try {
            socket = new Socket(serverIP, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            new Thread(this::listen).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "無法連線至伺服器，請檢查網路或 Port。");
        }
    }

    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("【Client 接收中】: " + line);
                final String message = line;
                handleServerMessage(message);
            }
        } catch (IOException e) {
            System.out.println("連線中斷。");
        }
    }

    private void handleServerMessage(String message) {
        String[] parts = message.split("\\|");
        String type = parts[0];
        switch (type) {
            case "LOGIN_SUCCESS":
                if (authDialog != null){
                    this.myUserName = authDialog.getLoginAccount();
                    authDialog.dispose();
                }
                lobbyFrame = new LobbyFrame(this);
                lobbyFrame.revalidate();
                lobbyFrame.setVisible(true);
                System.out.println("大廳視窗已成功建立並顯示。");
                break;
            case "ERROR":
                String errorMsg = (parts.length > 1) ? parts[1] : "未知錯誤";
                if (errorMsg.equals("ALREADY_LOGGED_IN")) errorMsg = "該帳號已在其他地方登入！";
                JOptionPane.showMessageDialog(null, errorMsg, "系統提示", JOptionPane.ERROR_MESSAGE);
                break;

            case "LOGIN_FAIL":
                JOptionPane.showMessageDialog(null, "登入失敗：帳號或密碼錯誤", "錯誤", JOptionPane.ERROR_MESSAGE);
                break;

            case "REGISTER_RESULT":
                if (parts[1].equals("SUCCESS")) {
                    JOptionPane.showMessageDialog(authDialog, "註冊成功！");
                    authDialog.switchToLoginCard(); 
                } else {
                    JOptionPane.showMessageDialog(authDialog, "註冊失敗：帳號可能已存在");
                }
                break;
            
            case "ROOM_STATUS":
                System.out.println("收到房間狀態，準備更新 UI: " + parts[1]); 
                if (mainFrame != null) {
                    mainFrame.dispose();
                    mainFrame = null;
                    System.out.println("偵測到回到等待室狀態，已銷毀遊戲畫面。");
                }

                if (waitingRoom == null) {
                    // 如果是剛加入房間的人，這時waitingRoom還是 null
                    if (lobbyFrame != null) lobbyFrame.dispose();
                    waitingRoom = new WaitingRoom(this);
                    waitingRoom.setVisible(true);
                }
                waitingRoom.updateStatus(parts[1]);
                break;

            case "CREATE_SUCCESS":
                String id = parts[1];
                String name = parts[2];
                lobbyFrame.dispose();
                waitingRoom = new WaitingRoom(this);
                waitingRoom.setRoomName(name); // 顯示房間名稱
                waitingRoom.setVisible(true);
                break;

            case "NEW_ROOM":
                if (lobbyFrame != null) {
                    // 格式：NEW_ROOM|ID|Name|Count
                    String roomId = parts[1];
                    String roomName = parts[2];
                    int count = Integer.parseInt(parts[3]);
                    lobbyFrame.addRoomToList(roomId, roomName, count);
                }
                break;

            case "LEAVE_SUCCESS": // Server處理完離開後回傳
                waitingRoom.dispose();
                waitingRoom = null;
                lobbyFrame = new LobbyFrame(this);
                lobbyFrame.setVisible(true);
                sendMessage("GET_ROOMS");
                break;

            case "UPDATE":
                if(waitingRoom != null){
                    waitingRoom.dispose();
                    waitingRoom = null;
                }

                if(mainFrame == null) {
                    if (lobbyFrame != null) lobbyFrame.dispose();
                    mainFrame = new MainFrame(this);
                    mainFrame.setVisible(true);
                }
                mainFrame.updateUI(message);
                break;

            case "WINNER":
                if (mainFrame != null) {
                    mainFrame.triggerWinnerDialog(message);
                }
                break;
        }
    }

    public String getMyUserName(){
        return myUserName;
    }

    public void backToLobby() {
        SwingUtilities.invokeLater(() -> {
            if (mainFrame != null) {
                mainFrame.setVisible(false); 
                mainFrame.dispose();       
                mainFrame = null;          
                System.out.println("遊戲畫面已銷毀，正在返回大廳...");
            }

            if (lobbyFrame == null) {
                lobbyFrame = new LobbyFrame(this);
            }
            lobbyFrame.setVisible(true);
            
            sendMessage("GET_ROOMS");
        });
    }
}