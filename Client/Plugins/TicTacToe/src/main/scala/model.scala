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


class Piece(val filename:String, val offset:Int)
case object Nought extends Piece("nought.png", 16)
case object Cross extends Piece("cross.png", 32)


class GameScala
{
	val board = Array.ofDim[Option[Piece]](3, 3)
	var isOver = false
	var next:Piece = Cross

	def place(x:Int, y:Int): Option[Piece] =
	{
		if ((x == 2) && (next.equals(Cross))) isOver = true

		if (isOver) None
		else if (board(x)(y) != None) None
		else
		{
			val placed = next
			board(x)(y) = Some(placed)

			next = placed match {
				case Cross => Nought
				case Nought => Cross
			}

			Some(placed)
		}
	}
}
