
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Player {

    private final String serverName;
    private final int serverPort;

    //assigns a random id number with a concatenation to 'Player'
    private final String playerUUID;
    private Socket socket;
    private OutputStream serverOut;
    private InputStream serverIn;
    private BufferedReader bufferedIn;
    private Boolean hasBall = true;

    private ArrayList<PlayerStatusListener> playerStatusListeners = new ArrayList<>();
    private ArrayList<ActionListener> actionListeners = new ArrayList<>();

    //Player constructor initializes need fields
    public Player(String serverName, int serverPort, String playerUUID) {
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.playerUUID = playerUUID;
    }

    public boolean connect() {
        try {
            this.socket = new Socket(serverName, serverPort);
            this.serverOut = socket.getOutputStream();
            this.serverIn = socket.getInputStream();
            this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    //scan for player iput and handle validate the input accordingly
    public void scanPlayerInput() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Type 'pass' and the playerId to pass to \nOr type 'exit' to leave the game");

        String playerAction = "";
        String[] playerActionTokens = {""};
        String action = "";

        //keep requesting for player pass input
        while (true) {
            try {
                playerAction = reader.readLine().toLowerCase();
                playerActionTokens = playerAction.split(" ");
                action = playerActionTokens[0];
            } catch (InputMismatchException e) {
                System.err.println("Only Strings Allowed!");
            }

            switch (action) {
                case "pass":
                    if (this.hasBall == true) {
                        if (validatePlayerPassInput(playerActionTokens)) {
                            passBall(playerActionTokens);
                        }
                    } else {
                        System.err.println("You do not currently possess the ball, wait your turn");
                    }
                    break;
                case "exit":
                        exitGame();
                    break;
                default:
                    System.out.println("Type 'pass' and the playerId to pass the ball \nOr 'exit' to leave the game");
            }
        }
    }

    //sends a 'join command to the server to enter the game'
    public boolean joinGame(String playerUUID) throws IOException {
        String playerAction = "join " + playerUUID + "\n";
        serverOut.write(playerAction.getBytes());

        String serverResponse = bufferedIn.readLine();
        System.out.println("Server- " + serverResponse);

        if (serverResponse.equalsIgnoreCase(">>player joined")) {
            startResponseReader();
            return true;
        } else {
            return false;
        }
    }

    //event listeners
    public void addPlayerStatusListener(PlayerStatusListener listener) {
        playerStatusListeners.add(listener);
    }

    public void removePlayerStatusListener(PlayerStatusListener listener) {
        playerStatusListeners.remove(listener);
    }

    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }

    //starts a new thread to read and process responses from the server
    private void startResponseReader() {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    readResponseLoop();
                } catch (IOException ex) {
                    Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        t.start();
    }

    //while the input buffer is not null get responses from the server and handle them
    private void readResponseLoop() throws IOException {
        try {
            String response;
            while ((response = bufferedIn.readLine()) != null) {
                String[] responseTokens = response.split(" ");
                if (responseTokens != null && responseTokens.length > 0) {
                    String received = responseTokens[0];
                    if (received.equalsIgnoreCase("online:")) {
                        handlePlayerEntry(responseTokens);
                    } else if (received.equalsIgnoreCase("offline:")) {
                        handlePlayerExit(responseTokens);
                    } else if (received.equalsIgnoreCase("invalid:")) {
                        handleInvalidPlayerEvent(); 
                    } else if (received.equalsIgnoreCase("(0)")) {
                        handleReceiveAction(responseTokens);
                    } else if (received.equalsIgnoreCase("game-in-session:")) {
                        this.hasBall = false;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                socket.close();
            } catch (IOException ex1) {
                Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    //executes the online event and updates other players
    private void handlePlayerEntry(String[] responseTokens) {
        String playerUniqueId = responseTokens[1];
        for (PlayerStatusListener listener : playerStatusListeners) {
            listener.online(playerUniqueId);
        }

    }

    //executes the offline event and updates other players
    private void handlePlayerExit(String[] responseTokens) {
        String playerUniqueId = responseTokens[1];
        for (PlayerStatusListener listener : playerStatusListeners) {
            listener.offline(playerUniqueId);
        }
    }

    //sends an exit command to the server to leave the game
    public void exitGame() throws IOException {
        String playerAction = "exit";
        serverOut.write(playerAction.getBytes());
        System.exit(0);
    }

    //sends a pass command to the server to pass to a recipient player UUID
    private void passBall(String[] passAction) throws IOException {
        String playerAction = "pass " + passAction[1] + " from " + this.playerUUID + "\n";
        serverOut.write(playerAction.getBytes());
    }

    //handles the onBallReceieve event
    private void handleReceiveAction(String[] responseTokens) throws IOException {
        String fromPlayer = responseTokens[2];
        String ball = responseTokens[0] + "\n";
        String toPlayer = responseTokens[4];

        System.out.println("the ball was passed to: " + toPlayer);
        hasBall = toPlayer.equalsIgnoreCase(playerUUID);

        if (hasBall == true) {
            serverOut.write(ball.getBytes());
            for (ActionListener listener : actionListeners) {
                listener.onBallReceive(fromPlayer, ball);
            }
        }
    }

    private void handleInvalidPlayerEvent() {
        
            for (ActionListener listener : actionListeners) {
                listener.onInvalidPlayerEvent();
            }
    }

    private boolean validatePlayerPassInput(String[] playerInput) {
        if (playerInput.length < 2 || playerInput.length > 2) {
            System.err.println("Please specify the 'pass' action and the player ID");
            return false;
        }
        return true;
    }

    //returns the current player UUID
    public String getPlayerUUID() {
        return playerUUID;
    }

}
