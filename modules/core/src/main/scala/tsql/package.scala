package doobie

import doobie.imports._
import java.sql.{ PreparedStatement, ResultSet }
// import doobie.util.process.repeatEvalChunks
import fs2.Stream
import fs2.Stream. { eval, bracket }
import cats.implicits._

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
  ): Stream[ConnectionIO, A] = {

    def prepared(ps: PreparedStatement): Stream[ConnectionIO, PreparedStatement] =
      eval[ConnectionIO, PreparedStatement] {
        val fs = HPS.setFetchSize(chunkSize)
        FC.lift(ps, fs *> prep).map(_ => ps)
      }

    def unrolled(rs: ResultSet): Stream[ConnectionIO, A] =
      ???
      // repeatEvalChunks(FC.lift(rs, HRS.getNextChunk[A](chunkSize)))

    val preparedStatement: Stream[ConnectionIO, PreparedStatement] =
      bracket(create)(prepared, FC.lift(_, FPS.close))

    def results(ps: PreparedStatement): Stream[ConnectionIO, A] =
      bracket(FC.lift(ps, exec))(unrolled, FC.lift(_, FRS.close))

    preparedStatement.flatMap(results)

  }

}
