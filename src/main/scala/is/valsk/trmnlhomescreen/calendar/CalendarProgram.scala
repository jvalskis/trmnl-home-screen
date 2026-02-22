package is.valsk.trmnlhomescreen.calendar

import is.valsk.trmnlhomescreen.{Program, TemplateRenderer}
import zio.{Console, Duration, RLayer, Schedule, Task, URLayer, ZIO, ZLayer}

trait CalendarProgram extends Program

object CalendarProgram {

  private class CalendarProgramLive(config: CalendarConfig, client: CalDavClient, renderer: CalendarRenderer)
      extends CalendarProgram {

    def run: Task[Unit] =
      for
        _ <- runIfEnabled(config.enabled, "Calendar feature is disabled") {
          for
            _ <- ZIO.logInfo(s"Starting calendar sync from ${config.calendarUrl}")
            interval = Duration.fromSeconds(config.fetchIntervalMinutes.toLong * 60)
            _ <- fetchAndPrintCalendar(client, renderer)
              .catchAll(e => ZIO.logError(s"Failed to fetch calendar: ${e.getMessage}"))
              .repeat(Schedule.fixed(interval))
          yield ()
        }
      yield ()

    private def fetchAndPrintCalendar(
        client: CalDavClient,
        renderer: CalendarRenderer,
    ): Task[Unit] =
      for
        events <- client.fetchEvents()
        rendered <- renderer.render(events)
        _ <- Console.printLine(rendered)
      yield ()

  }

  val layer: URLayer[CalendarRenderer & CalDavClient & CalendarConfig, CalendarProgram] = ZLayer {
    for {
      config <- ZIO.service[CalendarConfig]
      client <- ZIO.service[CalDavClient]
      renderer <- ZIO.service[CalendarRenderer]
    } yield CalendarProgramLive(config, client, renderer)
  }

  val configuredLayer: RLayer[CalendarRenderer & CalDavClient, CalendarProgram] = CalendarConfig.layer >>> layer
}
