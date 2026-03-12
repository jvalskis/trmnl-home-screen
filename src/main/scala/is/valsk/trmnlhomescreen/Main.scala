package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.accuweather.{AccuWeatherClient, AccuWeatherProgram, AccuWeatherPropertiesExtractor, AccuWeatherStateRepository}
import is.valsk.trmnlhomescreen.calendar.{CalDavClient, CalendarProgram, CalendarPropertiesExtractor, CalendarStateRepository}
import is.valsk.trmnlhomescreen.homeassistant.{HomeAssistantAreaRepository, HomeAssistantProgram, HomeAssistantPropertiesExtractor, HomeAssistantStateRepository}
import is.valsk.trmnlhomescreen.trmnl.TrmnlClient
import is.valsk.trmnlhomescreen.util.ApiClient
import is.valsk.trmnlhomescreen.weather_api.{WeatherApiClient, WeatherApiProgram, WeatherApiPropertiesExtractor, WeatherApiStateRepository}
import zio.*
import zio.http.Client
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override def run: ZIO[Any, Any, Any] =
    val program =
      ZIO.serviceWithZIO[AccuWeatherProgram](_.run) <&>
        ZIO.serviceWithZIO[WeatherApiProgram](_.run) <&>
        ZIO.serviceWithZIO[CalendarProgram](_.run) <&>
        ZIO.serviceWithZIO[HomeAssistantProgram](_.run) <&>
        ZIO.serviceWithZIO[RenderProgram](_.run)

    program.provide(
      Client.default,
      ApiClient.layer,
      AccuWeatherClient.configuredLayer,
      WeatherApiClient.configuredLayer,
      CalDavClient.configuredLayer,
      AccuWeatherStateRepository.layer,
      WeatherApiStateRepository.layer,
      CalendarStateRepository.layer,
      HomeAssistantStateRepository.layer,
      HomeAssistantAreaRepository.layer,
      extractorsLayer,
      ScreenRenderer.configuredLayer,
      TrmnlClient.configuredLayer,
      RenderProgram.configuredLayer,
      AccuWeatherProgram.configuredLayer,
      WeatherApiProgram.configuredLayer,
      CalendarProgram.configuredLayer,
      HomeAssistantProgram.configuredLayer,
    )

  private val extractorsLayer: ZLayer[
    CalendarStateRepository & AccuWeatherStateRepository & WeatherApiStateRepository & HomeAssistantStateRepository & HomeAssistantAreaRepository,
    Throwable,
    List[PropertiesExtractor],
  ] = ZLayer.scoped {
    for
      calendarExtractor <- CalendarPropertiesExtractor.configuredLayer.build.map(_.get[PropertiesExtractor])
      accuWeatherExtractor <- AccuWeatherPropertiesExtractor.configuredLayer.build.map(_.get[PropertiesExtractor])
      weatherApiExtractor <- WeatherApiPropertiesExtractor.configuredLayer.build.map(_.get[PropertiesExtractor])
      homeAssistantExtractor <- HomeAssistantPropertiesExtractor.configuredLayer.build.map(_.get[PropertiesExtractor])
    yield List(calendarExtractor, accuWeatherExtractor, weatherApiExtractor, homeAssistantExtractor)
  }
