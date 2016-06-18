package tsql

import shapeless.{ HList, ProductArgs }
import doobie.imports.{ HC, HPS, FRS, PreparedStatementIO, ConnectionIO }

class QueryIO[I,O](sql: String) extends ProductArgs {
  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): QueryO[O] =
    new QueryO[O](sql, ev.write(1, a))
}

class QueryO[O](sql: String, prepare: PreparedStatementIO[Unit]) {
  def as[FA](implicit rr: ReadResult[O, FA]): ConnectionIO[FA] =
    HC.prepareStatement(sql)(prepare.flatMap(_ => HPS.executeQuery(FRS.raw(rr.run))))
}

class Update[I](sql: String) extends ProductArgs {
  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): ConnectionIO[Int] =
    HC.prepareStatement(sql)(ev.write(1, a).flatMap(_ => HPS.executeUpdate))
}


