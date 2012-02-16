package me.footlights.demos.tictactoe;

import static me.footlights.demos.tictactoe.Game.Piece.*;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;


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
		size = 3;
		board = new Piece[size][];
		for (int i = 0; i < board.length; i++)
			board[i] = new Piece[size];

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

		Status result = result(Piece.CROSS);
		switch (result)
		{
			case PLAYING: nextTurn = ((nextTurn == CROSS) ? NOUGHT : CROSS); break;
			default: endGame(result);
		}

		return placed;
	}

	private synchronized void endGame(Status status)
	{
		if (state != Status.PLAYING)
			throw new IllegalStateException("Attempting to end a game that is already over");

		this.state = status;
	}

	private Status result(Piece mySide)
	{
		List<Iterable<Piece>> sequences = new LinkedList<Iterable<Piece>>();
		for (int i = 0; i < size; i++)
		{
			sequences.add(row(i));
			sequences.add(col(i));
		}
		sequences.add(diagonal(true));
		sequences.add(diagonal(false));

		for (Iterable<Piece> sequence : sequences)
		{
			Piece winner = checkForWinner(sequence);
			if (winner == null) continue;
			else if (winner == mySide) return Status.WON;
			else return Status.LOST;
		}

		return Status.PLAYING;
	}

	private Piece checkForWinner(Iterable<Piece> spaces)
	{
		Piece winner = null;
		for (Piece space : spaces)
			if (space == null) return null;
			else if (winner == null) winner = space;
			else if (space != winner) return null;

		return winner;
	}

	private Iterable<Piece> row(int row)
	{
		List<Piece> pieces = new ArrayList<Game.Piece>(size);
		for (int i = 0; i < size; i++) pieces.add(board[row][i]);
		return pieces;
	}

	private Iterable<Piece> col(int col)
	{
		List<Piece> pieces = new ArrayList<Game.Piece>(size);
		for (int i = 0; i < size; i++) pieces.add(board[i][col]);
		return pieces;
	}

	private Iterable<Piece> diagonal(boolean positive)
	{
		List<Piece> pieces = new ArrayList<Game.Piece>(size);
		for (int i = 0; i < size; i++)
		{
			int j = positive ? i : size - i - 1;
			pieces.add(board[i][j]);
		}
		return pieces;
	}


	private Status state = Status.PLAYING;
	private Piece nextTurn;

	private final int size;
	private final Piece[][] board;
}
