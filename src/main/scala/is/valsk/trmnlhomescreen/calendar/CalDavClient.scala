package is.valsk.trmnlhomescreen.calendar

import biweekly.Biweekly
import biweekly.component.VEvent
import biweekly.util.ICalDate
import is.valsk.trmnlhomescreen.calendar.CalDavClient.Api.ReportEndpoint
import is.valsk.trmnlhomescreen.util.ApiClient.RequestMiddleware
import is.valsk.trmnlhomescreen.util.{ApiClient, Endpoint}
import zio.*
import zio.http.*

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import scala.jdk.CollectionConverters.*
import scala.xml.XML

trait CalDavClient:
  def fetchEvents(): Task[List[CalendarEvent]]

object CalDavClient:

  val layer: ZLayer[ApiClient & CalendarConfig, Nothing, CalDavClient] =
    ZLayer.fromFunction(LiveCalDavClient.apply)

  val configuredLayer: ZLayer[ApiClient, Config.Error, CalDavClient] = CalendarConfig.layer >>> layer

  object Api {
    val ReportEndpoint = Endpoint[Unit, String](
      Method.CUSTOM("REPORT"),
      _ => "",
    )
  }

  private final case class LiveCalDavClient(client: ApiClient, config: CalendarConfig) extends CalDavClient:

    private val baseUrl: URL = URL.decode(config.calendarUrl).toOption.get

    def fetchEvents(): Task[List[CalendarEvent]] =
      val now = ZonedDateTime.now(ZoneOffset.UTC)
      val end = now.plusDays(config.daysAhead)

      for {
        response <- client.call(
          baseUrl, ReportEndpoint, (), middlewares,
          body = Body.fromString(filterByTime(now, end)),
        )
        events <- parseResponse(response)
      } yield events.sortBy(_.startDate)

    private val middlewares: List[RequestMiddleware] = List(
      _.addHeader(Header.ContentType(MediaType.application.xml)),
      _.addHeader(resolveAuthHeader(config)),
      _.addHeader(Header.Custom("Depth", "1")),
    )

    private def parseResponse(xmlResponse: String): Task[List[CalendarEvent]] =
      ZIO.attempt {
        (XML.loadString(xmlResponse) \\ "calendar-data")
          .map(_.text.trim)
          .filter(_.nonEmpty)
          .flatMap(text => Option(Biweekly.parse(text).first()))
          .flatMap(_.getEvents.asScala.toList)
          .flatMap(createCalendarEvent).toList
      }

  private def resolveAuthHeader(config: CalendarConfig) =
    config.authType.toLowerCase match
      case "bearer" => Header.Authorization.Bearer(config.password)
      case _ => Header.Authorization.Basic(config.username, config.password)

  private def createCalendarEvent(event: VEvent) =
    for start <- Option(event.getDateStart).map(_.getValue)
    yield CalendarEvent(
      summary = Option(event.getSummary).map(_.getValue).getOrElse(CalendarEvent.DefaultSummary),
      startDate = start.toLocalDateTime,
      endDate = Option(event.getDateEnd).map(_.getValue).map(_.toLocalDateTime),
      location = Option(event.getLocation).map(_.getValue).filter(_.nonEmpty),
      description = Option(event.getDescription).map(_.getValue).filter(_.nonEmpty),
    )

  private val calDavFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

  private def filterByTime(start: ZonedDateTime, end: ZonedDateTime) =
    s"""<?xml version="1.0" encoding="UTF-8"?>
      |<C:calendar-query xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
      |  <D:prop>
      |    <D:getetag/>
      |    <C:calendar-data/>
      |  </D:prop>
      |  <C:filter>
      |    <C:comp-filter name="VCALENDAR">
      |      <C:comp-filter name="VEVENT">
      |        <C:time-range start="${start.format(calDavFormatter)}" end="${end.format(calDavFormatter)}"/>
      |      </C:comp-filter>
      |    </C:comp-filter>
      |  </C:filter>
      |</C:calendar-query>""".stripMargin

  extension (date: ICalDate) {
    def toLocalDateTime: LocalDateTime = LocalDateTime.ofInstant(date.toInstant, ZoneOffset.UTC)
  }
