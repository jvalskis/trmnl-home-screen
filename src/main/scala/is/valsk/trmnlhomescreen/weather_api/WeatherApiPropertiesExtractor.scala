package is.valsk.trmnlhomescreen.weather_api

import is.valsk.trmnlhomescreen.PropertiesExtractor
import is.valsk.trmnlhomescreen.PropertiesExtractor.{MapProperty, asScalar}
import zio.{RLayer, UIO, URLayer, ZIO, ZLayer}

import java.time.LocalDate

object WeatherApiPropertiesExtractor:

  private def weatherIconName(conditionCode: Int, isDay: Boolean): String =
    val prefix = if isDay then "day" else "night"
    conditionCode match
      case 1000 => if isDay then "wi-day-sunny" else "wi-night-clear"
      case 1003 => if isDay then "wi-day-cloudy" else "wi-night-alt-partly-cloudy"
      case 1006 => if isDay then "wi-cloudy" else "wi-night-alt-cloudy"
      case 1009 => "wi-cloudy"
      case 1030 => if isDay then "wi-fog" else "wi-night-fog"
      case 1063 => if isDay then "wi-day-showers" else "wi-night-alt-showers"
      case 1066 => if isDay then "wi-day-snow" else "wi-night-alt-snow"
      case 1069 => "wi-sleet"
      case 1072 => "wi-rain-mix"
      case 1087 => if isDay then "wi-day-thunderstorm" else "wi-night-alt-thunderstorm"
      case 1114 | 1117 => "wi-snow"
      case 1135 | 1147 => if isDay then "wi-fog" else "wi-night-fog"
      case 1150 | 1153 => if isDay then "wi-day-showers" else "wi-night-alt-showers"
      case 1168 | 1171 => "wi-rain-mix"
      case 1180 | 1183 => if isDay then "wi-day-showers" else "wi-night-alt-showers"
      case 1186 | 1189 => if isDay then "wi-day-rain" else "wi-night-alt-rain"
      case 1192 | 1195 => "wi-rain"
      case 1198 | 1201 => "wi-rain-mix"
      case 1204 | 1207 => "wi-sleet"
      case 1210 | 1213 | 1216 => if isDay then "wi-day-snow" else "wi-night-alt-snow"
      case 1219 | 1222 | 1225 => "wi-snow"
      case 1237 => "wi-sleet"
      case 1240 => if isDay then "wi-day-showers" else "wi-night-alt-showers"
      case 1243 => if isDay then "wi-day-rain" else "wi-night-alt-rain"
      case 1246 => "wi-rain"
      case 1249 | 1252 => "wi-sleet"
      case 1255 => if isDay then "wi-day-snow" else "wi-night-alt-snow"
      case 1258 => "wi-snow"
      case 1261 | 1264 => "wi-sleet"
      case 1273 => if isDay then "wi-day-thunderstorm" else "wi-night-alt-thunderstorm"
      case 1276 => "wi-thunderstorm"
      case 1279 => if isDay then "wi-day-storm-showers" else "wi-night-alt-storm-showers"
      case 1282 => "wi-thunderstorm"
      case _ => "wi-na"

  val layer: URLayer[WeatherApiStateRepository & WeatherApiConfig, PropertiesExtractor] = ZLayer {
    for {
      repository <- ZIO.service[WeatherApiStateRepository]
      config <- ZIO.service[WeatherApiConfig]
    } yield new PropertiesExtractor:
      def extract: UIO[MapProperty] =
        for {
          maybeCurrent <- repository.get
          forecastDays <- repository.getForecast
          inner = MapProperty(maybeCurrent.toSeq.flatMap { current =>
            Seq(
              Nil :+ "weather_text" -> current.condition.text.asScalar,
              Nil :+ "weather_icon" -> current.condition.code.asScalar,
              Nil :+ "weather_icon_name" -> weatherIconName(current.condition.code, current.isDay == 1).asScalar,
              Nil :+ "temp_c" -> current.tempC.asScalar,
              Nil :+ "temp_f" -> current.tempF.asScalar,
              Nil :+ "precip_mm" -> current.precipMm.asScalar,
              Nil :+ "is_day" -> current.isDay.asScalar,
              Nil :+ "last_updated" -> current.lastUpdated.asScalar,
              Nil :+ "humidity" -> current.humidity.asScalar,
              Nil :+ "cloud" -> current.cloud.asScalar,
              Nil :+ "uv" -> current.uv.asScalar,
              Nil :+ "wind_kph" -> current.windKph.asScalar,
              Nil :+ "wind_mph" -> current.windMph.asScalar,
              Nil :+ "wind_dir" -> current.windDir.asScalar,
              Nil :+ "wind_degree" -> current.windDegree.asScalar,
              Nil :+ "vis_km" -> current.visKm.asScalar,
              Nil :+ "vis_miles" -> current.visMiles.asScalar,
              Nil :+ "feelslike_c" -> current.feelslikeC.asScalar,
              Nil :+ "feelslike_f" -> current.feelslikeF.asScalar,
            )
          }.flatten: _*)
          forecastProperties = forecastDays.map { fd =>
            MapProperty(
              "date" -> LocalDate.parse(fd.date).asScalar,
              "high_c" -> fd.day.maxTempC.asScalar,
              "low_c" -> fd.day.minTempC.asScalar,
              "high_f" -> fd.day.maxTempF.asScalar,
              "low_f" -> fd.day.minTempF.asScalar,
              "condition_code" -> fd.day.condition.code.asScalar,
              "icon_name" -> weatherIconName(fd.day.condition.code, isDay = true).asScalar,
            )
          }.asList
        } yield MapProperty(
          "weatherapi" -> (inner + ("enabled" -> config.enabled.asScalar) + ("forecast" -> forecastProperties)),
        )
  }

  val configuredLayer: RLayer[WeatherApiStateRepository, PropertiesExtractor] = WeatherApiConfig.layer >>> layer
