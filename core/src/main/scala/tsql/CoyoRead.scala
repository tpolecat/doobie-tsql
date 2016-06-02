
package tsql

import spec._
import spec.JdbcType._
import java.sql.ResultSet
import doobie.imports.{ FRS, ResultSetIO }
import scalaz._, Scalaz._
import shapeless.{ Witness => W , _}
import shapeless.test._

object yetagain {

  sealed trait Nullity 
  sealed trait NoNulls         extends Nullity 
  sealed trait Nullable        extends Nullity
  sealed trait NullableUnknown extends NoNulls with Nullable

  /** 
   * Conformance means satisfaction; if CM1 <: CM2 then CM1 implies CM2. So the constraint
   * parameters are contravariant; if I satisfy CM[_, String, _] then I satisfy CM[_, String(a), _] 
   * for all a. */
  trait CM[J <: JdbcType, -S <: String, -N <: Nullity]

  /** Witness that we can read a column value of type `A` while satisfy constraints `M`. */
  final class Read[+M, A](val run: Coyoneda[(ResultSet, Int) => ?, A]) {
    
    def map[B](f: A => B): Read[M, B] =
      new Read(run.map(f))

    def unsafeGet(rs: ResultSet, n: Int): A =
      run.k(run.fi(rs, n))

    def get(n: Int): ResultSetIO[A] =
      FRS.raw(unsafeGet(_, n))

  }

  object Read extends ReadDerivations {

    def lift[M, A](f: (ResultSet, Int) => A): Read[M, A] =
      new Read(Coyoneda.lift[(ResultSet, Int) => ?, A](f))

    def basic[J <: JdbcType, A](f: (ResultSet, Int) => A): Read[CM[J, String, NoNulls], A] =
      lift(f)

    def advanced[J <: JdbcType, S <: String, A](f: (ResultSet, Int) => A): Read[CM[J, S, NoNulls], A] =
      lift(f)

  }

  trait ReadDerivations {

    // We can promote a non-option no-null single-column read to an option read for any nullity. The
    // coyoneda encoding allows us to do the initial column read and immediately stop if it was null,
    // rather than requiring that the continuation understand how to deal with it. This is the only
    // tricky bit.
    implicit def option[J <: JdbcType, S <: String, A](
      implicit ev: Read[CM[J, S, NoNulls], A],
               no: A <:!< Option[X] forSome { type X }
    ): Read[CM[J, S, Nullity], Option[A]] =
      Read.lift { (rs, n) =>
        val c = ev.run
        val i = c.fi(rs, n)
        if (rs.wasNull) None else Some(c.k(i))
      }

    implicit val hnil: Read[HNil, HNil] =
      Read.lift((_, _) => HNil)

    implicit def hcons[MH, H, MT <: HList, T <: HList](
      implicit h: Read[MH, H],
               t: Lazy[Read[MT, T]]
    ): Read[MH :: MT, H :: T] = 
      Read.lift((rs, n) => h.unsafeGet(rs, n) :: t.value.unsafeGet(rs, n + 1))

  }

  implicit class SingleColumnOps[J <: JdbcType, S <: String, N <: Nullity, A](read: Read[CM[J, S, N], A]) {
    def narrow[S0 <: S]: Read[CM[J, S0, N], A] = 
      read.asInstanceOf[Read[CM[J, S0, N], A]]
  }











  type Blah = W.`"Blah"`.T

  // FIRST TESTCASE

  implicit val rInt = Read.basic[JdbcInteger, Int](_ getInt _)

  // Check the variance of S
            implicitly[Read[CM[JdbcInteger, String, NoNulls], Int]]   // provided
            implicitly[Read[CM[JdbcInteger, Blah,   NoNulls], Int]]   // refined
  illTyped("implicitly[Read[CM[JdbcInteger, Any,    NoNulls], Int]]") // generalized

  // Check the variance of N
            implicitly[Read[CM[JdbcInteger, String, NoNulls        ], Int]]   // provided
            implicitly[Read[CM[JdbcInteger, String, NullableUnknown], Int]]   // refined
  illTyped("implicitly[Read[CM[JdbcInteger, String, Nullable       ], Int]]") // generalized

  // Option shold work with any nullity
            implicitly[Read[CM[JdbcInteger, String, Nullity        ], Option[Int]]]   // derived
            implicitly[Read[CM[JdbcInteger, String, NoNulls        ], Option[Int]]]   // refined
            implicitly[Read[CM[JdbcInteger, String, Nullable       ], Option[Int]]]   // refined
            implicitly[Read[CM[JdbcInteger, String, NullableUnknown], Option[Int]]]   // refined

  // SECOND

  implicit val rArr = Read.advanced[JdbcArray, Blah, java.sql.Array](_ getArray _).map { a =>
    a.getArray.asInstanceOf[Array[Integer]].map(_.toInt)
  }

  // Check the variance of S
            implicitly[Read[CM[JdbcArray, Blah,    NoNulls], Array[Int]]]   // provided
            implicitly[Read[CM[JdbcArray, Nothing, NoNulls], Array[Int]]]   // narrowed
  illTyped("implicitly[Read[CM[JdbcArray, String,  NoNulls], Array[Int]]]") // generalized


  implicitly[Read[String, Int] <:< Read[Any, Int]]


  // case class Woo(n: Int)

  // implicit val rWoo = rInt.map(Woo(_)).narrow[W.`"woo"`.T]

  // implicitly[Read[
  //   CM[JdbcInteger, W.`"woo"`  .T, NoNulls] ::
  //   CM[JdbcArray,   W.`"_int4"`.T, NoNulls] :: HNil,
  //   Woo :: Array[Int] :: HNil
  // ]]


  // implicitly[Read[CM[JdbcInteger, W.`"blah"`.T, NoNulls], Int]]
  // implicitly[Read[CM[JdbcArray,   W.`"_int4"`.T, NoNulls], Array[Int]]]
  // implicitly[Read[CM[JdbcArray,   W.`"_int4"`.T, NullableUnknown], Array[Int]]]





}


