package co.blocke.scalajack
package model

import scala.collection.mutable

case class StringBuilder() extends mutable.Builder[String, String] {

  private val buf = new StringBuffer()

  def addOne(elem: String): StringBuilder.this.type = {
    buf.append(elem)
    this
  }

  override def +=(elem: String) = addOne(elem)

  def clear() = buf.setLength(0)

  def result() = buf.toString
}
