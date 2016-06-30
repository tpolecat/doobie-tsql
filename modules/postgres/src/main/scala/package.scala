package tsql

import shapeless.{ Witness => W }

import java.util.UUID
import java.net.InetAddress

import org.postgis._
import org.postgresql.util._
import org.postgresql.geometric._

package object postgres {

  // Arw = ArrayReadWrite
  def pgArwBoxed[A <: AnyRef, S <: String](schemaElementType: String) = 
    ReadWrite(
      Read .refine(Read .array[A])                   .toSchema[S].done,
      Write.refine(Write.array[A](schemaElementType)).toSchema[S].done
    )

  // Orw = ObjectReadWrite
  def pgOrw[A <: AnyRef, S <: String](implicit ev: W.Aux[S]) =
    ReadWrite(
      Read.refine(Read.OtherObject).toSchema[S].done.map(_.asInstanceOf[A]),
      null // TODO
    )

  // Arrays of Boxed Primitives (note that Byte and Short are unsupported by the driver)
  implicit val PGBooleanArwBoxed = pgArwBoxed[java.lang.Boolean, W.`"_bit"`    .T]("bit"    )
  implicit val PGIntArwBoxed     = pgArwBoxed[java.lang.Integer, W.`"_int4"`   .T]("int4"   )
  implicit val PGLongArwBoxed    = pgArwBoxed[java.lang.Long,    W.`"_int8"`   .T]("int8"   )
  implicit val PGFloatArwBoxed   = pgArwBoxed[java.lang.Float,   W.`"_float4"` .T]("float4" )
  implicit val PGDoubleArwBoxed  = pgArwBoxed[java.lang.Double,  W.`"_float8"` .T]("float8" )
  implicit val PGStringArwBoxed  = pgArwBoxed[java.lang.String,  W.`"_varchar"`.T]("varchar")

  // Arrays of Unboxed Primitives
  implicit val PGBooleanArw = PGBooleanArwBoxed.axmap[Boolean](identity)(identity)
  implicit val PGIntArw     = PGIntArwBoxed    .axmap[Int    ](identity)(identity)
  implicit val PGLongArw    = PGLongArwBoxed   .axmap[Long   ](identity)(identity)
  implicit val PGFloatArw   = PGFloatArwBoxed  .axmap[Float  ](identity)(identity)
  implicit val PGDoubleArw  = PGDoubleArwBoxed .axmap[Double ](identity)(identity)

  // Geometric Types
  implicit val PGboxType      = pgOrw[PGbox,     W.`"box"`     .T]
  implicit val PGcircleType   = pgOrw[PGcircle,  W.`"circle"`  .T]
  implicit val PGlsegType     = pgOrw[PGlseg,    W.`"lseg"`    .T]
  implicit val PGpathType     = pgOrw[PGpath,    W.`"path"`    .T]
  implicit val PGpointType    = pgOrw[PGpoint,   W.`"point"`   .T]
  implicit val PGpolygonType  = pgOrw[PGpolygon, W.`"polygon"` .T]

  // Other Types
  implicit val UuidType       = pgOrw[UUID, W.`"uuid"`.T]

  // InetAddress works but has to be packed into a PGObject
  implicit val InetType = 
    pgOrw[PGobject, W.`"inet"`.T]
      .xmap(a => InetAddress.getByName(a.getValue)) { a =>
        val pg = new PGobject
        pg.setType("inet")
        pg.setValue(a.getHostAddress)
        pg 
      }

}

