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

import _root_.me.footlights.plugin.{KernelInterface,Plugin,ModifiablePreferences}
import _root_.java.util.NoSuchElementException
import _root_.java.util.logging.Logger


/**
 * A demo plugin for playing Tic-Tac-Toe against human or computer opponents.
 * @author Jonathan Anderson <jon@footlights.me>
 */
class TicTacToePlugin(playCounter:()=>Int) extends Plugin
{
	def ajaxHandler = new Ajax(this)

	var game = new Game
	def startNewGame = { println("play count: " + playCounter()); game = new Game }
}


/** Builder used by Footlights to find and initialize the plugin. */
object TicTacToe
{
	def init(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) = {
		log.warning("starting scala-based plugin")

		def counter():Int = {
			var playCount = -1
			try { playCount = prefs.getInt("playCount") + 1 }
			catch { case e:NoSuchElementException => playCount = 0 }

			prefs.set("playCount", playCount)

			playCount
		}

		new TicTacToePlugin(counter)
	}
}
