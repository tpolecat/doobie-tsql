import shapeless._

import doobie.imports._
import doobie.enum.nullability._
import scalaz._, Scalaz._, scalaz.effect.IO

object first {

  import tsql._
  import tsql.spec._

  val xa = DriverManagerTransactor[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres",
    ""
  )

  implicit val IntArray: AdvancedGet[JdbcType.JdbcArray, Witness.`"_int4"`.T, Array[Int]] =
    BasicGet.Array.narrow[Witness.`"_int4"`.T].map { a =>
      a.getArray.asInstanceOf[Array[Integer]].map(_.intValue)
    }

  def main(args: Array[String]): Unit = {

    val q = tsql"""
      SELECT name, population, 42, ARRAY[1, 2, 3]
      FROM   country
    """


    val cio = q.list[String :: Long :: Int :: Array[Int] :: HNil]

    val prog = cio.transact(xa).flatMap(as => as.map(_.toString).traverse(IO.putStrLn))

    prog.unsafePerformIO

    // q.as[(String, Int, Option[Int])]
    // q.as[Boo]

    // TODO: nesting

  }

}

