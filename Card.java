/*
Purpose:
Contributors: Surya
*/


import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class Card {
	int number;
	String suit;

	public Card (int p , int i) {
		number = p;
		if (i == 1) suit = "♣";
		else if (i == 2) suit = "♠";
		else if (i == 3) suit = "♦";
		else suit = "♥";
	}

	public static void initializeDeck(Card[] deck) {
		int i = 0;
		for (int y = 1; y <= 4; y++) {
			for (int p = 1; p <= 13; p++) {
				deck[i] = new Card (p,y);
				i+=1;
			}
		}
	}

	public static void shuffleDeck(Card [] deck) {
		Random rnd = ThreadLocalRandom.current();
		for (int i = deck.length - 1; i > 0; i--) {
			int index = rnd.nextInt(i + 1);
			Card a = deck[index];
			deck[index] = deck[i];
			deck[i] = a;
		}
	}

	// Accessors
	public int getNumber() {return this.number;}

	public String getSuit() {return this.suit;}
}