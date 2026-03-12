package is.valsk.trmnlhomescreen.util

import is.valsk.trmnlhomescreen.util.ApiClient.{ApiError, RequestMiddleware}
import is.valsk.trmnlhomescreen.util.ResponseDecoder.given
import zio.{IO, ZIO, ZLayer}
import zio.http.{Body, Client, Method, Request, URL}
import zio.json.{DecoderOps, JsonDecoder}

final case class Endpoint[I, O](
    method: Method,
    path: I => String,
)

trait ResponseDecoder[O]:
  def decode(body: String): Either[String, O]

object ResponseDecoder:
  given fromJson[O: JsonDecoder]: ResponseDecoder[O] =
    body => body.fromJson[O]

  given ResponseDecoder[String] =
    body => Right(body)

trait ApiClient {

  def call[I, O: ResponseDecoder](
      baseUrl: URL,
      endpoint: Endpoint[I, O],
      input: I,
      middlewares: List[RequestMiddleware] = Nil,
      body: Body = Body.empty,
  ): IO[ApiError, O]

}

object ApiClient {
  sealed trait ApiError extends Throwable

  object ApiError {
    case class Http(code: Int, body: String) extends ApiError

    case class Network(cause: Throwable) extends ApiError

    case class Decode(message: String) extends ApiError
  }

  type RequestMiddleware = Request => Request

  private class ApiClientLive(client: Client) extends ApiClient {

    override def call[I, O: ResponseDecoder](
        baseUrl: URL,
        endpoint: Endpoint[I, O],
        input: I,
        middlewares: List[RequestMiddleware] = Nil,
        body: Body = Body.empty,
    ): IO[ApiError, O] =
      val finalRequest = applyMiddlewares(
        Request(
          method = endpoint.method,
          url = baseUrl.addPath(endpoint.path(input)),
          body = body,
        ),
        middlewares,
      )
      for {
        response <- ZIO.scoped(
          client
            .request(finalRequest)
            .mapError(ApiError.Network(_)),
        )
        responseBody <- response.body.asString
          .mapError(ApiError.Network(_))
        _ <- ZIO
          .fail(ApiError.Http(response.status.code, responseBody))
          .unless(response.status.isSuccess)
        decoded <- ZIO
          .fromEither(summon[ResponseDecoder[O]].decode(responseBody))
          .mapError(ApiError.Decode(_))
      } yield decoded

    private def applyMiddlewares(request: Request, middlewares: List[RequestMiddleware] = Nil): Request =
      middlewares.foldLeft(request)((r, mw) => mw(r))

  }

  val layer: ZLayer[Client, Nothing, ApiClient] = ZLayer.fromFunction(ApiClientLive(_))

}
