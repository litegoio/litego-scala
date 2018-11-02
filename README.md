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

### Clone and publish locally
To use litego-scala as a dependency in your project, the easiest way right now is to clone and publish it locally.
Simply run:
```bash
git clone https://github.com/litegoio/litego-scala.git
cd litego-scala
sbt publishLocal
```
Now you should be able to refer to it in your application.
That's in sbt:
```sbtshell
libraryDependencies ++= Seq(
  "io.litego" %% "litego-scala" % "0.1"
) 
```
### Add dependency on github repository
Alternatively you can add dependency on github repository:
```sbtshell
lazy val myProject = (project in file("myProject"))
  .dependsOn(ProjectRef(uri("https://github.com/litegoio/litego-scala.git#master"), "litego-scala"))
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
val chargesListRequest = Charges.ChargesListRequest(paid = Some(true), page = Some(0), pageSize = Some(10))
val chargesListResponse: Future[Charges.ChargesList] = handle(Charges.chargesList(chargesListRequest))
```
- Get charge
```scala
val getChargeRequest = Charges.GetChargeRequest(Some(UUID.fromString("e7129f40-dc28-11e8-9ede-2d69f348ade2")))
val getChargeResponse: Future[Charges.Charge] = handle(Charges.getCharge(getChargeRequest))
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
- Request manual withdrawal
```scala
val withdrawalResponse: Future[Withdrawals.WithdrawalTransaction] = handle(Withdrawals.manualWithdrawal())
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