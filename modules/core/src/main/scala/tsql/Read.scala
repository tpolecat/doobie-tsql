package doobie.tsql

import doobie.imports.{ FRS, ResultSetIO }
import java.sql.ResultSet
import shapeless.{ HNil, HList, ::, Lazy, Generic, <:!<, =:!= }
import cats.free.Coyoneda
import JdbcType._
import scala.collection.generic.CanBuildFrom

/**
 * Witness that we can read a column value of type `A` while satisfying constraints `O` or any
 * more specific constraint.
 */
final class Read[+O, A](val run: Coyoneda[(ResultSet, Int) => ?, A]) {

  def map[B](f: A => B): Read[O, B] =
    new Read(run.map(f))

  def unsafeGet(rs: ResultSet, n: Int): A =
    run.k(run.fi(rs, n))

  def read(n: Int): ResultSetIO[A] =
    FRS.raw(unsafeGet(_, n))

}

object Read extends ReadInstances with ReadDerivations {

  def lift[O, A](f: (ResultSet, Int) => A): Read[O, A] =
    new Read(Coyoneda.lift[(ResultSet, Int) => ?, A](f))

  /** Construct a single-column Read asserting conformance with ColumnMeta[J, *, NoNulls]. */
  def basic[J <: Int] = new BasicPartiallyApplied[J]
  class BasicPartiallyApplied[J <: Int] {
    def apply[A](f: (ResultSet, Int) => A): Read[ColumnMeta[J, String, NoNulls, String, String], A] =
      lift(f)
  }

  /** Builder to refine phantom constraints for a single-column Read. */
  case class refine[J <: Int, S <: String, N <: Nullity, T <: String, C <: String, A](
    val done: Read[ColumnMeta[J, S, N, T, C], A]
  ) {
    def toJdbc   [JJ <: J] = asInstanceOf[refine[JJ,  S,  N,  T,  C,  A]]
    def toSchema [SS <: S] = asInstanceOf[refine[ J, SS,  N,  T,  C,  A]]
    def toNullity[NN <: N] = asInstanceOf[refine[ J,  S, NN,  T,  C,  A]]
    def toTable  [TT <: T] = asInstanceOf[refine[ J,  S,  N, TT,  C,  A]]
    def toColumn [CC <: C] = asInstanceOf[refine[ J,  S,  N,  T, CC,  A]]
  }

  /**
   * Construct a single-column Read for a given Array element type. Note that this is implemented
   * as a blind cast from `Object to `Array[A]`; success will depend on the vendor's encoding. No
   * instances are provided by default.
   */
  def array[A](implicit ev: A =:!= Nothing): Read[ColumnMeta[JdbcArray, String, NoNulls, String, String], Array[A]] =
    ArrayArray.map(_.getArray.asInstanceOf[Array[A]])

}

trait ReadInstances extends ReadInstances1 {

  // The JDBC Specification for reading basic types. One per Scala type so we have *some*
  // default, but the lower-priority ones are just as valid.
  implicit val TinyIntByte        = Read.basic[JdbcTinyInt  ](_ getByte       _)
  implicit val SmallIntShort      = Read.basic[JdbcSmallInt ](_ getShort      _)
  implicit val IntegerInt         = Read.basic[JdbcInteger  ](_ getInt        _)
  implicit val BigIntLong         = Read.basic[JdbcBigInt   ](_ getLong       _)
  implicit val RealFloat          = Read.basic[JdbcReal     ](_ getFloat      _)
  implicit val DoubleDouble       = Read.basic[JdbcDouble   ](_ getDouble     _)
  implicit val DecimalBigDecimal  = Read.basic[JdbcDecimal  ](_ getBigDecimal _)
  implicit val BitBoolean         = Read.basic[JdbcBit      ](_ getBoolean    _)
  implicit val VarCharString      = Read.basic[JdbcVarChar  ](_ getString     _)
  implicit val BinaryByte         = Read.basic[JdbcBinary   ](_ getBytes      _)
  implicit val DateDate           = Read.basic[JdbcDate     ](_ getDate       _)
  implicit val TimeTime           = Read.basic[JdbcTime     ](_ getTime       _)
  implicit val TimestampTimestamp = Read.basic[JdbcTimestamp](_ getTimestamp  _)

  // Advanced types (incomplete)
  implicit val ArrayArray   = Read.basic[JdbcArray ](_ getArray _)
  implicit val OtherObject  = Read.basic[JdbcOther ](_ getObject _)

}

trait ReadInstances1 {

