import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{
    private ArrayList<ConnectionHandler> connections; // connections list
    private ServerSocket server;
    private boolean lastIt;
    private ExecutorService pool;
    public Server() {
        connections = new ArrayList<>();
        lastIt = false;
    }

    @Override
    public void run() {
        try {
             server = new ServerSocket(9999);
             pool = Executors.newCachedThreadPool();
             while(!lastIt) {
                 Socket client = server.accept();
                 ConnectionHandler handler = new ConnectionHandler(client);
                 connections.add(handler);
                 pool.execute(handler);
             }
        } catch (Exception e) {
            shutdown();

        }
    }
    public void broadcast(String message){
        for(ConnectionHandler hs : connections){
            if(hs != null){
                hs.sendMessage(message);
            }
        }
    }

    public void shutdown(){
        lastIt = true;
        try {
            if (!server.isClosed()) {
                server.close();
            }
        }catch (IOException e){
            // ignore it
        }
    }

    class ConnectionHandler implements Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        public ConnectionHandler(Socket client){
            this.client = client;
        }
        @Override
        public void run() {
            try{
                out = new PrintWriter(client.getOutputStream() , true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a nickname: ");
                nickname = in.readLine();
                //assume that the client will enter a valid nickname.
                System.out.println(nickname + " Connected!"); // its just for the developer.
                broadcast(nickname + " joined the chat!");
                String message;  // this message we get from the client
                while((message = in.readLine()) != null){
                    if(message.startsWith("/nick ")){
                        String[] messageSplit = message.split(" " , 2);
                        if(messageSplit.length == 2){
                            broadcast(nickname + " renamed themselves to (broadcast) " + messageSplit[1]);
                            System.out.println(nickname + " renamed themselves to (sout) " + messageSplit[1]); // for the developer
                            nickname = messageSplit[1];
                            out.println("Successfully changed nickname to koko" + nickname);

                        }else{
                            out.println("No nickname provided!");
                        }

                    }else if(message.startsWith("/quit")) {
                        broadcast(nickname + " left the chat!");
                        shutdown();
                    }else{
                        broadcast(nickname + ": " + message);
                    }
                }
                
            }catch (IOException e){
                shutdown();
            }

        }
        public void sendMessage(String message){
            out.println(message);
        }
        public void shutdown(){
            try {
                lastIt = true;
                pool.shutdown();
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            }catch (IOException e){
                //ignore;
            }
        }


    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
