package tsql 

import doobie.imports.{ FRS, ResultSetIO }
import java.sql.ResultSet
import shapeless.{ HNil, HList, ::, Lazy, Generic }
import tsql.spec._

// @annotation.implicitNotFound("Can't map output columns to Scala type ${A}. Please ensure that type, arity, and nullity match up with the following fancy type: ${M}")
trait Read[M, A] { outer =>

  protected type I
  protected val ia: I => A
  protected val pget: (ResultSet, Int) => I

  def map[B](f: A => B): Read[M, B] =
    new Read[M, B] {
      type I = outer.I
      val ia = outer.ia andThen f
      val pget = outer.pget
    }

  def read(n: Int): ResultSetIO[A] =
    FRS.raw(unsafeRead(_, n))

  def unsafeRead(rs: ResultSet, n: Int): A =
    ia(pget(rs, n))

}

object Read extends ReadDerivations {

  def instance[M, A](pget0: (ResultSet, Int) => A): Read[M, A] =
    new Read[M, A] {
      type I = A
      val ia: I => A = identity
      val pget = pget0
    }

}

trait ReadDerivations extends ReadDerivations0 {

  implicit def basicGetInstance[J <: JdbcType, S, C, A](
    implicit ev: BasicGet[J, A]
  ): Read[ColumnMeta[J, S, ColumnNullable.NoNulls, C], A] =
    Read.instance[ColumnMeta[J, S, ColumnNullable.NoNulls, C], A](ev.unsafeRead)

  implicit def basicGetInstanceOp[J <: JdbcType, S, N <: ColumnNullable, C, A](
    implicit ev: BasicGet[J, A]
  ): Read[ColumnMeta[J, S, N, C], Option[A]] =
    Read.instance[ColumnMeta[J, S, N, C], Option[A]] { (rs, n) =>
      val a = ev.unsafeRead(rs, n)
      if (rs.wasNull) None else Some(a)
    }

  implicit def advancedGetInstance[J <: JdbcType, S <: String with Singleton, C, A](
    implicit ev: AdvancedGet[J, S, A]
  ): Read[ColumnMeta[J, S, ColumnNullable.NoNulls, C], A] =
    Read.instance[ColumnMeta[J, S, ColumnNullable.NoNulls, C], A] { (rs, n) =>
      ev.ia(ev.basic.unsafeRead(rs, n))
    }

  implicit def advancedGetInstanceOp[J <: JdbcType, S <: String with Singleton, N <: ColumnNullable, C, A](
    implicit ev: AdvancedGet[J, S, A]
  ): Read[ColumnMeta[J, S, N, C], Option[A]] =
    Read.instance[ColumnMeta[J, S, N, C], Option[A]] { (rs, n) =>
      val i = ev.basic.unsafeRead(rs, n)
      if (rs.wasNull) None else Some(ev.ia(i))
    }

}

trait ReadDerivations0 extends ReadDerivations1 {

  // If we can do a single-column get with NoNull we can also do it for NullableUnknown.
  implicit def unknownNullGet[J <: JdbcType, S <: String with Singleton, C, A](
    implicit r: Read[ColumnMeta[J, S, ColumnNullable.NoNulls, C], A]
  ): Read[ColumnMeta[J, S, ColumnNullable.NullableUnknown, C], A] =
    Read.instance[ColumnMeta[J, S, ColumnNullable.NullableUnknown, C], A](r.unsafeRead)

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


