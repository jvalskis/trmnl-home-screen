package is.valsk.trmnlhomescreen.accuweather

import is.valsk.trmnlhomescreen.accuweather.AccuWeatherModel.CurrentConditions
import zio.{Ref, UIO, ULayer, ZLayer}

trait AccuWeatherStateRepository:
  def update(conditions: CurrentConditions): UIO[Unit]
  def get: UIO[Option[CurrentConditions]]

object AccuWeatherStateRepository:

  private class LiveAccuWeatherStateRepository(ref: Ref[Option[CurrentConditions]]) extends AccuWeatherStateRepository:
    def update(conditions: CurrentConditions): UIO[Unit] = ref.set(Some(conditions))

    def get: UIO[Option[CurrentConditions]] = ref.get

  val layer: ULayer[AccuWeatherStateRepository] = ZLayer {
    Ref.make(Option.empty[CurrentConditions]).map(LiveAccuWeatherStateRepository(_))
  }
