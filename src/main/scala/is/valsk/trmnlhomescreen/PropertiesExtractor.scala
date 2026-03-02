package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.PropertiesExtractor.{MapProperty, PropertyEntry}
import zio.UIO

import scala.jdk.CollectionConverters.*

trait PropertiesExtractor:
  def extract: UIO[MapProperty]

object PropertiesExtractor {

  trait PropertyEntry {
    def asJava: Any
  }

  case class ScalarProperty(value: Any) extends PropertyEntry {
    override def asJava: Any = value match {
      case x: PropertyEntry => x.asJava
      case v => v
    }
  }

  case class ListProperty(value: Seq[PropertyEntry]) extends PropertyEntry {
    override def asJava: java.util.List[Any] = value.map(_.asJava).asJava
  }

  case class MapProperty(value: Map[String, PropertyEntry]) extends PropertyEntry {
    override def asJava: java.util.Map[String, Any] = value.map(_ -> _.asJava).asJava

    def +(pair: (String, PropertyEntry)): MapProperty = {
      new MapProperty(value + pair)
    }

    def ++(other: MapProperty): MapProperty = {
      new MapProperty(value ++ other.value)
    }

  }

  object MapProperty {

    def apply(pairs: (String, PropertyEntry)*): MapProperty = {
      new MapProperty(pairs.toMap)
    }

  }

  extension (any: Any) {
    def asScalar: ScalarProperty = ScalarProperty(any)
  }

  extension (any: Seq[PropertyEntry]) {

    def asList: ListProperty = ListProperty(any)
  }

  extension (any: Map[String, PropertyEntry]) {

    def asMap: MapProperty = MapProperty(any)
  }

}
