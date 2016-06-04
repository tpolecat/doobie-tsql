import shapeless._

import doobie.imports._
import scalaz._, Scalaz._, scalaz.effect.IO

object mdtest {

  val xa = DriverManagerTransactor[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres",
    ""
  )

  val prog: ConnectionIO[Unit] =
    HC.prepareStatement("select 42, ARRAY[1,2,3], c.name as cname, * from country c, city") {
      FPS.getMetaData.map { md =>
        (1 to md.getColumnCount).toList.map { i =>
          val j = md.getColumnType(i)
          val s = md.getColumnTypeName(i)
          val n = md.isNullable(i)
          val t = md.getTableName(i)
          val c = md.getColumnName(i)
          val l = md.getColumnLabel(i)
          val p = (md.getPrecision(i), md.getScale(i))
          val x = md.isSigned(i)
          val o = md.getColumnClassName(i)
          println((j, s, n, t, c, l, p, x, o))
        }      
      }
    }
  
  def main(args: Array[String]): Unit =
    prog.transact(xa).unsafePerformIO

}

