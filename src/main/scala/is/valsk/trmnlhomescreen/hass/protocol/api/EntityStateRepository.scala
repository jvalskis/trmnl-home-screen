package is.valsk.trmnlhomescreen.hass.protocol.api

import is.valsk.trmnlhomescreen.hass.EntityState
import zio.{Ref, UIO, ULayer, ZLayer}

trait EntityStateRepository {
  def add(entityId: String, state: EntityState): UIO[Unit]
  def get(entityId: String): UIO[Option[EntityState]]
  def getAll: UIO[Map[String, EntityState]]
}

object EntityStateRepository {

  private class EntityStateRepositoryLive(
      ref: Ref[Map[String, EntityState]],
  ) extends EntityStateRepository {

    def add(entityId: String, state: EntityState): UIO[Unit] =
      ref.update(_.updated(entityId, state))

    def get(entityId: String): UIO[Option[EntityState]] =
      ref.get.map(_.get(entityId))

    def getAll: UIO[Map[String, EntityState]] =
      ref.get
  }

  val layer: ULayer[EntityStateRepository] = ZLayer {
    Ref.make[Map[String, EntityState]](Map.empty).map(EntityStateRepositoryLive(_))
  }

}
