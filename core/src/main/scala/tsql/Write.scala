package tsql 

import doobie.imports.{ FPS, PreparedStatementIO }
import java.sql.PreparedStatement
import shapeless.{ HNil, HList, ::, Lazy, Generic, <:!<, Witness }
import scalaz.ContravariantCoyoneda
import JdbcType._

import scala.annotation.unchecked.uncheckedVariance

final class Write[+M, -A](val run: ContravariantCoyoneda[(PreparedStatement, Int, ?) => Unit, A @uncheckedVariance]) {

  def contramap[B](f: B => A): Write[M, B] =
    new Write(run.contramap(f))

  def unsafeSet(rs: PreparedStatement, n: Int, a: A): Unit =
    run.fi(rs, n, run.k(a))

  def write(n: Int, a: A): PreparedStatementIO[Unit] =
    FPS.raw(unsafeSet(_, n, a))

}


object Write extends WriteDerivations with WriteInstances {

  def lift[M, A](f: (PreparedStatement, Int, A) => Unit): Write[M, A] =
    new Write(ContravariantCoyoneda.lift[(PreparedStatement, Int, ?) => Unit, A](f))

  def basic[J <: Int] = new BasicPartiallyApplied[J]
  class BasicPartiallyApplied[J <: Int] {
    def apply[A](f: (PreparedStatement, Int, A) => Unit): Write[ParameterMeta[J, String, NoNulls, Int], A] =
      lift(f)
  }

}

trait WriteDerivations {

  implicit def option[J <: Int : Witness.Aux, S <: String, M <: Int, A](
    implicit ev: Write[ParameterMeta[J, S, NoNulls, M], A],
             no: A <:!< Option[X] forSome { type X }
  ): Write[ParameterMeta[J, S, Nullity, M], Option[A]] =
    Write.lift { 
      case (rs, n, Some(a)) => ev.unsafeSet(rs, n, a)
      case (rs, n, None)    => rs.setNull(n, JdbcType.valueOf[J]) //, JdbcType.valueOf[S]) hmm..
    }

//   // Todo: Array -> List, Vector, etc.
//   // Todo: decode a :: HNil as just a

  implicit val hnil: Write[HNil, HNil] =
    Write.lift((_, _, _) => ())

  implicit def hcons[MH, H, MT <: HList, T <: HList](
    implicit h: Write[MH, H],
             t: Lazy[Write[MT, T]]
  ): Write[MH :: MT, H :: T] = 
    Write.lift { (rs, n, l) => 
      h      .unsafeSet(rs, n,     l.head)
      t.value.unsafeSet(rs, n + 1, l.tail)
    }

  implicit def writeGeneric[M, A, B](
    implicit g: Generic.Aux[A, B], 
             r: Lazy[Write[M, B]]
   ): Write[M, A] =
    Write.lift((rs, n, a) => r.value.unsafeSet(rs, n, g.to(a)))

}

trait WriteInstances {

