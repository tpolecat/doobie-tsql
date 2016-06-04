import shapeless._

import doobie.imports._
import doobie.enum.nullability._
import scalaz._, Scalaz._, scalaz.effect.IO

object first {

  import tsql._

  val xa = DriverManagerTransactor[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres",
    ""
  )

  implicit val IntArray =
    Read.advanced[JdbcType.JdbcArray, Witness.`"_int4"`.T, Array[Int]] { (rs, n) =>
      rs.getArray(n).getArray.asInstanceOf[Array[Integer]].map(_.intValue)
    }

  implicit val StringArray =
    Read.advanced[JdbcType.JdbcArray, Witness.`"_text"`.T, Array[String]] { (rs, n) =>
      rs.getArray(n).getArray.asInstanceOf[Array[String]]
    }

  def main(args: Array[String]): Unit = {

    val q = tsql"""
      SELECT name, population, 42, ARRAY['x', 'y']
      FROM   country
    """


    val cio1 = q.list[String :: Long :: Int :: Array[String] :: HNil]
    val cio2 = q.list[(String, Long, Int, Array[String])]

    // tsql"select 1".list[Int] // not yet

    // val prog = cio.transact(xa).flatMap(as => as.map(_.toString).traverse(IO.putStrLn))

    // prog.unsafePerformIO

    // q.as[(String, Int, Option[Int])]
    // q.as[Boo]

    // TODO: nesting

  }

}

