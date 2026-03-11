package is.valsk.trmnlhomescreen.weather_api

import is.valsk.trmnlhomescreen.PropertiesExtractor
import is.valsk.trmnlhomescreen.PropertiesExtractor.{MapProperty, asScalar}
import zio.{RLayer, UIO, URLayer, ZIO, ZLayer}

object WeatherApiPropertiesExtractor:

  val layer: URLayer[WeatherApiStateRepository & WeatherApiConfig, PropertiesExtractor] = ZLayer {
    for {
      repository <- ZIO.service[WeatherApiStateRepository]
      config <- ZIO.service[WeatherApiConfig]
    } yield new PropertiesExtractor:
      def extract: UIO[MapProperty] =
        for {
          maybeCurrent <- repository.get
          inner = MapProperty(maybeCurrent.toSeq.flatMap { c =>
            Seq(
              Nil :+ "weather_text" -> c.condition.text.asScalar,
              Nil :+ "weather_icon" -> c.condition.code.asScalar,
              Nil :+ "temp_c" -> c.tempC.asScalar,
              Nil :+ "temp_f" -> c.tempF.asScalar,
              Nil :+ "precip_mm" -> c.precipMm.asScalar,
              Nil :+ "is_day" -> c.isDay.asScalar,
              Nil :+ "last_updated" -> c.lastUpdated.asScalar,
              Nil :+ "humidity" -> c.humidity.asScalar,
              Nil :+ "cloud" -> c.cloud.asScalar,
              Nil :+ "uv" -> c.uv.asScalar,
              Nil :+ "wind_kph" -> c.windKph.asScalar,
              Nil :+ "wind_mph" -> c.windMph.asScalar,
              Nil :+ "wind_dir" -> c.windDir.asScalar,
              Nil :+ "wind_degree" -> c.windDegree.asScalar,
              Nil :+ "vis_km" -> c.visKm.asScalar,
              Nil :+ "vis_miles" -> c.visMiles.asScalar,
              Nil :+ "feelslike_c" -> c.feelslikeC.asScalar,
              Nil :+ "feelslike_f" -> c.feelslikeF.asScalar,
            )
          }.flatten: _*)
        } yield MapProperty(
          "weatherapi" -> (inner + ("enabled" -> config.enabled.asScalar)),
        )
  }

  val configuredLayer: RLayer[WeatherApiStateRepository, PropertiesExtractor] = WeatherApiConfig.layer >>> layer
