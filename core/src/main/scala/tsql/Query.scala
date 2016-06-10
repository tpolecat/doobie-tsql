package tsql

import shapeless._
import doobie.imports._
import java.sql.ResultSet
import scalaz.syntax.apply._
import scalaz.Reducer
import scala.collection.generic.CanBuildFrom
import scalaz.stream.Process

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

// ok this is looking like it needs to be generalized

class QueryIO[I,O](sql: String) extends ProductArgs {

  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): QueryO[O] =
    new QueryO[O](sql, ev.write(1, a))
  
  def of[A](implicit ev: Read[O, A]): QueryI[I, A] =
    new QueryI[I, A](sql, ev.unsafeGet(_, 1))

  def in[A](implicit ev: Write[I, A]): A => QueryO[O] = a =>
    new QueryO[O](sql, ev.write(1, a))

}

class QueryI[I, O](sql: String, unsafeGet: ResultSet => O) extends ProductArgs {

  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): Query[O] =
    new Query[O](sql, ev.write(1, a), unsafeGet)

  def in[A](implicit ev: Write[I, A]): A => Query[O] = a =>
    new Query[O](sql, ev.write(1, a), unsafeGet)

}

class QueryO[O](sql: String, prepare: PreparedStatementIO[Unit]) {
  def of[A](implicit ev: Read[O, A]): Query[A] =
    new Query[A](sql, prepare, ev.unsafeGet(_, 1))
}

class Query[A](sql: String, prepare: PreparedStatementIO[Unit], unsafeGet: ResultSet => A) {

  def map[B](f: A => B): Query[B] =
    new Query(sql, prepare, unsafeGet andThen f)

  // accumulate results with a CBF
  def as[F[_]](implicit C: CanBuildFrom[Nothing, A, F[A]]): ConnectionIO[F[A]] =
    HC.prepareStatement(sql)(prepare *> HPS.executeQuery(FRS.raw { rs =>
      val b = C()
      while (rs.next)
        b += unsafeGet(rs)
      b.result()
    }))

  // accumulate results with a Reducer
  def reduce[F[_]](implicit R: Reducer[A, F[A]]): ConnectionIO[F[A]] =
    HC.prepareStatement(sql)(prepare *> HPS.executeQuery(FRS.raw { rs =>
      def go(fa: F[A]): F[A] =
        if (rs.next) go(R.snoc(fa, unsafeGet(rs)))
        else fa
      go(R.zero)
    }))

  def process: Process[ConnectionIO, A] = ???

  // assert exactly one
  def unique: ConnectionIO[A] = ???

  // assert at most one
  def option: ConnectionIO[Option[A]] = ???

}


class Update[I](sql: String) extends ProductArgs {

  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): ConnectionIO[Int] =
    HC.prepareStatement(sql)(ev.write(1, a) *> HPS.executeUpdate)

  def in[A](implicit ev: Write[I, A]): A => ConnectionIO[Int] = a =>
    HC.prepareStatement(sql)(ev.write(1, a) *> HPS.executeUpdate)

}


