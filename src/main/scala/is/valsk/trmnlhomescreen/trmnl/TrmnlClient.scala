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

  private case class ScreenRequest(markup: String)
  private object ScreenRequest:
    given JsonEncoder[ScreenRequest] = DeriveJsonEncoder.gen[ScreenRequest]

  private final case class LiveTrmnlClient(
      client: Client,
      config: TrmnlConfig,
  ) extends TrmnlClient:

    def pushScreen(markup: String): Task[Unit] =
      val urlStr = s"${config.baseUrl}/api/display/update?device_id=${config.deviceId}"
      for
        url <- ZIO.fromEither(URL.decode(urlStr))
          .mapError(e => RuntimeException(s"Invalid URL: $urlStr"))
        request = Request(
          method = Method.POST,
          url = url,
          headers = Headers(
            Header.Authorization.Bearer(config.token),
            Header.ContentType(MediaType.application.json)
          ),
          body = Body.fromString(ScreenRequest(markup).toJson)
        )


        response <- ZIO.scoped(client.request(request))
        status = response.status
        _ <- if status.isSuccess then ZIO.logInfo("Successfully pushed screen to TRMNL")
             else response.body.asString.flatMap(body =>
               ZIO.logError(s"Failed to push screen to TRMNL: HTTP $status - $body") *>
                 ZIO.fail(RuntimeException(s"TRMNL API returned HTTP $status"))
             )
      yield ()
