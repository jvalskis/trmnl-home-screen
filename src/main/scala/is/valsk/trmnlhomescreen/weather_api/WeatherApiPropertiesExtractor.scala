package is.valsk.trmnlhomescreen.weather_api

import is.valsk.trmnlhomescreen.PropertiesExtractor
import is.valsk.trmnlhomescreen.PropertiesExtractor.{MapProperty, asScalar}
import zio.{RLayer, UIO, URLayer, ZIO, ZLayer}

import java.time.LocalDate

object WeatherApiPropertiesExtractor:

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
            )
          }.asList
        } yield MapProperty(
          "weatherapi" -> (inner + ("enabled" -> config.enabled.asScalar) + ("forecast" -> forecastProperties)),
        )
  }

  val configuredLayer: RLayer[WeatherApiStateRepository, PropertiesExtractor] = WeatherApiConfig.layer >>> layer
