
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerThreads extends Thread {

    private final Socket playerSocket;
    private final Server server;
    private OutputStream outputStream;
    private String playerUUID;
    private int passCounter;
    private AtomicReference<String> ball = new AtomicReference<>("(0)");
    private boolean hasBall;

    //ServerThreads constructor initializes all the needed fields
    public ServerThreads(Server server, Socket playerSocket) {
        this.server = server;
        this.playerSocket = playerSocket;
    }

    @Override
    public void run() {
        synchronized (ball) {
            try {
                //handles players actions after connection
                handlePlayerConnections();
            } catch (IOException ex) {
                System.err.println(">>" + playerUUID + " left the game");
                try {
                    handlePlayerExit();
                } catch (IOException ex1) {
                    System.err.println("Connection ended");
                }

            } catch (InterruptedException ex) {
                Logger.getLogger(ServerThreads.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    //validates and processes commands from player connections
    private void handlePlayerConnections() throws IOException, InterruptedException {
        InputStream inputStream = playerSocket.getInputStream();
        this.outputStream = playerSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String playerAction;
        //keeps reading the input stream for player actions
        while ((playerAction = reader.readLine()) != null) {

            String[] actionTokens = playerAction.split(" ");
            if (actionTokens != null && actionTokens.length > 0) {
                String action = actionTokens[0];
                if (action.equals("join")) {
                    handlePlayerEntry(outputStream, actionTokens);
                } else if (action.equalsIgnoreCase("exit")) {
                    handlePlayerExit();
                    break;
                } else if (action.equalsIgnoreCase("pass")) {
                    handlePassAction(actionTokens);
                } else if (action.equalsIgnoreCase("(0)")) {
                    this.hasBall = true;
                } else {
                    String serverMsg = "unknown action "
                            + "'" + action + "'" + "\n";
                    outputStream.write(serverMsg.getBytes());
                }
            }
        }
    }

    //handles players 'join' command and notifies other players of online status
    private void handlePlayerEntry(OutputStream outputStream, String[] actionTokens) throws IOException {
        if (actionTokens.length == 2) {
            String uniqueId = actionTokens[1];
            String msg = ">>player joined\n";
            outputStream.write(msg.getBytes());
            this.playerUUID = uniqueId;
            System.out.println(">>" + uniqueId + " joined the game");
            handlePlayerGamePresence();
        } else {
            String msg = "Player entry error \n";
            outputStream.write(msg.getBytes());
        }
    }

    //returns current playerUUID
    public String getPlayerUUID() {
        return playerUUID;
    }

    public int getPassCounter() {
        return passCounter;
    }
    
    public boolean getHasBall() {
        return hasBall;
    }
    
    public String getBall() {
        return ball.get();
    }

    //notifies other players of game presence
    private void handlePlayerGamePresence() throws IOException {
        //get list of all server thread instances
        List<ServerThreads> threadList = server.getThreadList();

        //send current player all other player status
        for (ServerThreads thread : threadList) {
            if (thread.getPlayerUUID() != null) {
                if (!playerUUID.equals(thread.getPlayerUUID())) {
                    String entryMsg = "Online: "
                            + thread.getPlayerUUID() + " has joined" + "\n";
                    send(entryMsg);
                }
            }
        }

        for (ServerThreads thread : threadList) {
            if (thread.getPassCounter() > 0) {
                this.send("game-in-session: ball has already been passed \n");
                break;
            }
        }

        //send other online players current player status
        String onlineMsg = "Online: "
                + playerUUID + " has joined" + "\n";
        for (ServerThreads thread : threadList) {
            if (!playerUUID.equals(thread.getPlayerUUID())) {
                thread.send(onlineMsg);
            }
        }

    }

    private void handlePlayerExit() throws IOException {
        //gets a list of all the server threads
        List<ServerThreads> threadList = server.getThreadList();
        
        server.removeThread(this);

        //send other online players current player status
        String offlineMsg = "Offline: "
                + playerUUID + " has left" + "\n";
        for (ServerThreads thread : threadList) {
            thread.send(offlineMsg);
        }
        playerSocket.close();
    }

    //writes commands to the output stream 
    public void send(String msg) throws IOException {
        if (playerUUID != null) {
            outputStream.write(msg.getBytes());
        }
    }

    //format: ball from: "playerUUID" to: "passTo"
    private void handlePassAction(String[] actionTokens) throws IOException {
        String passTo = actionTokens[1];
        //get list of server threads
        List<ServerThreads> threadList = server.getThreadList();

        boolean playerExists = threadList.stream().anyMatch(
                x -> x.getPlayerUUID().equalsIgnoreCase(passTo)
        );

        if (playerExists == false) {
            System.err.println("invalid playerID specified by: " + this.playerUUID);
            this.send("invalid: Player doesn't exist \n");
        } else {
            System.out.println(this.ball.get() + " to " + passTo);
            passCounter++;
            this.hasBall = false;
            for (ServerThreads thread : threadList) {
                thread.send(this.ball.get() + " from: " + playerUUID + " to: " + passTo
                        + "\n");
            }
        }
    }

}
