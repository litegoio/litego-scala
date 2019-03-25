package io.litego.api.v1

import java.util.UUID

import akka.{Done, NotUsed}
import akka.http.scaladsl.HttpExt
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import io.circe.syntax._
import io.litego.api.v1.Wallet._
import io.litego.api.{AuthToken, Endpoint, Params}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object BitcoinWallet extends Wallet {

  val currency: String = "btc"

  def generateAddress()(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[BitcoinAddress]] = {
    implicit val token: Some[AuthToken] = Some(authToken)

    val finalUrl = s"${endpoint.url}/v1/$currency/address"

    createRequestPUT[BitcoinAddress](finalUrl, logger)
  }

  def sendCoins(request: SendBtcRequest)(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[SendBtcResponse]] = {
    implicit val token: Some[AuthToken] = Some(authToken)

    val finalUrl = s"${endpoint.url}/v1/$currency/sendcoins"

    createRequestPOST[SendBtcResponse](finalUrl, request.asJson, logger)
  }

  def sendMany(request: SendManyBtcRequest)(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[SendManyBtcResponse]] = {
    implicit val token: Some[AuthToken] = Some(authToken)

    val finalUrl = s"${endpoint.url}/v1/$currency/sendmany"

    createRequestPOST[SendManyBtcResponse](finalUrl, request.asJson, logger)
  }

  def getTransfer(request: TransferRequest)(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[BtcTransfer]] = {
    implicit val token: Some[AuthToken] = Some(authToken)

    val finalUrl = s"${endpoint.url}/v1/$currency/transfer/${request.id.getOrElse(new UUID(0L, 0L)).toString}"

    createRequestGET[BtcTransfer](finalUrl, Map.empty, logger)
  }

  def getTransfersList(request: TransfersListRequest)(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[BtcTransfersList]] = {
    val finalUrl = s"${endpoint.url}/v1/$currency/transfer"
    val queryParameters = Params.toParams(request)

    implicit val token: Some[AuthToken] = Some(authToken)

    createRequestGET[BtcTransfersList](finalUrl, queryParameters, logger)
  }

  def getBalance()(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[BtcBalance]] = {
    val finalUrl = s"${endpoint.url}/v1/$currency/balance"

    implicit val token: Some[AuthToken] = Some(authToken)

    createRequestGET[BtcBalance](finalUrl, Map.empty, logger)
  }

  def subscribeTransfers(incoming: Flow[Wallet.BtcTransferReceived, Done, NotUsed])(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    ec: ExecutionContext
  ): Future[Done] = {
    val finalUrl = s"${endpoint.url.replaceAll("http", "ws")}/v1/$currency/transfers/subscribe"

    implicit val token: Some[AuthToken] = Some(authToken)

    createWebsocketRequest[BtcTransferReceived](finalUrl, incoming, Sink.ignore, logger)
  }
}
