package is.valsk.trmnlhomescreen.calendar

import is.valsk.trmnlhomescreen.Program
import zio.{Duration, RLayer, Schedule, Task, URLayer, ZIO, ZLayer}

trait CalendarProgram extends Program

object CalendarProgram {

  private class CalendarProgramLive(config: CalendarConfig, client: CalDavClient, calendarStateRepository: CalendarStateRepository)
      extends CalendarProgram {

    def run: Task[Unit] =
      runIfEnabled(config.enabled, "Calendar feature is disabled") {
        for
          _ <- ZIO.logInfo(s"Starting calendar sync from ${config.calendarUrl}")
          interval = Duration.fromSeconds(config.fetchIntervalMinutes.toLong * 60)
          _ <- fetchAndStore(client)
            .catchAll(e => ZIO.logError(s"Failed to fetch calendar: ${e.getMessage}"))
            .repeat(Schedule.fixed(interval))
        yield ()
      }

    private def fetchAndStore(client: CalDavClient): Task[Unit] =
      for
        events <- client.fetchEvents()
        _ <- calendarStateRepository.update(events)
      yield ()

  }

  val layer: URLayer[CalendarStateRepository & CalDavClient & CalendarConfig, CalendarProgram] = ZLayer {
    for {
      config <- ZIO.service[CalendarConfig]
      client <- ZIO.service[CalDavClient]
      repo <- ZIO.service[CalendarStateRepository]
    } yield CalendarProgramLive(config, client, repo)
  }

  val configuredLayer: RLayer[CalendarStateRepository & CalDavClient, CalendarProgram] = CalendarConfig.layer >>> layer
}
