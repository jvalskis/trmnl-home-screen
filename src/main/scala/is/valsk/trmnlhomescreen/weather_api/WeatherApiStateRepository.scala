package is.valsk.trmnlhomescreen.weather_api

import is.valsk.trmnlhomescreen.weather_api.WeatherApiModel.{Current, ForecastDay}
import zio.{Ref, UIO, ULayer, ZLayer}

trait WeatherApiStateRepository:
  def update(current: Current): UIO[Unit]
  def get: UIO[Option[Current]]
  def updateForecast(forecast: List[ForecastDay]): UIO[Unit]
  def getForecast: UIO[List[ForecastDay]]

object WeatherApiStateRepository:

  private class LiveWeatherApiStateRepository(
      currentRef: Ref[Option[Current]],
      forecastRef: Ref[List[ForecastDay]],
  ) extends WeatherApiStateRepository:
    def update(current: Current): UIO[Unit] = currentRef.set(Some(current))
    def get: UIO[Option[Current]] = currentRef.get
    def updateForecast(forecast: List[ForecastDay]): UIO[Unit] = forecastRef.set(forecast)
    def getForecast: UIO[List[ForecastDay]] = forecastRef.get

  val layer: ULayer[WeatherApiStateRepository] = ZLayer {
    for
      currentRef <- Ref.make(Option.empty[Current])
      forecastRef <- Ref.make(List.empty[ForecastDay])
    yield LiveWeatherApiStateRepository(currentRef, forecastRef)
  }
