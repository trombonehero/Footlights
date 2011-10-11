package me.footlights.demo.tictactoe;

import static me.footlights.demo.tictactoe.Game.Piece.*;


/**
 * Model of the game itself.
 *
 * In this demo, we keep the model of the game on the Java side. The only reason for this is to
 * demonstrate how the two parts can work together.
 */
class Game
{
	enum Piece
	{
		NOUGHT("nought.png", 16),
		CROSS("cross.png", 32),
		;

		Piece(String filename, int offset)
		{
			this.filename = filename;
			this.offset = offset;
		}

		public final String filename;
		public final int offset;
	};

	enum Status
	{
		PLAYING,
		WON,
		LOST,
	}

	Game()
	{
		board = new Piece[3][];
		for (int i = 0; i < board.length; i++)
			board[i] = new Piece[3];

		nextTurn = CROSS;
	}

	boolean isOver() { return (state != Status.PLAYING); }
	Status state() { return state; }
	Piece next() { return nextTurn; }
	synchronized Piece place(int x, int y)
	{
		if (isOver()) return null;

		if (board[x][y] != null)
			throw new IllegalArgumentException(
				"Location (" + x + "," + y + ") already has a " + board[x][y] + " on it");

		Piece placed = nextTurn;
		board[x][y] = placed;

		if ((x == 2) && (placed == CROSS)) endGame(Status.WON);

		nextTurn = ((nextTurn == CROSS) ? NOUGHT : CROSS);
		return placed;
	}

	private synchronized void endGame(Status status)
	{
		if (state != Status.PLAYING)
			throw new IllegalStateException("Attempting to end a game that is already over");

		this.state = status;
	}


	private Status state = Status.PLAYING;
	private Piece nextTurn;

	private final Piece[][] board;
}
