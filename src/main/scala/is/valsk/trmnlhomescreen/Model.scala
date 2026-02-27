package is.valsk.trmnlhomescreen

import zio.json.*

final case class Country(
    @jsonField("LocalizedName")
    localizedName: String,
)

object Country:
  given JsonDecoder[Country] = DeriveJsonDecoder.gen[Country]

final case class Location(
    @jsonField("Key")
    key: String,
    @jsonField("LocalizedName")
    localizedName: String,
    @jsonField("Country")
    country: Country,
)

object Location:
  given JsonDecoder[Location] = DeriveJsonDecoder.gen[Location]

final case class TemperatureValue(
    @jsonField("Value")
    value: Double,
    @jsonField("Unit")
    unit: String,
)

object TemperatureValue:
  given JsonDecoder[TemperatureValue] = DeriveJsonDecoder.gen[TemperatureValue]

final case class Temperature(
    @jsonField("Metric")
    metric: TemperatureValue,
    @jsonField("Imperial")
    imperial: TemperatureValue,
)

object Temperature:
  given JsonDecoder[Temperature] = DeriveJsonDecoder.gen[Temperature]

final case class WindDirection(
    @jsonField("Degrees")
    degrees: Double,
    @jsonField("Localized")
    localized: String,
)

object WindDirection:
  given JsonDecoder[WindDirection] = DeriveJsonDecoder.gen[WindDirection]

final case class Wind(
    @jsonField("Direction")
    direction: WindDirection,
    @jsonField("Speed")
    speed: Temperature,
)

object Wind:
  given JsonDecoder[Wind] = DeriveJsonDecoder.gen[Wind]

final case class CurrentConditions(
    @jsonField("WeatherText")
    weatherText: String,
    @jsonField("WeatherIcon")
    weatherIcon: Int,
    @jsonField("Temperature")
    temperature: Temperature,
    @jsonField("RealFeelTemperature")
    realFeelTemperature: Option[Temperature],
    @jsonField("HasPrecipitation")
    hasPrecipitation: Boolean,
    @jsonField("IsDayTime")
    isDayTime: Boolean,
    @jsonField("RelativeHumidity")
    relativeHumidity: Option[Int],
    @jsonField("Wind")
    wind: Option[Wind],
    @jsonField("UVIndex")
    uvIndex: Option[Int],
    @jsonField("UVIndexText")
    uvIndexText: Option[String],
    @jsonField("Visibility")
    visibility: Option[Temperature],
    @jsonField("CloudCover")
    cloudCover: Option[Int],
    @jsonField("LocalObservationDateTime")
    localObservationDateTime: String,
)

object CurrentConditions:
  given JsonDecoder[CurrentConditions] = DeriveJsonDecoder.gen[CurrentConditions]
