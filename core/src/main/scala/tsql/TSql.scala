package tsql

import scala.collection.JavaConverters._
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context
import shapeless._
import macrocompat.bundle
import doobie.imports._
import scalaz._, Scalaz._, scalaz.effect.IO

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
    import spec._
    import spec.JdbcType._
    import c.universe._
    import doobie.enum.nullability._

    // TBD: derive this on the fly; I just don't know how to do it
    val jdbcTypeMap = Map[Int, Type](
      JdbcType.valueOf[JdbcArray]         -> typeOf[JdbcArray],
      JdbcType.valueOf[JdbcBigInt]        -> typeOf[JdbcBigInt],
      JdbcType.valueOf[JdbcBinary]        -> typeOf[JdbcBinary],
      JdbcType.valueOf[JdbcBit]           -> typeOf[JdbcBit],
      JdbcType.valueOf[JdbcBlob]          -> typeOf[JdbcBlob],
      JdbcType.valueOf[JdbcBoolean]       -> typeOf[JdbcBoolean],
      JdbcType.valueOf[JdbcChar]          -> typeOf[JdbcChar],
      JdbcType.valueOf[JdbcClob]          -> typeOf[JdbcClob],
      JdbcType.valueOf[JdbcDataLink]      -> typeOf[JdbcDataLink],
      JdbcType.valueOf[JdbcDate]          -> typeOf[JdbcDate],
      JdbcType.valueOf[JdbcDecimal]       -> typeOf[JdbcDecimal],
      JdbcType.valueOf[JdbcDistinct]      -> typeOf[JdbcDistinct],
      JdbcType.valueOf[JdbcDouble]        -> typeOf[JdbcDouble],
      JdbcType.valueOf[JdbcFloat]         -> typeOf[JdbcFloat],
      JdbcType.valueOf[JdbcInteger]       -> typeOf[JdbcInteger],
      JdbcType.valueOf[JdbcJavaObject]    -> typeOf[JdbcJavaObject],
      JdbcType.valueOf[JdbcLongnVarChar]  -> typeOf[JdbcLongnVarChar],
      JdbcType.valueOf[JdbcLongVarBinary] -> typeOf[JdbcLongVarBinary],
      JdbcType.valueOf[JdbcLongVarChar]   -> typeOf[JdbcLongVarChar],
      JdbcType.valueOf[JdbcNChar]         -> typeOf[JdbcNChar],
      JdbcType.valueOf[JdbcNClob]         -> typeOf[JdbcNClob],
      JdbcType.valueOf[JdbcNull]          -> typeOf[JdbcNull],
      JdbcType.valueOf[JdbcNumeric]       -> typeOf[JdbcNumeric],
      JdbcType.valueOf[JdbcNVarChar]      -> typeOf[JdbcNVarChar],
      JdbcType.valueOf[JdbcOther]         -> typeOf[JdbcOther],
      JdbcType.valueOf[JdbcReal]          -> typeOf[JdbcReal],
      JdbcType.valueOf[JdbcRef]           -> typeOf[JdbcRef],
      JdbcType.valueOf[JdbcRowId]         -> typeOf[JdbcRowId],
      JdbcType.valueOf[JdbcSmallInt]      -> typeOf[JdbcSmallInt],
      JdbcType.valueOf[JdbcSqlXml]        -> typeOf[JdbcSqlXml],
      JdbcType.valueOf[JdbcStruct]        -> typeOf[JdbcStruct],
      JdbcType.valueOf[JdbcTime]          -> typeOf[JdbcTime],
      JdbcType.valueOf[JdbcTimestamp]     -> typeOf[JdbcTimestamp],
      JdbcType.valueOf[JdbcTinyInt]       -> typeOf[JdbcTinyInt],
      JdbcType.valueOf[JdbcVarBinary]     -> typeOf[JdbcVarBinary],
      JdbcType.valueOf[JdbcVarChar]       -> typeOf[JdbcVarChar]
    )

    def setting(s: String, example: String): String =
      c.settings
       .find(_.startsWith("doobie." + s))
       .map(_.split("\\s*=\\s*", 2))
       .collect { case Array(_, v) => v }
       // .map { v => println(Console.GREEN + f"$s%-8s = $v" + Console.RESET); v }
       .getOrElse(c.abort(c.enclosingPosition, 
        s"""The tsql interpolator needs a value for doobie.$s; you can specify this in sbt like: scalacOptions += "-Xmacro-settings:doobie.$s=$example"""))

    def toType(n: Nullability): Type = {
      import spec.ColumnNullable
      n match {
        case NoNulls         => typeOf[ColumnNullable.NoNulls]
        case Nullable        => typeOf[ColumnNullable.Nullable]
        case NullableUnknown => typeOf[ColumnNullable.NullableUnknown]
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
      val prog = HC.prepareStatement(sqlString)(HPS.getColumnJdbcMeta tuple HPS.getParameterJdbcMeta)
      val (cm, pm) = prog.transact(xa).attemptSql.unsafePerformIO match {
        case -\/(e) => c.abort(c.enclosingPosition, e.getMessage)
        case \/-(d) => d
      }

      // final case class ParameterMeta(jdbcType: JdbcType, vendorTypeName: String, nullability: Nullability, mode: ParameterMode) 

      // Compute the input type
      val itype = packHList(pm.map {
        case doobie.util.analysis.ParameterMeta(j, sch, n, m) =>
          c.typecheck(tq"(${jdbcTypeMap(j.toInt)}, $sch, ${toType(n)})", c.TYPEmode).tpe
      })

      // Compute the output type
      val otype  = packHList(cm.map {
        case doobie.util.analysis.ColumnMeta(j, sch, n, col) => 
          c.typecheck(tq"ColumnMeta[${jdbcTypeMap(j.toInt)}, $sch, ${toType(n)}, $col]", c.TYPEmode).tpe
      })

      // Done!
      q"new TSql[$itype, $otype]($sql)"

    }

  }

}


