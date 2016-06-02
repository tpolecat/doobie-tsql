package tsql 

import doobie.imports.{ FRS, ResultSetIO }
import java.sql.ResultSet
import shapeless.{ HNil, HList, ::, Lazy, Generic }
import tsql.spec._
import tsql.spec.ColumnNullable._
import scalaz.Coyoneda

// @annotation.implicitNotFound("Can't map output columns to Scala type ${A}. Please ensure that type, arity, and nullity match up with the following fancy type: ${M}")
final case class Read[M, A](run: Coyoneda[(ResultSet, Int) => ?, A]) {

  def map[B](f: A => B): Read[M, B] =
    new Read(run.map(f))

  def read(n: Int): ResultSetIO[A] =
    FRS.raw(unsafeRead(_, n))

  def unsafeRead(rs: ResultSet, n: Int): A =
    run.k(run.fi(rs, n))

}

object Read extends ReadDerivations {

  def instance[M, A](fi: (ResultSet, Int) => A): Read[M, A] =
    new Read(Coyoneda.lift[(ResultSet, Int) => ?, A](fi))

}

trait ReadDerivations extends ReadDerivations0 {

  implicit def basicGetInstance[J <: JdbcType, S, C, A](
    implicit ev: BasicGet[J, A]
  ): Read[ColumnMeta[J, S, NoNulls, C], A] =
    Read[ColumnMeta[J, S, NoNulls, C], A](ev.run)

  implicit def basicGetInstanceOp[J <: JdbcType, S, N <: ColumnNullable, C, A](
    implicit ev: BasicGet[J, A]
  ): Read[ColumnMeta[J, S, N, C], Option[A]] =
    Read[ColumnMeta[J, S, N, C], Option[A]] {
      Coyoneda.lift[(ResultSet, Int) => ?, Option[A]] { (rs: ResultSet, n: Int) =>
        val c = ev.run
        val i = c.fi(rs, n)
        if (rs.wasNull) None else Some(c.k(i))
      }
    }

  implicit def advancedGetInstance[J <: JdbcType, S <: String with Singleton, C, A](
    implicit ev: AdvancedGet[J, S, A]
  ): Read[ColumnMeta[J, S, NoNulls, C], A] =
    Read[ColumnMeta[J, S, NoNulls, C], A](ev.run)

  implicit def advancedGetInstanceOp[J <: JdbcType, S <: String with Singleton, N <: ColumnNullable, C, A](
    implicit ev: AdvancedGet[J, S, A]
  ): Read[ColumnMeta[J, S, N, C], Option[A]] =
    Read[ColumnMeta[J, S, N, C], Option[A]] {
      Coyoneda.lift[(ResultSet, Int) => ?, Option[A]] { (rs: ResultSet, n: Int) =>
        val c = ev.run
        val i = c.fi(rs, n)
        if (rs.wasNull) None else Some(c.k(i))
      }
    }

}

trait ReadDerivations0 extends ReadDerivations1 {

  // If we can do a single-column get with NoNull we can also do it for NullableUnknown.
  implicit def unknownNullGet[J <: JdbcType, S <: String with Singleton, C, A](
    implicit r: Read[ColumnMeta[J, S, NoNulls, C], A]
  ): Read[ColumnMeta[J, S, NullableUnknown, C], A] =
    Read[ColumnMeta[J, S, NullableUnknown, C], A](r.run)

}

trait ReadDerivations1 {

  implicit val readHNil: Read[HNil, HNil] =
    Read.instance[HNil, HNil]((_, _) => HNil)

  implicit def readHCons[H, A, T <: HList, B <: HList](
    implicit h: Read[H, A], 
             t: Lazy[Read[T, B]]
   ): Read[H :: T, A :: B] =
    Read.instance[H :: T, A :: B]((rs, n) => h.unsafeRead(rs, n) :: t.value.unsafeRead(rs, n + 1))
    
  implicit def readGeneric[M, A, B](
    implicit g: Generic.Aux[A, B], 
             r: Lazy[Read[M, B]]
   ): Read[M, A] =
    Read.instance[M, A]((rs, n) => g.from(r.value.unsafeRead(rs, n)))

}


