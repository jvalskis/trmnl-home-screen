package is.valsk.trmnlhomescreen

import zio.config.magnolia.{DeriveConfig, deriveConfig}
import zio.config.typesafe.TypesafeConfigProvider
import zio.{Config, Layer, Tag, ZIO, ZLayer}

object Configs {

  def makeLayer[C](path: String, other: String*)(using dc: DeriveConfig[C], tag: Tag[C]): Layer[Config.Error, C] = {
    ZLayer.fromZIO(TypesafeConfigProvider.fromResourcePath().load(deriveConfig[C].nested(path, other*)))
  }
}