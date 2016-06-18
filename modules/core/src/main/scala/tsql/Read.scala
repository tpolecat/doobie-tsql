package tsql 

import doobie.imports.{ FRS, ResultSetIO }
import java.sql.ResultSet
import shapeless.{ HNil, HList, ::, Lazy, Generic, <:!<, =:!= }
import shapeless.ops.hlist.Prepend
import scalaz.Coyoneda
import JdbcType._

/** 
 * Witness that we can read a column value of type `A` while satisfy constraints `M` or any
 * more specific constraint. 
 */
final class Read[+M, A](val run: Coyoneda[(ResultSet, Int) => ?, A]) {

  def map[B](f: A => B): Read[M, B] =
    new Read(run.map(f))

  def unsafeGet(rs: ResultSet, n: Int): A =
    run.k(run.fi(rs, n))

  def read(n: Int): ResultSetIO[A] =
    FRS.raw(unsafeGet(_, n))

}

object Read extends ReadDerivations with ReadInstances {

  def lift[M, A](f: (ResultSet, Int) => A): Read[M, A] =
    new Read(Coyoneda.lift[(ResultSet, Int) => ?, A](f))

  /** Construct a single-column Read asserting conformance with ColumnMeta[J, *, NoNulls]. */
  def basic[J <: Int] = new BasicPartiallyApplied[J]
  class BasicPartiallyApplied[J <: Int] {
    def apply[A](f: (ResultSet, Int) => A): Read[ColumnMeta[J, String, NoNulls, String, String], A] =
      lift(f)
  }

  // TODO: get rid of
  def advanced[J <: Int, S <: String, A](f: (ResultSet, Int) => A): Read[ColumnMeta[J, S, NoNulls, String, String], A] =
    lift(f)

}

trait ReadDerivations  extends ReadDerivations1 {

  // We can promote a non-option no-null single-column read to an option read for any nullity. The
  // coyoneda encoding allows us to do the initial column read and immediately stop if it was null,
  // rather than requiring that the continuation understand how to deal with it. This is the only
  // tricky bit.
  implicit def option[J <: Int, S <: String, T <: String, C <: String, A](
    implicit ev: Read[ColumnMeta[J, S, NoNulls, T, C], A],
             no: A <:!< Option[X] forSome { type X }
  ): Read[ColumnMeta[J, S, Nullity, T, C], Option[A]] =
    Read.lift { (rs, n) =>
      val c = ev.run
      val i = c.fi(rs, n)
      if (rs.wasNull) None else Some(c.k(i))
    }

  // Todo: Array -> List, Vector, etc.

  implicit def unitary[M, A](implicit ev: Read[M, A]): Read[M :: HNil, A] =
    ev.asInstanceOf[Read[M :: HNil, A]]

  implicit def hnil[A]: Read[A, HNil] =
    Read.lift((_, _) => HNil)

  implicit def hcons[MH, H, MT <: HList, T <: HList](
    implicit h: Read[MH, H],
             t: Lazy[Read[MT, T]]
  ): Read[MH :: MT, H :: T] = 
    Read.lift((rs, n) => h.unsafeGet(rs, n) :: t.value.unsafeGet(rs, n + 1))

  implicit def readGeneric[M, A, B](
    implicit g: Generic.Aux[A, B], 
             r: Lazy[Read[M, B]]
   ): Read[M, A] =
    Read.lift((rs, n) => g.from(r.value.unsafeGet(rs, n)))

}

trait ReadDerivations1 {

  // We want to be able to derive instance Read[M, A :: ... :: HNil] where we need to expand A via
  // Generic, assuming we can derive Read[M, A0 :: A1 :: A2 :: ... :: HNil] with A's components.
  implicit def hconsG[M, H, HG <: HList, T <: HList, O <: HList](
    implicit g: Generic.Aux[H, HG],
             p: Concat.Aux[HG, T, O],
             r: Read[M, O]
  ): Read[M, H :: T] = 
    Read.lift { (rs, n) =>
      val (hg, t) = p.unapply(r.unsafeGet(rs, n))
      g.from(hg) :: t
    }

}