  // Remaining spec-supported mappings.
  implicit val TinyIntShort       = Read.basic[JdbcTinyInt      ](_ getShort      _)
  implicit val TinyIntInt         = Read.basic[JdbcTinyInt      ](_ getInt        _)
  implicit val SmallIntInt        = Read.basic[JdbcSmallInt     ](_ getInt        _)
  implicit val TinyIntLong        = Read.basic[JdbcTinyInt      ](_ getLong       _)
  implicit val SmallIntLong       = Read.basic[JdbcSmallInt     ](_ getLong       _)
  implicit val IntegerLong        = Read.basic[JdbcInteger      ](_ getLong       _)
  implicit val TinyIntFloat       = Read.basic[JdbcTinyInt      ](_ getFloat      _)
  implicit val SmallIntFloat      = Read.basic[JdbcSmallInt     ](_ getFloat      _)
  implicit val IntegerFloat       = Read.basic[JdbcInteger      ](_ getFloat      _)
  implicit val BigIntFloat        = Read.basic[JdbcBigInt       ](_ getFloat      _)
  implicit val DecimalFloat       = Read.basic[JdbcDecimal      ](_ getFloat      _)
  implicit val NumericFloat       = Read.basic[JdbcNumeric      ](_ getFloat      _)
  implicit val TinyIntDouble      = Read.basic[JdbcTinyInt      ](_ getDouble     _)
  implicit val SmallIntDouble     = Read.basic[JdbcSmallInt     ](_ getDouble     _)
  implicit val IntegerDouble      = Read.basic[JdbcInteger      ](_ getDouble     _)
  implicit val BigIntDouble       = Read.basic[JdbcBigInt       ](_ getDouble     _)
  implicit val RealDouble         = Read.basic[JdbcReal         ](_ getDouble     _)
  implicit val DecimalDouble      = Read.basic[JdbcDecimal      ](_ getDouble     _)
  implicit val NumericDouble      = Read.basic[JdbcNumeric      ](_ getDouble     _)
  implicit val NumericBigDecimal  = Read.basic[JdbcNumeric      ](_ getBigDecimal _)
  implicit val BooleanBoolean     = Read.basic[JdbcBoolean      ](_ getBoolean    _)
  implicit val CharString         = Read.basic[JdbcChar         ](_ getString     _)
  implicit val LongVarCharString  = Read.basic[JdbcLongVarChar  ](_ getString     _)
  implicit val VarBinaryByte      = Read.basic[JdbcVarBinary    ](_ getBytes      _)
  implicit val LongVarBinaryByte  = Read.basic[JdbcLongVarBinary](_ getBytes      _)

}

trait ReadDerivations extends ReadDerivations1 {

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

  implicit def hnil[A]: Read[A, HNil] =
    Read.lift((_, _) => HNil)

  implicit def hcons[MH, H, MT <: HList, T <: HList](
    implicit h: Read[MH, H],
             t: Lazy[Read[MT, T]]
  ): Read[MH :: MT, H :: T] =
    Read.lift((rs, n) => h.unsafeGet(rs, n) :: t.value.unsafeGet(rs, n + 1))

}

trait ReadDerivations1 extends ReadDerivations2 {

  // We want to be able to derive instance Read[O, A :: ... :: HNil] where we need to expand A via
  // Generic, assuming we can derive Read[O, A0 :: A1 :: A2 :: ... :: HNil] with A's components.
  implicit def hconsG[O, H, HG <: HList, T <: HList, A <: HList](
    implicit g: Generic.Aux[H, HG],
             p: Concat.Aux[HG, T, A],
             r: Read[O, A]
  ): Read[O, H :: T] =
    Read.lift { (rs, n) =>
      val (hg, t) = p.unapply(r.unsafeGet(rs, n))
      g.from(hg) :: t
    }

  // If we can read an Array then we can read to any CBF
  implicit def arrayCBF[F[_], O, A](
    implicit   r: Read[O, Array[A]],
             cbf: CanBuildFrom[Nothing, A, F[A]],
              no: F[Int] =:!= Array[Int]
  ): Read[O, F[A]] =
    r.map(_.to[F])

}

trait ReadDerivations2 {

  implicit def readGeneric[O, A, B](
    implicit g: Generic.Aux[A, B],
             r: Lazy[Read[O, B]]
   ): Read[O, A] =
    Read.lift((rs, n) => g.from(r.value.unsafeGet(rs, n)))

  implicit def unitary[O, A](implicit ev: Read[O, A]): Read[O :: HNil, A] =
    ev.asInstanceOf[Read[O :: HNil, A]]

}
