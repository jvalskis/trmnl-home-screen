package is.valsk.trmnlhomescreen.weather

import is.valsk.trmnlhomescreen.Program
import zio.{Duration, RLayer, Schedule, Task, URLayer, ZIO, ZLayer}

trait WeatherProgram extends Program

object WeatherProgram {

  private class WeatherProgramLive(config: WeatherConfig, client: AccuWeatherClient, weatherStateRepository: WeatherStateRepository)
      extends WeatherProgram {

    def run: Task[Unit] =
      runIfEnabled(config.enabled, "Weather feature is disabled") {
        for
          _ <- ZIO.logInfo(s"Starting weather app for ${config.city}")
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
        _ <- weatherStateRepository.update(conditions)
      yield ()

  }

  val layer: URLayer[WeatherStateRepository & AccuWeatherClient & WeatherConfig, WeatherProgram] = ZLayer {
    for {
      config <- ZIO.service[WeatherConfig]
      client <- ZIO.service[AccuWeatherClient]
      repo <- ZIO.service[WeatherStateRepository]
    } yield WeatherProgramLive(config, client, repo)
  }

  val configuredLayer: RLayer[WeatherStateRepository & AccuWeatherClient, WeatherProgram] = WeatherConfig.layer >>> layer
}
