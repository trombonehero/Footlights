/*
 * Copyright 2012 Jonathan Anderson
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
package me.footlights.ui.web

import java.net.URI
import java.util.logging

import me.footlights.api.WebRequest
import me.footlights.api.ajax._
import me.footlights.api.ajax.JSON._
import me.footlights.api.support.Either._
import me.footlights.api.support.Pipeline._
import me.footlights.api.support.Regex._

import me.footlights.core


/** The global context - code sent here has full DOM access. */
class GlobalContext(footlights:core.Footlights, reset:() => Unit,
		newContext:core.apps.AppWrapper => Unit)
		extends Context(classOf[GlobalContext]) {

	override def defaultResponse() = {
		val data = (this.getClass getResource "index.html").openStream
		Response.newBuilder setMimeType "text/html" setResponse data build
	}

	override val name = "Global context"
	override def openFile(name:String) = footlights open name map { case f:core.data.File => f }
	override def handleAjax(request:WebRequest):AjaxResponse = {
		request.path() match {
			case Init =>
				reset

				// Kill off any async channels waiting for events from previous generation.
				asyncQueueGeneration += 1
				asyncEvents.synchronized { asyncEvents.notifyAll }

				val launcher = "context.globals['launcher']"

				val js = new JavaScript()
					.append(clickableAjax(launcher, "Reset", Reset))
					.append(clickableAjax(launcher, "Open Root", OpenRoot))
					.append(clickableAjax(launcher, "Load App", PromptApplication))
					.append(launcher + ".appendElement('hr');")

				footlights.applications filter { _.isRight } map { _.get } sortBy { _._1 } map {
					case (name, classpath) =>
						clickableAjax(launcher, name, LoadApplication(classpath))
				} foreach js.append

				js
					.append(launcher + ".appendElement('hr');")

				footlights.identities.toSeq sortBy { _.name } map { user =>
					clickableAjax(launcher, user.name, EditUser(URLEncoded(user.fingerprint.toURI)))
				} foreach js.append

				js
					.append(setupAsyncChannel)
					.append("context.log('UI initialized.');")

			case PopupResponse(URLEncoded(response)) =>
				userResponded.synchronized {
					popupResponse = Right(response)
					userResponded.notify
				}
				JavaScript log { "user chose '%s'" format response }

			case EmptyPopupResponse => popupCancelled
			case PopupCancelled => popupCancelled

			case Reset =>
				while (!footlights.runningApplications.isEmpty)
					footlights unloadApplication footlights.runningApplications.head

				reset()
				new JavaScript().append("context.globals['window'].location.reload()")

			case AsyncChannel =>
				val generation = asyncQueueGeneration
				asyncEvents.synchronized {
					while (asyncEvents.isEmpty) asyncEvents.wait
					asyncEvents.notify
					if (asyncQueueGeneration != generation) throw new AbortedSessionException

					new JavaScript()
						.append(setupAsyncChannel)
						.append(asyncEvents.dequeue)
				}

			case OpenRoot =>
				JavaScript log {
					footlights openDirectory "/" map footlights.openWithApplication fold (
						ex => ex.getMessage,
						dir => "shared '%s'" format dir
					)
				}

			case PromptApplication =>
				footlights promptUser "Application class path" map {
					"load_app/%s".format(_) } map
					JavaScript.ajax fold(ex => JavaScript log ex.toString, js => js)

			case LoadApplication(URLEncoded(path)) =>
				footlights.loadApplication(URI create path) match {
					case Right(wrapper) =>
						newContext(wrapper)

						wrapper.app.ajaxHandler map {
							_ setAsyncQueue new JavaScript.Sink {
								override def accept(js:JavaScript) =
									fireEvent { uiSandbox(wrapper.name).exec(js) }
							}
						}

						createUISandbox(wrapper.name)

					case Left(error) =>
						log log (logging.Level.WARNING, "Error loading application", error)
						val stackTrace = new java.io.StringWriter
						error printStackTrace new java.io.PrintWriter(stackTrace)

						new JavaScript()
							.append("context.log('Error loading application:\\n%s');" format
									(JavaScript sanitizeText stackTrace.toString)
								)
				}

			case EditUser(URLEncoded(uri)) =>
				URI.create(uri) |>
					footlights.identity map {
					_.root } map
					footlights.openWithApplication

				new JavaScript

			case FillPlaceholder(name) => {
				JSON(
					"key" -> name,
					"value" -> ((footlights evaluate name getOrElse "(unknown)"):String)
				)
			}

			case unhandled =>
				val message = "Unknown Ajax command: '%s'" format unhandled
				log log (logging.Level.WARNING, message)
				JavaScript log message
		}
	}


	def promptUser(title:String, question:String, callback: Either[core.UIException,String] => Any,
			default:Option[String] = None) =
		prompt { popup(title, question, new JavaScript("this.appendElement('input');")) }

	def choose(title:String, promptText:String,
			options: Iterable[String],
			default:Option[String] = None) =
		prompt {
			chooser(title, promptText,
				options map { label => label -> (JavaScript ajax PopupResponse(label)) }
			)
		}

	private val asyncEvents = collection.mutable.Queue[JavaScript]()
	private var asyncQueueGeneration = 0
	private[web] def fireEvent(event:JavaScript) = asyncEvents.synchronized {
		asyncEvents enqueue event
		asyncEvents notify
	}

	private def prompt(code:JavaScript) = userResponded.synchronized {
		fireEvent { code }
		userResponded.wait
		popupResponse
	}

	private val userResponded = new Object()
	private var popupResponse:Either[core.UIException,String] = _

	private def chooser(title:String, question:String, options:Iterable[(String,JavaScript)]) = {
		val optionHandlers = options map { case (label, handler) =>
			val completeHandler = new JavaScript("this.goaway();").append(handler)

			clickableText("popup", label, completeHandler,
				new JavaScript("this.goaway = function goaway() { popup.die(); };"))
		}

		popup(title, question, optionHandlers.toArray : _*)
	}

	/**
	 * @param  initActions       Initialization actions for the HTML form.
	 *                           Forms in popup dialogs come pre-populated with Submit and Cancel
	 *                           options, but require init actions like
	 *                           "this.appendElement('input')" to do anything useful.
	 */
	private def popup(title:String, text:String, initActions:JavaScript*) = {
		val setup = new JavaScript("""
var popup = context.root.appendElement('div');
popup.class = 'popup';
var head = popup.appendElement('div');
head.class = 'header';
head.style['border-bottom'] = '1px solid';
head.style['margin-bottom'] = '.25em';
head.appendText('""").appendText(title).append("""');

popup.appendText('""").appendText(text).append("""');

var form = popup.appendElement('form');
form.alldone = function() { popup.die(); };
form.onsubmit = function(value) {
	this.alldone();
	context.ajax('""").append(PopupResponse("' + value + '")).append("""');
};""")

		val input = initActions map { code =>
			new JavaScript("form['init'] = %s; form['init']()" format code.asFunction)
		} toList

		val submit = new JavaScript("form.appendElement('input').type = 'submit'")
		val cancel = clickableText("popup", "Cancel",
			new JavaScript("this.cancel()"),
			new JavaScript("this.cancel = " +
					(JavaScript ajax PopupCancelled).append("popup.die()").asFunction)
		)

		val epilogue = new JavaScript("return popup")

		(setup :: input) :+ submit :+ cancel :+ epilogue reduce { _ append _ }
	}

	private def popupCancelled = {
		userResponded.synchronized {
			popupResponse = Left(new core.CanceledException)
			userResponded.notify
		}
		JavaScript log "user cancelled popup"
	}

	private def clickableAjax(parent:String, label:String, ajaxText:String) =
		clickableText(parent, label, JavaScript ajax ajaxText)

	/**
	 * @param  parent        the JavaScript object to add the clickable text to
	 * @param  label         the clickable text to display
	 * @param  action        the action to take on click
	 * @param  initActions   Additional initialization actions, like setting up methods that
	 *                       refer to the surrounding scope.
	 *
	 *                       Event handlers don't have access to any scope but themselves, so
	 *                       in order to e.g. delete a popup dialog, the "dialog.die()" call should
	 *                       be a function set up with an init action (e.g.
	 *                       "this.cancel = function(){...}") and the click handler can then call
	 *                       "this.cancel()".
	 */
	private def clickableText(parent:String, label:String, action:JavaScript,
			initActions:JavaScript*) = {

		val js = new JavaScript()
			.append("""
var a = %s.appendElement('div').appendElement('a');
a.appendText('%s');
a.onclick = %s;
""" format (parent, JavaScript sanitizeText label, action asFunction "clickHandler"))

		initActions map { code =>
			"a['init'] = %s; a['init']();" format code.asFunction
		} foreach js.append

		js
	}

	private def createUISandbox(name:URI) = uiSandbox(name).append(".ajax('init');")
	private def uiSandbox(name:URI) =
		new JavaScript()
			.append("""
var sb = context.globals['sandboxes'].getOrCreate(
	'%s', context.globals['content'], context.log, {})
""" format (URLEncoded(name.toString).encoded, "utf-8"))


	private val Init            = "init"
	private val AsyncChannel    = "async_channel"
	private val Reset           = "reset"
	private val FillPlaceholder = """fill_placeholder/(\S+)""".r
	private val OpenRoot        = "open_root"
	private val PromptApplication = "prompt_for_app"
	private val LoadApplication = """load_app/(\S+)""".r
	private val EditUser        = """edit_user/(\S+)""".r

	private val EmptyPopupResponse = "user_prompt_response/"
	private val PopupResponse   = ("""%s(\S+)""" format EmptyPopupResponse).r
	private val PopupCancelled  = "cancel_popup"

	private val setupAsyncChannel =
		new JavaScript().append("context.globals['setupAsyncChannel']();")

	private val log = logging.Logger getLogger classOf[GlobalContext].getCanonicalName
}
