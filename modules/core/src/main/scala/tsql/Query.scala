package tsql

import shapeless.{ HList, ProductArgs }
import doobie.imports.{ HC, HPS, FRS, PreparedStatementIO, ConnectionIO }
import scalaz._, Scalaz._, scalaz.stream.Process

class QueryIO[I,O](sql: String) extends ProductArgs {

  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): QueryO[O] =
    new QueryO[O](sql, ev.write(1, a))

}

class QueryO[O](sql: String, prepare: PreparedStatementIO[_]) {
  
  def as[FA](implicit rr: ReadResult[O, FA]): ConnectionIO[FA] =
    HC.prepareStatement(sql)(prepare.flatMap(_ => HPS.executeQuery(rr.run)))

  def process[A](implicit r: Read[O, A]): Process[ConnectionIO, A] =
    ???

  def unique[A](implicit r: Read[O, A]): ConnectionIO[A] =
    as(ReadResult.uniqueReadResult)

  def option[A](implicit r: Read[O, A]): ConnectionIO[Option[A]] =
    as(ReadResult.optionReadResult)

}

class Update[I](sql: String) extends ProductArgs {

  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): ConnectionIO[Int] =
    HC.prepareStatement(sql)(ev.write(1, a).flatMap(_ => HPS.executeUpdate))

  def batch[F[_]: Foldable, A](fa: F[A])(implicit ev: Write[I, A]): ConnectionIO[Int] =
    HC.prepareStatement(sql)(fa.foldRight(HPS.executeBatch) { (a, b) => 
      ev.write(1, a) *> HPS.addBatch *> b
    }) .map(_.sum)

}


