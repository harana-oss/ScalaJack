package co.blocke.scalajack
package json.misc

import util._
import scala.reflect.runtime.universe._

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import typeadapter._

class LooseChange extends AnyFunSpec with Matchers {

  val sj = ScalaJack()

  describe(
    "-----------------------\n:  Lose Change Tests  :\n-----------------------"
  ) {
      it("Bijective compose") {
        val bi1 = BijectiveFunction[Int, Boolean](
          (i: Int) => if (i > 3) true else false,
          (b: Boolean) => if (b) 3 else 0
        )
        val bi2 = BijectiveFunction[String, Int](
          (s: String) => s.length,
          (i: Int) => "x" * i
        )
        val bi3 = bi1.compose[String](bi2)
        bi3("blather") should be(true)
      }
      it("Bijective double inversion") {
        val bi = BijectiveFunction[String, Int](
          (s: String) => s.length,
          (i: Int) => "x" * i
        )
        val x = bi.inverse
        x(3) should be("xxx")
        val y = x.inverse
        y("foo") should be(3)
      }
      it("Can find collection and key annotations on case class") {
        val adapter =
          sj.taCache.typeAdapter(typeOf[DefaultOpt]).as[CaseClassTypeAdapter[_]]
        adapter.dbCollectionName should be(Some("myDefaults"))
        adapter.dbKeys.head.dbKeyIndex should be(Some(1))
      }
      it("Can find collection and key annotations on plain class") {
        val adapter =
          sj.taCache.typeAdapter(typeOf[Plain]).as[PlainClassTypeAdapter[_]]
        adapter.dbCollectionName should be(Some("plains"))
        adapter.dbKeys.head.dbKeyIndex should be(Some(1))
      }
      it("BinaryTypeAdapter") {
        val bytes = "This is a test".getBytes
        val binary = sj.render(bytes)
        binary should equal("\"VGhpcyBpcyBhIHRlc3Q=\"")
        sj.read[Array[Byte]](binary) should equal(bytes)
      }
    }
}
