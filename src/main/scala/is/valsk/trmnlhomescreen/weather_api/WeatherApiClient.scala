package is.valsk.trmnlhomescreen.weather_api

import is.valsk.trmnlhomescreen.util.ApiClient.RequestMiddleware
import is.valsk.trmnlhomescreen.util.{ApiClient, Endpoint}
import is.valsk.trmnlhomescreen.weather_api.WeatherApiClient.Api.ForecastEndpoint
import is.valsk.trmnlhomescreen.weather_api.WeatherApiModel.*
import zio.*
import zio.http.*
import zio.json.*

trait WeatherApiClient:
  def forecast(query: String, days: Int): Task[ForecastResponse]

object WeatherApiClient:

  def forecast(query: String, days: Int): ZIO[WeatherApiClient, Throwable, ForecastResponse] =
    ZIO.serviceWithZIO[WeatherApiClient](_.forecast(query, days))

  val layer: ZLayer[ApiClient & WeatherApiConfig, Nothing, WeatherApiClient] =
    ZLayer.fromFunction(LiveWeatherApiClient.apply)

  val configuredLayer: ZLayer[ApiClient, Config.Error, WeatherApiClient] = WeatherApiConfig.layer >>> layer

  object Api {
    val BaseUrl: URL = URL.decode("https://api.weatherapi.com").toOption.get

    val ForecastEndpoint = Endpoint[(String, Int), ForecastResponse](
      Method.GET,
      _ => "/v1/forecast.json",
      { case (query, days) => Map("q" -> query, "days" -> days.toString) },
    )

  }

  private final case class LiveWeatherApiClient(
      client: ApiClient,
      config: WeatherApiConfig,
  ) extends WeatherApiClient:

    def forecast(query: String, days: Int): Task[ForecastResponse] =
      for {
        response <- client.call(Api.BaseUrl, ForecastEndpoint, (query, days), middlewares)
      } yield response

    private val middlewares: List[RequestMiddleware] = List(
      _.addQueryParam("key", config.apiKey),
      _.addHeader(Header.Accept(MediaType.application.json)),
    )
