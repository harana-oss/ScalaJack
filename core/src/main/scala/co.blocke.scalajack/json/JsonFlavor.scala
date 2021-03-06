package co.blocke.scalajack
package json

import model._
import typeadapter.AnyMapKeyTypeAdapter

import scala.reflect.runtime.universe._

/**
 * This class is a cut'n paste copy of JsonFlavor with some mods to lock in a type.  There's currently an
 * unfortunate amount of boilerplate copying between this class and JsonFlavor, but it facilitates a clean
 * user experience--smooth API for ScalaJack:  val fooSerializer = sj.forType[Foo];  fooSerializer.read(input)
 */
case class JsonFlavorFor[J](
    ta:                              TypeAdapter[J],
    override val defaultHint:        String                       = "_hint",
    override val permissivesOk:      Boolean                      = false,
    override val customAdapters:     List[TypeAdapterFactory]     = List.empty[TypeAdapterFactory],
    override val hintMap:            Map[Type, String]            = Map.empty[Type, String],
    override val hintValueModifiers: Map[Type, HintValueModifier] = Map.empty[Type, HintValueModifier],
    override val typeValueModifier:  HintValueModifier            = DefaultHintModifier,
    override val parseOrElseMap:     Map[Type, Type]              = Map.empty[Type, Type],
    override val enumsAsInt:         Boolean                      = false
) extends JackFlavorFor[JSON, J] {

  def read[T](js: JSON)(implicit tt: TypeTag[T]): T = {
    val parser = JsonParser(js, this)
    taCache.typeAdapter(tt.tpe.dealias).read(parser).asInstanceOf[T]
  }

  def read(js: JSON): J = ta.read(json.JsonParser(js, this))
  def render(t: J): JSON = {
    val sb = model.StringBuilder()
    ta.write(t, writer, sb)
    sb.result()
  }
  def forType[U](implicit tu: TypeTag[U]): JackFlavorFor[JSON, U] =
    this
      .copy(ta = taCache.typeAdapter(tu.tpe.dealias))
      .asInstanceOf[JackFlavorFor[JSON, U]]

  def render[T](t: T)(implicit tt: TypeTag[T]): JSON = {
    val sb = model.StringBuilder()
    taCache
      .typeAdapter(tt.tpe.dealias)
      .asInstanceOf[TypeAdapter[T]]
      .write(t, writer, sb)
    sb.result()
  }

  // $COVERAGE-OFF$All this is carbon-copy from JsonFlavor, which has test coverage.
  def parse(input: JSON): Parser = JsonParser(input, this)

  private val writer = JsonWriter()

  override val stringifyMapKeys: Boolean = true
  override lazy val anyMapKeyTypeAdapter: AnyMapKeyTypeAdapter =
    typeadapter.AnyMapKeyTypeAdapter(this, anyTypeAdapter)

  def allowPermissivePrimitives(): JackFlavor[JSON] =
    this.copy(permissivesOk = true)
  def enumsAsInts(): JackFlavor[JSON] = this.copy(enumsAsInt = true)
  def parseOrElse(poe: (Type, Type)*): JackFlavor[JSON] =
    this.copy(parseOrElseMap = this.parseOrElseMap ++ poe)
  def withAdapters(ta: TypeAdapterFactory*): JackFlavor[JSON] =
    this.copy(customAdapters = this.customAdapters ++ ta.toList)
  def withDefaultHint(hint: String): JackFlavor[JSON] =
    this.copy(defaultHint = hint)
  def withHints(h: (Type, String)*): JackFlavor[JSON] =
    this.copy(hintMap = this.hintMap ++ h)
  def withHintModifiers(hm: (Type, HintValueModifier)*): JackFlavor[JSON] =
    this.copy(hintValueModifiers = this.hintValueModifiers ++ hm)
  def withTypeValueModifier(tm: HintValueModifier): JackFlavor[JSON] =
    this.copy(typeValueModifier = tm)

  def stringWrapTypeAdapterFactory[T](
      wrappedTypeAdapter: TypeAdapter[T],
      emptyStringOk:      Boolean        = true
  )(implicit tt: TypeTag[T]): TypeAdapter[T] =
    StringWrapTypeAdapter(wrappedTypeAdapter, emptyStringOk)
  // $COVERAGE-ON$
}

case class JsonFlavor(
    override val defaultHint:        String                       = "_hint",
    override val permissivesOk:      Boolean                      = false,
    override val customAdapters:     List[TypeAdapterFactory]     = List.empty[TypeAdapterFactory],
    override val hintMap:            Map[Type, String]            = Map.empty[Type, String],
    override val hintValueModifiers: Map[Type, HintValueModifier] = Map.empty[Type, HintValueModifier],
    override val typeValueModifier:  HintValueModifier            = DefaultHintModifier,
    override val parseOrElseMap:     Map[Type, Type]              = Map.empty[Type, Type],
    override val enumsAsInt:         Boolean                      = false
) extends JackFlavor[JSON] {

  def read[T](js: JSON)(implicit tt: TypeTag[T]): T = {
    val parser = JsonParser(js, this)
    taCache.typeAdapter(tt.tpe.dealias).read(parser).asInstanceOf[T]
  }

  def forType[U](implicit tu: TypeTag[U]): JackFlavorFor[JSON, U] =
    JsonFlavorFor(
      taCache.typeAdapter(tu.tpe.dealias).asInstanceOf[TypeAdapter[U]],
      defaultHint,
      permissivesOk,
      customAdapters,
      hintMap,
      hintValueModifiers,
      typeValueModifier,
      parseOrElseMap,
      enumsAsInt
    )

  def render[T](t: T)(implicit tt: TypeTag[T]): JSON = {
    val sb = model.StringBuilder()
    taCache
      .typeAdapter(tt.tpe.dealias)
      .asInstanceOf[TypeAdapter[T]]
      .write(t, writer, sb)
    sb.result()
  }

  def parse(input: JSON): Parser = JsonParser(input, this)

  private val writer = JsonWriter() //(this)

  override val stringifyMapKeys: Boolean = true
  override lazy val anyMapKeyTypeAdapter: AnyMapKeyTypeAdapter =
    typeadapter.AnyMapKeyTypeAdapter(this, anyTypeAdapter)

  def allowPermissivePrimitives(): JackFlavor[JSON] =
    this.copy(permissivesOk = true)
  def enumsAsInts(): JackFlavor[JSON] = this.copy(enumsAsInt = true)
  def parseOrElse(poe: (Type, Type)*): JackFlavor[JSON] =
    this.copy(parseOrElseMap = this.parseOrElseMap ++ poe)
  def withAdapters(ta: TypeAdapterFactory*): JackFlavor[JSON] =
    this.copy(customAdapters = this.customAdapters ++ ta.toList)
  def withDefaultHint(hint: String): JackFlavor[JSON] =
    this.copy(defaultHint = hint)
  def withHints(h: (Type, String)*): JackFlavor[JSON] =
    this.copy(hintMap = this.hintMap ++ h)
  def withHintModifiers(hm: (Type, HintValueModifier)*): JackFlavor[JSON] =
    this.copy(hintValueModifiers = this.hintValueModifiers ++ hm)
  def withTypeValueModifier(tm: HintValueModifier): JackFlavor[JSON] =
    this.copy(typeValueModifier = tm)

  def stringWrapTypeAdapterFactory[T](
      wrappedTypeAdapter: TypeAdapter[T],
      emptyStringOk:      Boolean        = true
  )(implicit tt: TypeTag[T]): TypeAdapter[T] =
    StringWrapTypeAdapter(wrappedTypeAdapter, emptyStringOk)
}
