/*
Purpose: Handles all of the main logic components for the poker game, including determining the winner and
other miscellaneous operations dealing with the cards and game statistics.
Contributors: Aryan, Raman, Surya, Harjevan
*/

public class Poker {
	private static PlayerHandler head; //head of linked list of Players (PlayerHandler for server convention)

	private static double pot = 0; //holds the value of the pot when playing
	private static double gameFee = 2; //game fee to enter game, arbitrary value set at $2


	// Poker methods
	public String printPlayerHandler() {
    //prints the usernames of all the players that are currently in the game (linked list)
    
		String strRepr = " ";
		for (PlayerHandler temp = head; temp != null; temp = temp.link) {
			strRepr += temp.getUserName() + " -> ";
		}
		return strRepr;
	}

	public void dealCards() {
    //deals 3 cards to all players that are currently in the game
    //deck is first initialized and shuffled to ensure that each player gets a different hand every time they play
    
		Card[] deck = new Card[52];
		Card.initializeDeck(deck);
		Card.shuffleDeck(deck);

		PlayerHandler temp = head;
		Card [] tempcards = new Card [3];
		int count = 0;

		for (int i = 0; i <= deck.length && temp != null; i++){
			tempcards[count] = deck[i];
			count += 1;
			if (count==3){
				for (int y = 0; y< temp.hand.length; y++){
					temp.hand[y] = tempcards[y];
				}
				temp = temp.link;
				count = 0;
			}
		}
	}

	public static void sortHand(Card[] hand) {
    //sorts the 3 card hand of a particular player in numerical order
    //uses selection sort to accomplish this
    
		for (int top = hand.length - 1; top > 0; top--) {
			int largeLoc = 0;
			for (int i = 1; i <= top; i++) {
				if (hand[i].getNumber() > hand[largeLoc].getNumber())
					largeLoc = i;
			}
			Card temp = hand[top] ;
			hand[top] = hand[largeLoc] ;
			hand[largeLoc] = temp;
		}
	}

	public static int handType(Card[] hand) {
    //determins the type of hand a particular player currently has
    //method returns an integer which represents what type of hand a player has, taking in an explicit hand parameter of type Card[]
    //Used to help determine a winner when there are two players left at the table
    
    //Hand Classification Legend:
		// 0. Straight Flush
		// 1. Three of a kind
		// 2. Straight
		// 3. Flush
		// 4. Pair
		// 5. Random

		// Sorts cards numerically from least to greatest
		sortHand(hand);

		boolean threeOfAKind = true;
		boolean straight = true;
		boolean flush = true;

		// Checks if three of a kind -> case 5
		for (int i = 0; i < hand.length - 1 && threeOfAKind; i++) {
			if (hand[i].getNumber() == hand[i + 1].getNumber()) continue;
			threeOfAKind = false;
		}
		if (threeOfAKind) return 1;

		// Checks if straight -> case 2
		for (int i = 0; i < hand.length - 1 && straight; i++) {
			boolean decreasing = hand[i].getNumber() - 1 == hand[i + 1].getNumber();
			boolean increasing = hand[i].getNumber() + 1 == hand[i + 1].getNumber();

			if (increasing || decreasing) continue;
			straight = false;
		}

		// Checks if flush -> case 3
		for (int i = 0; i < hand.length - 1 && flush; i++) {
			if (hand[i].getSuit().equals(hand[i + 1].getSuit())) continue;
			flush = false;
		}

		// Checks whether straight, flush, or straight flush
		if (straight && flush) return 0;
		if (straight) return 2;
		if (flush) return 3;


		// Checks if pair -> case 4
		if (hand[0].getNumber() == hand[1].getNumber()) return 4;
		if (hand[0].getNumber() == hand[2].getNumber()) return 4;
		if (hand[1].getNumber() == hand[2].getNumber()) return 4;

		// Means the player's hand is random
		return 5;
	}

