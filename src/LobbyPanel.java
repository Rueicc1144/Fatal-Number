import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 遊戲大廳面板
 * <p>
 * 負責顯示可加入的房間列表及處理房間管理操作。
 * 採用現代深色風格設計 (Dark Theme UI) 以提升使用者體驗。
 * <p>
 * 修正紀錄：
 * - 修正 RoomInfo 屬性存取錯誤 (count -> currentPlayers)
 */
public class LobbyPanel extends JPanel {
    // UI 常數定義：統一管理色票與字型
    private static final Color BG_COLOR = new Color(40, 44, 52); // 深灰背景
    private static final Color LIST_BG_COLOR = new Color(50, 54, 62); // 列表背景
    private static final Color ACCENT_COLOR = new Color(97, 175, 239); // 強調色 (藍色)
    private static final Color TEXT_COLOR = new Color(220, 223, 228); // 主要文字色
    private static final Color BUTTON_COLOR = new Color(60, 64, 72); // 按鈕背景
    private static final Color BUTTON_HOVER_COLOR = new Color(80, 84, 92); // 按鈕懸停背景

    private static final Font TITLE_FONT = new Font("Microsoft JhengHei", Font.BOLD, 28);
    private static final Font TEXT_FONT = new Font("Microsoft JhengHei", Font.PLAIN, 14);

    private MainFrame frame;
    private GameClient client;
    private DefaultListModel<RoomInfo> roomListModel = new DefaultListModel<>();
    private JList<RoomInfo> roomList;

    /**
     * 初始化大廳面板並設定 UI 佈局。
     * * @param frame 主視窗引用
     * 
     * @param client 網路通訊客戶端
     */
    public LobbyPanel(MainFrame frame, GameClient client) {
        this.frame = frame;
        this.client = client;

        // 面板基礎設定
        setLayout(new BorderLayout(0, 20)); // 元件間距 20px
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(30, 40, 30, 40)); // 整體內縮

        // 1. 標題區域
        JLabel titleLabel = new JLabel("遊戲大廳");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(ACCENT_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(titleLabel, BorderLayout.NORTH);

        // 2. 房間列表區域
        initRoomList();
        JScrollPane scrollPane = new JScrollPane(roomList);
        scrollPane.setBorder(BorderFactory.createLineBorder(BUTTON_COLOR));
        scrollPane.getViewport().setBackground(LIST_BG_COLOR);
        add(scrollPane, BorderLayout.CENTER);

        // 3. 底部功能按鈕區域
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        btnPanel.setOpaque(false);

        JButton btnCreate = createStyledButton("建立房間");
        JButton btnJoin = createStyledButton("加入房間");
        JButton btnRefresh = createStyledButton("刷新列表");

        // 綁定事件監聽器
        btnCreate.addActionListener(e -> handleCreateRoom());
        btnJoin.addActionListener(e -> handleJoinRoom());
        btnRefresh.addActionListener(e -> handleRefresh());

        btnPanel.add(btnCreate);
        btnPanel.add(btnJoin);
        btnPanel.add(btnRefresh);

        add(btnPanel, BorderLayout.SOUTH);
    }

    /**
     * 初始化房間列表元件及其渲染器 (Renderer)
     */
    private void initRoomList() {
        roomList = new JList<>(roomListModel);
        roomList.setBackground(LIST_BG_COLOR);
        roomList.setForeground(TEXT_COLOR);
        roomList.setFont(TEXT_FONT);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.setFixedCellHeight(50); // 固定高度

        // 設定客製化渲染器
        roomList.setCellRenderer(new RoomListRenderer());
    }

    /**
     * 建立統一風格的按鈕
     */
    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(TEXT_FONT);
        btn.setForeground(TEXT_COLOR);
        btn.setBackground(BUTTON_COLOR);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BUTTON_COLOR.darker()),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(BUTTON_HOVER_COLOR);
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(BUTTON_COLOR);
                btn.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        return btn;
    }

    // --- 事件處理邏輯區 ---

    private void handleCreateRoom() {
        String name = JOptionPane.showInputDialog(this, "請輸入房間名稱:", "建立房間", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            client.sendMessage("CREATE_ROOM|" + name.trim());
        }
    }

    private void handleJoinRoom() {
        RoomInfo sel = roomList.getSelectedValue();
        if (sel != null) {
            client.sendMessage("JOIN_ROOM|" + sel.id);
        } else {
            JOptionPane.showMessageDialog(this, "請先選擇一個房間以加入", "提示", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleRefresh() {
        roomListModel.clear();
        client.sendMessage("GET_ROOMS");
    }

    /**
     * 更新或維護房間清單數據。
     * * @param id 房間唯一識別碼
     * 
     * @param name  房間顯示名稱
     * @param count 房間當前人數
     */
    public void addRoomToList(String id, String name, int count) {
        SwingUtilities.invokeLater(() -> {
            boolean found = false;
            for (int i = 0; i < roomListModel.size(); i++) {
                RoomInfo info = roomListModel.get(i);
                if (info.id.equals(id)) {
                    if (count <= 0) {
                        roomListModel.remove(i);
                    } else {
                        // 這裡使用 RoomInfo(String, String, int) 建構子
                        roomListModel.set(i, new RoomInfo(id, name, count));
                    }
                    found = true;
                    break;
                }
            }

            if (!found && count > 0) {
                roomListModel.addElement(new RoomInfo(id, name, count));
            }
        });
    }

    /**
     * 自訂列表渲染器
     */
    private class RoomListRenderer extends JPanel implements ListCellRenderer<RoomInfo> {
        private JLabel nameLabel;
        private JLabel statusLabel;

        public RoomListRenderer() {
            setLayout(new BorderLayout(10, 0));
            setBorder(new EmptyBorder(5, 15, 5, 15));
            setOpaque(true);

            nameLabel = new JLabel();
            nameLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));

            statusLabel = new JLabel();
            statusLabel.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14));

            add(nameLabel, BorderLayout.WEST);
            add(statusLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends RoomInfo> list, RoomInfo value, int index,
                boolean isSelected, boolean cellHasFocus) {

            nameLabel.setText(value.name);
            // 修正點：使用 value.currentPlayers 而非 value.count
            statusLabel.setText("線上人數: " + value.currentPlayers + "/4");

            if (isSelected) {
                setBackground(ACCENT_COLOR);
                nameLabel.setForeground(Color.WHITE);
                statusLabel.setForeground(Color.WHITE);
            } else {
                setBackground(index % 2 == 0 ? LIST_BG_COLOR : LIST_BG_COLOR.brighter());
                nameLabel.setForeground(TEXT_COLOR);
                statusLabel.setForeground(Color.GRAY);
            }

            return this;
        }
    }
}