package doobie.tsql

import doobie.imports.{ FPS, PreparedStatementIO }
import java.sql.PreparedStatement
import shapeless.{ HNil, HList, ::, Lazy, Generic, =:!=, <:!<, Witness }
import scalaz.ContravariantCoyoneda
import JdbcType._

import scala.annotation.unchecked.uncheckedVariance

final class Write[+I, -A](val run: ContravariantCoyoneda[(PreparedStatement, Int, ?) => Unit, A @uncheckedVariance]) {

  def contramap[B](f: B => A): Write[I, B] =
    new Write(run.contramap(f))

  def unsafeSet(rs: PreparedStatement, n: Int, a: A): Unit =
    run.fi(rs, n, run.k(a))

  def write(n: Int, a: A): PreparedStatementIO[Unit] =
    FPS.raw(unsafeSet(_, n, a))

}

object Write extends WriteDerivations with WriteInstances {

  def lift[I, A](f: (PreparedStatement, Int, A) => Unit): Write[I, A] =
    new Write(ContravariantCoyoneda.lift[(PreparedStatement, Int, ?) => Unit, A](f))

  def basic[J <: Int] = new BasicPartiallyApplied[J]
  class BasicPartiallyApplied[J <: Int] {
    def apply[A](f: (PreparedStatement, Int, A) => Unit): Write[ParameterMeta[J, String], A] =
      lift(f)
  }

  def array[A <: AnyRef](schemaElementType: String)(
    implicit ev: A =:!= Nothing
  ): Write[ParameterMeta[JdbcArray, String], Array[A]] =
    lift { (ps, n, as) =>
      ps.setArray(n, ps.getConnection.createArrayOf(schemaElementType, as.asInstanceOf[Array[AnyRef]]))
    }

  /** Builder to refine phantom constraints for a single-column Write. */
  case class refine[J <: Int, S <: String, A](val done: Write[ParameterMeta[J, S], A]) {
    def toJdbc  [JJ <: J] = asInstanceOf[refine[JJ,  S, A]]
    def toSchema[SS <: S] = asInstanceOf[refine[ J, SS, A]]
  }

}

trait WriteDerivations {

  implicit def option[J <: Int : Witness.Aux, S <: String, A](
    implicit ev: Write[ParameterMeta[J, S], A],
             no: A <:!< Option[X] forSome { type X }
  ): Write[ParameterMeta[J, S], Option[A]] =
    Write.lift {
      case (rs, n, Some(a)) => ev.unsafeSet(rs, n, a)
      case (rs, n, None)    => rs.setNull(n, JdbcType.valueOf[J]) //, JdbcType.valueOf[S]) hmm..
    }

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

  implicit def writeGeneric[I, A, B](
    implicit g: Generic.Aux[A, B],
             r: Lazy[Write[I, B]]
   ): Write[I, A] =
    Write.lift((rs, n, a) => r.value.unsafeSet(rs, n, g.to(a)))


  // If we can write an Array then we can read to any CBF
  // doesn't work, rats ...
  // implicit def arrayCBF[F[a] <: TraversableOnce[a], I, A](
  //   implicit   r: Write[I, Array[A]],
  //            cbf: CanBuildFrom[F[A], A, Array[A]],
  //             no: F[Int] =:!= Array[Int]
  // ): Write[I, F[A]] =
  //   r.contramap(_.to[Array])

}

// N.B. Write instances are prioritized to be unique by Java type, which is reasonable because the
// specification singles out primary write targets. This allows us to infer Any for the input
// constraint when terrible databases like MySQL refuse to compute parameter metadata and get a
// reasonable Write instance; i.e., Write[Any, (Int, String, Float)] will just give us our default
// mappings and all is well.
trait WriteInstances extends WriteInstances0 {

  // Primary write targets, as defined by the JDBC specification
  implicit val TinyIntByte        = Write.basic[JdbcTinyInt      ](_.setByte      (_, _: Byte                 ))
  implicit val SmallIntShort      = Write.basic[JdbcSmallInt     ](_.setShort     (_, _: Short                ))
  implicit val IntegerInt         = Write.basic[JdbcInteger      ](_.setInt       (_, _: Int                  ))
  implicit val BigIntLong         = Write.basic[JdbcBigInt       ](_.setLong      (_, _: Long                 ))
  implicit val RealFloat          = Write.basic[JdbcReal         ](_.setFloat     (_, _: Float                ))
  implicit val DoubleDouble       = Write.basic[JdbcDouble       ](_.setDouble    (_, _: Double               ))
  implicit val NumericBigDecimal  = Write.basic[JdbcNumeric      ](_.setBigDecimal(_, _: java.math.BigDecimal ))
  implicit val BitBoolean         = Write.basic[JdbcBit          ](_.setBoolean   (_, _: Boolean              ))
  implicit val VarCharString      = Write.basic[JdbcVarChar      ](_.setString    (_, _: String               ))
  implicit val BinaryByte         = Write.basic[JdbcBinary       ](_.setBytes     (_, _: Array[Byte]          ))
  implicit val DateDate           = Write.basic[JdbcDate         ](_.setDate      (_, _: java.sql.Date        ))
  implicit val TimeTime           = Write.basic[JdbcTime         ](_.setTime      (_, _: java.sql.Time        ))
  implicit val TimestampTimestamp = Write.basic[JdbcTimestamp    ](_.setTimestamp (_, _: java.sql.Timestamp   ))

}

trait WriteInstances0 extends WriteInstances1 {

  // Secondary write targets, as defined by the JDBC specification
  implicit val BooleanBoolean     = Write.basic[JdbcBoolean      ](_.setBoolean   (_, _: Boolean              ))
  implicit val CharString         = Write.basic[JdbcChar         ](_.setString    (_, _: String               ))
  implicit val VarBinaryByte      = Write.basic[JdbcVarBinary    ](_.setBytes     (_, _: Array[Byte]          ))

}

trait WriteInstances1 {

  // Tertiary write targets, as defined by the JDBC specification
  implicit val LongVarCharString  = Write.basic[JdbcLongVarChar  ](_.setString    (_, _: String               ))
  implicit val LongVarBinaryByte  = Write.basic[JdbcLongVarBinary](_.setBytes     (_, _: Array[Byte]          ))

}
