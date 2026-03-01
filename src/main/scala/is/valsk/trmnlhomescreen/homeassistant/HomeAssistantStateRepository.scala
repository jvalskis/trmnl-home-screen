package is.valsk.trmnlhomescreen.homeassistant

import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event.EntityState
import zio.{Ref, UIO, ULayer, ZLayer}

trait HomeAssistantStateRepository:
  def updateEntityState(entityId: String, state: EntityState): UIO[Unit]
  def updateEntityStates(states: Map[String, EntityState]): UIO[Unit]
  def get: UIO[Map[String, EntityState]]

object HomeAssistantStateRepository:

  private class LiveHomeAssistantStateRepository(ref: Ref[Map[String, EntityState]])
      extends HomeAssistantStateRepository:

    def updateEntityState(entityId: String, state: EntityState): UIO[Unit] =
      ref.update { current =>
        current.updated(entityId, state)
      }

    def updateEntityStates(states: Map[String, EntityState]): UIO[Unit] =
      ref.update { current =>
        current ++ states
      }

    def get: UIO[Map[String, EntityState]] = ref.get

  val layer: ULayer[HomeAssistantStateRepository] = ZLayer {
    Ref.make(Map.empty[String, EntityState]).map(LiveHomeAssistantStateRepository(_))
  }
