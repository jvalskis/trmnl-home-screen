package is.valsk.trmnlhomescreen.hass.protocol.api

import is.valsk.trmnlhomescreen.hass.messages.Type
import zio.{Ref, UIO, ULayer, ZLayer}

trait RequestRepository {
  def add(requestId: Int, messageType: Type): UIO[Unit]
  def get(requestId: Int): UIO[Option[Type]]
}

object RequestRepository {

  private class RequestRepositoryLive(
      ref: Ref[Map[Int, Type]],
  ) extends RequestRepository {

    def add(requestId: Int, messageType: Type): UIO[Unit] = ref.update(_.updated(requestId, messageType))

    def get(requestId: Int): UIO[Option[Type]] = ref.get.map(_.get(requestId))
  }

  val layer: ULayer[RequestRepository] = ZLayer {
    Ref.make[Map[Int, Type]](Map.empty).map(RequestRepositoryLive(_))
  }

}
