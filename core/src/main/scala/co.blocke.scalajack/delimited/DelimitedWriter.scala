package co.blocke.scalajack
package delimited

import compat.StringBuilder
import typeadapter._
import co.blocke.scalajack.model.ClassHelper.ExtraFieldValue
import co.blocke.scalajack.model.{ ClassHelper, JackFlavor, SJError, TypeAdapter, Writer }
import co.blocke.scalajack.typeadapter.classes.PlainClassTypeAdapter

import scala.collection.Map
import scala.collection.immutable.ListMap
import scala.collection.mutable.Builder

case class DelimitedWriter(delimiter: Char, jackFlavor: JackFlavor[String]) extends Writer[String] {

  def writeArray[Elem](t: Iterable[Elem], elemTypeAdapter: TypeAdapter[Elem], out: Builder[String, String]): Unit =
    if (t != null) {
      val sb = new StringBuilder()
      val iter = t.iterator
      while (iter.hasNext) {
        elemTypeAdapter.write(iter.next, this, sb, false)
        if (iter.hasNext)
          sb += delimiter.toString
      }
      writeString(sb.result(), out)
    }

  def writeBigInt(t: BigInt, out: Builder[String, String]): Unit = out += t.toString
  def writeBoolean(t: Boolean, out: Builder[String, String]): Unit = out += t.toString
  def writeDecimal(t: BigDecimal, out: Builder[String, String]): Unit = out += t.toString
  def writeDouble(t: Double, out: Builder[String, String]): Unit = out += t.toString
  def writeInt(t: Int, out: Builder[String, String]): Unit = out += t.toString
  def writeLong(t: Long, out: Builder[String, String]): Unit = out += t.toString

  def writeMap[Key, Value, To](t: Map[Key, Value], keyTypeAdapter: TypeAdapter[Key], valueTypeAdapter: TypeAdapter[Value], out: Builder[String, String])(implicit keyTT: TypeTag[Key]): Unit =
    throw new SJError("Map-typed data is not supported for delimited output")

  def writeNull(out: Builder[String, String]): Unit = {} // write empty field

  def writeObject[T](
      t:            T,
      fieldMembers: ListMap[String, ClassHelper.ClassFieldMember[T, Any]],
      out:          Builder[String, String],
      extras:       List[(String, ExtraFieldValue[_])]                    = List.empty[(String, ExtraFieldValue[_])]): Unit =
    if (t != null) {
      var first = true
      fieldMembers.values.foreach { f =>
        if (first)
          first = false
        else
          out += delimiter.toString
        if (f.valueTypeAdapter.isInstanceOf[classes.CaseClassTypeAdapter[_]] || f.valueTypeAdapter.isInstanceOf[PlainClassTypeAdapter[_]]) {
          val sb = new StringBuilder()
          f.valueTypeAdapter.write(f.valueIn(t), this, sb, false)
          writeString(sb.result(), out)
        } else
          f.valueTypeAdapter.write(f.valueIn(t), this, out, false)
      }
    }

  def writeRawString(t: String, out: Builder[String, String]): Unit = out += t.replaceAll("\"", "\"\"")
  def writeString(t: String, out: Builder[String, String]): Unit =
    if (t != null) {
      val t0 = t.replaceAll("\"", "\"\"")
      val toWrite = if (t0 != t || t0.contains(delimiter))
        "\"" + t0 + "\""
      else
        t
      out += toWrite
    }

  def writeTuple(writeFns: List[(Writer[String], Builder[String, String]) => Unit], out: Builder[String, String]): Unit = {
    var first = true
    writeFns.map { f =>
      if (first)
        first = false
      else
        out += delimiter.toString
      f(this, out)
    }
  }
}
