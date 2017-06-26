package doobie.tsql

import shapeless.{ HList, ProductArgs }
import doobie.imports._
import cats.implicits._
import fs2.Stream

class QueryIO[I,O](sql: String) extends ProductArgs {

  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): QueryO[O] =
    new QueryO[O](sql, ev.write(1, a))

}

class QueryO[O](sql: String, prepare: PreparedStatementIO[_]) {

  def this(sql: String) =
    this(sql, ().pure[PreparedStatementIO])

  def as[FA](implicit rr: ReadResult[O, FA]): ConnectionIO[FA] =
    HC.prepareStatement(sql)(prepare.flatMap(_ => HPS.executeQuery(rr.run)))

  def process[A](implicit r: Read[O, A]): Stream[ConnectionIO, A] =
    liftProcess[O,A](FC.prepareStatement(sql), prepare.void, FPS.executeQuery)

  def unique[A](implicit r: Read[O, A]): ConnectionIO[A] =
    as(ReadResult.uniqueReadResult)

  def option[A](implicit r: Read[O, A]): ConnectionIO[Option[A]] =
    as(ReadResult.optionReadResult)

}
