package doobie

import doobie.imports._
import doobie.util.process._
import java.sql.{ PreparedStatement, ResultSet }
import scalaz._, Scalaz._
import scalaz.stream._

package object tsql {

  implicit def toTsqlInterpolator(sc: StringContext): TSql.Interpolator =
    new TSql.Interpolator(sc)

  // borrowed from hi.connection
  private[tsql] def liftProcess[O, A](
    create: ConnectionIO[PreparedStatement],
    prep:   PreparedStatementIO[Unit],
    exec:   PreparedStatementIO[ResultSet])(
    implicit ev: Read[O, A]
  ): Process[ConnectionIO, A] = {

    // val preparedStatement: Process[ConnectionIO, PreparedStatement] =
    //   resource(
    //     create)(ps =>
    //     FC.lift(ps, FPS.close))(ps =>
    //     Option(ps).point[ConnectionIO]).take(1) // note
    //
    // def results(ps: PreparedStatement): Process[ConnectionIO, A] =
    //   resource(
    //     FC.lift(ps, exec))(rs =>
    //     FC.lift(rs, FRS.close))(rs =>
    //     FC.lift(rs, FRS.next.flatMap {
    //       case false => Option.empty[A].point[ResultSetIO]
    //       case true  => ev.read(1).map(Option(_))
    //     }))
    //
    // for {
    //   ps <- preparedStatement
    //   _  <- Process.eval(FC.lift(ps, prep))
    //   a  <- results(ps)
    // } yield a

    ???

  }

}
