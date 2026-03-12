package is.valsk.trmnlhomescreen.weather_api

import zio.json.*

object WeatherApiModel:

  final case class Condition(
      text: String,
      icon: String,
      code: Int,
  )

  object Condition:
    given JsonDecoder[Condition] = DeriveJsonDecoder.gen[Condition]

  final case class Current(
      @jsonField("temp_c")
      tempC: Double,
      @jsonField("temp_f")
      tempF: Double,
      @jsonField("is_day")
      isDay: Int,
      condition: Condition,
      @jsonField("wind_mph")
      windMph: Double,
      @jsonField("wind_kph")
      windKph: Double,
      @jsonField("wind_degree")
      windDegree: Int,
      @jsonField("wind_dir")
      windDir: String,
      humidity: Int,
      cloud: Int,
      @jsonField("feelslike_c")
      feelslikeC: Double,
      @jsonField("feelslike_f")
      feelslikeF: Double,
      @jsonField("vis_km")
      visKm: Double,
      @jsonField("vis_miles")
      visMiles: Double,
      uv: Double,
      @jsonField("precip_mm")
      precipMm: Double,
      @jsonField("last_updated")
      lastUpdated: String,
  )

  object Current:
    given JsonDecoder[Current] = DeriveJsonDecoder.gen[Current]

  final case class CurrentWeatherResponse(
      current: Current,
  )

  object CurrentWeatherResponse:
    given JsonDecoder[CurrentWeatherResponse] = DeriveJsonDecoder.gen[CurrentWeatherResponse]
