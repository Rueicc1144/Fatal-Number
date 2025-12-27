import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String playerId;
    private AccountManager accountManager;

    public ClientHandler(Socket socket, GameServer server, AccountManager accountManager) {
        this.socket = socket;
        this.server = server;
        this.accountManager = accountManager;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String inputLine;
            // 直接進入迴圈監聽LOGIN,REGISTER或ACTION
            while ((inputLine = in.readLine()) != null) {
                // 將訊息傳給Server處理，並傳入this
                server.processCommand(inputLine, this);
            }
        } catch (IOException e) {
            System.out.println("玩家 " + (playerId != null ? playerId : "未登入用戶") + " 斷開連線。");
        } finally {
            if (this.playerId != null && accountManager != null) {
                accountManager.logout(this.playerId);
            }
            cleanup();
        }
    }

    // 由GameServer呼叫，當登入成功時設定身分
    public void setPlayerId(String id) {
        this.playerId = id;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message); // println 會自動加上換行符號 \n
        }
    }

    private void cleanup() {
        try {
            server.removeHandler(this);
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerId() { return playerId; }
}