package is.valsk.trmnlhomescreen.hass.messages

import zio.*

trait MessageIdGenerator {

  def generate(): UIO[Int]
}