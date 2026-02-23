package is.valsk.trmnlhomescreen.hass.messages

import is.valsk.trmnlhomescreen.hass.messages.MessageParser.ParseError
import zio.*

trait MessageParser[T] {

  def parseMessage(json: String): IO[ParseError, T]
}

object MessageParser {

  case class ParseError(message: String, underlying: Option[Throwable] = None) extends Exception(s"Parse error: $message", underlying.orNull)
}
