/*
Purpose: Creates server for players to join
Contributors: Raman, Aryan, Harjevan, Surya

In general:
Most of the code is written by Raman. However,
the logic for much of the code was a team effort.
*/


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Host {
    // Server fields
    private final ServerSocket listener;    // Listens for any players wanting to join

    // Game fields
    private final Poker game = new Poker(); // Creates a game of Poker
    private static int playerCount = 0;     // Keeps track of the number of players

    // Game-event fields
    private boolean started = false;        // Stores whether host has started game or not
    public static boolean roundDone;        // Tracks whether the current round is done



    // Constructors
    public Host(ServerSocket serverSocket) {
        /*
        Takes in "serverSocket" of type ServerSocket.

        Sets listener to serverSocket
        */
        this.listener = serverSocket;
    }



    // Server instance methods
    public void startServer() {
        /*
        Listens for any players that wants to join.

        If a connection is made, the player is initialized and
        added to the list of players.

        If 3 players have joined, the host has the option to start
        the game.
        The host can admit up to a max of 17 players.

        In case of an error, the connection between the host and player
        is terminated
        */
        try {
            Scanner input = new Scanner(System.in);

            // Listens on port 9090 for any new players
            while (!this.listener.isClosed() && !started) {
                System.out.println("Waiting for players to join...");
                Socket player = this.listener.accept();

                PlayerHandler playerHandler = new PlayerHandler(player);
                System.out.println(playerHandler.getUserName() + " has joined..");
                this.game.addPlayer(playerHandler);

                Thread playerThread = new Thread(playerHandler);
                playerThread.start();
                Host.playerCount++;

                if (Host.playerCount >= 3 && Host.playerCount < 17) {
                    System.out.print("Start game (Y/N): ");
                    String choice = input.nextLine();
                    if (choice.equalsIgnoreCase("Y")) this.started = true;
                }

                if (Host.playerCount == 17) {
                    System.out.println("Max # of player accepted. The game will start now.");
                    this.started = true;
                }
            }
        } catch (IOException e) {closeServer();}

        PlayerHandler.gameStarted = true;
        runGame();
    }



    public void closeServer() {
        // Shuts down the Host file
        try {if (listener != null) listener.close();}
        catch (IOException e) {e.printStackTrace();}
    }

    public void broadcastMsg(String msg) {
        /*
        Takes "msg" of type String.

        Returns if msg is null.
        Otherwise, sends "msg" to the player.

        In case of an error, the stack trace is printed.
        */
        if (msg == null) return;

        for (PlayerHandler curr = Poker.getHead(); curr != null; curr = curr.link) {
            try {
                curr.toPlayer.write(msg);
                curr.toPlayer.newLine();
                curr.toPlayer.write("> ");
                curr.toPlayer.flush();
            }
            catch (IOException e) {e.printStackTrace();}
        }
    }



    // Game related instance and class methods
    public void showHand() throws IOException {
        /*
        Loops over the array of cards for each player
        and displays each player their hand.
        */
        for (PlayerHandler curr = Poker.getHead(); curr != null; curr = curr.link) {
            String[] n = new String[3];     // Holds the value of the cards
            String[] s = new String[3];     // Holds the suits of the cards

            for (int i = 0; i < curr.hand.length; i++) {
                n[i] = Integer.toString(curr.hand[i].getNumber());

                // Changes the numbers to letters
                if (n[i].equals("1")) n[i] = "A";
                if (n[i].equals("11")) n[i] = "J";
                if (n[i].equals("12")) n[i] = "Q";
                if (n[i].equals("13")) n[i] = "K";

                s[i] = curr.hand[i].getSuit();
            }

            curr.toPlayer.write("Your hand:");
            curr.toPlayer.newLine();
            curr.toPlayer.write("---------\t---------\t---------");
            curr.toPlayer.newLine();
            curr.toPlayer.write("|       |\t|       |\t|       |");
            curr.toPlayer.newLine();


            curr.toPlayer.write("|  " + n[0] + " " + s[0] + "  |\t" +
                    "|  " + n[1] + " " + s[1] + "  |\t" +
                    "|  " + n[2] + " " + s[2] + "  |");
            curr.toPlayer.newLine();

            curr.toPlayer.write("|       |\t|       |\t|       |");
            curr.toPlayer.newLine();
            curr.toPlayer.write("---------\t---------\t---------");
            curr.toPlayer.newLine();
            curr.toPlayer.flush();
        }
    }

    public void printMenu() throws IOException {
        // Displays the menu to each player

        for (PlayerHandler curr = Poker.getHead(); curr != null; curr = curr.link) {
            curr.toPlayer.newLine();
            curr.toPlayer.write("> [1] Continue to next round");
            curr.toPlayer.newLine();
            curr.toPlayer.write("> [2] Fold and leave the game");
            curr.toPlayer.newLine();
            curr.toPlayer.write("> Note: A number outside the range will result in folding from the round");
            curr.toPlayer.newLine();
            curr.toPlayer.write("> Choice:");
            curr.toPlayer.newLine();
            curr.toPlayer.flush();
        }
    }

    public void printStats() throws IOException {
        // Displays the current stats of the game to each player

        for (PlayerHandler curr = Poker.getHead(); curr != null; curr = curr.link) {
            curr.toPlayer.newLine();
            curr.toPlayer.write("Game stats: ");
            curr.toPlayer.write("Pot: " + game.getPot());
            curr.toPlayer.newLine();
            curr.toPlayer.write("# of players: " + playerCount);
            curr.toPlayer.newLine();
            curr.toPlayer.write("Your balance: " + curr.getBalance());
            curr.toPlayer.newLine();
            curr.toPlayer.flush();
        }
    }

    public void printWinnerStats(PlayerHandler winner) throws IOException {
        String winnerName = winner.getUserName();
        winner.updateBalance(game.getPot());

        broadcastMsg("[HOST]: " + winnerName + " has won the game!");
        broadcastMsg("[HOST]: " + winnerName + " made $" + game.getPot());
    }

    public void printTieStats(PlayerHandler w1, PlayerHandler w2) throws IOException {
        PlayerHandler winner1 = Poker.getHead();
        PlayerHandler winner2 = Poker.getHead().link;

        String winner1Name = winner1.getUserName();
        String winner2Name = winner2.getUserName();

        double split = Math.round((game.getPot() / 2.0) * 100) / 100.0;

        broadcastMsg("[HOST]: " + winner1Name + " & " + winner2Name + " tied");
        broadcastMsg("[HOST]: " + winner1Name + " made $" + split);
        broadcastMsg("[HOST]: " + winner2Name + " made $" + split);
    }

    public PlayerHandler determineOverallWinner() {
        /*
        ! Method only used when everyone is bankrupt at the same time

        Determines and returns the player with best hand in the current round.
        */

        int winnerID = 1;
        for (PlayerHandler curr = Poker.getHead(); curr.link != null; curr = curr.link) {
            int tempWinnerID = Poker.determineWinner(curr, curr.link);

            if (winnerID != tempWinnerID) winnerID = tempWinnerID;
        }
        return game.findPlayer(winnerID);
    }

    public static void decreasePlayerCount() {
        // Decrements the player count by one
        playerCount--;
    }



    // Game-event instance and class methods
    public void gameIntro() {
        broadcastMsg("The game has begun!");
        try {Thread.sleep(1000);}
        catch (InterruptedException e) {Thread.currentThread().interrupt();}

        broadcastMsg("Shuffling cards...");
        try {Thread.sleep(1000);}
        catch (InterruptedException e) {Thread.currentThread().interrupt();}

        broadcastMsg("Dealing cards...");
        try {Thread.sleep(1000);}
        catch (InterruptedException e) {Thread.currentThread().interrupt();}
    }

    public static void waitForRound() {
        /*
        Waits until all users have decided to either fold or continue.
        */
        Host.roundDone = false;
        while (!Host.roundDone) {
            Host.roundDone = true;
            for (PlayerHandler curr = Poker.getHead(); curr != null; curr = curr.link) {
                if (!curr.getDecisionMade()) {
                    Host.roundDone = false;
                    break;
                }
            }
            try {Thread.sleep(1000);}
            catch (InterruptedException e) {Thread.currentThread().interrupt();}
        }
    }




    // Accessors
    public int getPlayerCount() {
        // Returns the # of players in the game
        return Host.playerCount;
    }

    public boolean getRoundDone() {
        // Returns whether the current round is done or not
        return Host.roundDone;
    }



    // Main game loop
    public void runGame() {
        gameIntro();
        game.dealCards();

        while (playerCount > 0) {
            // Waits one second so variables across classes can update for each user
            try {Thread.sleep(1000);}
            catch (InterruptedException e) {Thread.currentThread().interrupt();}


            // Shows hand and stats to each player
            try {
                showHand();
                printStats();
                printMenu();
            }
            catch (IOException e) {closeServer();}


            // Handles case where all players are bankrupt at the same time
            if (PlayerHandler.allBankrupt) {
                try {printWinnerStats(determineOverallWinner());}
                catch (IOException e) {e.printStackTrace();}
                finally {closeServer();}
            }


            // Handles case where one person is left in the game
            else if (playerCount == 1) {
                try {printWinnerStats(Poker.getHead());}
                catch (IOException e) {e.printStackTrace();}
                finally {closeServer();}
            }


            // Handles case where two players are left in the game
            else if (playerCount == 2) {
                int winnerID = Poker.determineWinner(Poker.getHead(), Poker.getHead().link);

                // Handles case where there is no tie
                if (winnerID != -1) {
                    try {printWinnerStats(game.findPlayer(winnerID));}
                    catch (IOException e) {e.printStackTrace();}
                    finally {closeServer();}
                }

                // Handles case where there is a tie
                else {
                    try {printTieStats(Poker.getHead(), Poker.getHead().link);}
                    catch (IOException e) {e.printStackTrace();}
                    finally {closeServer();}
                }
            }

            // Waits for all other players to make a decision
            waitForRound();
        }
    }



    // Main
    public static void main(String[] args) throws IOException {
        // Starts server
        ServerSocket serverSocket = new ServerSocket(9090);
        Host server = new Host(serverSocket);
        server.startServer();
    }
}
