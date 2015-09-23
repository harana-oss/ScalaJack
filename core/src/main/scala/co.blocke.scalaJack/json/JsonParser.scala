package co.blocke.scalajack
package json

import scala.reflect.runtime.currentMirror
import PrimitiveTypes._
import scala.reflect.runtime.universe._
import JsonTokens._
import scala.collection.mutable.{Map => MMap,ListBuffer => MList}

case class JsonParser(sjTName:String, idx:JsonIndex, vctx:VisitorContext) {

	def parse[T]()(implicit tt:TypeTag[T]) : T = {
		var i = 0  // index into idx

		def _makeClass[U]( ccTypeFn : ()=>CCType, t:AType )(implicit ty:TypeTag[U]) = {
			val objFields = MMap.empty[Any,Any]
			if( idx.tokType(i) != JSobjStart ) throw new JsonParseException(s"Expected JSobjStart and found ${JsonTokens.toName(idx.tokType(i))} at token $i",0)
			i += 1

			val sjT = ccTypeFn()

			// read key/value pairs
			while( idx.tokType(i) != JSobjEnd && idx.tokType(i) != JSobjEndInList  && idx.tokType(i) != JSobjEndObjKey) {
				if( idx.tokType(i) != JSstringObjKey ) throw new JsonParseException(s"Expected JSstringObjKey and found ${JsonTokens.toName(idx.tokType(i))}",0)
				val fieldName = idx.getToken(i)
				i += 1
				sjT.members.find(_._1 == fieldName).fold( skipValue )( f => objFields.put(fieldName, _parse(f._2)) )
			}
			i+=1

			// Ensure all needed class fields are there, setting aside any missing Optional fields (which become None)
			// val missing = sj.fields.map(_.fieldName).toSet.diff(objFields.keySet)
			val fnames = objFields.keySet
			// For all of the below:  _1 is the field name and _2 is the field type (AType)
			val missing = sjT.members.collect{
				case f if(fnames.contains(f._1)) => 
					None  // field found -- don't collect
				case f if(f._2.isInstanceOf[CollType] && f._2.asInstanceOf[CollType].isOptional) =>
				 	// Missing but optional => None  -- don't collect
					objFields.put(f._1,None)
					None
				case f => 
					Some(f)
				}.flatten.map(_._1)

			if(missing.size > 0) throw new JsonParseException(s"""No values parsed for field(s) ${missing.mkString(",")} for class ${t.name}""",0)
			Util.poof( sjT, objFields.toMap.asInstanceOf[Map[String,Any]] )(ty)
		}

		// def _parse( t:SjType, params:Map[String,SjType] = Map.empty[String,SjType] ) : Any = t match {
		def _parse( t:AType, topLevel:Boolean = false ) : Any = t match {
			case sj:CCType =>
				_makeClass( ()=>{sj}, t )

			case sj:TraitType =>
				_makeClass( ()=>{
					// Look-ahead and find type hint--figure out what kind of object his is and inspect it.
					val objClass = findTypeHint(vctx.hintMap.getOrElse(sj.name,vctx.hintMap("default")))//.get(vctx.typeHint)
					if( !objClass.isDefined )
						throw new JsonParseException(s"No type hint given for trait ${sj.name}",0)
					val sjObjType = Analyzer.inspectByName(objClass.get.toString,Some(sj))
					if( !sjObjType.isInstanceOf[CCType] ) throw new JsonParseException(s"Type hint $objClass does not specify a case class",0)
					sjObjType.asInstanceOf[CCType]
					}, t)

			case sj:PrimType =>
				val v = idx.tokType(i) match {
					case pt if(sj.name == "scala.Any") => 
						val (newI, value) = inferSimpleType(idx,i)
						i = newI-1
						value
					case JSstringObjKey | JSstring | JSstringInList | JSnumberObjKey | JSnumber | JSnumberInList =>
						PrimitiveTypes.primitiveTypes(sj.name)( Unicode.unescape_perl_string(idx.getToken(i)) )
					case JStrue  | JStrueInList  => true
					case JSfalse | JSfalseInList => false
					case JSnull  | JSnullInList  => null
					case _ => throw new JsonParseException(s"Expected primitive value but saw ${JsonTokens.toName(idx.tokType(i))} obj ${idx.getToken(i)}",0)
				}
				i += 1
				v

			case sj:EnumType =>
				try {
					sj.enum.withName( idx.getToken(i) )
				} catch {
					case w:Throwable => throw new JsonParseException(s"Value ${idx.getToken(i)} is not a valid for enum ${sj.enum.toString}",0)
				} finally {
					i += 1
				}

			case sj:CollType =>
				val ret = { 
					if( sj.isOptional ) {
						val parsed = _parse(sj.colTypes(0))
						i -= 1  // compensate for increment later
						Some(parsed)
					} else if(sj.name.endsWith("Map")) {
						val mapAcc = MMap.empty[Any,Any]
						if( idx.tokType(i) != JSobjStart ) 
							throw new JsonParseException(s"Expected JSlistStart and found ${JsonTokens.toName(idx.tokType(i))}",0)
						i += 1
						while( idx.tokType(i) != JSobjEnd && idx.tokType(i) != JSobjEndInList ) {
							val key = _parse(sj.colTypes(0))
							val value = _parse(sj.colTypes(1)) 
							mapAcc.put(key,value)
						}
						PrimitiveTypes.scalaCollections(sj.name)(mapAcc.toList)
					} else {
						val listAcc = MList.empty[Any]
						if( idx.tokType(i) != JSlistStart ) 
							throw new JsonParseException(s"Expected JSlistStart and found ${JsonTokens.toName(idx.tokType(i))}",0)
						i += 1
						while( idx.tokType(i) != JSlistEnd && idx.tokType(i) != JSlistEndInList ) {
							listAcc.append(_parse(sj.colTypes(0)))
						}
						PrimitiveTypes.scalaCollections(sj.name)(listAcc)
					}
				}
				i += 1
				ret

			// This is messed up (in Scala)...  If sj is a type parameter (of something else) then we wrap the value in the
			// value class, otherwise just supply the primitive value and let the JVM do the rest. <sigh>
			case sj:ValueClassType =>
				if( sj.isTypeParam || topLevel ) {
					parseValueClass(sj, parseValueClassPrimitive(sj)).asInstanceOf[T]
				} else {
					parseValueClassPrimitive(sj)
				}

			case sj:CustomType =>
				val v = sj.readers("default")(idx.getToken(i))
				i += 1
				v
		}

		def parseValueClassPrimitive( vc:ValueClassType ) = 
			vctx.valClassMap.get(vc.name).map( handler => 
				handler.read( _parse(PrimType("String")).asInstanceOf[String] ).asInstanceOf[AnyRef]
			).orElse( Some(_parse(vc.vcType).asInstanceOf[AnyRef]) ).get
		def parseValueClass( vc:ValueClassType, primitive:AnyRef ) = Class.forName(vc.name).getConstructors()(0).newInstance(primitive)

		def findTypeHint( hint:String ) : Option[String] = {
			var saveI = i
			var done = false
			val imax = idx.tokCount
			var retval : Option[String] = None
			while( !done ) {
				idx.tokType(i) match {
					case JSobjStart => skipValue()
					case JSstringObjKey if(idx.getToken(i) == hint) => 
						i += 1
						retval = Some(idx.getToken(i))
						done = true
					case _ => i += 1
				}
				if( i == idx.tokCount ) done = true
			}
			i = saveI
			retval
		}

		def skipValue() : Unit = {
			idx.tokType(i) match {
				case JSlistStart | JSobjStart =>
					i += 1
					var done = false
					while( !done ) {
						idx.tokType(i) match {
							case JSlistStart | JSobjStart =>
								skipValue()
							case JSlistEnd | JSlistEndInList | JSobjEnd | JSobjEndInList => 
								i += 1
								done = true
							case _ =>
								i += 1
						}
					}
				case _ => i += 1  // simple, single value
			}
		}

		def inferSimpleType(idx:JsonIndex, start:Int):(Int,Any) = {
			var i = start
			idx.tokType(i) match {
				case JSlistStart => 
					i += 1
					val acc = new scala.collection.mutable.ListBuffer[Any]()
					while(idx.tokType(i) != JSlistEnd && idx.tokType(i) != JSlistEndInList) {
						val (newI,v) = inferSimpleType(idx,i)
						i = newI
						acc += v
					}
					(i+1,acc.toList)
				case JSobjStart => 
					i += 1
					val acc = new scala.collection.mutable.HashMap[String,Any]()
					while(idx.tokType(i) != JSobjEnd && idx.tokType(i) != JSobjEndInList) {
						val (newI,k) = inferSimpleType(idx,i)
						i = newI
						val (newI2,v) = inferSimpleType(idx,i)
						i = newI2
						acc += (k.toString -> v)
					}
					(i+1,acc.toMap)
				case JSstringObjKey | JSstring | JSstringInList =>
					(i+1,PrimitiveTypes.primitiveTypes("String")( Unicode.unescape_perl_string(idx.getToken(i)) ))
				case JSnumberObjKey | JSnumber | JSnumberInList =>
					val raw = Unicode.unescape_perl_string(idx.getToken(i))
					if( raw.contains('.') )
						(i+1,PrimitiveTypes.primitiveTypes("scala.Double")( raw ))
					else
						(i+1,PrimitiveTypes.primitiveTypes("scala.Int")( raw ))
				case JStrue | JStrueInList | JSfalse | JSfalseInList =>
					(i+1,PrimitiveTypes.primitiveTypes("scala.Boolean")( Unicode.unescape_perl_string(idx.getToken(i)) ))
				case JSnull | JSnullInList =>
					(i+1,null)
				// case z => println("Boom: "+z)
				// (0,null)
			}
		}

		// Make it happen!
		_parse(Analyzer.inspectByName(sjTName),true).asInstanceOf[T]
	}
}
