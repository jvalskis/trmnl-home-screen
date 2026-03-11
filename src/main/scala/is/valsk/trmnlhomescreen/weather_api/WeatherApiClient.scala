package is.valsk.trmnlhomescreen.weather_api

import is.valsk.trmnlhomescreen.util.ApiClient.RequestMiddleware
import is.valsk.trmnlhomescreen.util.{ApiClient, Endpoint}
import is.valsk.trmnlhomescreen.weather_api.WeatherApiClient.Api.CurrentWeatherEndpoint
import is.valsk.trmnlhomescreen.weather_api.WeatherApiModel.*
import zio.*
import zio.http.*
import zio.json.*

import java.net.URLEncoder

trait WeatherApiClient:
  def currentWeather(query: String): Task[Current]

object WeatherApiClient:

  def currentWeather(query: String): ZIO[WeatherApiClient, Throwable, Current] =
    ZIO.serviceWithZIO[WeatherApiClient](_.currentWeather(query))

  val layer: ZLayer[ApiClient & WeatherApiConfig, Nothing, WeatherApiClient] =
    ZLayer.fromFunction(LiveWeatherApiClient.apply)

  val configuredLayer: ZLayer[ApiClient, Config.Error, WeatherApiClient] = WeatherApiConfig.layer >>> layer

  object Api {
    val BaseUrl: URL = URL.decode("https://api.weatherapi.com").toOption.get

    val CurrentWeatherEndpoint = Endpoint[String, CurrentWeatherResponse](
      Method.GET,
      query => s"/v1/current.json?q=${URLEncoder.encode(query, "UTF-8")}",
    )

  }

  private final case class LiveWeatherApiClient(
      client: ApiClient,
      config: WeatherApiConfig,
  ) extends WeatherApiClient:

    def currentWeather(query: String): Task[Current] =
      for {
        response <- client.call(Api.BaseUrl, CurrentWeatherEndpoint, query, middlewares)
      } yield response.current

    private val middlewares: List[RequestMiddleware] = List(
      _.addQueryParam("key", config.apiKey),
      _.addHeader(Header.Accept(MediaType.application.json)),
    )
