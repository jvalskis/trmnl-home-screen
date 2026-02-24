package is.valsk.trmnlhomescreen.homeassistant.message.model

trait HassMessage {
  def `type`: String
}

trait HassIdentifiableMessage {
  def id: Int
}

trait HassRequestMessage extends HassMessage

trait HassResponseMessage extends HassMessage
