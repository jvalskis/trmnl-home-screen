package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.calendar.CalendarEvent
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event.EntityState

final case class ScreenState(
    calendarEvents: Option[List[CalendarEvent]] = None,
    weatherConditions: Option[CurrentConditions] = None,
    entityStates: Option[Map[String, EntityState]] = None,
)
