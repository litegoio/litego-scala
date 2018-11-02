package io.litego.api

/**
  * This exception is thrown when there is an error when max number of retries exceeded
  *
  * @param numberOfRetries The number of retries
  */
case class MaxNumberOfRetriesException(numberOfRetries: Int) extends Exception {
  override def getMessage =
    s"Wen't over max number of retries, retries is $numberOfRetries"
}
