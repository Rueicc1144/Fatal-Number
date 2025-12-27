import java.io.*;
import java.net.*;
import javax.swing.*;

public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String serverIP;
    private int port;
    private String myUserName;
    private MainFrame mainFrame;

    public GameClient(String serverIP, int port) {
        this.serverIP = serverIP;
        this.port = port;
    }

    public void startApp() {
        connect();
        mainFrame = new MainFrame(this);
    }

    private void connect() {
        try {
            socket = new Socket(serverIP, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(this::listen).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "無法連線至伺服器。");
        }
    }

    public void sendMessage(String msg) {
        if (out != null)
            out.println(msg);
    }

    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleServerMessage(line);
            }
        } catch (IOException e) {
            System.out.println("連線中斷。");
        }
    }

    private void handleServerMessage(String message) {
        String[] parts = message.split("\\|");
        String type = parts[0];

        SwingUtilities.invokeLater(() -> {
            switch (type) {
                case "LOGIN_SUCCESS":
                    this.myUserName = parts[1];
                    mainFrame.showPanel("LOBBY");
                    sendMessage("GET_ROOMS");
                    break;

                case "CREATE_SUCCESS":
                    mainFrame.setWaitingRoomName(parts[2]);
                    mainFrame.showPanel("WAITING");
                    break;

                case "ROOM_STATUS":
                    if (parts.length >= 3) {
                        mainFrame.setWaitingRoomName(parts[1]);
                        mainFrame.updateWaitingStatus(parts[2]);
                    }
                    break;

                case "NEW_ROOM":
                    mainFrame.addRoom(parts[1], parts[2], Integer.parseInt(parts[3]));
                    break;

                case "UPDATE":
                    mainFrame.enterGame(myUserName);
                    mainFrame.updateUI(message);
                    break;

                case "WINNER":
                    mainFrame.triggerWinnerDialog(message);
                    break;

                case "LEAVE_SUCCESS":
                    mainFrame.showPanel("LOBBY");
                    sendMessage("GET_ROOMS");
                    break;
            }
        });
    }

    public String getMyUserName() {
        return myUserName;
    }

    public void backToLobby() {
        SwingUtilities.invokeLater(() -> {
            if (mainFrame != null) {
                mainFrame.showPanel("LOBBY");
                sendMessage("GET_ROOMS");
            }
        });
    }

    public static void main(String[] args) {
        new GameClient("127.0.0.1", 8964).startApp();
    }
}