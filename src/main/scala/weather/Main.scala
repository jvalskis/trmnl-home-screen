package weather

import zio.*
import zio.http.Client

object Main extends ZIOAppDefault:

  override def run: ZIO[Any, Any, Any] =
    val program = for
      config <- ZIO.service[WeatherConfig]
      client <- ZIO.service[AccuWeatherClient]
      _ <- ZIO.logInfo(s"Starting weather app for ${config.city}")
      location <- client.searchCity(config.city)
      _ <- ZIO.logInfo(
        s"Found: ${location.localizedName}, ${location.country.localizedName} (key: ${location.key})",
      )
      interval = Duration.fromSeconds(config.fetchIntervalMinutes.toLong * 60)
      _ <- fetchAndPrint(client, location.key)
        .catchAll(e => ZIO.logError(s"Failed to fetch weather: ${e.getMessage}"))
        .repeat(Schedule.fixed(interval))
    yield ()

    program.provide(
      Client.default,
      WeatherConfig.layer,
      AccuWeatherClient.layer,
    )

  private def fetchAndPrint(client: AccuWeatherClient, locationKey: String): Task[Unit] =
    for
      conditions <- client.currentConditions(locationKey)
      _ <- Console.printLine(formatConditions(conditions))
    yield ()

  private def formatConditions(c: CurrentConditions): String =
    s"""
      |--- Current Weather ---
      |Time:          ${c.localObservationDateTime}
      |Conditions:    ${c.weatherText}
      |Temperature:   ${c.temperature.metric.value}°${c.temperature.metric.unit} / ${c.temperature.imperial.value}°${c.temperature.imperial.unit}
      |Precipitation: ${if c.hasPrecipitation then "Yes" else "No"}
      |Day time:      ${if c.isDayTime then "Yes" else "No"}
      |""".stripMargin
