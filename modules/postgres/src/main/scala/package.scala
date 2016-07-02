package tsql

import shapeless.{ Witness => W, _ }

import java.util.UUID
import java.net.InetAddress

import org.postgresql.util._
import org.postgresql.geometric._

import tsql.JdbcType._

package object postgres extends PGReadInstances with PGWriteInstances

trait PGReadInstances {

  private def ar[A <: AnyRef, S <: String]: Read[ColumnMeta[JdbcArray, S, NoNulls, String, String], Array[A]] = 
    Read.refine(Read.array[A]).toSchema[S].done

  private def or[A <: AnyRef, S <: String]: Read[ColumnMeta[JdbcOther, S, NoNulls, String, String], A] =
    Read.refine(Read.OtherObject).toSchema[S].done.map(_.asInstanceOf[A])

  // Arrays of Boxed Primitives (note that Byte and Short are unsupported by the driver)
  implicit val PGReadArrayBoolAsArrayBoxedBoolean  = ar[java.lang.Boolean, W.`"_bool"`  .T]
  implicit val PGReadArrayBitAsArrayBoxedBoolean   = ar[java.lang.Boolean, W.`"_bit"`   .T]
  implicit val PGReadArrayInt4AsArrayBoxedInt      = ar[java.lang.Integer, W.`"_int4"`  .T]
  implicit val PGReadArrayInt8AsArrayBoxedLong     = ar[java.lang.Long,    W.`"_int8"`  .T]
  implicit val PGReadArrayFloat4AsArrayBoxedFloat  = ar[java.lang.Float,   W.`"_float4"`.T]
  implicit val PGReadArrayFloat8AsArrayBoxedDouble = ar[java.lang.Double,  W.`"_float8"`.T]

  // Arrays of Unboxed Primitives
  implicit val PGReadArrayBoolAsArrayBoolean  = PGReadArrayBoolAsArrayBoxedBoolean .map(_.map(a => a: Boolean))
  implicit val PGReadArrayBitAsArrayBoolean   = PGReadArrayBitAsArrayBoxedBoolean  .map(_.map(a => a: Boolean))
  implicit val PGReadArrayInt4AsArrayInt      = PGReadArrayInt4AsArrayBoxedInt     .map(_.map(a => a: Int    ))
  implicit val PGReadArrayInt8AsArrayLong     = PGReadArrayInt8AsArrayBoxedLong    .map(_.map(a => a: Long   ))
  implicit val PGReadArrayFloat4AsArrayFloat  = PGReadArrayFloat4AsArrayBoxedFloat .map(_.map(a => a: Float  ))
  implicit val PGReadArrayFloat8AsArrayDouble = PGReadArrayFloat8AsArrayBoxedDouble.map(_.map(a => a: Double ))

  // Arrays of Strings
  implicit val PGReadArrayTextAsArrayString    = ar[String, W.`"_text"`  .T]
  implicit val PGReadArrayVarcharAsArrayString = ar[String, W.`"_varchar"`  .T]

  // Geometric Types
  implicit val PGReadBox     = or[PGbox,     W.`"box"`     .T]
  implicit val PGReadCircle  = or[PGcircle,  W.`"circle"`  .T]
  implicit val PGReadLseg    = or[PGlseg,    W.`"lseg"`    .T]
  implicit val PGReadPath    = or[PGpath,    W.`"path"`    .T]
  implicit val PGReadPoint   = or[PGpoint,   W.`"point"`   .T]
  implicit val PGReadPolygon = or[PGpolygon, W.`"polygon"` .T]

  // Other Types
  implicit val PGReadUuid = or[UUID, W.`"uuid"`.T]

  // InetAddress works but is packed into a PGObject
  implicit val PGReadInet = 
    or[PGobject, W.`"inet"`.T].map { a => 
      InetAddress.getByName(a.getValue)
    }

}

trait PGWriteInstances {

    private def aw[A <: AnyRef, S <: String](schemaElementType: String) = 
      Write.refine(Write.array[A](schemaElementType)).toSchema[S].done

    // TODO

}
