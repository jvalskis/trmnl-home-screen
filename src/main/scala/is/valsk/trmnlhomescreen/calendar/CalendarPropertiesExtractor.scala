package is.valsk.trmnlhomescreen.calendar

import is.valsk.trmnlhomescreen.PropertiesExtractor
import zio.{RLayer, UIO, URLayer, ZIO, ZLayer}

import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*

object CalendarPropertiesExtractor:

  private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  val layer: URLayer[CalendarConfig & CalendarStateRepository, PropertiesExtractor] = ZLayer {
    for {
      repository <- ZIO.service[CalendarStateRepository]
      config <- ZIO.service[CalendarConfig]
    } yield new PropertiesExtractor:
      def extract: UIO[Map[String, Any]] = {
        repository.get.map { events =>
          Seq(
            "calendar_enabled" -> config.enabled,
            "events" -> events.map { event =>
              Map[String, Any](
                "summary" -> event.summary,
                "start" -> event.dtStart.format(displayFormatter),
                "end" -> event.dtEnd.fold("")(_.format(displayFormatter)),
                "location" -> event.location.getOrElse(""),
                "description" -> event.description.getOrElse(""),
              ).asJava
            }.asJava,
          ).toMap
        }

      }
  }

  val configuredLayer: RLayer[CalendarStateRepository, PropertiesExtractor] = CalendarConfig.layer >>> layer
