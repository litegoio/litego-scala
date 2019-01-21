package io.litego.api.v1

import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.HttpExt
import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import io.litego.api.v1.Withdrawals.WithdrawalAddress
import io.litego.api.v1.defaults._
import io.litego.api.{AuthToken, Endpoint, RefreshToken}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Merchants extends LazyLogging {

  case class AuthenticateResponse(id: UUID, authToken: String, refreshToken: String)

  object AuthenticateResponse {
    implicit val authenticateResponseDecoder: Decoder[AuthenticateResponse] = Decoder.forProduct3(
      "id",
      "auth_token",
      "refresh_token"
    )(AuthenticateResponse.apply)
  }

  case class RefreshAuthTokenResponse(authToken: String)

  object RefreshAuthTokenResponse {
    implicit val refreshAuthTokenResponseDecoder: Decoder[RefreshAuthTokenResponse] = Decoder.forProduct1(
      "auth_token"
    )(RefreshAuthTokenResponse.apply)
  }

  case class MerchantInfo(id: UUID,
                          name: String,
                          availableBalanceSatoshi: Long,
                          pendingWithdrawalSatoshi: Long,
                          withdrawnTotalSatoshi: Long,
                          withdrawalTransactionId: Option[UUID],
                          withdrawalAddress: Option[WithdrawalAddress],
                          notificationUrl: Option[String],
                          `object`: String)

  object MerchantInfo {
    implicit val getMerchantSummaryResponseDecoder: Decoder[MerchantInfo] = Decoder.forProduct9(
      "id",
      "name",
      "available_balance_satoshi",
      "pending_withdrawal_satoshi",
      "withdrawn_total_satoshi",
      "withdrawal_transaction_id",
      "withdrawal_address",
      "notification_url",
      "object"
    )(MerchantInfo.apply)
  }

  case class NotificationUrl(url: String, `object`: String)

  object NotificationUrl {
    implicit val notificationUrlDecoder: Decoder[NotificationUrl] = Decoder.forProduct2(
      "url",
      "object"
    )(NotificationUrl.apply)
  }

  case class NotificationResponse(chargeId: UUID, address: String, request: String, status: Int, statusText: String, timestamp: Instant)

  object NotificationResponse {
    implicit val notificationResponseDecoder: Decoder[NotificationResponse] = Decoder.forProduct6(
      "charge_id",
      "address",
      "request",
      "status",
      "status_text",
      "timestamp"
    )(NotificationResponse.apply)
  }

  case class NotificationResponsesList(data: Seq[NotificationResponse], page: Int, pageSize: Int, count: Int, `object`: String)

  object NotificationResponsesList {
    implicit val notificationResponsesListDecoder: Decoder[NotificationResponsesList] = Decoder.forProduct5(
      "data",
      "page",
      "page_size",
      "count",
      "object"
    )(NotificationResponsesList.apply)
  }

  case class ReferralPaymentResponse(amountMsat: Long, paidAt: Instant, `object`: String)

  object ReferralPaymentResponse {
    implicit val referralPaymentResponseDecoder: Decoder[ReferralPaymentResponse] = Decoder.forProduct3(
      "amount_msat",
      "paid_at",
      "object"
    )(ReferralPaymentResponse.apply)
  }

  case class ReferralPaymentsList(data: Seq[ReferralPaymentResponse], page: Int, pageSize: Int, count: Int, `object`: String)

  object ReferralPaymentsList {
    implicit val notificationResponsesListDecoder: Decoder[ReferralPaymentsList] = Decoder.forProduct5(
      "data",
      "page",
      "page_size",
      "count",
      "object"
    )(ReferralPaymentsList.apply)
  }

  case class AuthenticateRequest(merchantId: Option[UUID] = None, secretKey: String = "")

  object AuthenticateRequest {
    implicit val authenticateRequestEncoder: Encoder[AuthenticateRequest] = Encoder.forProduct2(
      "merchant_id",
      "secret_key"
    )(x => (x.merchantId, x.secretKey))
  }

  case class SetNotificationUrlRequest(url: String = "")

  object SetNotificationUrlRequest {
    implicit val setNotificationUrlRequestEncoder: Encoder[SetNotificationUrlRequest] = Encoder.forProduct1(
      "url"
    )(x => x.url)
  }

  def authenticate(request: AuthenticateRequest)(
    implicit endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[AuthenticateResponse]] = {

    val finalUrl = endpoint.url + "/v1/merchant/authenticate"

    createRequestPOST[AuthenticateResponse](finalUrl, request.asJson, logger)
  }

  def refreshAuthToken()(
    implicit refreshToken: RefreshToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[RefreshAuthTokenResponse]] = {

    implicit val token: Some[RefreshToken] = Some(refreshToken)

    val finalUrl = endpoint.url + "/v1/merchant/me/refresh-auth"

    createRequestPUT[RefreshAuthTokenResponse](finalUrl, logger)
  }

  def getInfo()(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[MerchantInfo]] = {

    implicit val token: Some[AuthToken] = Some(authToken)

    val finalUrl = endpoint.url + "/v1/merchant/me"

    createRequestGET[MerchantInfo](finalUrl, Map.empty, logger)
  }

  def setNotificationUrl(request: SetNotificationUrlRequest)(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[NotificationUrl]] = {

    implicit val token: Some[AuthToken] = Some(authToken)

    val finalUrl = endpoint.url + "/v1/merchant/me/notification-url"

    createRequestPOST[NotificationUrl](finalUrl, request.asJson, logger)
  }

  def notificationResponsesList()(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[NotificationResponsesList]] = {

    val finalUrl = s"${endpoint.url}/v1/merchant/me/notification-responses"

    implicit val token: Some[AuthToken] = Some(authToken)

    createRequestGET[NotificationResponsesList](finalUrl, Map.empty, logger)
  }

  def referralPaymentsList()(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[ReferralPaymentsList]] = {

    val finalUrl = s"${endpoint.url}/v1/merchant/me/referral-payments"

    implicit val token: Some[AuthToken] = Some(authToken)

    createRequestGET[ReferralPaymentsList](finalUrl, Map.empty, logger)
  }

}
