import shapeless._

import doobie.imports._
import doobie.enum.nullability._
import scalaz.effect.IO
import tsql._
import JdbcType._
import scalaz._, Scalaz._

object first {

  val xa = DriverManagerTransactor[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres",
    ""
  )

  implicit val StringArray =
    Read.advanced[JdbcType.JdbcArray, Witness.`"_text"`.T, Array[String]] { (rs, n) =>
      rs.getArray(n).getArray.asInstanceOf[Array[String]]
    }

  def main(args: Array[String]): Unit = {

    val q = tsql"""
      SELECT name, population, 42, ARRAY['x', 'y']
      FROM   country
      WHERE  code <> ?
    """

    case class Country(name: String, population: Long, const: Int, arr: Array[String])

    val cio = q("USA").of[Country].as[List]

    val cio2 = q.of[Country].apply("USA").as[List]


    val prog = cio.transact(xa).flatMap(as => as.map(_.toString).traverseU(IO.putStrLn))

    prog.unsafePerformIO


  }

}


