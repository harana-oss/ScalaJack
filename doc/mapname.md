## Change Field Names

If you are using ScalaJack with 3rd party JSON or Mongo documents, you may be in a situation where you don't own or control the names of the fields.  You may wish the field names of your classes to be different than the names in the 3rd party format.

ScalaJack provides the @Change annotation.  Marking a field with @Change in your case class allows you to map its name to a different target name in JSON or Mongo.

```scala
case class MapFactor(
  @Change(name = "foo_bar") fooBar:String,
  @Change(name = "a_b") thingy:   Long,
  count:                           Int,
  @Change(name = "big_mac") bigMac:String
)
```

If you serialize an instance of this class to JSON you'd get something like:

```JSON
{"foo_bar":"hey","a_b":25,"count":3,"big_mac":"hungry"}
```

Notice that several of these field names are re-mapped to new values, presumably to match the format required by a 3rd party JSON provider.

This works in Mongo too, including mixing it up with the @DBKey annotation:

```scala
case class MapFactorId2(
  @DBKey @Change(name = "foo_bar") fooBar:String,
  @DBKey @Change(name = "a_b") thingy:  Long,
  @DBKey hey:                            Int,
  count:                                 Int,
  @Change(name = "big_mac") bigMac:     String
)
```

A serialized instance of this class might look like this:

```json
{ "_id" : { "foo_bar" : "wonder", "a_b" : { "$numberLong" : "25" }, "hey" : 1 }, "count" : 3, "big_mac" : "hungry" }
```

Again you can see the @Change annotated fields had their name re-mapped to the specified values.
