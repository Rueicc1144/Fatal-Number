import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer {
    // 總連線清單 (包含在大廳與房間內的所有人)
    private List<ClientHandler> handlers = new CopyOnWriteArrayList<>();
    // 帳號管理系統
    private AccountManager accountManager = new AccountManager();
    // 房間地圖：房號 -> 房間物件
    private Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    
    private DeathHistoryLogger logger = new DeathHistoryLogger();
    private CommandFactory factory = new CommandFactory(logger);

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.startServer(8888);
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

    /**
     * 指令調度中心
     */
    public synchronized void processCommand(String message, ClientHandler sender) {
        String[] parts = message.split("\\|");
        String type = parts[0];

        // --- 階段一：帳號與大廳邏輯 ---
        if (type.equals("LOGIN")) {
            handleLogin(parts, sender);
            return;
        }

        if (type.equals("REGISTER")) {
            String result = accountManager.register(parts[1], parts[2]);
            sender.sendMessage("REGISTER_RESULT|" + result);
            return;
        }

        // --- 階段二：房間管理邏輯 ---
        if (type.equals("CREATE_ROOM")) {
            String rName = parts[1]; 
            String rId = String.format("%03d", rooms.size() + 1);
            GameRoom newRoom = new GameRoom(rId, rName);
            newRoom.addPlayer(sender);
            rooms.put(rId, newRoom);
            
            // 💡 關鍵修正 1：必須發送這個給房主，他才會跳轉視窗
            sender.sendMessage("CREATE_SUCCESS|" + rId + "|" + rName);
            
            // 廣播給所有人更新列表
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

        // --- 階段三：遊戲內邏輯 (ACTION/RESTART) ---
        if (type.equals("ACTION")) {
            // ACTION|玩家ID|TYPE|VALUE
            String actorId = parts[1]; 
            GameRoom room = findRoomByPlayer(actorId); 
        
            if (room != null && room.getGameState() != null) {
                String currentPlayer = room.getGameState().players.get(room.getGameState().currentPlayerIdx);
                if (!actorId.equals(currentPlayer)) return; 

                GameCommand cmd = factory.createCommand(message);
                if (cmd != null) {
                    cmd.execute(room.getGameState());
                    checkWinner(room);
                    broadcastGameState(room);
                    
                    room.resetTurnTimer(this); 
                }
            }
        }

        if(type.equals("RESTART")){
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
            // 同步目前所有房間給新登入的人
            for (GameRoom room : rooms.values()) {
                sender.sendMessage("NEW_ROOM|" + room.getRoomId() + "|" + room.getRoomName() + "|" + room.getPlayerCount());
            }
        } else if (status == 2) {
            sender.sendMessage("ERROR|ALREADY_LOGGED_IN");
        } else {
            sender.sendMessage("LOGIN_FAIL");
        }
    }

    private void broadcastRoomStatus(GameRoom room) {
        String statusMsg = room.getRoomStatusMsg();
        for (ClientHandler h : room.getMembers()) {
            h.sendMessage(statusMsg);
        }
    }

    private void broadcastGameState(GameRoom room) {
        for (ClientHandler h : room.getMembers()) {
            String syncMsg = room.getGameState().serializeState(h.getPlayerId());
            h.sendMessage(syncMsg);
        }
    }

    private void broadcastToLobby(String msg) {
        // 發送給不在房間內的連線者
        for (ClientHandler h : handlers) {
            if (findRoomByPlayer(h.getPlayerId()) == null) {
                h.sendMessage(msg);
            }
        }
    }

    private GameRoom findRoomByPlayer(String playerId) {
        if (playerId == null) return null;
        for (GameRoom room : rooms.values()) {
            for (ClientHandler h : room.getMembers()) {
                if (playerId.equals(h.getPlayerId())) return room;
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
                    room.stopTimer();
                    broadcastRoomStatus(room);
                }
            }
        }
    }

    private void checkWinner(GameRoom room) {
        GameState s = room.getGameState();
        long aliveCount = s.playerAliveStatus.values().stream().filter(v -> v).count();
        if (aliveCount == 1) {
            String winnerId = s.players.stream().filter(id -> s.playerAliveStatus.get(id)).findFirst().orElse("");
            String msg = "WINNER|" + winnerId + "|" + logger.getAllDeathReasons();
            for (ClientHandler h : room.getMembers()) {
                h.sendMessage(msg);
            }
            room.stopTimer(); 
            System.out.println("房間 " + room.getRoomId() + " 遊戲結束，計時器已關閉。");
        }
    }

    private void handlePlayerLeave(ClientHandler sender) {
        String pId = sender.getPlayerId();
        if (pId == null) return;

        GameRoom room = findRoomByPlayer(pId);
        if (room != null) {
            System.out.println("玩家 " + pId + " 正在離開房間: " + room.getRoomId());
            
            // 從房間名單移除
            room.removePlayer(pId);
            
            // 判定房間是否該關閉或廣播更新
            if (room.getPlayerCount() == 0) {
                rooms.remove(room.getRoomId());
                System.out.println("房間 " + room.getRoomId() + " 已空，正式關閉。");
            } else {
                // 房間還有人，更新房間內狀態
                broadcastRoomStatus(room);
            }

            broadcastToLobby("NEW_ROOM|" + room.getRoomId() + "|" + room.getRoomName() + "|" + room.getPlayerCount());
            sender.sendMessage("LEAVE_SUCCESS");
        }
    }

    // 房間物件
    class GameRoom{
        private String roomId;
        private String roomName;
        private List<ClientHandler> members = new CopyOnWriteArrayList<>();
        private Map<String, Boolean> readyStatus = new ConcurrentHashMap<>();
        private GameState gameState;
        private Timer turnTimer;

        public GameRoom(String id, String name){ 
            this.roomId = id; 
            this.roomName = name; 
        }
        

        public String getRoomId(){ 
            return roomId; 
        }

        public String getRoomName(){ 
            return roomName; 
        }

        public int getPlayerCount(){
            return members.size(); 
        }

        public List<ClientHandler> getMembers(){ 
            return members; 
        }

        public GameState getGameState(){ 
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
                readyStatus.put(pId, false); // 所有人設為未準備
            }
        }

        public void setReady(String pId, boolean ready) { readyStatus.put(pId, ready); }

        public boolean isAllReady() {
            return members.size() == 4 && readyStatus.values().stream().allMatch(r -> r);
        }

        public void initGame(DeathHistoryLogger logger) {
            List<String> ids = new ArrayList<>();
            for (ClientHandler h : members) ids.add(h.getPlayerId());
            this.gameState = new GameState(ids);
        }

        public void startGaming() { System.out.println("房間 " + roomId + " 遊戲開始！"); }

        public String getRoomStatusMsg() {
            StringBuilder sb = new StringBuilder("ROOM_STATUS|");
            for (ClientHandler h : members) {
                String id = h.getPlayerId();
                sb.append(id).append(":").append(readyStatus.get(id) ? "READY" : "WAIT").append(";");
            }
            return sb.toString();
        }

        public void resetTurnTimer(GameServer server) {
            if (turnTimer != null) {
                turnTimer.cancel(); // 取消之前的計時
            }
            turnTimer = new Timer();
            turnTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handleTimeout(server);
                }
            }, 15000); // 15秒
        }

        private void handleTimeout(GameServer server) {
            if (gameState == null) return;
            
            String timedOutPlayer = gameState.players.get(gameState.currentPlayerIdx);
            System.out.println("玩家 " + timedOutPlayer + " 超時！系統強制加 1 並換人。");
            String autoCmd = "ACTION|" + timedOutPlayer + "|CALL|1";
            server.processCommand(autoCmd, null); 
        }

        public void stopTimer() {
            if (turnTimer != null) turnTimer.cancel();
        }

    }
}