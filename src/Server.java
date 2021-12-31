
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server extends Thread {

    private final int serverPort;
    private ArrayList<ServerThreads> threadList = new ArrayList<>();

    public Server(int serverPort) {
        this.serverPort = serverPort;
    }

    public List<ServerThreads> getThreadList() {
        return threadList;
    }

    @Override
    public void run() {
        try {
            //create new server socket object 
            ServerSocket serverSocket = new ServerSocket(serverPort);
            //server keeps listening for connections from client
            while (true) {
                System.out.println("Listening for connections...");
                Socket clientSocket = serverSocket.accept();
                //prints accepted connection from client socket
                System.out.println("Connection accepted from " + clientSocket);
                ServerThreads serverThread = new ServerThreads(this, clientSocket);
                threadList.add(serverThread);
                serverThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //handles removing thread when player exits
    void removeThread(ServerThreads serverThread) throws IOException {
        if (serverThread.getHasBall() == true) {
            if (threadList.size() > 1) {
                for (ServerThreads thread : threadList) {
                    if (!serverThread.getPlayerUUID().equals(thread.getPlayerUUID())) {
                        thread.send(serverThread.getBall() + " from: " + serverThread.getPlayerUUID() + " to: "
                                + thread.getPlayerUUID()
                                + "\n");
                        break;
                    }
                }
            }
        }
        threadList.remove(serverThread);
    }
}
