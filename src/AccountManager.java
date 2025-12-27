import java.io.*;
import java.util.*;

public class AccountManager {
    private final String filePath = "users.txt";
    private Map<String, String> userMap = new HashMap<>();
    private Set<String> onlineUsers = Collections.synchronizedSet(new HashSet<>());

    public AccountManager() {
        loadUsers();
    }

    // 從檔案讀取現有帳號到記憶體
    private void loadUsers() {
        File file = new File(filePath);
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    userMap.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // 0: 成功, 1: 密碼錯誤或帳號不存在, 2: 重複登入
    public synchronized int checkLogin(String username, String password) {
        String cleanName = username.trim().toLowerCase();
        
        // 驗證帳號密碼
        if (!userMap.containsKey(cleanName) || !userMap.get(cleanName).equals(password)) {
            return 1; 
        }
        
        // 檢查是否已經在線上
        if (onlineUsers.contains(cleanName)) {
            return 2; 
        }
        
        // 驗證通過，將玩家加入在線清單
        onlineUsers.add(cleanName);
        return 0;
    }

    public void logout(String username) {
        if (username != null) {
            onlineUsers.remove(username.trim().toLowerCase());
            System.out.println("玩家登出，已移出在線清單: " + username);
        }
    }

    // 註冊新帳號
    public synchronized String register(String username, String password) {
        String cleanName = username.trim().toLowerCase();
        if (userMap.containsKey(cleanName)) {
            System.out.println("[Account] 註冊失敗: " + cleanName + " 已存在");
            return "EXISTS"; // 帳號已存在
        }
        
        userMap.put(cleanName, password);
        try (PrintWriter out = new PrintWriter(new FileWriter(filePath, true))) {
            out.println(cleanName + ":" + password);
            System.out.println("[Account] 註冊成功: " + cleanName);
            return "SUCCESS";
        } catch (IOException e) {
            userMap.remove(cleanName);
            return "ERROR";
        }
    }
}