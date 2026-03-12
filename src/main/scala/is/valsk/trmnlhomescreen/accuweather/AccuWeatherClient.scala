package is.valsk.trmnlhomescreen.accuweather

import is.valsk.trmnlhomescreen.accuweather.AccuWeatherClient.Api.{CurrentConditionsEndpoint, SearchCityEndpoint}
import is.valsk.trmnlhomescreen.accuweather.AccuWeatherModel.*
import is.valsk.trmnlhomescreen.util.ApiClient.RequestMiddleware
import is.valsk.trmnlhomescreen.util.{ApiClient, Endpoint}
import zio.*
import zio.http.*
import zio.json.*

trait AccuWeatherClient:
  def searchCity(city: String): Task[Location]
  def currentConditions(locationKey: String): Task[CurrentConditions]

object AccuWeatherClient:

  def searchCity(city: String): ZIO[AccuWeatherClient, Throwable, Location] =
    ZIO.serviceWithZIO[AccuWeatherClient](_.searchCity(city))

  def currentConditions(locationKey: String): ZIO[AccuWeatherClient, Throwable, CurrentConditions] =
    ZIO.serviceWithZIO[AccuWeatherClient](_.currentConditions(locationKey))

  val layer: ZLayer[ApiClient & AccuWeatherConfig, Nothing, AccuWeatherClient] =
    ZLayer.fromFunction(LiveAccuWeatherClient.apply)

  val configuredLayer: ZLayer[ApiClient, Config.Error, AccuWeatherClient] = AccuWeatherConfig.layer >>> layer

  object Api {
    val BaseUrl: URL = URL.decode("https://dataservice.accuweather.com").toOption.get

    val SearchCityEndpoint = Endpoint[String, List[Location]](
      Method.GET,
      _ => "/locations/v1/cities/search",
      city => Map("q" -> city),
    )

    val CurrentConditionsEndpoint = Endpoint[String, List[CurrentConditions]](
      Method.GET,
      locationKey => s"/currentconditions/v1/$locationKey",
      _ => Map("details" -> "true"),
    )
  }

  private final case class LiveAccuWeatherClient(
      client: ApiClient,
      config: AccuWeatherConfig,
  ) extends AccuWeatherClient:

    def searchCity(city: String): Task[Location] =
      for {
        locations <- client.call(Api.BaseUrl, SearchCityEndpoint, city, middlewares)
        location <- ZIO.fromOption(locations.headOption)
          .orElseFail(RuntimeException(s"No location found for: $city"))
      } yield location

    def currentConditions(locationKey: String): Task[CurrentConditions] =
      for {
        conditions <- client.call(Api.BaseUrl, CurrentConditionsEndpoint, locationKey, middlewares)
        condition <- ZIO.fromOption(conditions.headOption)
          .orElseFail(RuntimeException("No conditions data available"))
      } yield condition

    private val middlewares: List[RequestMiddleware] = List(
      _.addHeader(Header.Authorization.Bearer(config.apiKey)),
      _.addHeader(Header.Accept(MediaType.application.json))
    )
