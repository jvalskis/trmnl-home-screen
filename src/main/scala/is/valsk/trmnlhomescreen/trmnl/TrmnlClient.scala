package is.valsk.trmnlhomescreen.trmnl

import is.valsk.trmnlhomescreen.trmnl.TrmnlClient.Api.UpdateDisplayEndpoint
import is.valsk.trmnlhomescreen.util.ApiClient.RequestMiddleware
import is.valsk.trmnlhomescreen.util.{ApiClient, Endpoint}
import zio.*
import zio.http.*
import zio.json.*

trait TrmnlClient:
  def pushScreen(markup: String): Task[Unit]

object TrmnlClient:

  def pushScreen(markup: String): ZIO[TrmnlClient, Throwable, Unit] =
    ZIO.serviceWithZIO[TrmnlClient](_.pushScreen(markup))

  val layer: ZLayer[ApiClient & TrmnlConfig, Nothing, TrmnlClient] =
    ZLayer.fromFunction(LiveTrmnlClient.apply)

  val configuredLayer: ZLayer[ApiClient, Config.Error, TrmnlClient] = TrmnlConfig.layer >>> layer

  private case class ScreenRequest(markup: String)

  private object ScreenRequest:
    given JsonEncoder[ScreenRequest] = DeriveJsonEncoder.gen[ScreenRequest]

  object Api {

    val UpdateDisplayEndpoint = Endpoint[String, String](
      Method.POST,
      _ => "/api/display/update",
      deviceId => Map("device_id" -> deviceId),
    )

  }

  private final case class LiveTrmnlClient(
      client: ApiClient,
      config: TrmnlConfig,
  ) extends TrmnlClient:

    def pushScreen(markup: String): Task[Unit] =
      val baseUrl = URL.decode(config.baseUrl).toOption.get
      for
        _ <- ZIO.logDebug(s"Pushing screen to TRMNL: ${config.baseUrl}")
        _ <- client.call(
          baseUrl,
          UpdateDisplayEndpoint,
          config.deviceId,
          middlewares,
          body = ScreenRequest(markup),
        )
        _ <- ZIO.logInfo("Successfully pushed screen to TRMNL")
      yield ()

    private val middlewares: List[RequestMiddleware] = List(
      _.addHeader(Header.Authorization.Bearer(config.token)),
      _.addHeader(Header.ContentType(MediaType.application.json)),
      _.addHeader(Header.Accept(MediaType.application.json)),
    )
