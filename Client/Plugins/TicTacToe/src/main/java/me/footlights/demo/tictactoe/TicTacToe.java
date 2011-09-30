/*
 * Copyright 2011 Jonathan Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.footlights.demo.tictactoe;

import java.util.logging.Logger;

import me.footlights.demo.tictactoe.Game.Piece;
import me.footlights.plugin.AjaxHandler;
import me.footlights.plugin.Context;
import me.footlights.plugin.JavaScript;
import me.footlights.plugin.KernelInterface;
import me.footlights.plugin.WebRequest;


/**
 * A demo plugin for playing Tic-Tac-Toe against human or computer opponents.
 * @author Jonathan Anderson <jon@footlights.me>
 */
public class TicTacToe implements me.footlights.plugin.Plugin
{
	public static TicTacToe init(KernelInterface kernel, Logger log)
	{
		log.info("Loading " + TicTacToe.class.getCanonicalName());
		return new TicTacToe();
	}


	@Override public AjaxHandler ajaxHandler() { return ajax; }

	private TicTacToe()
	{
		this.ajax = new Context()
		{
			{
				register("init", new AjaxHandler()
					{	
						@Override
						public JavaScript service(WebRequest request)
						{
							game = new Game();
							return new JavaScript()
								.append("context.log('starting new game...');")
								.append("context.load('init.js');")
								;
						}
					});

				register("clicked", new AjaxHandler()
					{
						@Override public JavaScript service(WebRequest request)
						{
							String[] tokens = request.path().split(",");
							if (tokens.length != 2)
								throw new IllegalArgumentException(
									"Expected coordinates (e.g. '1,2'), got " + request.path());

							int x = Integer.parseInt(tokens[0]);
							int y = Integer.parseInt(tokens[1]);

							try
							{
								Piece placed = game.place(x, y);
								if (placed == null) return new JavaScript();

								return new JavaScript()
									.append(place(placed, x, y))
									.append(changeNextBox(game.whoseTurnIsNext()))
									;
							}
							catch (GameOverException e)
							{
								return new JavaScript()
									.append("context.log('Game over! Result: ")
									.appendText(e.getMessage())
									.append("');")
									;
							}
						}
					});
			}
		};
	}


	private static final JavaScript place(Piece piece, int x, int y)
	{
		return new JavaScript()
			.append("context.globals.place('")
			.append(piece.filename)
			.append("', ")
			.append(Integer.toString(x))
			.append(", ")
			.append(Integer.toString(y))
			.append(", ")
			.append(Integer.toString(piece.offset))
			.append(");")
			;
	}

	private static final JavaScript changeNextBox(Piece piece)
	{
		return new JavaScript()
			.append("context.globals.next.src = '")
			.append(piece.filename)
			.append("';")
			.append("context.globals.next.onload = ")
			.append(new JavaScript()
				.append("this.style.position = 'relative';")
				.append("this.style.top = " + (0 - piece.offset) + ";")
				.append("this.style.left = " + (0 - piece.offset) + ";")
				.asFunction()
				)
			.append(";")
			;
	}


	private final AjaxHandler ajax;
	private Game game;
}
