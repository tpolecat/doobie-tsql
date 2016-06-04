package tsql

import scala.collection.JavaConverters._
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context
import shapeless._
import macrocompat.bundle
import doobie.imports._
import scalaz._, Scalaz._, scalaz.effect.IO
import java.sql.ResultSetMetaData._
import JdbcType._

final case class TSql[I, O](sql: String) {
  def list[A](implicit ev: Read[O, A]): ConnectionIO[List[A]] = // no params yet
    HC.prepareStatement(sql)(HPS.executeQuery(ev.read(1).whileM[List](HRS.next)))
}


object TSql {

  final class Interpolator(val sc: StringContext) extends AnyVal {
    def tsql(): TSql[_, _] =
      macro TSqlMacros.tsqlImpl
  }

  @bundle
  class TSqlMacros(val c: Context) {
    import c.universe._

    def setting(s: String, example: String): String =
      c.settings
       .find(_.startsWith("doobie." + s))
       .map(_.split("\\s*=\\s*", 2))
       .collect { case Array(_, v) => v }
       .getOrElse(c.abort(c.enclosingPosition, 
        s"""The tsql interpolator needs a value for doobie.$s; you can specify this in sbt like: scalacOptions += "-Xmacro-settings:doobie.$s=$example"""))

    def toType(n: Int): Type = {
      n match {
        case `columnNoNulls`         => typeOf[NoNulls]
        case `columnNullable`        => typeOf[Nullable]
        case `columnNullableUnknown` => typeOf[NullableUnknown]
      }
    }

    def packHList(ts: List[Type]): Type = 
      ts.foldRight(typeOf[HNil])((a, b) => c.typecheck(tq"shapeless.::[$a, $b]", c.TYPEmode).tpe)

    def tsqlImpl(): Tree = {

      // Construct a transactor from compiler settings
      val driver   = setting("driver",   "org.h2.Driver")
      val connect  = setting("connect",  "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
      val user     = setting("user",     "bobDole")
      val password = setting("password", "banana // or leave empty")
      val xa       = DriverManagerTransactor[IO](driver, connect, user, password)

      // The sql string itsef will be a single value.
      val q"tsql.`package`.toTsqlInterpolator(scala.StringContext.apply($sql))" = c.prefix.tree
      val Literal(Constant(sqlString: String)) = sql

      // Get column and parameter metadata
      val prog = HC.prepareStatement(sqlString)(columnMeta tuple parameterMeta)
      val (cm, pm) = prog.transact(xa).attemptSql.unsafePerformIO match {
        case -\/(e) => c.abort(c.enclosingPosition, e.getMessage)
        case \/-(d) => d
      }

      // Compute the input type
      val itype = packHList(pm.map {
        case (j, sch, n, m) =>
          c.typecheck(tq"ParameterMeta[$j, $sch, ${toType(n)}, $m]", c.TYPEmode).tpe
      })

      def singleton[A: Liftable](a: A): Type =
        c.typecheck(tq"$a").tpe

      // Compute the output type
      val otype  = packHList(cm.map {
        case (j, sch, n, t, col) => 
          c.typecheck(tq"ColumnMeta[$j, $sch, ${toType(n)}, $t, $col]", c.TYPEmode).tpe
      })

      // Done!
      q"new TSql[$itype, $otype]($sql)"

    }

    val columnMeta: PreparedStatementIO[List[(Int, String, Int, String, String)]] =
      FPS.getMetaData.map { 
        case null => Nil
        case md   =>
          (1 to md.getColumnCount).toList.map { i =>
            val j = md.getColumnType(i)
            val s = md.getColumnTypeName(i)
            val n = md.isNullable(i)
            val t = md.getTableName(i)
            val c = md.getColumnName(i)
            (j, s, n, t, c)
          }      
      }


    val parameterMeta: PreparedStatementIO[List[(Int, String, Int, Int)]] =
      FPS.getParameterMetaData.map { 
        case null => Nil
        case md   =>
          (1 to md.getParameterCount).toList.map { i =>
            val j = md.getParameterType(i)
            val s = md.getParameterTypeName(i)
            val n = md.isNullable(i)
            val m = md.getParameterMode(i)
            (j, s, n, m)
          }      
      }
  }

}


