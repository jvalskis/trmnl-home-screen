package is.valsk.trmnlhomescreen.trmnl

import zio.*
import zio.http.*
import zio.json.*

trait TrmnlClient:
  def pushScreen(markup: String): Task[Unit]

object TrmnlClient:

  def pushScreen(markup: String): ZIO[TrmnlClient, Throwable, Unit] =
    ZIO.serviceWithZIO[TrmnlClient](_.pushScreen(markup))

  val layer: ZLayer[Client & TrmnlConfig, Nothing, TrmnlClient] =
    ZLayer.fromFunction(LiveTrmnlClient.apply)

  val configuredLayer: ZLayer[Client, Config.Error, TrmnlClient] = TrmnlConfig.layer >>> layer

  private case class ImageContent(content: String)
  private object ImageContent:
    given JsonEncoder[ImageContent] = DeriveJsonEncoder.gen[ImageContent]

  private case class ScreenRequest(image: ImageContent)
  private object ScreenRequest:
    given JsonEncoder[ScreenRequest] = DeriveJsonEncoder.gen[ScreenRequest]

  private final case class LiveTrmnlClient(
      client: Client,
      config: TrmnlConfig,
  ) extends TrmnlClient:

    def pushScreen(markup: String): Task[Unit] =
      val urlStr = s"${config.baseUrl}/api/screens"
      val payload = ScreenRequest(ImageContent(markup))
      for
        url <- ZIO.fromEither(URL.decode(urlStr))
          .mapError(e => RuntimeException(s"Invalid URL: $urlStr"))
        request = Request
          .post(url, Body.fromString(payload.toJson))
          .addHeader(Header.Custom("access-token", config.apiKey))
          .addHeader(Header.Custom("id", config.macAddress))
          .addHeader(Header.ContentType(MediaType.application.json))
        response <- ZIO.scoped(client.request(request))
        status = response.status
        _ <- if status.isSuccess then ZIO.logInfo("Successfully pushed screen to TRMNL")
             else response.body.asString.flatMap(body =>
               ZIO.logError(s"Failed to push screen to TRMNL: HTTP $status - $body") *>
                 ZIO.fail(RuntimeException(s"TRMNL API returned HTTP $status"))
             )
      yield ()