  // TODO: this isn't quite right! it's just copy/paste from Read
  implicit val TinyIntByte        = Write.basic[JdbcTinyInt      ](_.setByte      (_, _: Byte                 ))
  implicit val TinyIntShort       = Write.basic[JdbcTinyInt      ](_.setShort     (_, _: Short                ))
  implicit val SmallIntShort      = Write.basic[JdbcSmallInt     ](_.setShort     (_, _: Short                ))
  implicit val TinyIntInt         = Write.basic[JdbcTinyInt      ](_.setInt       (_, _: Int                  ))
  implicit val SmallIntInt        = Write.basic[JdbcSmallInt     ](_.setInt       (_, _: Int                  ))
  implicit val IntegerInt         = Write.basic[JdbcInteger      ](_.setInt       (_, _: Int                  ))
  implicit val TinyIntLong        = Write.basic[JdbcTinyInt      ](_.setLong      (_, _: Long                 ))
  implicit val SmallIntLong       = Write.basic[JdbcSmallInt     ](_.setLong      (_, _: Long                 ))
  implicit val IntegerLong        = Write.basic[JdbcInteger      ](_.setLong      (_, _: Long                 ))
  implicit val BigIntLong         = Write.basic[JdbcBigInt       ](_.setLong      (_, _: Long                 ))
  implicit val TinyIntFloat       = Write.basic[JdbcTinyInt      ](_.setFloat     (_, _: Float                ))
  implicit val SmallIntFloat      = Write.basic[JdbcSmallInt     ](_.setFloat     (_, _: Float                ))
  implicit val IntegerFloat       = Write.basic[JdbcInteger      ](_.setFloat     (_, _: Float                ))
  implicit val BigIntFloat        = Write.basic[JdbcBigInt       ](_.setFloat     (_, _: Float                ))
  implicit val RealFloat          = Write.basic[JdbcReal         ](_.setFloat     (_, _: Float                ))
  implicit val DecimalFloat       = Write.basic[JdbcDecimal      ](_.setFloat     (_, _: Float                ))
  implicit val NumericFloat       = Write.basic[JdbcNumeric      ](_.setFloat     (_, _: Float                ))
  implicit val TinyIntDouble      = Write.basic[JdbcTinyInt      ](_.setDouble    (_, _: Double               ))
  implicit val SmallIntDouble     = Write.basic[JdbcSmallInt     ](_.setDouble    (_, _: Double               ))
  implicit val IntegerDouble      = Write.basic[JdbcInteger      ](_.setDouble    (_, _: Double               ))
  implicit val BigIntDouble       = Write.basic[JdbcBigInt       ](_.setDouble    (_, _: Double               ))
  implicit val RealDouble         = Write.basic[JdbcReal         ](_.setDouble    (_, _: Double               ))
  implicit val DoubleDouble       = Write.basic[JdbcDouble       ](_.setDouble    (_, _: Double               ))
  implicit val DecimalDouble      = Write.basic[JdbcDecimal      ](_.setDouble    (_, _: Double               ))
  implicit val NumericDouble      = Write.basic[JdbcNumeric      ](_.setDouble    (_, _: Double               ))
  implicit val DecimalBigDecimal  = Write.basic[JdbcDecimal      ](_.setBigDecimal(_, _: java.math.BigDecimal ))
  implicit val NumericBigDecimal  = Write.basic[JdbcNumeric      ](_.setBigDecimal(_, _: java.math.BigDecimal ))
  implicit val BitBoolean         = Write.basic[JdbcBit          ](_.setBoolean   (_, _: Boolean              ))
  implicit val BooleanBoolean     = Write.basic[JdbcBoolean      ](_.setBoolean   (_, _: Boolean              ))
  implicit val CharString         = Write.basic[JdbcChar         ](_.setString    (_, _: String               ))
  implicit val VarCharString      = Write.basic[JdbcVarChar      ](_.setString    (_, _: String               ))
  implicit val LongVarCharString  = Write.basic[JdbcLongVarChar  ](_.setString    (_, _: String               ))
  implicit val BinaryByte         = Write.basic[JdbcBinary       ](_.setBytes     (_, _: Array[Byte]          ))
  implicit val VarBinaryByte      = Write.basic[JdbcVarBinary    ](_.setBytes     (_, _: Array[Byte]          ))
  implicit val LongVarBinaryByte  = Write.basic[JdbcLongVarBinary](_.setBytes     (_, _: Array[Byte]          ))
  implicit val DateDate           = Write.basic[JdbcDate         ](_.setDate      (_, _: java.sql.Date        ))
  implicit val TimeTime           = Write.basic[JdbcTime         ](_.setTime      (_, _: java.sql.Time        ))
  implicit val TimestampTimestamp = Write.basic[JdbcTimestamp    ](_.setTimestamp (_, _: java.sql.Timestamp   ))

}
