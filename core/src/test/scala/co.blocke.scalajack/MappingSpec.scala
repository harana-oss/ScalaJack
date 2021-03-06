package co.blocke.scalajack

import co.blocke.scalajack.json.JsonFlavor
import co.blocke.scalajack.model.JackFlavor
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import yaml._
import Converters._
import org.json4s._

trait Human
case class Person(name: String, age: Int) extends Human
case class Typey[T](thing: T) {
  type foom = T
}

class MappingSpec extends AnyFunSpec with Matchers {

  val sj: JsonFlavor        = ScalaJack()
  val sjY: JackFlavor[YAML] = ScalaJack(YamlFlavor())

  val simple: Person        = Person("Fred", 34)
  val complex: Typey[Human] = Typey[Human](Person("Fred", 34))
  val simpleJson            = """{"name":"Fred","age":34}"""
  val complexJson           = """{"foom":"co.blocke.scalajack.Human","thing":{"_hint":"co.blocke.scalajack.Person","name":"Fred","age":34}}"""
  val simpleJson4s          = JObject(List(("name", JString("Fred")), ("age", JInt(34))))
  val complexJson4s = JObject(
    List(
      ("foom", JString("co.blocke.scalajack.Human")),
      ("thing", JObject(List(("_hint", JString("co.blocke.scalajack.Person")), ("name", JString("Fred")), ("age", JInt(34)))))
    ))
  val simpleYaml      = """name: Fred
                     |age: 34
                     |""".stripMargin
  val complexYaml     = """foom: co.blocke.scalajack.Human
                      |thing:
                      |  _hint: co.blocke.scalajack.Person
                      |  name: Fred
                      |  age: 34
                      |""".stripMargin
  val simpleDelimited = "Fred,34"

