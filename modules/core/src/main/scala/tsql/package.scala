package doobie

import doobie.imports._
import java.sql.{ PreparedStatement, ResultSet }
import scalaz._, Scalaz._
import scalaz.stream._
import scalaz.stream.Process.{ eval, eval_, bracket }

package object tsql {

  implicit def toTsqlInterpolator(sc: StringContext): TSql.Interpolator =
    new TSql.Interpolator(sc)

  private[tsql] def liftProcess[O, A](
    create: ConnectionIO[PreparedStatement],
    prep:   PreparedStatementIO[Unit],
    exec:   PreparedStatementIO[ResultSet],
    chunkSize: Int = 512
  )(
    implicit ev: Read[O, A]
  ): Process[ConnectionIO, A] = {

    def prepared(ps: PreparedStatement): Process[ConnectionIO, PreparedStatement] =
      eval[ConnectionIO, PreparedStatement] {
        val fs = HPS.setFetchSize(chunkSize)
        FC.embed(ps, fs >> prep).map(_ => ps)
      }

    def unrolled(rs: ResultSet): Process[ConnectionIO, A] =
      ???
      // repeatEvalChunks(FC.embed(rs, HRS.getNextChunk[A](chunkSize)))

    val preparedStatement: Process[ConnectionIO, PreparedStatement] =
      bracket(create)(ps => eval_(FC.embed(ps, FPS.close)))(prepared)

    def results(ps: PreparedStatement): Process[ConnectionIO, A] =
      bracket(FC.embed(ps, exec))(rs => eval_(FC.embed(rs, FRS.close)))(unrolled)

    preparedStatement.flatMap(results)

  }


}
