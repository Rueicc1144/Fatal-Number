import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer {
    private List<ClientHandler> handlers = new CopyOnWriteArrayList<>();
    private AccountManager accountManager = new AccountManager();
    private Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    private DeathHistoryLogger logger = new DeathHistoryLogger();
    private CommandFactory factory = new CommandFactory(logger);

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.startServer(8964);
    }

    public void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("致命數字伺服器已啟動，等待連線中...");

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this, accountManager);
                handlers.add(handler);
                new Thread(handler).start();
                System.out.println("新玩家連線！目前總在線人數: " + handlers.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void processCommand(String message, ClientHandler sender) {
        String[] parts = message.split("\\|");
        String type = parts[0];

        if (type.equals("LOGIN")) {
            handleLogin(parts, sender);
            return;
        }

        if (type.equals("REGISTER")) {
            String result = accountManager.register(parts[1], parts[2]);
            sender.sendMessage("REGISTER_RESULT|" + result);
            return;
        }

        if (type.equals("CREATE_ROOM")) {
            String rName = parts[1];
            String rId = String.format("%03d", rooms.size() + 1);
            GameRoom newRoom = new GameRoom(rId, rName);
            newRoom.addPlayer(sender);
            rooms.put(rId, newRoom);

            sender.sendMessage("CREATE_SUCCESS|" + rId + "|" + rName);

            String lobbyMsg = "NEW_ROOM|" + rId + "|" + rName + "|1";
            for (ClientHandler h : handlers) {
                h.sendMessage(lobbyMsg);
            }
            broadcastRoomStatus(newRoom);
            System.out.println("玩家 " + sender.getPlayerId() + " 建立了房間: " + rId);
        }

        if (type.equals("JOIN_ROOM")) {
            String roomId = parts[1];
            GameRoom room = rooms.get(roomId);
            if (room != null && room.addPlayer(sender)) {
                broadcastRoomStatus(room);
            } else {
                sender.sendMessage("ERROR|房間已滿或不存在");
            }
        }

        if (type.equals("GET_ROOMS")) {
            for (GameRoom r : rooms.values()) {
                sender.sendMessage("NEW_ROOM|" + r.getRoomId() + "|" + r.getRoomName() + "|" + r.getPlayerCount());
            }
        }

        if (type.equals("LEAVE_ROOM")) {
            handlePlayerLeave(sender);
        }

        if (type.equals("READY") || type.equals("CANCEL_READY")) {
            String pId = (sender != null) ? sender.getPlayerId() : parts[1];
            GameRoom room = findRoomByPlayer(pId);
            if (room != null) {
                room.setReady(pId, type.equals("READY"));
                broadcastRoomStatus(room);

                if (room.isAllReady()) {
                    room.initGame(logger);
                    room.startGaming();
                    broadcastGameState(room);
                    room.resetTurnTimer(this);
                }
            }
        }

        if (type.equals("ACTION")) {
            String actorId = parts[1];
            GameRoom room = findRoomByPlayer(actorId);

            if (room != null && room.getGameState() != null) {
                String currentPlayer = room.getGameState().players.get(room.getGameState().currentPlayerIdx);
                if (!actorId.equals(currentPlayer))
                    return;

                GameCommand cmd = factory.createCommand(message);
                if (cmd != null) {
                    cmd.execute(room.getGameState());

                    // 執行勝負判定
                    checkWinner(room);

                    // 只有當遊戲「還沒結束」時，才繼續計時與廣播
                    if (room.getGameState() != null) {
                        broadcastGameState(room);
                        room.resetTurnTimer(this);
                    }
                }
            }
        }

        if (type.equals("RESTART")) {
            GameRoom room = findRoomByPlayer(sender.getPlayerId());
            if (room != null) {
                room.stopTimer();
                room.stopGaming();
                room.resetAllReadyStatus();
                broadcastRoomStatus(room);

                System.out.println("房間 " + room.getRoomId() + " 請求重開，已退回等待室。");
            }
        }
    }

    private void handleLogin(String[] parts, ClientHandler sender) {
        int status = accountManager.checkLogin(parts[1], parts[2]);
        if (status == 0) {
            sender.setPlayerId(parts[1]);
            sender.sendMessage("LOGIN_SUCCESS|" + parts[1]);
            for (GameRoom room : rooms.values()) {
                sender.sendMessage(
                        "NEW_ROOM|" + room.getRoomId() + "|" + room.getRoomName() + "|" + room.getPlayerCount());
            }
        } else if (status == 2) {
            sender.sendMessage("ERROR|ALREADY_LOGGED_IN");
        } else {
            sender.sendMessage("LOGIN_FAIL");
        }
    }

    private void broadcastRoomStatus(GameRoom room) {
        StringBuilder sb = new StringBuilder("ROOM_STATUS|");
        sb.append(room.getRoomName()).append("|");
        for (ClientHandler h : room.getMembers()) {
            String id = h.getPlayerId();
            sb.append(id).append(":").append(room.readyStatus.get(id) ? "READY" : "WAIT").append(";");
        }

        String msg = sb.toString();
        for (ClientHandler h : room.getMembers()) {
            h.sendMessage(msg);
        }
    }

    private void broadcastGameState(GameRoom room) {
        if (room.getGameState() == null)
            return;
        for (ClientHandler h : room.getMembers()) {
            String syncMsg = room.getGameState().serializeState(h.getPlayerId());
            h.sendMessage(syncMsg);
        }
    }

    private void broadcastToLobby(String msg) {
        for (ClientHandler h : handlers) {
            if (findRoomByPlayer(h.getPlayerId()) == null) {
                h.sendMessage(msg);
            }
        }
    }

    private GameRoom findRoomByPlayer(String playerId) {
        if (playerId == null)
            return null;
        for (GameRoom room : rooms.values()) {
            for (ClientHandler h : room.getMembers()) {
                if (playerId.equals(h.getPlayerId()))
                    return room;
            }
        }
        return null;
    }

    public synchronized void removeHandler(ClientHandler handler) {
        handlers.remove(handler);
        String pId = handler.getPlayerId();
        if (pId != null) {
            GameRoom room = findRoomByPlayer(pId);
            if (room != null) {
                room.removePlayer(pId);
                if (room.getPlayerCount() == 0) {
                    room.stopTimer();
                    rooms.remove(room.getRoomId());
                } else {
                    // 有人斷線時也進行勝負判定
                    checkWinner(room);
                    if (room.getGameState() != null) {
                        broadcastRoomStatus(room);
                    }
                }
            }
        }
    }

    private void checkWinner(GameRoom room) {
        GameState s = room.getGameState();
        if (s == null)
            return;

        long aliveCount = s.playerAliveStatus.values().stream().filter(v -> v).count();
        if (aliveCount == 1) {
            String winnerId = s.players.stream().filter(id -> s.playerAliveStatus.get(id)).findFirst().orElse("");
            String msg = "WINNER|" + winnerId + "|" + logger.getAllDeathReasons();

            for (ClientHandler h : room.getMembers()) {
                h.sendMessage(msg);
            }

            room.stopTimer();
            room.stopGaming(); // 清除遊戲狀態，防止後續計時器繼續執行
            System.out.println("房間 " + room.getRoomId() + " 遊戲結束，贏家為: " + winnerId);
        }
    }

    private void handlePlayerLeave(ClientHandler sender) {
        String pId = sender.getPlayerId();
        if (pId == null)
            return;

        GameRoom room = findRoomByPlayer(pId);
        if (room != null) {
            System.out.println("玩家 " + pId + " 正在離開房間: " + room.getRoomId());

            room.removePlayer(pId);

            if (room.getPlayerCount() == 0) {
                rooms.remove(room.getRoomId());
                System.out.println("房間 " + room.getRoomId() + " 已空，正式關閉。");
            } else {
                checkWinner(room);
                if (room.getGameState() != null) {
                    broadcastRoomStatus(room);
                }
            }

            broadcastToLobby("NEW_ROOM|" + room.getRoomId() + "|" + room.getRoomName() + "|" + room.getPlayerCount());
            sender.sendMessage("LEAVE_SUCCESS");
        }
    }

    class GameRoom {
        private String roomId;
        private String roomName;
        private List<ClientHandler> members = new CopyOnWriteArrayList<>();
        private Map<String, Boolean> readyStatus = new ConcurrentHashMap<>();
        private GameState gameState;
        private Timer turnTimer;

        public GameRoom(String id, String name) {
            this.roomId = id;
            this.roomName = name;
        }

        public String getRoomId() {
            return roomId;
        }

        public String getRoomName() {
            return roomName;
        }

        public int getPlayerCount() {
            return members.size();
        }

        public List<ClientHandler> getMembers() {
            return members;
        }

        public GameState getGameState() {
            return gameState;
        }

        public void stopGaming() {
            this.gameState = null;
        }

        public boolean addPlayer(ClientHandler h) {
            if (members.size() < 4) {
                members.add(h);
                readyStatus.put(h.getPlayerId(), false);
                return true;
            }
            return false;
        }

        public void removePlayer(String pId) {
            members.removeIf(h -> pId.equals(h.getPlayerId()));
            readyStatus.remove(pId);
        }

        public void resetAllReadyStatus() {
            for (String pId : readyStatus.keySet()) {
                readyStatus.put(pId, false);
            }
        }

        public void setReady(String pId, boolean ready) {
            readyStatus.put(pId, ready);
        }

        public boolean isAllReady() {
            return members.size() >= 2 && readyStatus.values().stream().allMatch(r -> r);
        }

        public void initGame(DeathHistoryLogger logger) {
            List<String> ids = new ArrayList<>();
            for (ClientHandler h : members)
                ids.add(h.getPlayerId());
            this.gameState = new GameState(ids);
        }

        public void startGaming() {
            System.out.println("房間 " + roomId + " 遊戲開始！");
        }

        public void resetTurnTimer(GameServer server) {
            if (turnTimer != null) {
                turnTimer.cancel();
            }
            // 若遊戲已結束 (gameState 為 null)，不再啟動新計時器
            if (gameState == null)
                return;

            turnTimer = new Timer();
            turnTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handleTimeout(server);
                }
            }, 15000);
        }

        private void handleTimeout(GameServer server) {
            if (gameState == null)
                return;

            String timedOutPlayer = gameState.players.get(gameState.currentPlayerIdx);
            System.out.println("玩家 " + timedOutPlayer + " 超時！系統強制加 1 並換人。");
            String autoCmd = "ACTION|" + timedOutPlayer + "|CALL|1";
            server.processCommand(autoCmd, null);
        }

        public void stopTimer() {
            if (turnTimer != null)
                turnTimer.cancel();
        }
    }
}