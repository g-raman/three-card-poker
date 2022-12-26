/*
Purpose: Connect player to server
Contributors: Raman
*/


import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Player {
    // Instance fields
    private Socket socket;              // Connection to server
    private BufferedReader fromHost;    // Reads messages from server
    private BufferedWriter toHost;      // Sends messages to server
    private String username;            // Username of player

    // Constructors
    public Player(Socket socket, String username) {
        /*
        Takes in "socket" of type Socket and "username" of type username.
        Sets socket equal to the instance field socket.
        Sets username to the instance field username.

        Initializes fromHost and toHost, so they can read and send messages
        to the host.

        In case of an error, the connection between the player and the host terminates.
        */

        try {
            this.socket = socket;
            this.username = username;
            fromHost = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            toHost = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            closeEverything(socket, fromHost, toHost);
        }
    }


    // Instance methods
    public void sendMsgs() {
        /*
        This method sends user input to the server.

        In case of an error, the connection between the player and host terminates.
        */
        try {
            // Sends the username to the server
            toHost.write(username);
            toHost.newLine();
            toHost.flush();

            Scanner in = new Scanner(System.in);
            while (socket.isConnected()) {
                System.out.print("> ");
                String msg = in.nextLine();

                if (msg == null) continue;

                toHost.write(msg);
                toHost.newLine();
                toHost.flush();
            }

        } catch (IOException e) {
            closeEverything(socket, fromHost, toHost);
        }
    }

    public void listenToMsgs() {
        /*
        This method starts a new thread for each individual player.
        This allows players to read and send messages to the host at the same time.

        In case of an error, the connection between the player and host terminates.
        */
        new Thread(() -> {
            try {
                String msgFromHost = " ";
                while (socket.isConnected() && msgFromHost != null) {
                    msgFromHost = fromHost.readLine();
                    System.out.println(msgFromHost);
                }
            } catch (IOException e) {
                closeEverything(socket, fromHost, toHost);
            }
        }).start();
    }

    public void closeEverything(Socket socket, BufferedReader in, BufferedWriter out) {
        /*
        Takes in "socket" of type Socket, "in" of type BufferedReader,
        "out" of type BufferedWriter.

        Closes each object and terminates the connection between the player and host.

        In case of an error, the stack trace is printed.
        */
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {e.printStackTrace();}
    }

    public static void main(String[] args) throws IOException {
        // Gathers server info for connection
        Scanner input = new Scanner(System.in);
        String username, SERVER_IP;
        int SERVER_PORT;

        System.out.print("Enter username for game: ");
        username = input.nextLine();

        System.out.print("Enter IP address of server: ");
        SERVER_IP = input.nextLine();

        System.out.print("Enter PORT number: ");
        SERVER_PORT = Integer.parseInt(input.nextLine());

        System.out.println("Wait for host to start the game...");

        // Attempts to connect to the server
        Socket socket = new Socket(SERVER_IP, SERVER_PORT);
        Player player = new Player(socket, username);

        player.listenToMsgs();
        player.sendMsgs();
    }
}