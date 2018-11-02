package io.litego.api

trait Params[T] {
  def toMap(t: T): Map[String, String]

  def flatten[K, V](input: Map[K, Option[V]]): Map[K, V] = Params.flatten(input)
}

object Params {
  def toParams[T](optionOfT: Option[T])(implicit params: Params[T]): Map[String, String] =
    optionOfT.map(params.toMap).getOrElse(Map.empty)

  def toParams[T](t: T)(implicit params: Params[T]): Map[String, String] =
    params.toMap(t)

  def params[T](transformer: T => Map[String, String]): Params[T] = new Params[T] {
    override def toMap(t: T) = transformer(t)
  }

  def flatten[K, V](input: Map[K, Option[V]]): Map[K, V] =
    input.collect({ case (k, Some(v)) => (k, v) })

  def flatten[K, V](input: List[(K, Option[V])]): List[(K, V)] =
    input.collect({ case (k, Some(v)) => (k, v) })
}
