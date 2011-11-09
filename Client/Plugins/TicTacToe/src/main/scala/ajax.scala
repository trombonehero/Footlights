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

import _root_.me.footlights.plugin.{AjaxHandler,JavaScript,WebRequest}


/** Translates Ajax events to/from model events. */
class Ajax(plugin:TicTacToePlugin) extends AjaxHandler
{
	def service(request:WebRequest) =
	{
		request.path() match
		{
			case "init" | "new_game" => {
				plugin.startNewGame
				new JavaScript()
					.append("context.root.clear();")
					.append("context.log('starting new game...');")
					.append("context.load('init.js');")
			}

			case ClickCoordinates(x,y) => {
				val placed = plugin.game.place(x.toInt, y.toInt)

				val response = new JavaScript()

				if (placed != null)
					response
						.append(place(placed, x.toInt, y.toInt))
						.append(changeNextBox(plugin.game.next))

				if (plugin.game.isOver())
					response
						.append("context.log('Game over! Result: ")
						.appendText(plugin.game.state().toString())
						.append("');")

				response
			}
		}
	}


	private val ClickCoordinates = """clicked/(\d),(\d)""".r

	private def place(piece:Game.Piece, x:Int, y:Int) =
		"context.globals.place('%s', %d, %d, %d)".format(piece.filename, x, y, piece.offset)

	private def changeNextBox(piece:Game.Piece) =
		"""
		context.globals.next.src = '%s';
		context.globals.next.onload = %s;
		""".format(
			piece.filename,
			new JavaScript().append(
				"""
				this.style.position = 'relative';
				this.style.top = %d;
				this.style.left = %d;
				""".format(0 - piece.offset, 0 - piece.offset)
			).asFunction()
		)
}
