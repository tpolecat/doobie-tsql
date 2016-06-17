package tsql

import scala.collection.JavaConverters._
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context
import shapeless._
import macrocompat.bundle
import doobie.imports._
import scalaz._, Scalaz._, scalaz.effect.IO
import java.sql.ResultSetMetaData._
import java.sql.ParameterMetaData._
import JdbcType._

object TSql {

  /** The `tsql` string interpolator. */
  final class Interpolator(sc: StringContext) {
    object tsql extends ProductArgs {
      def applyProduct[A](a: A): Any = macro TSqlMacros.impl[A]
    }
  }

  @bundle
  class TSqlMacros(val c: Context) {
    import c.universe._

    val checkParameters = false

    val HNilType: Type = typeOf[HNil]
    val AnyType:  Type = typeOf[Any]

    /**
     * Get a setting, passed to the compiler as `-Xmacro-settings:doobie.foo=bar` (you can pass
     * `-Xmacro-settings` many times) or abort with an error message that includes an example use
     * of the setting.
     */
    def setting(s: String, example: String): String =
      c.settings
       .find(_.startsWith("doobie." + s))
       .map(_.split("\\s*=\\s*", 2))
       .collect { case Array(_, v) => v }
       .getOrElse(c.abort(c.enclosingPosition, 
        s"""macro setting not found: doobie.$s
          |
          |The tsql interpolator needs a value for doobie.$s; you can specify it in sbt like:
          |
          |  scalacOptions += "-Xmacro-settings:doobie.$s=$example"
         """.stripMargin))

    /** Translate a JDBC nullability constant to some `T <: Nullity`. */
    def jdbcNullabilityType(n: Int): Type = {
      n match { // N.B. alternatices in each pair are equal, which raises a warning
        case `columnNoNulls`         /* | `parameterNoNulls`         */ => typeOf[NoNulls]
        case `columnNullable`        /* | `parameterNullable`        */ => typeOf[Nullable]
        case `columnNullableUnknown` /* | `parameterNullableUnknown` */ => typeOf[NullableUnknown]
      }
    }

    /** Get a Transactor[IO] from macro settings. */
    def xa: Transactor[IO] = {
      val driver   = setting("driver",   "org.h2.Driver")
      val connect  = setting("connect",  "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
      val user     = setting("user",     "bobDole")
      val password = setting("password", "banana // or leave empty")
      DriverManagerTransactor[IO](driver, connect, user, password)
    }

    /** Pack a list of types into an `HList`. */
    def packHList(ts: List[Type]): Type = 
      ts.foldRight(HNilType)((a, b) => c.typecheck(tq"shapeless.::[$a, $b]", c.TYPEmode).tpe)

    /** Compute the column constraint, which will be an `HList` of `ColumnMeta`. */
    val columnConstraint: PreparedStatementIO[Type] =
      FPS.getMetaData.map { 
        case null => HNilType
        case md   =>
          packHList((1 to md.getColumnCount).toList.map { i =>
            val j = md.getColumnType(i)
            val s = md.getColumnTypeName(i)
            val n = md.isNullable(i)
            val t = md.getTableName(i)
            val k = md.getColumnName(i)
            c.typecheck(tq"ColumnMeta[$j, $s, ${jdbcNullabilityType(n)}, $t, $k]", c.TYPEmode).tpe
          })
      }

    /** Compute the parameter constraint, which will be an `HList` of `ParameterMeta`. */
    val parameterConstraint: PreparedStatementIO[(Type, Int)] =
      FPS.getParameterMetaData.map { 
        case null => (HNilType, 0)
        case md   =>
          (packHList((1 to md.getParameterCount).toList.map { i =>
            val j = md.getParameterType(i)
            val s = md.getParameterTypeName(i)
            val n = md.isNullable(i)
            val m = md.getParameterMode(i)
            c.typecheck(tq"ParameterMeta[$j, $s, ${jdbcNullabilityType(n)}, $m]", c.TYPEmode).tpe
          }), md.getParameterCount)
      } .exceptSql { e =>

        if (checkParameters) {

          c.abort(c.enclosingPosition, s"""your database is terrible
            |
            |The ${setting("driver", "")} driver does not seem to support parameter metadata, which
            |means doobie cannot typecheck your statement inputs. You can disable this warning in sbt
            |as follows (your program will still compile but statement inputs will be unchecked):
            |
            |  scalacOptions += "-Xmacro-settings:doobie.checkParameters=false"
            |
            |The underlying error was:
            |
            |  > SQLState ${e.getSQLState}
            |  > ${e.getMessage}
            """.stripMargin)

          } else (AnyType, -1).point[PreparedStatementIO]
      
      }

    /** The interpolator implementation. */
    def impl[A](a: Tree): Tree = {

      // Our SQL
      val q"tsql.`package`.toTsqlInterpolator(scala.StringContext.apply(..$parts)).tsql" = c.prefix.tree
      val sql = parts.map { case Literal(Constant(s: String)) => s } .mkString("?")

      // Get column and parameter metadata
      val prog = HC.prepareStatement(sql)(parameterConstraint tuple columnConstraint)
      val ((it, pcount), ot) = prog.transact(xa).attemptSql.unsafePerformIO match {
        case -\/(e) => c.abort(c.enclosingPosition, e.getMessage)
        case \/-(d) => d
      }

      // TODO: get rid of repetition of the it =:= HNil cases.

      // There are two cases. If there is only one string literal "part" then there are no
      // interpolated values in the query and we only need to handle `?` placeholders. Otherwise
      // we only need to handle interplolated values.
      if (parts.length == 1) {

        // Done!
        (it, ot) match {
          case (HNilType, HNilType) => q"doobie.imports.HC.prepareStatement($sql)(doobie.imports.HPS.executeUpdate)"
          case (_       , HNilType) => q"new tsql.Update[$it]($sql)"
          case (HNilType, _       ) => q"new tsql.QueryO[$ot]($sql, doobie.imports.HPS.delay(()))"
          case (_       , _       ) => q"new tsql.QueryIO[$it, $ot]($sql)"
        }

      } else {

        // If the input type and the arg types aren't aligned correctly (this can happen if you have
        // both interpolated values and `?`s) then abort with an error. We can't compute a residual
        // type because we would have to figure out the position of the placeholders.
        if (checkParameters && parts.length != pcount + 1)
          c.abort(c.enclosingPosition, "SQL literals can contain placeholders (?) or interpolated values ($x) but not both.")

        // Ok the game now is to match up `it` with `a.tpe`, so we need a Write[it, a.tpe]
        val need = c.typecheck(tq"tsql.Write[$it, ${a.tpe}]", c.TYPEmode).tpe

        // Ok now look it up
        val write = c.inferImplicitValue(need) match {
          case EmptyTree => c.abort(c.enclosingPosition, "parameter types don't match up, sorry") // TODO: normal error
          case t         => t
        }

        // Done!
        (it, ot) match {
          case (HNilType, HNilType) => q"doobie.imports.HC.prepareStatement($sql)(doobie.imports.HPS.executeUpdate)"
          case (_       , HNilType) => q"(new tsql.Update[$it]($sql)).applyProduct($a)($write)"
          case (HNilType, _       ) => q"new tsql.QueryO[$ot]($sql, doobie.imports.HPS.delay(()))"
          case (_       , _       ) => q"(new tsql.QueryIO[$it, $ot]($sql)).applyProduct($a)($write)"
        }

      }

    }
  }

}


