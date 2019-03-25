package io.litego.api.v1

import akka.http.scaladsl.model.HttpResponse
import io.circe.{Decoder, Encoder}

object Errors {

  case class ApiError(name: Option[String], detail: Option[String])

  object ApiError {
    implicit val apiErrorDecoder: Decoder[ApiError] = Decoder.forProduct2(
      "name",
      "detail"
    )(ApiError.apply)
  }

  case class ErrorResponse(code: Int,
                           `type`: Option[String] = None,
                           message: Option[String] = None,
                           name: Option[String] = None,
                           detail: Option[String] = None
                          ) extends Exception {
    override def toString: String = s"""Error($code, ${`type`}, $message, $name, $detail)"""

    override def getMessage: String = toString
  }

  object ErrorResponse {
    implicit val responseErrorDecoder: Decoder[ErrorResponse] = Decoder.forProduct5(
      "code",
      "type",
      "message",
      "name",
      "detail"
    )(ErrorResponse.apply)

    implicit val responseErrorEncoder: Encoder[ErrorResponse] = Encoder.forProduct5(
      "code",
      "type",
      "message",
      "name",
      "detail"
    )(
      x =>
        (
          x.code,
          x.`type`,
          x.message,
          x.name,
          x.detail
        )
    )
  }

  /**
    * @param httpResponse
    */
  case class ServerError(httpResponse: HttpResponse) extends Exception {
    override def getMessage =
      s"Server error, status code is ${httpResponse.status.intValue()}"
  }

  case class UnhandledServerError(httpResponse: HttpResponse) extends Exception {
    override def getMessage =
      s"Unhandled server error, status code is ${httpResponse.status.intValue()}"
  }

}
