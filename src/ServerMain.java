import java.io.IOException;

//main server function
public class ServerMain {
   public static void main(String[] args) throws IOException {
       //server socket number defined
       int portNum = 1234;
       Server server = new Server(portNum);
       server.start();
       }
   }
