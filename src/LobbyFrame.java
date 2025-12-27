import javax.swing.*;
import java.awt.*;

public class LobbyFrame extends JFrame {
    private DefaultListModel<RoomInfo> roomListModel = new DefaultListModel<>();
    private JList<RoomInfo> roomList = new JList<>(roomListModel);
    private GameClient client;

    public LobbyFrame(GameClient client) {
        this.client = client;
        
        setTitle("遊戲大廳");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("可加入的房間"));
        leftPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        
        JButton btnCreate = new JButton("建立房間");
        JButton btnJoin = new JButton("加入房間");
        JButton btnRefresh = new JButton("整理列表");

        // 建立房間功能
        btnCreate.addActionListener(e -> {
            String roomName = JOptionPane.showInputDialog(this, "請輸入房間名稱:", "建立房間", JOptionPane.PLAIN_MESSAGE);
            if (roomName != null && !roomName.trim().isEmpty()) {
                // 發送 "CREATE_ROOM|房間名稱"
                client.sendMessage("CREATE_ROOM|" + roomName.trim());
            }
            System.out.println("發送建立房間請求...");
        });


        // 加入房間功能
        btnJoin.addActionListener(e -> {
            RoomInfo selected = roomList.getSelectedValue(); // 直接拿到 RoomInfo 物件
            if (selected != null) {
                client.sendMessage("JOIN_ROOM|" + selected.id);
            } else {
                JOptionPane.showMessageDialog(this, "請先選擇一個房間");
            }
        });

        // 刷新列表功能
        btnRefresh.addActionListener(e -> {
            roomListModel.clear();
            client.sendMessage("GET_ROOMS");
        });

        rightPanel.add(btnCreate);
        rightPanel.add(btnJoin);
        rightPanel.add(btnRefresh);

        setLayout(new BorderLayout());
        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    // 給GameClient呼叫，用來更新列表
    public void addRoomToList(String roomId, String roomName, int count) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < roomListModel.size(); i++) {
                if (roomListModel.get(i).id.equals(roomId)) {
                    roomListModel.remove(i);
                    break;
                }
            }
            roomListModel.addElement(new RoomInfo(roomId, roomName, count));
        
            roomList.revalidate();
            roomList.repaint();
        });
    }
}