	public static int sumBasedWinner(PlayerHandler p1, PlayerHandler p2) {
    //determines the winner and returns their user ID in the case where both player's hand are random -> case 5
    //in that case, this method will determine the winner based on the cumulative sum of their cards
    //higher sum will result in that player winning
    
    
		int p1Sum = 0, p2Sum = 0;
		for (int i = 0; i < p1.hand.length; i++) {
			p1Sum += p1.hand[i].getNumber();
			if (p1.hand[i].getNumber() == 1) p1Sum += 13;

			p2Sum += p2.hand[i].getNumber();
			if (p2.hand[i].getNumber() == 1) p2Sum += 13;
		}

		if (p1Sum > p2Sum) return p1.getID();
		else if (p1Sum < p2Sum) return p2.getID();
		return -1;
	}

	public static int determineWinner(PlayerHandler p1, PlayerHandler p2) {
    //determines the winner when there is only two players left at the table
    //checks the respective hands of each player and checks to see which is better than the other using the handType() method in conjunction
    //method returns the userID of the winning player

    //determining the hand type for each player
		int p1HandType = handType(p1.hand);
		int p2HandType = handType(p2.hand);

		if (p1HandType < p2HandType) return p1.getID();
		else if (p1HandType > p2HandType) return p2.getID();
		else if (p1HandType == 4) return sumBasedWinner(p1, p2);
		else {
			int p1Pair = 0, p2Pair = 0;
			if (p1.hand[1] == p1.hand[2]) p1Pair = 1;
			if (p2.hand[1] == p2.hand[2]) p2Pair = 1;

			if (p1.hand[p1Pair].getNumber() > p2.hand[p2Pair].getNumber()) return p1.getID();
			else if (p1.hand[p1Pair].getNumber() < p2.hand[p2Pair].getNumber()) return p2.getID();
			else return sumBasedWinner(p1, p2);
		}
	}


	// Linked List methods
	public int getPlayerCount() {
    //returns the number of players that are currently in the game (Linked List)
    
		int count = 0;
		for (PlayerHandler temp = head; temp != null; temp = temp.link) {
			count++;
		}
		return count;
	}

	public void addPlayer(PlayerHandler x) {
  //adds a player to the game (Linked List)
    
		x.link = head;
		head = x;
	}

	public static void removePlayer(int id) {
    //removes a player from the game (Linked List) by locating them using their ID
    
		PlayerHandler current = head;
		PlayerHandler previous = null;

		boolean found = false;
		while (!found && current != null) {
			if (id == current.getID()) {
				found = true;
			}
			else {
				previous = current;
				current = current.link;
			}
		}

    //checks to see if linked list is empty or not
		if (found) {
			if (current == head) {
				head = head.link;
			}
			else {
				previous.link = current.link;
			}
		}
	}

	public PlayerHandler findPlayer(int userID) {
    //finds and locates a player in the game using their userID
    //returns the player once and if found
    
		PlayerHandler found = null;
		for (PlayerHandler temp = head; (temp.link!=null) && (found==null); temp = temp.link) {
			if (temp.getID() == userID)
				found = temp;
		}

		return found;
	}


	//accessors
	public double getPot() {
    //returns pot of game
    
		return pot;
	}

	public static double getGameFee() {
    //returns the entry fee to enter the game
    
		return gameFee;
	}

	public static PlayerHandler getHead() {
    //returns the head of the linked list (game)
    //represents the last player to enter the game
    
    return head;
  }

	//mutators
	public void putPot(double amount) {
    //takes in an explicit parameter amount
    //alters the value for the pot of the game
    
		pot = amount;
	}

	public static void addToPot(double amount) {
    //takes in an explicit parameter amount
    //alters the value for the pot of the game by increasing it by the value of amount
    
		pot += amount;
	}

	public void putGameFee(double amount) {
    //takes in an explicit parameter amount
    //alters the value for the game entry fee
    
		gameFee = amount;
	}
}
