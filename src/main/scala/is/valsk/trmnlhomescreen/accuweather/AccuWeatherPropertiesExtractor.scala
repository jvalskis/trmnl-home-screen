package is.valsk.trmnlhomescreen.accuweather

import is.valsk.trmnlhomescreen.accuweather.AccuWeatherModel.*
import is.valsk.trmnlhomescreen.PropertiesExtractor
import is.valsk.trmnlhomescreen.PropertiesExtractor.{MapProperty, asScalar}
import zio.{RLayer, UIO, URLayer, ZIO, ZLayer}

object AccuWeatherPropertiesExtractor:

  val layer: URLayer[AccuWeatherStateRepository & AccuWeatherConfig, PropertiesExtractor] = ZLayer {
    for {
      repository <- ZIO.service[AccuWeatherStateRepository]
      config <- ZIO.service[AccuWeatherConfig]
    } yield new PropertiesExtractor:
      def extract: UIO[MapProperty] =
        for {
          maybeCurrentConditions <- repository.get
          inner = MapProperty(maybeCurrentConditions.toSeq.flatMap { conditions =>
            Seq(
              Nil :+ "weather_text" -> conditions.weatherText.asScalar,
              Nil :+ "weather_icon" -> conditions.weatherIcon.asScalar,
              Nil :+ "temp_metric_value" -> conditions.temperature.metric.value.asScalar,
              Nil :+ "temp_metric_unit" -> conditions.temperature.metric.unit.asScalar,
              Nil :+ "temp_imperial_value" -> conditions.temperature.imperial.value.asScalar,
              Nil :+ "temp_imperial_unit" -> conditions.temperature.imperial.unit.asScalar,
              Nil :+ "has_precipitation" -> conditions.hasPrecipitation.asScalar,
              Nil :+ "is_day_time" -> conditions.isDayTime.asScalar,
              Nil :+ "observation_time" -> conditions.localObservationDateTime.asScalar,
              conditions.relativeHumidity.toSeq.map("relative_humidity" -> _.asScalar),
              conditions.cloudCover.toSeq.map("cloud_cover" -> _.asScalar),
              conditions.uvIndex.toSeq.map("uv_index" -> _.asScalar),
              conditions.uvIndexText.toSeq.map("uv_index_text" -> _.asScalar),
              conditions.wind.toSeq.flatMap { wind =>
                Seq(
                  "wind_speed_metric_value" -> wind.speed.metric.value.asScalar,
                  "wind_speed_metric_unit" -> wind.speed.metric.unit.asScalar,
                  "wind_speed_imperial_value" -> wind.speed.imperial.value.asScalar,
                  "wind_speed_imperial_unit" -> wind.speed.imperial.unit.asScalar,
                  "wind_direction" -> wind.direction.localized.asScalar,
                  "wind_direction_degrees" -> wind.direction.degrees.asScalar,
                )
              },
              conditions.visibility.toSeq.flatMap { vis =>
                Seq(
                  "visibility_metric_value" -> vis.metric.value.asScalar,
                  "visibility_metric_unit" -> vis.metric.unit.asScalar,
                  "visibility_imperial_value" -> vis.imperial.value.asScalar,
                  "visibility_imperial_unit" -> vis.imperial.unit.asScalar,
                )
              },
              conditions.realFeelTemperature.toSeq.flatMap { realFeel =>
                Seq(
                  "real_feel_metric_value" -> realFeel.metric.value.asScalar,
                  "real_feel_metric_unit" -> realFeel.metric.unit.asScalar,
                  "real_feel_imperial_value" -> realFeel.imperial.value.asScalar,
                  "real_feel_imperial_unit" -> realFeel.imperial.unit.asScalar,
                )
              },
            )
          }.flatten: _*)
        } yield MapProperty(
          "accuweather" -> (inner + ("enabled" -> config.enabled.asScalar)),
        )
  }

  val configuredLayer: RLayer[AccuWeatherStateRepository, PropertiesExtractor] = AccuWeatherConfig.layer >>> layer
