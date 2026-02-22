package is.valsk.trmnlhomescreen.weather

import is.valsk.trmnlhomescreen.{Program, TemplateRenderer, WeatherConfig}
import zio.{Console, Duration, RLayer, Schedule, Task, URLayer, ZIO, ZLayer}

trait WeatherProgram extends Program

object WeatherProgram {

  private class WeatherProgramLive(config: WeatherConfig, client: AccuWeatherClient, renderer: TemplateRenderer)
      extends WeatherProgram {
    def run: Task[Unit] =
      for
        _ <- runIfEnabled(config.enabled, "Weather feature is disabled") {
          for
            _ <- ZIO.logInfo(s"Starting weather app for ${config.city}")
            location <- client.searchCity(config.city)
            _ <- ZIO.logInfo(
              s"Found: ${location.localizedName}, ${location.country.localizedName} (key: ${location.key})",
            )
            interval = Duration.fromSeconds(config.fetchIntervalMinutes.toLong * 60)
            _ <- fetchAndPrintWeather(client, renderer, location.key)
              .catchAll(e => ZIO.logError(s"Failed to fetch weather: ${e.getMessage}"))
              .repeat(Schedule.fixed(interval))
          yield ()
        }
      yield ()

    private def fetchAndPrintWeather(
        client: AccuWeatherClient,
        renderer: TemplateRenderer,
        locationKey: String,
    ): Task[Unit] =
      for
        conditions <- client.currentConditions(locationKey)
        rendered <- renderer.render(conditions)
        _ <- Console.printLine(rendered)
      yield ()

  }

  val layer: URLayer[TemplateRenderer & AccuWeatherClient & WeatherConfig, WeatherProgram] = ZLayer {
    for {
      config <- ZIO.service[WeatherConfig]
      client <- ZIO.service[AccuWeatherClient]
      renderer <- ZIO.service[TemplateRenderer]
    } yield WeatherProgramLive(config, client, renderer)
  }

  val configuredLayer: RLayer[TemplateRenderer & AccuWeatherClient, WeatherProgram] = WeatherConfig.layer >>> layer
}
