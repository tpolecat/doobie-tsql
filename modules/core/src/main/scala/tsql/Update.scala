package doobie.tsql

import shapeless.{ HList, ProductArgs }
import doobie.imports._
import scalaz._, Scalaz._, scalaz.stream.Process
import java.sql._

class UpdateIO[I,O](sql: String, returning: List[String]) extends ProductArgs {

  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): UpdateO[O] =
    new UpdateO(sql, returning, ev.write(1, a), HPS.executeUpdate *> FPS.getGeneratedKeys)

  def batch[F[_]: Foldable, A](fa: F[A])(implicit ev: Write[I, A]): UpdateO[O] =
    new UpdateO(sql, returning, fa.foldRight(().point[PreparedStatementIO]) { (a, b) => 
      ev.write(1, a) *> HPS.addBatch *> b
    }, HPS.executeBatch *> FPS.getGeneratedKeys)

}

class UpdateI[I](sql: String) extends ProductArgs {

  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): ConnectionIO[Int] =
    HC.prepareStatement(sql)(ev.write(1, a).flatMap(_ => HPS.executeUpdate))

  def batch[F[_]: Foldable, A](fa: F[A])(implicit ev: Write[I, A]): ConnectionIO[Int] =
    HC.prepareStatement(sql)(fa.foldRight(HPS.executeBatch) { (a, b) => 
      ev.write(1, a) *> HPS.addBatch *> b
    }) .map(_.sum)

}

class UpdateO[O](sql: String, returning: List[String], prepare: PreparedStatementIO[Unit], exec: PreparedStatementIO[ResultSet]) {

  def this(sql: String, returning: List[String]) =
    this(sql, returning, ().point[PreparedStatementIO], HPS.executeUpdate *> FPS.getGeneratedKeys)
  
  def as[FA](implicit rr: ReadResult[O, FA]): ConnectionIO[FA] =
    HC.prepareStatementS(sql, returning)(prepare *> exec >>= (FPS.lift(_, rr.run)))

  def process[A](implicit r: Read[O, A]): Process[ConnectionIO, A] =
    liftProcess[O,A](FC.prepareStatement(sql, returning.toArray), prepare, exec)

  def unique[A](implicit r: Read[O, A]): ConnectionIO[A] =
    as(ReadResult.uniqueReadResult)

  def option[A](implicit r: Read[O, A]): ConnectionIO[Option[A]] =
    as(ReadResult.optionReadResult)

}

