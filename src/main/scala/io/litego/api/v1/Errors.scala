package io.litego.api.v1

import akka.http.scaladsl.model.HttpResponse
import io.circe.{Decoder, Encoder}

object Errors {

  case class ErrorResponse(code: Int, `type`: Option[String] = None, message: Option[String] = None) extends Exception{
    override def toString: String   = s"""Error($code, ${`type`}, $message)"""
    override def getMessage: String = toString
  }

  object ErrorResponse {
    implicit val responseErrorDecoder: Decoder[ErrorResponse] = Decoder.forProduct3(
      "code",
      "type",
      "message"
    )(ErrorResponse.apply)

    implicit val responseErrorEncoder: Encoder[ErrorResponse] = Encoder.forProduct3(
      "code",
      "type",
      "message"
    )(
      x =>
        (
          x.code,
          x.`type`,
          x.message
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
