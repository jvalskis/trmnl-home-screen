package is.valsk.trmnlhomescreen.weather_api

import is.valsk.trmnlhomescreen.Program
import zio.{Duration, RLayer, Schedule, Task, URLayer, ZIO, ZLayer}

trait WeatherApiProgram extends Program

object WeatherApiProgram {

  private class WeatherApiProgramLive(
      config: WeatherApiConfig,
      client: WeatherApiClient,
      repository: WeatherApiStateRepository,
  ) extends WeatherApiProgram {

    def run: Task[Unit] =
      runIfEnabled(config.enabled, "WeatherAPI feature is disabled") {
        for
          _ <- ZIO.logInfo(s"Starting WeatherAPI app for ${config.city}")
          interval = Duration.fromSeconds(config.fetchIntervalMinutes.toLong * 60)
          _ <- fetchAndStore(config.city)
            .catchAll(e => ZIO.logError(s"Failed to fetch weather from WeatherAPI: ${e.getMessage}"))
            .repeat(Schedule.fixed(interval))
        yield ()
      }

    private def fetchAndStore(city: String): Task[Unit] =
      for
        response <- client.forecast(city, config.forecastDays)
        _ <- repository.update(response.current)
        _ <- repository.updateForecast(response.forecast.forecastday)
      yield ()

  }

  val layer: URLayer[WeatherApiStateRepository & WeatherApiClient & WeatherApiConfig, WeatherApiProgram] = ZLayer {
    for {
      config <- ZIO.service[WeatherApiConfig]
      client <- ZIO.service[WeatherApiClient]
      repo <- ZIO.service[WeatherApiStateRepository]
    } yield WeatherApiProgramLive(config, client, repo)
  }

  val configuredLayer: RLayer[WeatherApiStateRepository & WeatherApiClient, WeatherApiProgram] =
    WeatherApiConfig.layer >>> layer

}
