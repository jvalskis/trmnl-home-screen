package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.calendar.{CalDavClient, CalendarConfig, CalendarRenderer}
import is.valsk.trmnlhomescreen.weather.AccuWeatherClient
import zio.*
import zio.http.Client

object Main extends ZIOAppDefault:

  override def run: ZIO[Any, Any, Any] =
    val weatherProgram = for
      config <- ZIO.service[WeatherConfig]
      client <- ZIO.service[AccuWeatherClient]
      renderer <- ZIO.service[TemplateRenderer]
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

    val calendarProgram = for
      config <- ZIO.service[CalendarConfig]
      client <- ZIO.service[CalDavClient]
      renderer <- ZIO.service[CalendarRenderer]
      _ <- ZIO.logInfo(s"Starting calendar sync from ${config.calendarUrl}")
      interval = Duration.fromSeconds(config.fetchIntervalMinutes.toLong * 60)
      _ <- fetchAndPrintCalendar(client, renderer)
        .catchAll(e => ZIO.logError(s"Failed to fetch calendar: ${e.getMessage}"))
        .repeat(Schedule.fixed(interval))
    yield ()

    (weatherProgram <&> calendarProgram).provide(
      Client.default,
      WeatherConfig.layer,
      AccuWeatherClient.layer,
      TemplateRenderer.layer,
      CalendarConfig.layer,
      CalDavClient.layer,
      CalendarRenderer.layer,
    )

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

  private def fetchAndPrintCalendar(
      client: CalDavClient,
      renderer: CalendarRenderer,
  ): Task[Unit] =
    for
      events <- client.fetchEvents()
      rendered <- renderer.render(events)
      _ <- Console.printLine(rendered)
    yield ()
