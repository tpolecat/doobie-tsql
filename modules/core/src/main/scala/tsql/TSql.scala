package doobie.tsql

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context
import shapeless.{ HNil, ProductArgs }
import macrocompat.bundle
import doobie.imports._
import scalaz._, Scalaz._, scalaz.effect.IO
import java.sql.ResultSetMetaData._

object TSql {

  val checkParametersKey = "checkParameters"

  /** The `tsql` string interpolator. */
  final class Interpolator(sc: StringContext) {
    object tsql extends ProductArgs {
      def applyProduct[A](a: A): Any = macro TSqlMacros.impl[A]
    }
  }

  @bundle
  class TSqlMacros(val c: Context) {
    import c.universe._

    val checkParameters = setting(checkParametersKey).forall(_ != "false")

    val HNilType: Type = typeOf[HNil]
    val AnyType:  Type = typeOf[Any]

    def setting(s: String): Option[String] =
      c.settings
       .find(_.startsWith("doobie." + s))
       .map(_.split("\\s*=\\s*", 2))
       .collect { case Array(_, v) => v }

    /**
     * Get a setting, passed to the compiler as `-Xmacro-settings:doobie.foo=bar` (you can pass
     * `-Xmacro-settings` many times) or abort with an error message that includes an example use
     * of the setting.
     */
    def settingOrFail(s: String, example: String): String =
      setting(s).getOrElse(c.abort(c.enclosingPosition,
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
    def xa: Transactor[IO, _] = {
      val driver   = settingOrFail("driver",   "org.h2.Driver")
      val connect  = settingOrFail("connect",  "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
      val user     = settingOrFail("user",     "bobDole")
      val password = settingOrFail("password", "banana // or leave empty")
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
            c.typecheck(tq"ParameterMeta[$j, $s]", c.TYPEmode).tpe
          }), md.getParameterCount)
      } .exceptSql { e =>

        if (checkParameters && e.getSQLState == "S1C00") {

          c.abort(c.enclosingPosition, s"""your database is terrible
            |
            |The ${setting("driver").getOrElse("")} driver does not seem to support parameter metadata, which
            |means doobie cannot typecheck your statement inputs. You can disable this warning in sbt
            |as follows (your program will still compile but statement inputs will be unchecked):
            |
            |  scalacOptions += "-Xmacro-settings:doobie.${checkParametersKey}=false"
            |
            |The underlying error was:
            |
            |  > SQLState ${e.getSQLState}
            |  > ${e.getMessage}
            """.stripMargin)

          } else (AnyType, -1).point[PreparedStatementIO]

      }

    // // unpack an hlist
    // def unpack(t: Type): List[Type] =
    //   t.typeArgs match {
    //     case h :: t :: Nil => h :: unpack(t)
    //     case Nil => Nil
    //   }

