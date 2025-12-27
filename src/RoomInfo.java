public class RoomInfo {
    public String id;
    public String name;
    public int currentPlayers;

    public RoomInfo(String id, String name, int current) {
        this.id = id;
        this.name = name;
        this.currentPlayers = current;
    }

    @Override
    public String toString() {
        return String.format("房號: %s | 名稱: %s | (%d/4)", id, name, currentPlayers);
    }
}