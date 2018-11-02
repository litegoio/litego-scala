package io.litego.api

trait Token {
  val value: String
}

case class AuthToken(value: String) extends Token

case class RefreshToken(value: String) extends Token
