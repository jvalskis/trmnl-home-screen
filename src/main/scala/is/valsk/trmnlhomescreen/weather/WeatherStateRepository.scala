package is.valsk.trmnlhomescreen.weather

import zio.{Ref, UIO, ULayer, ZLayer}

trait WeatherStateRepository:
  def update(conditions: CurrentConditions): UIO[Unit]
  def get: UIO[Option[CurrentConditions]]

object WeatherStateRepository:

  private class LiveWeatherStateRepository(ref: Ref[Option[CurrentConditions]]) extends WeatherStateRepository:
    def update(conditions: CurrentConditions): UIO[Unit] = ref.set(Some(conditions))

    def get: UIO[Option[CurrentConditions]] = ref.get

  val layer: ULayer[WeatherStateRepository] = ZLayer {
    Ref.make(Option.empty[CurrentConditions]).map(LiveWeatherStateRepository(_))
  }
