package io.litego.api

/**
  * The litego API endpoint. Make sure you leave out any
  * trailing '/' character.
  *
  * @param url
  */
case class Endpoint(url: String) extends AnyVal
