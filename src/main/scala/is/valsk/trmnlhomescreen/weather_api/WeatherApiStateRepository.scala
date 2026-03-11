package is.valsk.trmnlhomescreen.weather_api

import is.valsk.trmnlhomescreen.weather_api.WeatherApiModel.Current
import zio.{Ref, UIO, ULayer, ZLayer}

trait WeatherApiStateRepository:
  def update(current: Current): UIO[Unit]
  def get: UIO[Option[Current]]

object WeatherApiStateRepository:

  private class LiveWeatherApiStateRepository(ref: Ref[Option[Current]]) extends WeatherApiStateRepository:
    def update(current: Current): UIO[Unit] = ref.set(Some(current))

    def get: UIO[Option[Current]] = ref.get

  val layer: ULayer[WeatherApiStateRepository] = ZLayer {
    Ref.make(Option.empty[Current]).map(LiveWeatherApiStateRepository(_))
  }
