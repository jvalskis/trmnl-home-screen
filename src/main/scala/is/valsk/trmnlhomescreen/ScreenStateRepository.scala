package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.calendar.CalendarEvent
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event.EntityState
import is.valsk.trmnlhomescreen.weather.CurrentConditions
import zio.{Ref, UIO, ULayer, ZLayer}

trait ScreenStateRepository:
  def updateCalendarEvents(events: List[CalendarEvent]): UIO[Unit]
  def updateWeatherConditions(conditions: CurrentConditions): UIO[Unit]
  def updateEntityState(entityId: String, state: EntityState): UIO[Unit]
  def updateEntityStates(states: Map[String, EntityState]): UIO[Unit]
  def addStates(states: Map[String, Any]): UIO[Unit]
  def get: UIO[ScreenState]

object ScreenStateRepository:

  private class ScreenStateRepositoryLive(ref: Ref[ScreenState]) extends ScreenStateRepository:

    def updateCalendarEvents(events: List[CalendarEvent]): UIO[Unit] =
      ref.update(_.copy(calendarEvents = Some(events)))

    def updateWeatherConditions(conditions: CurrentConditions): UIO[Unit] =
      ref.update(_.copy(weatherConditions = Some(conditions)))

    def updateEntityState(entityId: String, state: EntityState): UIO[Unit] =
      ref.update { s =>
        val current = s.entityStates.getOrElse(Map.empty)
        s.copy(entityStates = Some(current.updated(entityId, state)))
      }

    def updateEntityStates(states: Map[String, EntityState]): UIO[Unit] =
      ref.update { s =>
        val current = s.entityStates.getOrElse(Map.empty)
        s.copy(entityStates = Some(current ++ states))
      }

    override def addStates(states: Map[String, Any]): UIO[Unit] =
      ref.update(s => s.copy(state = s.state ++ states))

    def get: UIO[ScreenState] = ref.get

  val layer: ULayer[ScreenStateRepository] = ZLayer {
    Ref.make(ScreenState()).map(ScreenStateRepositoryLive(_))
  }
