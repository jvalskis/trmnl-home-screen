package is.valsk.trmnlhomescreen

import zio.*
import zio.http.Client

object Main extends ZIOAppDefault:

  override def run: ZIO[Any, Any, Any] =
    val program = for
      config <- ZIO.service[WeatherConfig]
      client <- ZIO.service[AccuWeatherClient]
      renderer <- ZIO.service[TemplateRenderer]
      _ <- ZIO.logInfo(s"Starting weather app for ${config.city}")
      location <- client.searchCity(config.city)
      _ <- ZIO.logInfo(
        s"Found: ${location.localizedName}, ${location.country.localizedName} (key: ${location.key})",
      )
      interval = Duration.fromSeconds(config.fetchIntervalMinutes.toLong * 60)
      _ <- fetchAndPrint(client, renderer, location.key)
        .catchAll(e => ZIO.logError(s"Failed to fetch weather: ${e.getMessage}"))
        .repeat(Schedule.fixed(interval))
    yield ()

    program.provide(
      Client.default,
      WeatherConfig.layer,
      AccuWeatherClient.layer,
      TemplateRenderer.layer,
    )

  private def fetchAndPrint(
      client: AccuWeatherClient,
      renderer: TemplateRenderer,
      locationKey: String,
  ): Task[Unit] =
    for
      conditions <- client.currentConditions(locationKey)
      rendered <- renderer.render(conditions)
      _ <- Console.printLine(rendered)
    yield ()
