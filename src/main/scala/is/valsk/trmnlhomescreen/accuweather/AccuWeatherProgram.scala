package is.valsk.trmnlhomescreen.accuweather

import is.valsk.trmnlhomescreen.accuweather.AccuWeatherModel.*
import is.valsk.trmnlhomescreen.Program
import zio.{Duration, RLayer, Schedule, Task, URLayer, ZIO, ZLayer}

trait AccuWeatherProgram extends Program

object AccuWeatherProgram {

  private class AccuWeatherProgramLive(config: AccuWeatherConfig, client: AccuWeatherClient, repository: AccuWeatherStateRepository)
      extends AccuWeatherProgram {

    def run: Task[Unit] =
      runIfEnabled(config.enabled, "AccuWeather feature is disabled") {
        for
          _ <- ZIO.logInfo(s"Starting AccuWeather app for ${config.city}")
          location <- client.searchCity(config.city)
          _ <- ZIO.logInfo(
            s"Found: ${location.localizedName}, ${location.country.localizedName} (key: ${location.key})",
          )
          interval = Duration.fromSeconds(config.fetchIntervalMinutes.toLong * 60)
          _ <- fetchAndStore(client, location.key)
            .catchAll(e => ZIO.logError(s"Failed to fetch weather: ${e.getMessage}"))
            .repeat(Schedule.fixed(interval))
        yield ()
      }

    private def fetchAndStore(client: AccuWeatherClient, locationKey: String): Task[Unit] =
      for
        conditions <- client.currentConditions(locationKey)
        _ <- repository.update(conditions)
      yield ()

  }

  val layer: URLayer[AccuWeatherStateRepository & AccuWeatherClient & AccuWeatherConfig, AccuWeatherProgram] = ZLayer {
    for {
      config <- ZIO.service[AccuWeatherConfig]
      client <- ZIO.service[AccuWeatherClient]
      repo <- ZIO.service[AccuWeatherStateRepository]
    } yield AccuWeatherProgramLive(config, client, repo)
  }

  val configuredLayer: RLayer[AccuWeatherStateRepository & AccuWeatherClient, AccuWeatherProgram] = AccuWeatherConfig.layer >>> layer
}
