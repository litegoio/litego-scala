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

  case class TransfersListRequest(
    page: Option[Int] = None,
    pageSize: Option[Int] = None
  )

  object TransfersListRequest {
    implicit val transfersListRequestParams: Params[TransfersListRequest] =
      Params.params[TransfersListRequest] { request =>
        flatten(
          Map(
            "page"     -> request.page.map(_.toString),
            "page_size" -> request.pageSize.map(_.toString)
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

  // WS
  //  {
  //    "id":"fe8ee7a1-f5b7-3d5e-b30c-8a986bf5f33d",
  //    "merchant_id":"0314d53e-d149-4988-80a2-8f3299399224",
  //    "txid":"90ad9d5ff60839c71a8cea927f2019fdeb7c96b5fd9983f0c4c610cf3a07c008",
  //    "amount_sat":30000,
  //    "received_at":"2019-03-25T08:37:40.135Z"
  //  }

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

  //WS
  //  {
  //    "id":"f0f6c638-318a-3ce6-9492-78b8dcb08090",
  //    "merchant_id":"0314d53e-d149-4988-80a2-8f3299399224",
  //    "txid":"b2e2b78dffe7239c885df9038bdb9dbfffa24d0663b393a804264341f9240ce0",
  //    "amount_eos":0.0001,
  //    "memo":"0314d53e-d149-4988-80a2-8f3299399224*from...",
  //    "received_at":"2019-03-25T08:41:25.554Z"
  //  }

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
}