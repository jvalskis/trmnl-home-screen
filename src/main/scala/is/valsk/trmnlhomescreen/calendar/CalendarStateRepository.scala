package is.valsk.trmnlhomescreen.calendar

import zio.{Ref, UIO, ULayer, ZLayer}

trait CalendarStateRepository:
  def update(events: List[CalendarEvent]): UIO[Unit]
  def get: UIO[List[CalendarEvent]]

object CalendarStateRepository:

  private class LiveCalendarStateRepository(ref: Ref[List[CalendarEvent]]) extends CalendarStateRepository:
    def update(events: List[CalendarEvent]): UIO[Unit] = ref.set(events)

    def get: UIO[List[CalendarEvent]] = ref.get

  val layer: ULayer[CalendarStateRepository] = ZLayer {
    Ref.make(List.empty[CalendarEvent]).map(LiveCalendarStateRepository(_))
  }
