package tsql 

import doobie.imports.{ FRS, HRS, ResultSetIO }
import java.sql.ResultSet
import scala.collection.generic.CanBuildFrom
import scalaz._, Scalaz._

// i can read the whole resultset into an FA
case class ReadResult[O, FA](run: ResultSetIO[FA]) // TODO: ResultSetIO[FA]

object ReadResult {

  def raw[O, A](f: ResultSet => A): ReadResult[O, A] =
    ReadResult(FRS.raw(f))

  implicit def CBFReadResult[F[_], O, A](implicit 
    r: Read[O, A],
    C: CanBuildFrom[Nothing, A, F[A]]
  ): ReadResult[O, F[A]] =
    raw { rs =>
      val b = C()
      while (rs.next)
        b += r.unsafeGet(rs, 1)
      b.result()
    }

  implicit def CBFMappyReadResult[F[_, _], O, A, B](implicit
    r: Read[O, (A, B)],
    C: CanBuildFrom[Nothing, (A, B), F[A, B]]
  ): ReadResult[O, F[A, B]] =
    raw { rs =>
      val b = C()
      while (rs.next)
        b += r.unsafeGet(rs, 1)
      b.result()
    }

  // non-implicit because it could be ambiguous ... right? maybe not
  def uniqueReadResult[O, A](implicit 
    r: Read[O, A]
  ): ReadResult[O, A] =
    raw { rs =>
      if (rs.next) {
        val a = r.unsafeGet(rs, 1)
        if (rs.next) sys.error("more than one") else a
      } else sys.error("not enough")
    }

  // non-implicit because it could be ambiguous ... could be a unique row, possibly null
  def optionReadResult[O, A](implicit 
    r: Read[O, A]
  ): ReadResult[O, Option[A]] =
    raw { rs =>
      if (rs.next) {
        val a = r.unsafeGet(rs, 1)
        if (rs.next) sys.error("more than one") else Some(a)
      } else None
    }

}
