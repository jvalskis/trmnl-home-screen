package is.valsk.trmnlhomescreen.calendar

import biweekly.Biweekly
import biweekly.component.VEvent
import biweekly.util.ICalDate
import zio.*
import zio.http.*
import zio.http.Status.MultiStatus

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import scala.jdk.CollectionConverters.*
import scala.xml.XML

trait CalDavClient:
  def fetchEvents(): Task[List[CalendarEvent]]

object CalDavClient:

  val layer: ZLayer[Client & CalendarConfig, Nothing, CalDavClient] =
    ZLayer.fromFunction(LiveCalDavClient.apply)

  private final case class LiveCalDavClient(client: Client, config: CalendarConfig) extends CalDavClient:

    def fetchEvents(): Task[List[CalendarEvent]] = {
      val now = ZonedDateTime.now(ZoneOffset.UTC)
      val end = now.plusDays(config.daysAhead)

      for
        response <- sendReport(filterByTime(now, end))
        events <- parseResponse(response)
      yield events.sortBy(_.dtStart)
    }

    private def sendReport(xmlBody: String): Task[String] =
      val urlStr = config.calendarUrl
      for
        url <- ZIO.fromEither(URL.decode(urlStr))
          .mapError(e => RuntimeException(s"Invalid URL: $urlStr"))
        response <- ZIO.scoped {
          for
            resp <- client.request(createRequest(xmlBody, url))
            body <- resp.body.asString
            _ <- ZIO.logInfo(s"CalDAV response status: ${resp.status.code}, headers: ${resp.headers.toList.map(h =>
                s"${h.headerName}: ${h.renderedValue}",
              ).mkString(", ")}")
            _ <- ZIO.when(resp.status.code != MultiStatus.code) {
              ZIO.fail(RuntimeException(s"CalDAV REPORT failed with status ${resp.status.code}: $body"))
            }
          yield body
        }
      yield response

    private def createRequest(xmlBody: String, url: URL) = {
      Request(
        method = Method.CUSTOM("REPORT"),
        url = url,
        headers = Headers(
          Header.ContentType(MediaType.application.xml),
          resolveAuthHeader(config),
          Header.Custom("Depth", "1"),
        ),
        body = Body.fromString(xmlBody),
      )
    }

    private def parseResponse(xmlResponse: String): Task[List[CalendarEvent]] =
      ZIO.attempt {
        (XML.loadString(xmlResponse) \\ "calendar-data")
          .map(_.text.trim)
          .filter(_.nonEmpty)
          .flatMap(text => Option(Biweekly.parse(text).first()))
          .flatMap(_.getEvents.asScala.toList)
          .flatMap(createCalendarEvent).toList
      }

  private def resolveAuthHeader(config: CalendarConfig) = {
    config.authType.toLowerCase match
      case "bearer" => Header.Authorization.Bearer(config.password)
      case _ => Header.Authorization.Basic(config.username, config.password)
  }

  def createCalendarEvent(event: VEvent) = {
    for start <- Option(event.getDateStart).map(_.getValue)
    yield CalendarEvent(
      summary = Option(event.getSummary).map(_.getValue).getOrElse(CalendarEvent.DefaultSummary),
      dtStart = start.toLocalDateTime,
      dtEnd = Option(event.getDateEnd).map(_.getValue).map(_.toLocalDateTime),
      location = Option(event.getLocation).map(_.getValue).filter(_.nonEmpty),
      description = Option(event.getDescription).map(_.getValue).filter(_.nonEmpty),
    )
  }

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