trait ReadInstances {

  // The JDBC Specification for reading basic types.
  implicit val TinyIntByte        = Read.basic[JdbcTinyInt      ](_ getByte       _)
  implicit val TinyIntShort       = Read.basic[JdbcTinyInt      ](_ getShort      _)
  implicit val SmallIntShort      = Read.basic[JdbcSmallInt     ](_ getShort      _)
  implicit val TinyIntInt         = Read.basic[JdbcTinyInt      ](_ getInt        _)
  implicit val SmallIntInt        = Read.basic[JdbcSmallInt     ](_ getInt        _)
  implicit val IntegerInt         = Read.basic[JdbcInteger      ](_ getInt        _)
  implicit val TinyIntLong        = Read.basic[JdbcTinyInt      ](_ getLong       _)
  implicit val SmallIntLong       = Read.basic[JdbcSmallInt     ](_ getLong       _)
  implicit val IntegerLong        = Read.basic[JdbcInteger      ](_ getLong       _)
  implicit val BigIntLong         = Read.basic[JdbcBigInt       ](_ getLong       _)
  implicit val TinyIntFloat       = Read.basic[JdbcTinyInt      ](_ getFloat      _)
  implicit val SmallIntFloat      = Read.basic[JdbcSmallInt     ](_ getFloat      _)
  implicit val IntegerFloat       = Read.basic[JdbcInteger      ](_ getFloat      _)
  implicit val BigIntFloat        = Read.basic[JdbcBigInt       ](_ getFloat      _)
  implicit val RealFloat          = Read.basic[JdbcReal         ](_ getFloat      _)
  implicit val DecimalFloat       = Read.basic[JdbcDecimal      ](_ getFloat      _)
  implicit val NumericFloat       = Read.basic[JdbcNumeric      ](_ getFloat      _)
  implicit val TinyIntDouble      = Read.basic[JdbcTinyInt      ](_ getDouble     _)
  implicit val SmallIntDouble     = Read.basic[JdbcSmallInt     ](_ getDouble     _)
  implicit val IntegerDouble      = Read.basic[JdbcInteger      ](_ getDouble     _)
  implicit val BigIntDouble       = Read.basic[JdbcBigInt       ](_ getDouble     _)
  implicit val RealDouble         = Read.basic[JdbcReal         ](_ getDouble     _)
  implicit val DoubleDouble       = Read.basic[JdbcDouble       ](_ getDouble     _)
  implicit val DecimalDouble      = Read.basic[JdbcDecimal      ](_ getDouble     _)
  implicit val NumericDouble      = Read.basic[JdbcNumeric      ](_ getDouble     _)
  implicit val DecimalBigDecimal  = Read.basic[JdbcDecimal      ](_ getBigDecimal _)
  implicit val NumericBigDecimal  = Read.basic[JdbcNumeric      ](_ getBigDecimal _)
  implicit val BitBoolean         = Read.basic[JdbcBit          ](_ getBoolean    _)
  implicit val BooleanBoolean     = Read.basic[JdbcBoolean      ](_ getBoolean    _)
  implicit val CharString         = Read.basic[JdbcChar         ](_ getString     _)
  implicit val VarCharString      = Read.basic[JdbcVarChar      ](_ getString     _)
  implicit val LongVarCharString  = Read.basic[JdbcLongVarChar  ](_ getString     _)
  implicit val BinaryByte         = Read.basic[JdbcBinary       ](_ getBytes      _)
  implicit val VarBinaryByte      = Read.basic[JdbcVarBinary    ](_ getBytes      _)
  implicit val LongVarBinaryByte  = Read.basic[JdbcLongVarBinary](_ getBytes      _)
  implicit val DateDate           = Read.basic[JdbcDate         ](_ getDate       _)
  implicit val TimeTime           = Read.basic[JdbcTime         ](_ getTime       _)
  implicit val TimestampTimestamp = Read.basic[JdbcTimestamp    ](_ getTimestamp  _)

}
