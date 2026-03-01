package is.valsk.trmnlhomescreen.weather

import is.valsk.trmnlhomescreen.PropertiesExtractor
import zio.{RLayer, UIO, URLayer, ZIO, ZLayer}

object WeatherPropertiesExtractor:

  val layer: URLayer[WeatherStateRepository & WeatherConfig, PropertiesExtractor] = ZLayer {
    for {
      repository <- ZIO.service[WeatherStateRepository]
      config <- ZIO.service[WeatherConfig]
    } yield new PropertiesExtractor:
      def extract: UIO[Map[String, Any]] =
        for {
          maybeCurrentConditions <- repository.get
          result = maybeCurrentConditions.toSeq.flatMap { conditions =>
            Seq(
              Nil :+ "weather_text" -> conditions.weatherText,
              Nil :+ "weather_icon" -> conditions.weatherIcon,
              Nil :+ "temp_metric_value" -> conditions.temperature.metric.value,
              Nil :+ "temp_metric_unit" -> conditions.temperature.metric.unit,
              Nil :+ "temp_imperial_value" -> conditions.temperature.imperial.value,
              Nil :+ "temp_imperial_unit" -> conditions.temperature.imperial.unit,
              Nil :+ "has_precipitation" -> conditions.hasPrecipitation,
              Nil :+ "is_day_time" -> conditions.isDayTime,
              Nil :+ "observation_time" -> conditions.localObservationDateTime,
              conditions.relativeHumidity.toSeq.map("relative_humidity" -> _),
              conditions.cloudCover.toSeq.map("cloud_cover" -> _),
              conditions.uvIndex.toSeq.map("uv_index" -> _),
              conditions.uvIndexText.toSeq.map("uv_index_text" -> _),
              conditions.wind.toSeq.flatMap { wind =>
                Seq(
                  "wind_speed_metric_value" -> wind.speed.metric.value,
                  "wind_speed_metric_unit" -> wind.speed.metric.unit,
                  "wind_speed_imperial_value" -> wind.speed.imperial.value,
                  "wind_speed_imperial_unit" -> wind.speed.imperial.unit,
                  "wind_direction" -> wind.direction.localized,
                  "wind_direction_degrees" -> wind.direction.degrees,
                )
              },
              conditions.visibility.toSeq.flatMap { vis =>
                Seq(
                  "visibility_metric_value" -> vis.metric.value,
                  "visibility_metric_unit" -> vis.metric.unit,
                  "visibility_imperial_value" -> vis.imperial.value,
                  "visibility_imperial_unit" -> vis.imperial.unit,
                )
              },
              conditions.realFeelTemperature.toSeq.flatMap { realFeel =>
                Seq(
                  "real_feel_metric_value" -> realFeel.metric.value,
                  "real_feel_metric_unit" -> realFeel.metric.unit,
                  "real_feel_imperial_value" -> realFeel.imperial.value,
                  "real_feel_imperial_unit" -> realFeel.imperial.unit,
                )
              },
            )
          }.flatten.toMap
        } yield result + ("weather_enabled" -> config.enabled)
  }

  val configuredLayer: RLayer[WeatherStateRepository, PropertiesExtractor] = WeatherConfig.layer >>> layer
