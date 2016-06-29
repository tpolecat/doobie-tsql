package tsql 

import doobie.imports.{ FRS, HRS, ResultSetIO }
import java.sql.ResultSet
import scala.collection.generic.CanBuildFrom
import scalaz._, Scalaz._

/** 
 * Typeclass witnessing that a `ResultSet` can be read and accumulated into some type `A` under
 * row-level constraint `O`.
 */
case class ReadResult[+O, A](run: ResultSetIO[A])

object ReadResult extends ReadResultInstances1 {

  // Low-level constructor for optimized reads.
  protected def raw[O, A](f: ResultSet => A): ReadResult[O, A] =
    ReadResult(FRS.raw(f))

  /** If we can read a row into A we can read all remaining rows into an F[A] given a CBF */
  implicit def CBFReadResult[F[_], O, A](
    implicit r: Read[O, A],
             c: CanBuildFrom[Nothing, A, F[A]]
  ): ReadResult[O, F[A]] =
    raw { rs =>
      val b = c()
      while (rs.next)
        b += r.unsafeGet(rs, 1)
      b.result()
    }

  /** 
   * If we can read a row into (A, B) we can read all remaining rows into an F[A, B] given a CBF.
   * The common use case here would be Map[A, B].
   */
  implicit def CBFReadResult2[F[_, _], O, A, B](
    implicit r: Read[O, (A, B)],
             c: CanBuildFrom[Nothing, (A, B), F[A, B]]
  ): ReadResult[O, F[A, B]] =
    raw { rs =>
      val b = c()
      while (rs.next)
        b += r.unsafeGet(rs, 1)
      b.result()
    }

  /** 
   * If we can read a row as `A` under constraint `O`, we can read a 1-row resultset into `A`.
   * Note that this instance is not implicit.
   */
  def uniqueReadResult[O, A](
    implicit r: Read[O, A]
  ): ReadResult[O, A] =
    raw { rs =>
      if (rs.next) {
        val a = r.unsafeGet(rs, 1)
        if (rs.next) sys.error("more than one") else a
      } else sys.error("not enough")
    }

  /** 
   * If we can read a row as `A` under constraint `O`, we can read a 0 or 1-row resultset into `A`.
   * Note that this instance is not implicit.
   */
  def optionReadResult[O, A](
    implicit r: Read[O, A]
  ): ReadResult[O, Option[A]] =
    raw { rs =>
      if (rs.next) {
        val a = r.unsafeGet(rs, 1)
        if (rs.next) sys.error("more than one") else Some(a)
      } else None
    }

}

trait ReadResultInstances1 extends ReadResultInstances2 { this: ReadResult.type =>

  /** 
   * If we can read a row into A under constraint `O` we can read all remaining rows into a 
   * MonadPlus. 
   */
  implicit def MonadPlusReadResult[M[_], O, A](
    implicit r: Read[O, A],
             m: MonadPlus[M]
  ): ReadResult[O, M[A]] =
    raw { rs =>      
      var ma = m.empty[A]
      while (rs.next)
        ma = m.plus(ma, m.point(r.unsafeGet(rs, 1)))
      ma
    }

}

trait ReadResultInstances2 { this: ReadResult.type =>

  /** 
   * If we can read a row into A under constraint `O` and reduce many As to FA we can read all rows
   * into FA. 
   */
  implicit def ReducerReadResult[FA, O, A](
    implicit r: Read[O, A],
             x: Reducer[A, FA]
  ): ReadResult[O, FA] =
    raw { rs =>      
      var fa = x.zero
      while (rs.next)
        fa = x.snoc(fa, r.unsafeGet(rs, 1))
      fa
    }

}
