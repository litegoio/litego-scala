package io.litego.api.v1

import java.time.Instant
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.circe._
import io.circe.parser.decode
import io.litego.api.Params
import io.litego.api.Params._
import io.litego.api.v1.defaults._

import scala.collection.immutable
import scala.util.Either

private[v1] trait Wallet extends LazyLogging

object Wallet {
  case class BitcoinAddress(address: String)

  object BitcoinAddress {
    implicit val bitcoinAddressDecoder: Decoder[BitcoinAddress] =
      Decoder.forProduct1(
        "address"
      )(BitcoinAddress.apply)
  }

  case class EosAddress(address: UUID, account: String)

  object EosAddress {
    implicit val eosAddressDecoder: Decoder[EosAddress] =
      Decoder.forProduct2(
        "address",
        "account"
      )(EosAddress.apply)
  }

  case class SendBtcRequest(
    address: String = "",
    amountSat: Long = 0L,
    comment: Option[String] = None
  )

  object SendBtcRequest {
    implicit val sendBtcRequestEncoder: Encoder[SendBtcRequest] = Encoder.forProduct3(
      "address",
      "amount_sat",
      "comment"
    )(x => (x.address, x.amountSat, x.comment))
  }

  case class SendEosRequest(
    account: String = "",
    amountEos: Double = 0D,
    memo: Option[String] = None
  )

  object SendEosRequest {
    implicit val sendEosRequestEncoder: Encoder[SendEosRequest] = Encoder.forProduct3(
      "account",
      "amount_eos",
      "memo"
    )(x => (x.account, x.amountEos, x.memo))
  }

  case class SendBtcResponse(
    id: UUID,
    txid: String,
    amountSat: Long,
    blockchainFee: Long,
    comment: Option[String]
  )

  object SendBtcResponse {
    implicit val sendBtcResponseDecoder: Decoder[SendBtcResponse] = Decoder.forProduct5(
      "id",
      "txid",
      "amount_sat",
      "blockchain_fee",
      "comment"
    )(SendBtcResponse.apply)
  }

  case class SendEosResponse(
    id: UUID,
    txid: String,
    amountEos: Double,
    memo: Option[String]
  )

  object SendEosResponse {
    implicit val sendEosResponseDecoder: Decoder[SendEosResponse] = Decoder.forProduct4(
      "id",
      "txid",
      "amount_eos",
      "memo"
    )(SendEosResponse.apply)
  }

  case class SendManyBtcRequest(
    amounts: Seq[SendBtcRequest] = Seq.empty[SendBtcRequest],
    comment: Option[String] = None
  )

  object SendManyBtcRequest {
    implicit val sendBtcRequestEncoder: Encoder[SendManyBtcRequest] = Encoder.forProduct2(
      "amounts",
      "comment"
    )(x => (x.amounts, x.comment))
  }

  case class SendManyEosRequest(
    amounts: Seq[SendEosRequest] = Seq.empty[SendEosRequest],
    comment: Option[String] = None
  )

  object SendManyEosRequest {
    implicit val sendBtcRequestEncoder: Encoder[SendManyEosRequest] = Encoder.forProduct2(
      "amounts",
      "comment"
    )(x => (x.amounts, x.comment))
  }

  case class SendManyBtcResponse(transfers: immutable.Seq[SendBtcResponse])

  object SendManyBtcResponse {
    implicit val sendManyBtcResponseDecoder: Decoder[SendManyBtcResponse] = Decoder.forProduct1(
      "transfers"
    )(SendManyBtcResponse.apply)
  }

  case class SendManyEosResponse(transfers: immutable.Seq[SendEosResponse])

  object SendManyEosResponse {
    implicit val sendManyBtcResponseDecoder: Decoder[SendManyEosResponse] = Decoder.forProduct1(
      "transfers"
    )(SendManyEosResponse.apply)
  }

  case class BtcTransfer(
    id: UUID,
    txid: String,
    address: String,
    amountSat: Long,
    blockchainFee: Option[Long],
    status: String,
    direction: String,
    comment: Option[String],
    createdAt: Instant,
    statusChangedAt: Instant
  )

  object BtcTransfer {
    implicit val btcTransferDecoder: Decoder[BtcTransfer] = Decoder.forProduct10(
      "id",
      "txid",
      "address",
      "amount_sat",
      "blockchain_fee",
      "status",
      "direction",
      "comment",
      "created_at",
      "status_changed_at"
    )(BtcTransfer.apply)
  }

  case class EosTransfer(
    id: UUID,
    txid: String,
    amountEos: Double,
    status: String,
    direction: String,
    memo: Option[String],
    createdAt: Instant,
    statusChangedAt: Instant
  )

  object EosTransfer {
    implicit val btcTransferDecoder: Decoder[EosTransfer] = Decoder.forProduct8(
      "id",
      "txid",
      "amount_eos",
      "status",
      "direction",
      "memo",
      "created_at",
      "status_changed_at"
    )(EosTransfer.apply)
  }

  case class TransferRequest(id: Option[UUID] = None)

