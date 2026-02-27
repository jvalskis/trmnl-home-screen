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

final case class Value(
    @jsonField("Value")
    value: Double,
    @jsonField("Unit")
    unit: String,
)

object Value:
  given JsonDecoder[Value] = DeriveJsonDecoder.gen[Value]

final case class ValueContainer(
    @jsonField("Metric")
    metric: Value,
    @jsonField("Imperial")
    imperial: Value,
)

object ValueContainer:
  given JsonDecoder[ValueContainer] = DeriveJsonDecoder.gen[ValueContainer]

case class Direction(
    @jsonField("Degrees")
    degrees: Int,
    @jsonField("Localized")
    localized: String,
    @jsonField("English")
    english: String,
)

object Direction:
  given JsonDecoder[Direction] = DeriveJsonDecoder.gen[Direction]

final case class Wind(
    @jsonField("Direction")
    direction: Direction,
    @jsonField("Speed")
    speed: ValueContainer,
)

object Wind:
  given JsonDecoder[Wind] = DeriveJsonDecoder.gen[Wind]

final case class CurrentConditions(
    @jsonField("WeatherText")
    weatherText: String,
    @jsonField("WeatherIcon")
    weatherIcon: Int,
    @jsonField("Temperature")
    temperature: ValueContainer,
    @jsonField("RealFeelTemperature")
    realFeelTemperature: Option[Temperature],
    @jsonField("HasPrecipitation")
    hasPrecipitation: Boolean,
    @jsonField("PrecipitationType")
    precipitationType: Option[String],
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
    @jsonField("RealFeelTemperature")
    realFeelTemperature: ValueContainer,
    @jsonField("RelativeHumidity")
    relativeHumidity: Int,
    @jsonField("Wind")
    wind: Wind,
    @jsonField("UVIndex")
    uvIndex: Int,
)

object CurrentConditions:
  given JsonDecoder[CurrentConditions] = DeriveJsonDecoder.gen[CurrentConditions]
