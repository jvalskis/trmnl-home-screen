package is.valsk.trmnlhomescreen

import zio.{UIO, URLayer, ZIO, ZLayer}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object GeneralPropertiesExtractor:

  val layer: URLayer[Any, PropertiesExtractor] = ZLayer.succeed {
    new PropertiesExtractor:
      def extract: UIO[Map[String, Any]] = ZIO.succeed {
        val today = LocalDate.now()
        Map(
          "today_date" -> today.format(DateTimeFormatter.ISO_LOCAL_DATE),
          "today_day_of_week" -> today.getDayOfWeek.toString.toLowerCase.capitalize,
          "today_day_of_month" -> today.getDayOfMonth,
          "today_month" -> today.getMonth.toString.toLowerCase.capitalize,
          "today_year" -> today.getYear,
        )
      }
  }
