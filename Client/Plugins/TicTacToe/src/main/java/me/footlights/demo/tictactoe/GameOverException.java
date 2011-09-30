package me.footlights.demo.tictactoe;


/** The game is over. */
class GameOverException extends Exception
{
	enum EndState { WIN, LOSS, ERROR };

	public GameOverException(EndState endState) { this(endState, null); }
	public GameOverException(EndState endState, Throwable cause)
	{
		super("Game over: " + endState, cause);
	}

	private static final long serialVersionUID =
		("2011-09-29 1434h" + GameOverException.class.getCanonicalName()).hashCode();
}
