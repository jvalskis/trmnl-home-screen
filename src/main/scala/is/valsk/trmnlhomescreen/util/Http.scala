package is.valsk.trmnlhomescreen.util

import is.valsk.trmnlhomescreen.util.ApiClient.{ApiError, RequestMiddleware}
import zio.{IO, ZIO, ZLayer}
import zio.http.{Body, Client, Method, Request, URL}
import zio.json.{DecoderOps, EncoderOps, JsonDecoder, JsonEncoder}

final case class Endpoint[I, O](
    method: Method,
    path: I => String,
    queryParams: I => Map[String, String] = (_: I) => Map.empty,
)

trait ResponseDecoder[O]:
  def decode(body: String): Either[String, O]

object ResponseDecoder:
  given fromJson[O: JsonDecoder]: ResponseDecoder[O] = body => body.fromJson[O]

  given ResponseDecoder[String] = body => Right(body)

trait RequestEncoder[B]:
  def encode(value: B): Body

object RequestEncoder:
  given RequestEncoder[Unit] = _ => Body.empty

  given RequestEncoder[String] = value => Body.fromString(value)

  given fromJson[B: JsonEncoder]: RequestEncoder[B] = value => Body.fromString(value.toJson)

trait ApiClient {

  def call[I, O: ResponseDecoder, B: RequestEncoder](
      baseUrl: URL,
      endpoint: Endpoint[I, O],
      input: I,
      middlewares: List[RequestMiddleware] = Nil,
      body: B = (),
  ): IO[ApiError, O]

}

object ApiClient {
  sealed trait ApiError extends Throwable

  object ApiError {

    case class Http(code: Int, body: String) extends ApiError:
      override def getMessage: String = s"HTTP $code: $body"

    case class Network(cause: Throwable) extends ApiError:
      override def getMessage: String = s"Network error: ${cause.getMessage}"
      override def getCause: Throwable = cause

    case class Decode(message: String) extends ApiError:
      override def getMessage: String = s"Decode error: $message"

  }

  type RequestMiddleware = Request => Request

  private class ApiClientLive(client: Client) extends ApiClient {

    override def call[I, O: ResponseDecoder, B: RequestEncoder](
        baseUrl: URL,
        endpoint: Endpoint[I, O],
        input: I,
        middlewares: List[RequestMiddleware] = Nil,
        body: B = (),
    ): IO[ApiError, O] =
      val request = buildRequest(baseUrl, endpoint, input, middlewares, body)
      (for {
        responseBody <- ZIO.scoped(
          for {
            response <- client
              .request(request)
              .mapError(ApiError.Network(_))
            body <- response.body.asString
              .mapError(ApiError.Network(_))
            _ <- ZIO
              .fail(ApiError.Http(response.status.code, body))
              .unless(response.status.isSuccess)
          } yield body,
        )
        decoded <- ZIO
          .fromEither(summon[ResponseDecoder[O]].decode(responseBody))
          .mapError(ApiError.Decode(_))
      } yield decoded).tapError(e => ZIO.logError(s"Request failed [${endpoint.method} ${request.url}]: ${e.getMessage}"))

    private def buildRequest[B: RequestEncoder, O: ResponseDecoder, I](
        baseUrl: URL,
        endpoint: Endpoint[I, O],
        input: I,
        middlewares: List[RequestMiddleware],
        body: B,
    ) = {
      val url = endpoint.queryParams(input).foldLeft(baseUrl.addPath(endpoint.path(input))) {
        case (accUrl, (queryKey, queryValue)) => accUrl.addQueryParam(queryKey, queryValue)
      }
      val finalRequest = applyMiddlewares(
        Request(
          method = endpoint.method,
          url = url,
          body = summon[RequestEncoder[B]].encode(body),
        ),
        middlewares,
      )
      finalRequest
    }

    private def applyMiddlewares(request: Request, middlewares: List[RequestMiddleware] = Nil): Request =
      middlewares.foldLeft(request)((r, mw) => mw(r))

  }

  val layer: ZLayer[Client, Nothing, ApiClient] = ZLayer.fromFunction(ApiClientLive(_))

}
