package io.litego.api.v1

import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.HttpExt
import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import io.litego.api.Params.flatten
import io.litego.api.v1.defaults._
import io.litego.api.{AuthToken, Endpoint, Params}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Withdrawals extends LazyLogging {

  val REGULAR_ADDRESS_TYPE = "regular"
  val EXTENDED_ADDRESS_TYPE = "extended"

  val CREATED_STATUS = "created"
  val PERFORMED_STATUS = "performed"
  val CONFIRMED_STATUS = "confirmed"

  case class WithdrawalAddress(`type`: String, value: Option[String], `object`: String)

  object WithdrawalAddress {
    implicit val withdrawalAddressDecoder: Decoder[WithdrawalAddress] = Decoder.forProduct3(
      "type",
      "value",
      "object"
    )(WithdrawalAddress.apply)
  }

  case class WithdrawalTransaction(transactionId: UUID,
                                   merchantId: UUID,
                                   status: String,
                                   address: Option[String],
                                   transactionIdStr: Option[String],
                                   totalAmount: Long,
                                   relativeFee: Long,
                                   manualFee: Long,
                                   createdAt: Instant,
                                   statusChangedAt: Instant,
                                   `type`: String,
                                   `object`: String)

  object WithdrawalTransaction {
    implicit val withdrawalTransactionDecoder: Decoder[WithdrawalTransaction] = Decoder.forProduct12(
      "transaction_id",
      "merchant_id",
      "status",
      "address",
      "transaction_id_str",
      "total_amount",
      "relative_fee",
      "manual_fee",
      "created_at",
      "status_changed_at",
      "type",
      "object"
    )(WithdrawalTransaction.apply)
  }

  case class WithdrawalsList(data: Seq[WithdrawalTransaction], page: Int, pageSize: Int, count: Int, `object`: String)

  object WithdrawalsList {
    implicit val withdrawalsListDecoder: Decoder[WithdrawalsList] = Decoder.forProduct5(
      "data",
      "page",
      "page_size",
      "count",
      "object"
    )(WithdrawalsList.apply)
  }

  case class WithdrawalSettings(
    withdrawalFee: Double,
    withdrawalManualFee: Long,
    withdrawalMinAmount: Long
  )

  object WithdrawalSettings {
    implicit val withdrawalsListDecoder: Decoder[WithdrawalSettings] = Decoder.forProduct3(
      "withdrawal_fee",
      "withdrawal_manual_fee",
      "withdrawal_min_amount"
    )(WithdrawalSettings.apply)
  }

  case class SetWithdrawalAddressRequest(`type`: String = "", value: String = "")

  object SetWithdrawalAddressRequest {
    implicit val setWithdrawalAddressRequestEncoder: Encoder[SetWithdrawalAddressRequest] = Encoder.forProduct2(
      "type",
      "value"
    )(x => (x.`type`, x.value))
  }

  case class WithdrawalsListRequest(status: Option[String] = None,
                                    page: Option[Int] = None,
                                    pageSize: Option[Int] = None)

  object WithdrawalsListRequest {
    implicit val withdrawalsListRequestParams: Params[WithdrawalsListRequest] =
      Params.params[WithdrawalsListRequest] { request =>
        flatten(
          Map(
            "status" -> request.status,
            "page" -> request.page.map(_.toString),
            "page_size" -> request.pageSize.map(_.toString)
          )
        )
      }
  }

  def setWithdrawalAddress(request: SetWithdrawalAddressRequest)(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[WithdrawalAddress]] = {

    implicit val token: Some[AuthToken] = Some(authToken)

    val finalUrl = endpoint.url + "/v1/merchant/me/withdrawal/address"

    createRequestPOST[WithdrawalAddress](finalUrl, request.asJson, logger)
  }

  def manualWithdrawal()(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[WithdrawalTransaction]] = {

    implicit val token: Some[AuthToken] = Some(authToken)

    val finalUrl = endpoint.url + "/v1/merchant/me/withdrawal/manual"

    createRequestPUT[WithdrawalTransaction](finalUrl, logger)
  }

  def withdrawalsList(request: WithdrawalsListRequest)(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[WithdrawalsList]] = {

    val finalUrl = s"${endpoint.url}/v1/merchant/me/withdrawals"
    val queryParameters = Params.toParams(request)

    implicit val token: Some[AuthToken] = Some(authToken)

    createRequestGET[WithdrawalsList](finalUrl, queryParameters, logger)
  }

  def withdrawalSettings()(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[WithdrawalSettings]] = {

    val finalUrl = s"${endpoint.url}/v1/merchant/withdrawal/settings"

    implicit val token: Some[AuthToken] = Some(authToken)

    createRequestGET[WithdrawalSettings](finalUrl, Map.empty, logger)
  }

}
