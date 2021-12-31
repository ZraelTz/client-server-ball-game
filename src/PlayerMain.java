import java.io.IOException;
import java.util.UUID;

/**
 *
 * @author zrael
 */
public class PlayerMain {
        public static void main(String[] arg) throws IOException {
        Player player = new Player("localhost", 1234, "Player-" + UUID.randomUUID().toString());
        
        //player staus listener for handling online and offline player presence
        player.addPlayerStatusListener(new PlayerStatusListener() {
            @Override
            public void online(String playerUUID) {
                System.out.println("JOINED: " + playerUUID);
            }

            @Override
            public void offline(String playerUUID) {
                System.err.println("LEFT: " + playerUUID);
            }
        });

        //player action staus listener for handling OnBallRecieve
        player.addActionListener(new ActionListener() {
            @Override
            public void onBallReceive(String fromPlayer, String ball) {
                System.out.println(fromPlayer + " passed to you: " + ball);
            }
            
            @Override
            public void onInvalidPlayerEvent(){
                System.err.println("Player with the specified ID doesn't exist");
            }
        });

        //player fails to connect print error message
        if (!player.connect()) {
            System.err.println("Connection to Server Failed");
        } else {
            //if connection is successful print success message and scan for player input
            if (player.joinGame(player.getPlayerUUID())) {
                System.out.printf("Connection Successful, Your Player ID: " + player.getPlayerUUID() + "\n");
                System.out.println("You can Choose to pass the ball first");
                player.scanPlayerInput();
            } else {
                System.err.println("Game Entry Failed..");
            }
            player.exitGame();
        }
    }
}
