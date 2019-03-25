package io.litego.api

import java.time.Instant

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.ws.{PeerClosedConnectionException, TextMessage, WebSocketRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.{Done, NotUsed}
import com.typesafe.scalalogging.Logger
import de.knutwalker.akka.http.support.CirceHttpSupport._
import de.knutwalker.akka.stream.support.CirceStreamSupport
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Errors => _, _}
import io.litego.api.v1.Errors.{ApiError, ErrorResponse, ServerError, UnhandledServerError}

import scala.concurrent.{ExecutionContext, Future}
import scala.util._

package object v1 {

  object defaults {

    implicit val decodeInstant: Decoder[Instant] = Decoder[String].map { timestamp =>
      Instant.parse(timestamp)
    }

    implicit val encodeInstant: Encoder[Instant] = Encoder.instance[Instant] { instant =>
      instant.toString.asJson
    }

  }

  /**
    * A helper function which creates a GET request through akka-http
    *
    * @param finalUrl           The URL for the request
    * @param getQueryParameters The GET query parameters
    * @param logger             The logger to use, should the logger for the model for
    *                           easy debugging
    * @tparam M The model which this request should return
    * @return
    */
  private[v1] def createRequestGET[M](finalUrl: Uri,
                                      getQueryParameters: Map[String, String],
                                      logger: Logger)(
      implicit client: HttpExt,
      materializer: Materializer,
      executionContext: ExecutionContext,
      decoder: Decoder[M],
      token: Option[Token] = None
  ): Future[Try[M]] = {

    val uri     = finalUrl.withQuery(Query(getQueryParameters))
    val headers = buildHeaders(token)
    val req =
      HttpRequest(uri = uri, method = HttpMethods.GET, headers = headers)

    for {
      response <- client.singleRequest(req)
      _ = logger.debug(s"Received response $response")
      parsed <- parseServerError[M](response, finalUrl, Some(getQueryParameters), None, logger)
      result = parsed match {
        case Right(triedValue) =>
          util.Success(triedValue.get)
        case Left(error) =>
          util.Failure(error)
      }
    } yield result
  }

  /**
    * A helper function which creates a POST request through akka-http
    *
    * @param finalUrl           The URL for the request
    * @param postJsonParameters The POST json parameters
    * @param logger             The logger to use, should the logger for the model for
    *                           easy debugging
    * @tparam M The model which this request should return
    * @return
    */
  private[v1] def createRequestPOST[M](finalUrl: String, postJsonParameters: Json, logger: Logger)(
      implicit client: HttpExt,
      materializer: Materializer,
      executionContext: ExecutionContext,
      decoder: Decoder[M],
      token: Option[Token] = None
  ): Future[Try[M]] = {

    val headers = buildHeaders(token)

    val req = HttpRequest(
      uri = finalUrl,
      entity = HttpEntity(ContentTypes.`application/json`, postJsonParameters.noSpaces),
      method = HttpMethods.POST,
      headers = headers
    )

    for {
      response <- client.singleRequest(req)
      _ = logger.debug(s"Received response $response")
      parsed <- parseServerError[M](response, finalUrl, None, Some(postJsonParameters), logger)
      result = parsed match {
        case Right(triedValue) =>
          util.Success(triedValue.get)
        case Left(error) =>
          util.Failure(error)
      }

    } yield result
  }

  /**
    * A helper function which creates websocket request through akka-http
    *
    * @param finalUrl The URL for the request
    * @param incoming The incoming flow to receive messages from websocket
    * @param logger   The logger to use, should the logger for the model for
    *                 easy debugging
    * @tparam M The model which this request should return
    * @return tuple of (upgradeResponse, closed)
    *         upgradeResponse is a Future[WebSocketUpgradeResponse] that
    *         completes or fails when the connection succeeds or fails
    *         and closed is a Future[Done] representing the stream completion from above
    */
  def createWebsocketRequest[M](
    finalUrl: String,
    incoming: Flow[M, Done, NotUsed],
    sink: Sink[Done, Future[Done]],
    logger: Logger
  )(implicit client: HttpExt,
    mat: Materializer,
    token: Option[Token] = None,
    decoder: Decoder[M],
    ec: ExecutionContext): Future[Done] = {

    val webSocketFlow = client.webSocketClientFlow(WebSocketRequest(finalUrl, buildHeaders(token)))

    def parseMessage(message: String): M =
      parse(message)
        .flatMap(_.as[M])
        .fold(
          err => {
            logger.error(s"Parsing message error $err")
            throw err
          },
          identity
        )

    val (upgradeResponse, closed) = Source.repeat(TextMessage(""))
      .viaMat(webSocketFlow)(Keep.right)
      .collect {
        case TextMessage.Strict(message) =>
          logger.debug(s"Received websocket message $message")
          parseMessage(message)
      }
      .viaMat(incoming)(Keep.left)
      .toMat(sink)(Keep.both)
      .run()

    val connected = upgradeResponse.map { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Done
      } else {
        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }
    }

    connected.onComplete {
      case Success(c) =>
        logger.debug(s"Connected: $c")
      case Failure(e) =>
        logger.error(s"${e.getMessage}")
        throw e
    }

    closed.transformWith {
      case Success(value) =>
        logger.debug("Connection closed gracefully")
        Future.successful(value)
      case Failure(e: PeerClosedConnectionException) =>
        logger.error(s"Connection closed with an error: code: ${e.closeCode}, reason: ${e.closeReason}")
        Future.failed(e)
      case Failure(e) =>
        logger.error(s"Connection closed with an error: ${e.getMessage}")
        Future.failed(e)
    }
  }

  /**
    * A helper function which creates a PUT request through akka-http
    *
    * @param finalUrl           The URL for the request
    * @param logger             The logger to use, should the logger for the model for
    *                           easy debugging
    * @tparam M The model which this request should return
    * @return
    */
  private[v1] def createRequestPUT[M](finalUrl: Uri, logger: Logger)(
      implicit client: HttpExt,
      materializer: Materializer,
      executionContext: ExecutionContext,
      decoder: Decoder[M],
      token: Option[Token] = None
  ): Future[Try[M]] = {

    val headers = buildHeaders(token)
    val req =
      HttpRequest(uri = finalUrl, method = HttpMethods.PUT, headers = headers)

    for {
      response <- client.singleRequest(req)
      _ = logger.debug(s"Received response $response")
      parsed <- parseServerError[M](response, finalUrl, None, None, logger)
      result = parsed match {
        case Right(triedValue) =>
          util.Success(triedValue.get)
        case Left(error) =>
          util.Failure(error)
      }
    } yield result
  }

  private def buildHeaders(token: Option[Token]): List[HttpHeader] =
    List(
      token.map(token => Authorization(OAuth2BearerToken(token.value)))
    ).flatten

  /**
    * A function which does the simplest ideal handling for making a litego request.
    * It handles specific litego errors, and will retry the request for errors that
    * indicate some sort of network error.
    *
    * @param request         The request that you are making with Litego
    * @param numberOfRetries Number of retries, provided by default in [[io.litego.api.Config]]
    * @tparam T The returning Litego object for the request
    * @return
    */
  def handle[T](request: Future[Try[T]], numberOfRetries: Int = Config.numberOfRetries)(
      implicit executionContext: ExecutionContext
  ): Future[T] = {
    def responseBlock = request

    def responseBlockWithRetries(currentRetryCount: Int): Future[Try[T]] =
      if (currentRetryCount > numberOfRetries) {
        Future.failed {
          MaxNumberOfRetriesException(currentRetryCount)
        }
      } else {
        responseBlock.flatMap {
          case scala.util.Success(value) =>
            Future.successful(Success(value))
          case scala.util.Failure(failure) =>
            failure match {
              case Errors.ServerError(_) =>
                responseBlockWithRetries(currentRetryCount + 1)
              case _ =>
                Future.failed(failure)
            }
        }
      }

    responseBlockWithRetries(0).flatMap {
      case Success(success) =>
        Future.successful(success)
      case Failure(throwable) =>
        Future.failed(throwable)
    }
  }

  /**
    * Parses a response from dispatch and attempts to do error process handling for specific errors
    *
    * @param response
    * @param finalUrl
    * @param getQueryParameters
    * @param postJsonParameters
    * @return Will return a [[Left]] if we catch one of the handled errors. Will return a [[Right]] if no server errors
    *         are made. Will throw an [[UnhandledServerError]] for uncaught errors.
    */
  private[v1] def parseServerError[A](
      response: HttpResponse,
      finalUrl: Uri,
      getQueryParameters: Option[Map[String, String]],
      postJsonParameters: Option[Json],
      logger: Logger
  )(implicit executionContext: ExecutionContext,
    materializer: Materializer,
    decoder: Decoder[A]): Future[Either[ErrorResponse, Try[A]]] = {
    val httpCode = response.status.intValue()

    logger.debug(s"Response status code is $httpCode")

    for {
      result <- {
        if (response.status.isSuccess()) {
          Unmarshal(response.entity.httpEntity.withContentType(ContentTypes.`application/json`))
            .to[A]
            .map(x => Right(util.Success(x)))
            .recover {
              case e: CirceStreamSupport.JsonParsingException => Right(util.Failure(e))
            }
        } else {
          httpCode match {
            case 400 | 403 | 404 | 406 =>
              for {
                json <- {
                  Unmarshal(
                    response.entity.httpEntity.withContentType(ContentTypes.`application/json`)
                  ).to[Json]
                    .map { json =>
                      val apiError: Option[ApiError] = json.as[ApiError].toOption
                      util.Success(
                        ErrorResponse(httpCode,
                                      Some(response.status.reason()),
                                      Some(response.status.defaultMessage()),
                                      apiError.flatMap(_.name),
                                      apiError.flatMap(_.detail))
                      )
                    }
                    .recover { case e => util.Failure(e) }
                }
              } yield {
                Left {
                  json match {
                    case util.Success(error)     => error
                    case util.Failure(throwable) => throw throwable
                  }
                }
              }
            case 500 =>
              response.discardEntityBytes()
              throw ServerError(response)
            case _ =>
              response.discardEntityBytes()
              throw UnhandledServerError(response)
          }
        }
      }
    } yield result
  }

}
