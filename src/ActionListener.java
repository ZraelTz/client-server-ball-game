


//interface to be defined for handling onPass events
public interface ActionListener {
    public void onBallReceive(String fromPlayerUUID, String ball);
    public void onInvalidPlayerEvent();
}
