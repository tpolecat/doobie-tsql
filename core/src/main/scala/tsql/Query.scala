package tsql

import shapeless._
import doobie.imports._
import java.sql.ResultSet
import scalaz.syntax.apply._
import scala.collection.generic.CanBuildFrom

/*
                                   .of
  tsql"select ... ?" --> QueryIO -------> QueryI
                           |                |
                           | .apply         | .apply
                           |                |
                           v       .of      v
  tsql"select ..." ----> QueryO  -------> Query
                                            |
                                            | .list, .option, etc.
                                            |
                                            v
  tsql"update ..." ------------------> ConnectionIO
                                            ^
                                            |
                                            | .apply
                                            |
  tsql"update ... ?" ------------------> UpdateI


 */


class QueryIO[I,O](sql: String) extends ProductArgs {
  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): QueryO[O] =
    new QueryO[O](sql, ev.write(1, a))
  def of[A](implicit ev: Read[O, A]): QueryI[I, A] =
    new QueryI[I, A](sql, ev.unsafeGet(_, 1))
}

class QueryI[I, O](sql: String, unsafeGet: ResultSet => O) extends ProductArgs {
  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): Query[O] =
    new Query[O](sql, ev.write(1, a), unsafeGet)
}

class QueryO[O](sql: String, prepare: PreparedStatementIO[Unit]) {
  def of[A](implicit ev: Read[O, A]): Query[A] =
    new Query[A](sql, prepare, ev.unsafeGet(_, 1))
}

class Query[A](sql: String, prepare: PreparedStatementIO[Unit], unsafeGet: ResultSet => A) {

  def map[B](f: A => B): Query[B] =
    new Query(sql, prepare, unsafeGet andThen f)

  // TODO: new set of HI operations
  def as[F[_]](implicit C: CanBuildFrom[Nothing, A, F[A]]): ConnectionIO[F[A]] =
    HC.prepareStatement(sql)(prepare *> HPS.executeQuery(FRS.raw { rs =>
      val b = C()
      while (rs.next)
        b += unsafeGet(rs)
      b.result()
    }))

}