  describe(
    "-------------------\n:  Mapping Tests  :\n-------------------"
  ) {
    /*
    def mapJson[T](fn: T => T)(implicit tt: TypeTag[T]): JSON           = holder.sjJson.render[T](fn(holder.sjJson.read[T](s)))
    def mapYaml[T](fn: T => T)(implicit tt: TypeTag[T]): JSON           = holder.sjYaml.render[T](fn(holder.sjYaml.read[T](s)))
    def mapDelimited[T](fn: T => T)(implicit tt: TypeTag[T]): DELIMITED = holder.sjDelimited.render[T](fn(holder.sjDelimited.read[T](s)))

    def mapJsonTo[T, S](toFlavor: JackFlavor[S])(fn: T => T)(implicit tt: TypeTag[T]): S      = { toFlavor.render[T](fn(holder.sjJson.read[T](s))) }
    def mapYamlTo[T, S](toFlavor: JackFlavor[S])(fn: T => T)(implicit tt: TypeTag[T]): S      = { toFlavor.render[T](fn(holder.sjYaml.read[T](s))) }
    def mapDelimitedTo[T, S](toFlavor: JackFlavor[S])(fn: T => T)(implicit tt: TypeTag[T]): S = { toFlavor.render[T](fn(holder.sjDelimited.read[T](s))) }

    def mapJson4s[T](fn: T => T)(implicit tt: TypeTag[T]): JValue                          = holder.sjJson4s.render[T](fn(holder.sjJson4s.read[T](s)))
    def mapJson4sTo[T, S](toFlavor: JackFlavor[S])(fn: T => T)(implicit tt: TypeTag[T]): S = { toFlavor.render[T](fn(holder.sjJson4s.read[T](s))) }
     */
    it("mapJson") {
      simpleJson.mapJson[Person](_.copy(age = 45)) should be("""{"name":"Fred","age":45}""")
    }
    it("mapJson4s") {
      simpleJson4s.mapJson4s[Person](_.copy(age = 45)) should be(JObject(List(("name", JString("Fred")), ("age", JInt(45)))))
    }
    it("mapYaml") {
      simpleYaml.mapYaml[Person](_.copy(age = 45)) should be("""name: Fred
                                                               |age: 45
                                                               |""".stripMargin)
    }
    it("mapDelimited") {
      simpleDelimited.mapDelimited[Person](_.copy(age = 45)) should be("""Fred,45""")
    }
    it("mapJsonTo") {
      simpleJson.mapJsonTo[Person, YAML](ScalaJack(YamlFlavor()))(_.copy(age = 45)) should be("""name: Fred
                                                                                                |age: 45
                                                                                                |""".stripMargin)
    }
    it("mapJson4sTo") {
      simpleJson4s.mapJson4sTo[Person, JSON](ScalaJack())(_.copy(age = 45)) should be("""{"name":"Fred","age":45}""")
    }
    it("mapYamlTo") {
      simpleYaml.mapYamlTo[Person, JSON](ScalaJack())(_.copy(age = 45)) should be("""{"name":"Fred","age":45}""")
    }
    it("mapDelimitedTo") {
      simpleDelimited.mapDelimitedTo[Person, JSON](ScalaJack())(_.copy(age = 45)) should be("""{"name":"Fred","age":45}""")
    }
  }
  describe(
    "-----------------------\n:  Convenience Tests  :\n-----------------------"
  ) {
    it("toJson") {
      simple.toJson[Person] should be(simpleJson)
      complex.toJson[Typey[Human]] should be(complexJson)
    }
    it("fromJson") {
      simpleJson.fromJson[Person] should be(simple)
      complexJson.fromJson[Typey[Human]] should be(complex)
    }
    it("toJson4s") {
      simple.toJson4s[Person] should be(simpleJson4s)
      complex.toJson4s[Typey[Human]] should be(complexJson4s)
    }
    it("fromJson4s") {
      simpleJson4s.fromJson4s[Person] should be(simple)
      complexJson4s.fromJson4s[Typey[Human]] should be(complex)
    }
    it("toYaml") {
      simple.toYaml[Person] should be(simpleYaml)
      complex.toYaml[Typey[Human]] should be(complexYaml)
    }
    it("fromYaml") {
      simpleYaml.fromYaml[Person] should be(simple)
      complexYaml.fromYaml[Typey[Human]] should be(complex)
    }
    it("toDelimited") {
      simple.toDelimited[Person] should be(simpleDelimited)
    }
    it("fromDelimited") {
      simpleDelimited.fromDelimited[Person] should be(simple)
    }
  }
  describe(
    "----------------------\n:  Converters Tests  :\n----------------------"
  ) {
    it("yamlToJson") {
      simpleYaml.yamlToJson should be(simpleJson)
      complexYaml.yamlToJson should be(complexJson)
    }
    it("yamlToJson4s") {
      simpleYaml.yamlToJson4s should be(simpleJson4s)
      complexYaml.yamlToJson4s should be(complexJson4s)
    }
    it("yamlToDelimited") {
      simpleYaml.yamlToDelimited[Person] should be(simpleDelimited)
    }
    it("jsonToYaml") {
      simpleJson.jsonToYaml should be(simpleYaml)
      complexJson.jsonToYaml should be(complexYaml)
    }
    it("jsonToJson4s") {
      simpleJson.jsonToJson4s should be(simpleJson4s)
      complexJson.jsonToJson4s should be(complexJson4s)
    }
    it("jsonToDelimited") {
      simpleJson.jsonToDelimited[Person] should be(simpleDelimited)
    }
    it("delimitedToYaml") {
      simpleDelimited.delimitedToYaml[Person] should be(simpleYaml)
    }
    it("delimitedToJson4s") {
      simpleDelimited.delimitedToJson4s[Person] should be(simpleJson4s)
    }
    it("delimitedToJson") {
      simpleDelimited.delimitedToJson[Person] should be(simpleJson)
    }
    it("json4sToYaml") {
      simpleJson4s.json4sToYaml should be(simpleYaml)
      complexJson4s.json4sToYaml should be(complexYaml)
    }
    it("json4sToJson") {
      simpleJson4s.json4sToJson should be(simpleJson)
      complexJson4s.json4sToJson should be(complexJson)
    }
    it("json4sToDelimited") {
      simpleJson4s.json4sToDelimited[Person] should be(simpleDelimited)
    }
  }
  describe(
    "-------------------------\n:  Configuration Tests  :\n-------------------------"
  ) {
    it("Make sure config changes are picked up") {
      complex.toJson[Typey[Human]] should be(complexJson)
      withConfig(Configuration().withDefaultHint("kind"))
      complex.toJson[Typey[Human]] should be("""{"foom":"co.blocke.scalajack.Human","thing":{"kind":"co.blocke.scalajack.Person","name":"Fred","age":34}}""")
    }
  }
}
