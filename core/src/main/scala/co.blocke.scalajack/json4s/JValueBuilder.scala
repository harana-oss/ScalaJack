package co.blocke.scalajack
package json4s

import org.json4s.JValue

import scala.collection.mutable

case class JValueBuilder() extends mutable.Builder[JValue, JValue] {
  private var internalValue: Option[JValue] = None

  def addOne(elem: JValue): this.type = {
    internalValue = Some(elem)
    this
  }

  def +=(elem: JValue) = addOne(elem)

  def clear(): Unit = internalValue = None

  def result(): JValue =
    internalValue.getOrElse(
      throw new ScalaJackError("No value set for internal json4s builder")
    )
}
