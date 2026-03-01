package is.valsk.trmnlhomescreen.weather

import is.valsk.trmnlhomescreen.PropertiesExtractor
import zio.*
import zio.test.*

object WeatherPropertiesExtractorSpec extends ZIOSpecDefault:

  private val enabledConfig = WeatherConfig(
    enabled = true,
    apiKey = "test-key",
    city = "Vilnius",
    fetchIntervalMinutes = 30,
  )

  private val testLayer =
    WeatherStateRepository.layer ++ ZLayer.succeed(enabledConfig) >>> (WeatherStateRepository.layer ++ WeatherPropertiesExtractor.layer)

  private val minimalConditions = CurrentConditions(
    weatherText = "Sunny",
    weatherIcon = 1,
    temperature = ValueContainer(
      metric = Value(22.0, "C"),
      imperial = Value(71.6, "F"),
    ),
    realFeelTemperature = None,
    hasPrecipitation = false,
    precipitationType = None,
    isDayTime = true,
    relativeHumidity = None,
    wind = None,
    uvIndex = None,
    uvIndexText = None,
    visibility = None,
    cloudCover = None,
    localObservationDateTime = "2026-03-01T12:00:00+02:00",
  )

  private val fullConditions = minimalConditions.copy(
    weatherText = "Partly cloudy",
    weatherIcon = 3,
    hasPrecipitation = true,
    relativeHumidity = Some(65),
    cloudCover = Some(40),
    uvIndex = Some(5),
    uvIndexText = Some("Moderate"),
    wind = Some(Wind(
      direction = Direction(degrees = 180, localized = "S", english = "S"),
      speed = ValueContainer(
        metric = Value(15.0, "km/h"),
        imperial = Value(9.3, "mi/h"),
      ),
    )),
    visibility = Some(ValueContainer(
      metric = Value(10.0, "km"),
      imperial = Value(6.2, "mi"),
    )),
    realFeelTemperature = Some(ValueContainer(
      metric = Value(20.0, "C"),
      imperial = Value(68.0, "F"),
    )),
  )

  private def withConditions(conditions: CurrentConditions) =
    for
      repo <- ZIO.service[WeatherStateRepository]
      _ <- repo.update(conditions)
      extractor <- ZIO.service[PropertiesExtractor]
      result <- extractor.extract
    yield result

  def spec = suite("WeatherPropertiesExtractor")(
    test("includes weather_enabled from config") {
      (for
        extractor <- ZIO.service[PropertiesExtractor]
        result <- extractor.extract
      yield assertTrue(result("weather_enabled") == true))
        .provide(testLayer)
    },
    test("weather_enabled is false when config disabled") {
      val disabledConfig = enabledConfig.copy(enabled = false)
      val layer = WeatherStateRepository.layer ++ ZLayer.succeed(disabledConfig) >>>
        (WeatherStateRepository.layer ++ WeatherPropertiesExtractor.layer)
      (for
        extractor <- ZIO.service[PropertiesExtractor]
        result <- extractor.extract
      yield assertTrue(result("weather_enabled") == false))
        .provide(layer)
    },
    test("returns only weather_enabled when repository is empty") {
      (for
        extractor <- ZIO.service[PropertiesExtractor]
        result <- extractor.extract
      yield assertTrue(
        result.size == 1,
        result("weather_enabled") == true,
      ))
        .provide(testLayer)
    },
    test("extracts core weather properties") {
      withConditions(minimalConditions)
        .map { result =>
          assertTrue(
            result("weather_text") == "Sunny",
            result("weather_icon") == 1,
            result("temp_metric_value") == 22.0,
            result("temp_metric_unit") == "C",
            result("temp_imperial_value") == 71.6,
            result("temp_imperial_unit") == "F",
            result("has_precipitation") == false,
            result("is_day_time") == true,
            result("observation_time") == "2026-03-01T12:00:00+02:00",
          )
        }
        .provide(testLayer)
    },
    test("omits optional fields when None") {
      withConditions(minimalConditions)
        .map { result =>
          assertTrue(
            !result.contains("relative_humidity"),
            !result.contains("cloud_cover"),
            !result.contains("uv_index"),
            !result.contains("uv_index_text"),
            !result.contains("wind_speed_metric_value"),
            !result.contains("wind_direction"),
            !result.contains("visibility_metric_value"),
            !result.contains("real_feel_metric_value"),
          )
        }
        .provide(testLayer)
    },
    test("extracts humidity and cloud cover") {
      withConditions(fullConditions)
        .map { result =>
          assertTrue(
            result("relative_humidity") == 65,
            result("cloud_cover") == 40,
          )
        }
        .provide(testLayer)
    },
    test("extracts UV index") {
      withConditions(fullConditions)
        .map { result =>
          assertTrue(
            result("uv_index") == 5,
            result("uv_index_text") == "Moderate",
          )
        }
        .provide(testLayer)
    },
    test("extracts wind properties") {
      withConditions(fullConditions)
        .map { result =>
          assertTrue(
            result("wind_speed_metric_value") == 15.0,
            result("wind_speed_metric_unit") == "km/h",
            result("wind_speed_imperial_value") == 9.3,
            result("wind_speed_imperial_unit") == "mi/h",
            result("wind_direction") == "S",
            result("wind_direction_degrees") == 180,
          )
        }
        .provide(testLayer)
    },
    test("extracts visibility properties") {
      withConditions(fullConditions)
        .map { result =>
          assertTrue(
            result("visibility_metric_value") == 10.0,
            result("visibility_metric_unit") == "km",
            result("visibility_imperial_value") == 6.2,
            result("visibility_imperial_unit") == "mi",
          )
        }
        .provide(testLayer)
    },
    test("extracts real feel temperature") {
      withConditions(fullConditions)
        .map { result =>
          assertTrue(
            result("real_feel_metric_value") == 20.0,
            result("real_feel_metric_unit") == "C",
            result("real_feel_imperial_value") == 68.0,
            result("real_feel_imperial_unit") == "F",
          )
        }
        .provide(testLayer)
    },
  )
