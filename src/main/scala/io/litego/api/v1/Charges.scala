package io.litego.api.v1

import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.HttpExt
import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import io.circe._
import io.circe.syntax._
import io.litego.api.Params._
import io.litego.api.v1.defaults._
import io.litego.api.{AuthToken, Endpoint, Params}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Charges extends LazyLogging {

  case class Charge(id: UUID,
                    merchantId: UUID,
                    description: String,
                    amount: Option[Long],
                    amountSatoshi: Option[Long],
                    amountPaidSatoshi: Option[Long],
                    paymentRequest: String,
                    paid: Boolean,
                    paidAt: Option[Instant],
                    created: Instant,
                    expirySeconds: Long,
                    `object`: String)

  object Charge {
    implicit val chargeDecoder: Decoder[Charge] = Decoder.forProduct12(
      "id",
      "merchant_id",
      "description",
      "amount",
      "amount_satoshi",
      "amount_paid_satoshi",
      "payment_request",
      "paid",
      "paid_at",
      "created",
      "expiry_seconds",
      "object"
    )(Charge.apply)
  }

  case class ChargesList(data: Seq[Charge], page: Int, pageSize: Int, count: Int, `object`: String)

  object ChargesList {
    implicit val chargesListDecoder: Decoder[ChargesList] = Decoder.forProduct5(
      "data",
      "page",
      "page_size",
      "count",
      "object"
    )(ChargesList.apply)
  }

  case class CreateChargeRequest(description: String = "", amountSatoshi: Option[Long] = None)

  object CreateChargeRequest {
    implicit val createChargeRequestEncoder: Encoder[CreateChargeRequest] = Encoder.forProduct2(
      "description",
      "amount_satoshi"
    )(
      x =>
        (
          x.description,
          x.amountSatoshi
      )
    )
  }

  case class ChargesListRequest(paid: Option[Boolean] = None,
                                page: Option[Int] = None,
                                pageSize: Option[Int] = None)

  object ChargesListRequest {
    implicit val chargesListRequestParams: Params[ChargesListRequest] =
      Params.params[ChargesListRequest] { request =>
        flatten(
          Map(
            "paidOnly" -> request.paid.map(_.toString),
            "page"     -> request.page.map(_.toString),
            "pageSize" -> request.pageSize.map(_.toString)
          )
        )
      }
  }

  case class GetChargeRequest(id: Option[UUID] = None)

  def create(request: CreateChargeRequest)(
      implicit authToken: AuthToken,
      endpoint: Endpoint,
      client: HttpExt,
      materializer: Materializer,
      executionContext: ExecutionContext
  ): Future[Try[Charge]] = {

    val finalUrl = s"${endpoint.url}/v1/charges"

    implicit val token: Some[AuthToken] = Some(authToken)

    createRequestPOST[Charge](finalUrl, request.asJson, logger)
  }

  def chargesList(request: ChargesListRequest)(
      implicit authToken: AuthToken,
      endpoint: Endpoint,
      client: HttpExt,
      materializer: Materializer,
      executionContext: ExecutionContext
  ): Future[Try[ChargesList]] = {

    val finalUrl        = s"${endpoint.url}/v1/charges"
    val queryParameters = Params.toParams(request)

    implicit val token: Some[AuthToken] = Some(authToken)

    createRequestGET[ChargesList](finalUrl, queryParameters, logger)
  }

  def getCharge(request: GetChargeRequest)(
      implicit authToken: AuthToken,
      endpoint: Endpoint,
      client: HttpExt,
      materializer: Materializer,
      executionContext: ExecutionContext
  ): Future[Try[Charge]] = {

    val finalUrl = s"${endpoint.url}/v1/charges/${request.id.getOrElse(new UUID(0L, 0L)).toString}"

    implicit val token: Some[AuthToken] = Some(authToken)

    createRequestGET[Charge](finalUrl, Map.empty, logger)
  }

}
