# litego-scala

litego-scala is a wrapper over the Litego REST API.
Litego API documentation can be found here [https://litego.io/documentation/](https://litego.io/documentation/)

## Libraries Used
- [circe](https://circe.github.io/circe/) for JSON (circe provides compile time macros for
reading/writing JSON from/to scala case classes). It also provides a very powerful API for validating/querying JSON
- [akka-http](http://doc.akka.io/docs/akka-http/current/scala.html) for making HTTP requests
- [akka-stream-json](https://github.com/knutwalker/akka-stream-json) for streaming JSON

litego-scala was intentionally designed to use bare minimum external dependencies so its easier to integrate with scala codebases

## Installation

Litego-scala available through a Maven repository. 
Get started by putting this into your build.sbt file.
```sbtshell
libraryDependencies += "io.litego" %% "litego-scala" % "0.5"
```

## Examples

First you need to choose API endpoint:
```scala
import io.litego.api.{Config, Endpoint}

implicit val endpoint: Endpoint = Config.testnetEndpoint
```
Endpoints for _mainnet_ and _testnet_ are stored in the [io.litego.api.Config](src/main/scala/io/litego/api/Config.scala)

Also, library methods require an implicit materializer, executionContext and HttpExt client
```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContextExecutor

implicit val system: ActorSystem = ActorSystem("my-system")
implicit val materializer: ActorMaterializer = ActorMaterializer()
implicit val executionContext: ExecutionContextExecutor = system.dispatcher

implicit val client: HttpExt = Http()
``` 
Now you will be able to call API methods.

litego-scala provides `handle` function which provides the typical way of dealing with errors.
It will attempt to retry the original request for errors which are deemed to be network related errors, else it will
return a failed `Future`. If it fails due to going over the retry limit, `handle` will also return a failed `Future` with
`MaxNumberOfRetries`

After registration on [https://litego.io](https://litego.io) and getting secret key and merchant ID values try to authenticate (get auth token for other requests)
Two ways how to get auth token:
- secret key and merchant ID
- refresh token (if exists). 
```scala
import java.util.UUID
import io.litego.api.v1.{Merchants, handle}
import scala.concurrent.Future

val authenticateRequest: Merchants.AuthenticateRequest = Merchants.AuthenticateRequest(Some(UUID.fromString("YOUR_MERCHANT_ID")), "YOUR_SECRET_KEY")
val authenticateResponse: Future[Merchants.AuthenticateResponse] = handle(Merchants.authenticate(authenticateRequest))
```
or
```scala
import io.litego.api.v1.{Merchants, handle}
import io.litego.api.RefreshToken
import scala.concurrent.Future

implicit val refreshToken: RefreshToken = RefreshToken("YOUR_REFRESH_TOKEN")

val refreshAuthTokenResponse: Future[Merchants.RefreshAuthTokenResponse] = handle(Merchants.refreshAuthToken())
```

You will get auth token and refresh token values. Auth token will be used then for other API requests. Refresh token should be saved for reauthentication when auth token is expired.

- Create charge
```scala
import io.litego.api.v1.{Charges, handle}
import io.litego.api.AuthToken
import scala.concurrent.Future

// Every API method (except authenticate()) requires implicit token in scope 
implicit val authToken: AuthToken = AuthToken("YOUR_AUTH_TOKEN")

val createChargeRequest: Charges.CreateChargeRequest = Charges.CreateChargeRequest("Some description", Some(1000L))
val crateChargeResponse: Future[Charges.Charge] = handle(Charges.create(createChargeRequest))
```
- Charges list 
```scala
val chargesListRequest = Charges.ChargesListRequest(paid = Some(true), page = Some(0), pageSize = Some(10), startDate = Some(1532864303), endDate = Some(1564400346), sortBy = Some("amount"), ascending = Some(true))
val chargesListResponse: Future[Charges.ChargesList] = handle(Charges.chargesList(chargesListRequest))
```
- Get charge
```scala
val getChargeRequest = Charges.GetChargeRequest(Some(UUID.fromString("e7129f40-dc28-11e8-9ede-2d69f348ade2")))
val getChargeResponse: Future[Charges.Charge] = handle(Charges.getCharge(getChargeRequest))
```
- Validate lightning invoice
```scala
val validateResponse: Future[Charges.ValidateLightningInvoiceResponse] = handle(Charges.validateLightningInvoice(ValidateLightningInvoiceRequest("lightning_payment_request", 1000L)))
```
- Get information about authenticated merchant
```scala
val getInfoResponse: Future[Merchants.MerchantInfo] = handle(Merchants.getInfo())
```
- Set withdrawal address
```scala
val setAddressResponse: Future[Withdrawals.WithdrawalAddress] = handle(
      Withdrawals.setWithdrawalAddress(Withdrawals.SetWithdrawalAddressRequest(`type` = Withdrawals.REGULAR_ADDRESS_TYPE, value = "some_address"))
    )
```
- Get withdrawal settings
```scala
val withdrawalSettings: Future[Withdrawals.WithdrawalSettings] = handle(Withdrawals.withdrawalSettings())
```
- Request manual withdrawal
```scala
val withdrawalResponse: Future[Withdrawals.WithdrawalTransaction] = handle(Withdrawals.manualWithdrawal())
```
- Request withdrawal to lightning invoice
```scala
val withdrawalResponse: Future[Withdrawals.WithdrawalTransaction] = handle(Withdrawals.lightningInvoiceWithdrawal(LightningWithdrawalRequest("lightning_payment_request", Some(1000L))))
```
- Request withdrawal to lightning channel
```scala
val withdrawalResponse: Future[Withdrawals.WithdrawalTransaction] = handle(Withdrawals.lightningChannelWithdrawal(LightningChannelWithdrawalRequest("03b882dcd309adaf4d66d1aadfbc6e85764bd65c6bdaf03689c55f1abd13f53fc5", "nodetestnet.litego.io:9735", Some(100000L))))
```
- Withdrawals list
```scala
val withdrawalsListResponse: Future[Withdrawals.WithdrawalsList] = handle(Withdrawals.withdrawalsList(Withdrawals.WithdrawalsListRequest()))
```
- Set webhook URL
```scala
val setWebhookResponse: Future[Merchants.NotificationUrl] = handle(Merchants.setNotificationUrl(Merchants.SetNotificationUrlRequest(url = "http://some.url")))
```
- List responses from webhook
```scala
val webhookResponsesList: Future[Merchants.NotificationResponsesList] = handle(Merchants.notificationResponsesList())
```
- List referral payments
```scala
val referralPaymentsList: Future[Merchants.ReferralPaymentsList] = handle(Merchants.referralPaymentsList())
```

### Websocket subscriptions

You can subscribe to topics with payments of all your charges or a single charge by it's ID

The Flow that is passed to this methods must emit exactly one Done element for each element that it receives. It must also emit them in the same order that the elements were received.
This means that you must not use methods such as filter or collect on the Flow which would drop elements.

- Subscribe for payments
```scala
val subscription: Future[Done] = Charges.subscribePayments(Flow[Charges.InvoiceSettled].map { message =>
  doSomethingWithTheMessage(message)
  println(message)
  Done
})
```
- Subscribe for payment of single charge
```scala
val subscription: Future[Done] = Charges.subscribePayment(UUID.fromString("2f551fc0-1faa-11e9-90e2-df42b3b425c0"), Flow[Charges.InvoiceSettled].map { message =>
  doSomethingWithTheMessage(message)
  println(message)
  Done
})
```

### BTC Wallet API

- Generate address
```scala
val address: Future[Wallet.BitcoinAddress] = handle(BitcoinWallet.generateAddress())
```
- Get balance
```scala
val balance: Future[Wallet.BtcBalance] = handle(BitcoinWallet.getBalance())
```
- Transfers list
```scala
val transfers: Future[Wallet.BtcTransfersList] = handle(BitcoinWallet.getTransfersList(Wallet.BtcTransfersListRequest()))
```
- Get transfer
```scala
val transfer: Future[Wallet.BtcTransfer] = handle(BitcoinWallet.getTransfer(Wallet.TransferRequest(Some(UUID.fromString("3c789c03-e5fd-3d64-8272-763b7325bd3b")))))
```
- Send coins
```scala
val sendCoinsResponse: Future[Wallet.SendBtcResponse] = handle(BitcoinWallet.sendCoins(Wallet.SendBtcRequest("mprD9vYaWUje3J16UMHevuwjirQ1aYU4RV", 1000L, Some("Some comment"))))
```
- Send many
```scala
val sendManyRequest = Wallet.SendManyBtcRequest(
    Seq(
      Wallet.SendBtcRequest("mprD9vYaWUje3J16UMHevuwjirQ1aYU4RV", 1000L),
      Wallet.SendBtcRequest("mwuxu2phV1XnRjzeYaq2A1euTnLYmQj4PX", 1100L),
      Wallet.SendBtcRequest("mme2c4STSQKpBUzVfNaKipPhaNsaiBKKJ6", 1200L)
    ),
    Some("Some comment")
  )
  val sendManyResponse: Future[Wallet.SendManyBtcResponse] = handle(BitcoinWallet.sendMany(sendManyRequest))
```
- Subscribe for incoming transfers
```scala
  val subscription: Future[Done] = BitcoinWallet.subscribeTransfers(Flow[Wallet.BtcTransferReceived].map { message =>
    println(message)
    Done
  })
```

### EOS Wallet API

- Generate address
```scala
val address: Future[Wallet.EosAddress] = handle(EosWallet.generateAddress())
```
- Get balance
```scala
val balance: Future[Wallet.EosBalance] = handle(EosWallet.getBalance())
```
- Transfers list
```scala
val transfers: Future[Wallet.EosTransfersList] = handle(EosWallet.getTransfersList(Wallet.EosTransfersListRequest()))
```
- Get transfer
```scala
val transfer: Future[Wallet.EosTransfer] = handle(EosWallet.getTransfer(Wallet.TransferRequest(Some(UUID.fromString("8aa76a1c-c43a-382e-b3f0-001382a9fba2")))))
```
- Send coins
```scala
val sendCoinsResponse: Future[Wallet.SendEosResponse] = handle(EosWallet.sendCoins(Wallet.SendEosRequest("eos_account", 0.0001, Some("Some memo"))))
```
- Subscribe for incoming transfers
```scala
  val subscription: Future[Done] = EosWallet.subscribeTransfers(Flow[Wallet.EosTransferReceived].map { message =>
    println(message)
    Done
  })
```