/*
Purpose:		
Create multiple threads for each player that can run in parallel.
Also stores player data and holds some game functionality.

Contributors: Aryan, Harjevan, and Raman

In general:
All server code is written by Raman. And all game related code
is written by Harjevan. Aryan also contributed to lots of the program logic for this file.
 */


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class PlayerHandler implements Runnable {
    // NOTE: some fields are static, so they can be accessed across classes

    // Server fields
    private Socket socket;              // Holds player connection to server
    public BufferedReader fromPlayer;   // Used to read messages from the player
    public BufferedWriter toPlayer;     // Used to send messages to the player

    // User data
    private int ID;                     // Unique user id
    private String username;            // Player's user name
    private double balance = 12;        // How much money the player has left
    public Card[] hand = new Card[3];   // Player's hand
    PlayerHandler link;                 // Holds link to next player

    // Game event variables
    public static volatile boolean gameStarted;     // Tracks whether game has started or not
    public boolean decisionMade;                    // Tracks whether all players have made a decision
    public static boolean allBankrupt = false;      // Tacks whether all players have a balance of 0

    // Miscellaneous variables
    public static ArrayList<PlayerHandler> players = new ArrayList<>();     // List of players



    // Constructor
    public PlayerHandler(Socket playerSocket) {
        /*
        Takes in "playerSocket" of type socket.

        Sets socket field to playerSocket.

        Sets username to username sent from user.
        Sets ID to the length of the list of players + 1.
        Subtracts game from the default balance.
        Updates the pot.

        Adds player to list of players

        Sends message to all other users announcing
        that the player has joined the game.


        In case of an error, the connection between the
        player and host is terminated.
        */

        try {
            this.socket = playerSocket;
            fromPlayer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            toPlayer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            this.username = fromPlayer.readLine();
            this.ID = players.size() + 1;

            this.balance -= Poker.getGameFee();
            Poker.addToPot(Poker.getGameFee());
            players.add(this);

            broadcastMsg("[HOST]: " + username + " has joined the game!");
        }
        catch (IOException e) {closeEverything(socket, fromPlayer, toPlayer);}

    }


    // Server instance methods
    public void broadcastMsg(String msg) {
        /*
        Takes in "msg" of type String.

        Returns if msg is null.

        Otherwise, sends the message to all players
        excluding the implicit one.

        In case of an error, the connection between the player and host is terminated
        */

        if (msg == null) return;
        for (PlayerHandler player : players) {
            try {
                if (player.getID() != this.getID()) {
                    player.toPlayer.write(msg);
                    player.toPlayer.newLine();
                    player.toPlayer.write("> ");
                    player.toPlayer.flush();
                }
            }
            catch (IOException e) {closeEverything(socket, fromPlayer, toPlayer);}
        }
    }

    public void writeToPlayer(String msg) throws IOException {
        /*
        Takes in "msg" of type String.

        Returns if the msg is null.

        Otherwise, it sends the message to the player.
        */

        if (msg == null) return;

        toPlayer.write(msg);
        toPlayer.newLine();
        toPlayer.write("> ");
        toPlayer.flush();
    }

    public void removeFromGame() throws IOException {
        /*
        Decreases the player count.

        Removes the implicit player from the list of players.
        Removes the implicit player from the list of players
        in the Poker class.

        Sends message to all other players that the user has
        left the game.

        Closes the connection between the player and host
        */

        Host.decreasePlayerCount();
        Poker.removePlayer(this.ID);
        players.remove(this);
        broadcastMsg("[HOST]: " + this.username + " folded...");
        writeToPlayer("[HOST]: Closing connection...");
        closeEverything(this.socket, this.fromPlayer, this.toPlayer);
    }

    public void closeEverything(Socket socket, BufferedReader in, BufferedWriter out) {
        /*
        Takes in "socket" of type Socket, "in" of type BufferedReader,
        "out" of type BufferedWriter.

        Closes each object and terminates the connection between the player and host.

        In case of an error, the stack trace is printed
        */

        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    // Game methods
    public boolean updateBalance(double x) {
        double temp = balance;
        temp += x;
        if (temp<0){
            return false;
        }
        else {
            balance = temp;
            return true;
        }
    }



    // Game-event instance methods
    public boolean getDecisionMade() {
        return this.decisionMade;
    }

    public boolean everyoneBankrupt() {
        /*
        Checks if all players have a balance of 0.

        If a minimum of two players are not bankrupt, returns false.

        Otherwise, returns true.
        */
        int notBankruptCount = 0;
        for (PlayerHandler curr = Poker.getHead(); curr != null; curr = curr.link) {
            if (curr.getBalance() > 0) notBankruptCount++;

            if (notBankruptCount == 2) return false;
        }
        return true;
    }



    // Accessors
    public int getID() {
        return this.ID;
    }

    public double getBalance() {
        return this.balance;
    }

    public String getUserName() {
        return this.username;
    }

    @Override
    public void run() {
        // Waits for the host to start the game
        while (socket.isConnected() && !gameStarted) Thread.onSpinWait();


        // Main game loop
        while (socket.isConnected()) {
            decisionMade = false;

            // Gets choice from user
            int choice = 0;
            try {
                String response = fromPlayer.readLine();
                choice = Integer.parseInt(response);
            }
            catch (IOException e) {closeEverything(socket, fromPlayer, toPlayer);}
            decisionMade = true;

            try {
                // Update users balance and moves them onto the next round if they have enough money
                if (choice == 1) {
                    double fee = Poker.getGameFee();
                    boolean hasEnoughMoney = updateBalance((fee * -1));

                    if (hasEnoughMoney) {
                        Poker.addToPot(fee);
                        broadcastMsg("[HOST]: " + this.username + " is going to the next round!");
                    } else {
                        Host.waitForRound();
                        if (everyoneBankrupt()) allBankrupt = true;
                        else {
                            removeFromGame();
                            break;
                        }
                    }
                }
                // A minimum of 3 players must be present in the game for you to fold
                else if (players.size() >= 3) removeFromGame();
            }
            catch (IOException e) {closeEverything(socket, fromPlayer, toPlayer);}

            try {writeToPlayer("[HOST]: Wait for other players");}
            catch (IOException e) {closeEverything(socket, fromPlayer, toPlayer);}

            Host.waitForRound();
        }
    }
}
