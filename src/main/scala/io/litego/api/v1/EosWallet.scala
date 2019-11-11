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

object EosWallet extends Wallet {

  val currency: String = "eos"

  def generateAddress()(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[EosAddress]] = {
    implicit val token: Some[AuthToken] = Some(authToken)

    val finalUrl = s"${endpoint.url}/v1/$currency/address"

    createRequestPUT[EosAddress](finalUrl, logger)
  }

  def sendCoins(request: SendEosRequest)(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[SendEosResponse]] = {
    implicit val token: Some[AuthToken] = Some(authToken)

    val finalUrl = s"${endpoint.url}/v1/$currency/sendcoins"

    createRequestPOST[SendEosResponse](finalUrl, request.asJson, logger)
  }

  def getTransfer(request: TransferRequest)(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[EosTransfer]] = {
    implicit val token: Some[AuthToken] = Some(authToken)

    val finalUrl = s"${endpoint.url}/v1/$currency/transfer/${request.id.getOrElse(new UUID(0L, 0L)).toString}"

    createRequestGET[EosTransfer](finalUrl, Map.empty, logger)
  }

  def getTransfersList(request: EosTransfersListRequest)(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[EosTransfersList]] = {
    val finalUrl = s"${endpoint.url}/v1/$currency/transfer"
    val queryParameters = Params.toParams(request)

    implicit val token: Some[AuthToken] = Some(authToken)

    createRequestGET[EosTransfersList](finalUrl, queryParameters, logger)
  }

  def getBalance()(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Try[EosBalance]] = {
    val finalUrl = s"${endpoint.url}/v1/$currency/balance"

    implicit val token: Some[AuthToken] = Some(authToken)

    createRequestGET[EosBalance](finalUrl, Map.empty, logger)
  }

  def subscribeTransfers(incoming: Flow[Wallet.EosTransferReceived, Done, NotUsed])(
    implicit authToken: AuthToken,
    endpoint: Endpoint,
    client: HttpExt,
    materializer: Materializer,
    ec: ExecutionContext
  ): Future[Done] = {
    val finalUrl = s"${endpoint.url.replaceAll("http", "ws")}/v1/$currency/transfers/subscribe"

    implicit val token: Some[AuthToken] = Some(authToken)

    createWebsocketRequest[EosTransferReceived](finalUrl, incoming, Sink.ignore, logger)
  }
}
