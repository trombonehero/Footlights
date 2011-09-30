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

	Game()
	{
		board = new Piece[3][];
		for (int i = 0; i < board.length; i++)
			board[i] = new Piece[3];

		nextTurn = CROSS;
	}

	Piece whoseTurnIsNext() { return nextTurn; }
	synchronized Piece place(int x, int y) throws GameOverException
	{
		if (isOver) return null;

		if (board[x][y] != null)
			throw new IllegalArgumentException(
				"Location (" + x + "," + y + ") already has a " + board[x][y] + " on it");

		Piece placed = nextTurn;
		board[x][y] = placed;

		if ((x == 2) && (placed == CROSS)) endGame();

		nextTurn = ((nextTurn == CROSS) ? NOUGHT : CROSS);
		return placed;
	}

	private synchronized void endGame() throws GameOverException
	{
		this.isOver = true;
		throw new GameOverException(GameOverException.EndState.WIN);
	}


	private Piece nextTurn;
	private boolean isOver;

	private final Piece[][] board;
}
