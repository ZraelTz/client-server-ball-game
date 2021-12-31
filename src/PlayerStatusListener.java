//interface to be defined for handle player game presence
public interface PlayerStatusListener {
    public void online(String playerUUID);
    public void offline(String playerUUID);
}