  case class BtcTransfersListRequest(
    page: Option[Int] = None,
    pageSize: Option[Int] = None,
    status: Option[String] = None,
    direction: Option[String] = None,
    minAmount: Option[Long] = None,
    maxAmount: Option[Long] = None,
    startCreatedAt: Option[Long] = None,
    endCreatedAt: Option[Long] = None,
    startStatusChangedAt: Option[Long] = None,
    endStatusChangedAt: Option[Long] = None,
    sortBy: Option[String] = None,
    ascending: Option[Boolean] = None
  )

  object BtcTransfersListRequest {
    implicit val btcTransfersListRequestParams: Params[BtcTransfersListRequest] =
      Params.params[BtcTransfersListRequest] { request =>
        flatten(
          Map(
            "page" -> request.page.map(_.toString),
            "page_size" -> request.pageSize.map(_.toString),
            "status" -> request.status,
            "direction" -> request.direction,
            "min_amount" -> request.minAmount.map(_.toString),
            "max_amount" -> request.maxAmount.map(_.toString),
            "start_created_at" -> request.startCreatedAt.map(_.toString),
            "end_created_at" -> request.endCreatedAt.map(_.toString),
            "start_status_changed_at" -> request.startStatusChangedAt.map(_.toString),
            "end_status_changed_at" -> request.endStatusChangedAt.map(_.toString),
            "sort_by" -> request.sortBy,
            "ascending" -> request.ascending.map(_.toString)
          )
        )
      }
  }

  case class BtcTransfersList(data: Seq[BtcTransfer], page: Int, pageSize: Int, count: Int, `object`: String)

  object BtcTransfersList {
    implicit val transfersListDecoder: Decoder[BtcTransfersList] = Decoder.forProduct5(
      "data",
      "page",
      "page_size",
      "count",
      "object"
    )(BtcTransfersList.apply)
  }

  case class EosTransfersList(data: Seq[EosTransfer], page: Int, pageSize: Int, count: Int, `object`: String)

  object EosTransfersList {
    implicit val transfersListDecoder: Decoder[EosTransfersList] = Decoder.forProduct5(
      "data",
      "page",
      "page_size",
      "count",
      "object"
    )(EosTransfersList.apply)
  }

  case class BtcBalance(
    balanceSat: Long,
    spendableBalanceSat: Long
  )

  object BtcBalance {
    implicit val getBtcBalanceResponseDecoder: Decoder[BtcBalance] = Decoder.forProduct2(
      "balance_sat",
      "spendable_balance_sat"
    )(BtcBalance.apply)
  }

  case class EosBalance(
    balanceEos: Double,
    spendableBalanceEos: Double
  )

  object EosBalance {
    implicit val getBtcBalanceResponseDecoder: Decoder[EosBalance] = Decoder.forProduct2(
      "balance_eos",
      "spendable_balance_eos"
    )(EosBalance.apply)
  }

  case class BtcTransferReceived(
    id: UUID,
    merchantId: UUID,
    txid: String,
    amountSat: Long,
    receivedAt: Instant
  )

  object BtcTransferReceived {
    implicit val btcEventDecoder: Decoder[BtcTransferReceived] = Decoder.forProduct5(
      "id",
      "merchant_id",
      "txid",
      "amount_sat",
      "received_at"
    )(BtcTransferReceived.apply)

    def decodeWsMsg(json: String): Either[Error, BtcTransferReceived] = {
      decode[BtcTransferReceived](json)
    }
  }

  case class EosTransferReceived(
    id: UUID,
    merchantId: UUID,
    txid: String,
    amountEos: Double,
    memo: String,
    receivedAt: Instant
  )

  object EosTransferReceived {
    implicit val btcEventDecoder: Decoder[EosTransferReceived] = Decoder.forProduct6(
      "id",
      "merchant_id",
      "txid",
      "amount_eos",
      "memo",
      "received_at"
    )(EosTransferReceived.apply)

    def decodeWsMsg(json: String): Either[Error, EosTransferReceived] = {
      decode[EosTransferReceived](json)
    }
  }

  case class EosTransfersListRequest(
    page: Option[Int] = None,
    pageSize: Option[Int] = None,
    status: Option[String] = None,
    direction: Option[String] = None,
    minAmount: Option[Double] = None,
    maxAmount: Option[Double] = None,
    startCreatedAt: Option[Long] = None,
    endCreatedAt: Option[Long] = None,
    startStatusChangedAt: Option[Long] = None,
    endStatusChangedAt: Option[Long] = None,
    sortBy: Option[String] = None,
    ascending: Option[Boolean] = None
  )

  object EosTransfersListRequest {
    implicit val eosTransfersListRequestParams: Params[EosTransfersListRequest] =
      Params.params[EosTransfersListRequest] { request =>
        flatten(
          Map(
            "page"     -> request.page.map(_.toString),
            "page_size" -> request.pageSize.map(_.toString),
            "status" -> request.status,
            "direction" -> request.direction,
            "min_amount" -> request.minAmount.map(_.toString),
            "max_amount" -> request.maxAmount.map(_.toString),
            "start_created_at" -> request.startCreatedAt.map(_.toString),
            "end_created_at" -> request.endCreatedAt.map(_.toString),
            "start_status_changed_at" -> request.startStatusChangedAt.map(_.toString),
            "end_status_changed_at" -> request.endStatusChangedAt.map(_.toString),
            "sort_by" -> request.sortBy,
            "ascending" -> request.ascending.map(_.toString)
          )
        )
      }
  }
}