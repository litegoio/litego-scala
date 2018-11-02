package io.litego.api

object Config {
  val testnetEndpoint: Endpoint = Endpoint("https://sandbox.litego.io:9000/api")

  val mainnetEndpoint: Endpoint = Endpoint("https://api.litego.io:9000/api")

  val numberOfRetries: Int = 5
}
