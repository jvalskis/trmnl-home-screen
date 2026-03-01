package is.valsk.trmnlhomescreen

import zio.UIO

trait PropertiesExtractor:
  def extract: UIO[Map[String, Any]]
