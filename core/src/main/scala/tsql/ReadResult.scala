package tsql 

import java.sql.ResultSet
import scala.collection.generic.CanBuildFrom

// i can read the whole resultset into an FA
case class ReadResult[O, FA](run: ResultSet => FA)

object ReadResult {

  // If we can read A under constraint O then we can read the entire resultset into F[A] given a
  // CanBuildFrom
  implicit def CBFReadResult[F[_], O, A](implicit 
    r: Read[O, A],
    C: CanBuildFrom[Nothing, A, F[A]]
  ): ReadResult[O, F[A]] =
    ReadResult { rs =>
      val b = C()
      while (rs.next)
        b += r.unsafeGet(rs, 1)
      b.result()
    }

  // If we can read A under constraint O then we can read a 1-element resultset into A
  implicit def UniqueReadResult[O, A](implicit 
    r: Read[O, A]
  ): ReadResult[O, A] =
    ReadResult { rs =>
      if (rs.next) {
        val a = r.unsafeGet(rs, 1)
        if (rs.next) sys.error("more than one") else a
      } else sys.error("not enough")
    }

  // If we can read A under constraint O then we can read a 0 or 1-element resultset into A
  implicit def OptionReadResult[O, A](implicit 
    r: Read[O, A]
  ): ReadResult[O, Option[A]] =
    ReadResult { rs =>
      if (rs.next) {
        val a = r.unsafeGet(rs, 1)
        if (rs.next) sys.error("more than one") else Some(a)
      } else None
    }

}