    /** The interpolator implementation. */
    def impl[A](a: Tree): Tree = {

      // Our SQL
      val q"doobie.tsql.`package`.toTsqlInterpolator(scala.StringContext.apply(..$parts)).tsql" = c.prefix.tree
      val sql = parts.map {
        case Literal(Constant(s: String)) => s
        case tree => c.abort(c.enclosingPosition, s"Implementation error, inexhaustive match. Expected Literal(Constant(...)), found $tree")
      } .mkString("?")

      // println("** types are " + unpack(a.tpe))
      // println("** parts are " + parts)
      //
      // import \&/._
      // val parts2 = parts.map { case Literal(Constant(s: String)) => s }
      // val xx = (parts2 align unpack(a.tpe)).map {
      //   case Both(s, ConstantType(Constant(k: String))) if s.endsWith("#") => s.init + k
      //   case Both(s, t) => s + "?"
      //   case This(s)    => s
      // }
      // println(xx.mkString)

      // For returning columns we use a comment. If returned columns are specified then the
      // statement must not have any output columns otherwise. The resulting QueryIO/QueryO will
      // have an output type of Any.
      val retPat = "-- returning"
      val returning: List[String] =
        sql.lines.toList.fproduct(_.indexOf(retPat)).find(_._2 >= 0).foldMap { case (s, i) =>
          s.substring(i + retPat.length).split(",").toList.map(_.trim)
        }
      val rot = packHList(returning.as(AnyType)) // returning 'out' type can check arity and nothing else

      // TODO: remove comment from sql since it messes with postgres (at least)

      // Source positon for offset `n` characters into the SQL itself. If the database reports an
      // error at pos N this method will give you the source position where the carat should go.
      def sqlPos(n: Int): Option[Position] = {
        def go(n: Int, ts: List[Tree]): Option[Position] = {
          ts match {
            case Nil => None
            case (t @ Literal(Constant(s: String))) :: ts =>
              val p = t.pos
              if (n < s.length) Some(p.withPoint(p.point + n))
              else go(n - s.length - 2, ts)
            case tree :: _ => c.abort(c.enclosingPosition, s"Implementation error, inexhaustive match. Expected Literal(Constant(...)), found $tree")
          }
        }
        go(n, parts)
      }

      // Get column and parameter metadata. Note that preparing the statement with `returning`
      // columns doesn't do anything; they don't appear in metadata for some reason.
      val prog = HC.prepareStatement(sql)(parameterConstraint tuple columnConstraint)
      val ((it, pcount), ot) = prog.transact(xa).attemptSql.unsafePerformIO match {
        case -\/(e) => //c.abort(c.enclosingPosition, e.getMessage)
          val pos = parts.head.pos
          SqlExceptions.pos(e.getMessage) match {
            case Some((s, n)) => c.abort(sqlPos(n).getOrElse(pos), s)
            case None         => c.abort(pos, e.getMessage)
          }
        case \/-(d) => d
      }

      // TODO: get rid of repetition of the it =:= HNil cases.

      // There are two cases. If there is only one string literal "part" then there are no
      // interpolated values in the query and we only need to handle `?` placeholders. Otherwise
      // we only need to handle interplolated values.
      if (parts.length == 1) {

        // Done!
        (it, ot, rot) match {
          case (HNilType, HNilType, HNilType) => q"doobie.hi.connection.prepareStatement($sql)(doobie.imports.HPS.executeUpdate)"
          case (HNilType, HNilType, _       ) => q"new doobie.tsql.UpdateO[$rot]($sql, $returning)"
          case (_       , HNilType, HNilType) => q"new doobie.tsql.UpdateI[$it]($sql)"
          case (_       , HNilType, _       ) => q"new doobie.tsql.UpdateIO[$it,$rot]($sql, $returning)"
          case (HNilType, _       , HNilType) => q"new doobie.tsql.QueryO[$ot]($sql)"
          case (_       , _       , HNilType) => q"new doobie.tsql.QueryIO[$it, $ot]($sql)"
          case _ =>
            c.abort(c.enclosingPosition, "A statement that returns columns cannot also have a `-- returning ...` pragma.")
        }

      } else {

        // If the input type and the arg types aren't aligned correctly (this can happen if you have
        // both interpolated values and `?`s) then abort with an error. We can't compute a residual
        // type because we would have to figure out the position of the placeholders.
        if (checkParameters && parts.length != pcount + 1)
          c.abort(c.enclosingPosition, "SQL literals can contain placeholders (?) or interpolated values ($x) but not both.")

        // Ok the game now is to match up `it` with `a.tpe`, so we need a Write[it, a.tpe]
        val need = c.typecheck(tq"doobie.tsql.Write[$it, ${a.tpe}]", c.TYPEmode).tpe

        // Ok now look it up
        val write = c.inferImplicitValue(need) match {
          case EmptyTree => c.abort(parts.head.pos, "parameter types don't match up, sorry") // TODO: normal error
          case t         => t
        }

        // Done!
       (it, ot, rot) match {
          case (HNilType, HNilType, HNilType) => q"doobie.hi.connection.prepareStatement($sql)(doobie.imports.HPS.executeUpdate)"
          case (HNilType, HNilType, _       ) => q"new doobie.tsql.UpdateO[$rot]($sql, $returning)"
          case (_       , HNilType, HNilType) => q"new doobie.tsql.UpdateI[$it]($sql).applyProduct($a)($write)"
          case (_       , HNilType, _       ) => q"new doobie.tsql.UpdateIO[$it,$rot]($sql, $returning).applyProduct($a)($write)"
          case (HNilType, _       , HNilType) => q"new doobie.tsql.QueryO[$ot]($sql)"
          case (_       , _       , HNilType) => q"new doobie.tsql.QueryIO[$it, $ot]($sql).applyProduct($a)($write)"
          case _ =>
            c.abort(c.enclosingPosition, "A statement that returns columns cannot also have a `-- returning ...` pragma.")
        }

      }

    }
  }

}


object SqlExceptions {

  /**
   * Try to get the error position from a SQLException message, returning the residual message
   * without the position information, and the position as a character offset.
   */
  def pos(e: String): Option[(String, Int)] =
    pgPos(e) // orElse ...

  // org.postgresql.util.PSQLException includes the 1-indexed position in the last line
  def pgPos(e: String): Option[(String, Int)] = {
    val Re = """\s+Position: (\d+)""".r
    e.lines.toList.reverse match {
      case Re(sp) :: rinit => Some((rinit.reverse.mkString("\n"), sp.toInt - 1))
      case _                  => None
    }
  }

}
