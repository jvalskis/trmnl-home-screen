package is.valsk.trmnlhomescreen.weather

import is.valsk.trmnlhomescreen.{CurrentConditions, Location}
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

  val layer: ZLayer[Client & WeatherConfig, Nothing, AccuWeatherClient] =
    ZLayer.fromFunction(LiveAccuWeatherClient.apply)

  val configuredLayer: ZLayer[Client, Config.Error, AccuWeatherClient] = WeatherConfig.layer >>> layer

  private final case class LiveAccuWeatherClient(
      client: Client,
      config: WeatherConfig,
  ) extends AccuWeatherClient:

    private val baseUrl = "https://dataservice.accuweather.com"

    def searchCity(city: String): Task[Location] =
      val encoded = java.net.URLEncoder.encode(city, "UTF-8")
      val urlStr = s"$baseUrl/locations/v1/cities/search?q=$encoded&apikey=${config.apiKey}"
      for
        url <- ZIO.fromEither(URL.decode(urlStr))
          .mapError(e => RuntimeException(s"Invalid URL: $urlStr"))
        body <- ZIO.scoped(client.request(Request.get(url)).flatMap(_.body.asString))
        locations <- ZIO.fromEither(body.fromJson[List[Location]])
          .mapError(msg => RuntimeException(s"JSON parse error: $msg"))
        location <- ZIO.fromOption(locations.headOption)
          .orElseFail(RuntimeException(s"No location found for: $city"))
      yield location

    def currentConditions(locationKey: String): Task[CurrentConditions] =
      val urlStr = s"$baseUrl/currentconditions/v1/$locationKey?apikey=${config.apiKey}&details=true"
      for
        url <- ZIO.fromEither(URL.decode(urlStr))
          .mapError(e => RuntimeException(s"Invalid URL: $urlStr"))
        body <- ZIO.scoped(client.request(Request.get(url)).flatMap(_.body.asString))
        _ <- ZIO.logInfo(body)
        conditions <- ZIO.fromEither(body.fromJson[List[CurrentConditions]])
          .mapError(msg => RuntimeException(s"JSON parse error: $msg"))
        condition <- ZIO.fromOption(conditions.headOption)
          .orElseFail(RuntimeException("No conditions data available"))
      yield condition