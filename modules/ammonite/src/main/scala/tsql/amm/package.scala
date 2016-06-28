package tsql

import pprint._
import shapeless._
import java.sql.Types._

package object amm {





  implicit def q1TPrint[I: TPrint, O: TPrint]: TPrint[QueryIO[I, O]] =
    TPrint.lambda { implicit cfg =>
      val ts = TPrint.literal("QueryIO").render(cfg)
      val (i, o) = (tprint[I], tprint[O])
      val i0 = brack(realign(i, 4), 2)
      val o0 = brack(realign(o, 5), 2)
      s"QueryIO[\n$i0,\n$o0\n]"
    }

  implicit def qoTPrint[O: TPrint]: TPrint[QueryO[O]] =
    TPrint.lambda { implicit cfg =>
      val ts = TPrint.literal("QueryO").render(cfg)
      val o = tprint[O]
      val o0 = brack(realign(o, 5), 2)
      s"QueryO[\n$o0\n]"
    }

  implicit def paramererMetaTPrint[
    J <: Int     : TPrint, // JDBC Type
    S <: String  : TPrint  // Schema Type
  ]: TPrint[ParameterMeta[J, S]] =
    TPrint.lambda { implicit cfg =>
      val (j, s) = (jdbc[J], tprint[S])
      val cm = TPrint.literal("ParameterMeta").render(cfg)
      s"""$cm[$j, $s]"""
    }

  implicit def paramhconsBlahblah[
    J <: Int     : TPrint, // JDBC Type
    S <: String  : TPrint, // Schema Type
    L <: HList   : TPrint
  ]: TPrint[ParameterMeta[J, S] :: L] =
    TPrint.lambda { implicit cfg =>
      "  " + tprint[ParameterMeta[J, S]] + " ::\n" ++ tprint[L]
    }

  implicit def columnMetaTPrint[
    J <: Int     : TPrint, // JDBC Type
    S <: String  : TPrint, // Schema Type
    N <: Nullity : TPrint, // Nullability
    T <: String  : TPrint, // Table Name (or alias)
    C <: String  : TPrint  // Column Name (or alias)
  ]: TPrint[ColumnMeta[J, S, N, T, C]] =
    TPrint.lambda { implicit cfg =>
      val (j, s, n, t, c) = (jdbc[J], tprint[S], tprint[N], tprint[T], tprint[C])
      val cm = TPrint.literal("ColumnMeta").render(cfg)
      s"""$cm[$j, $s, $n, $t, $c]"""
    }

  implicit def hconsBlahblah[
    J <: Int     : TPrint, // JDBC Type
    S <: String  : TPrint, // Schema Type
    N <: Nullity : TPrint, // Nullability
    T <: String  : TPrint, // Table Name (or alias)
    C <: String  : TPrint, // Column Name (or alias)
    L <: HList   : TPrint
  ]: TPrint[ColumnMeta[J, S, N, T, C] :: L] =
    TPrint.lambda { implicit cfg =>
      "  " + tprint[ColumnMeta[J, S, N, T, C]] + " ::\n" ++ tprint[L]
    }

  implicit val hnilBlah: TPrint[HNil] =
    TPrint.literal("  HNil")

  def jdbc[J <: Int: TPrint]: String = 
    tprint[J].toInt match {
      case ARRAY         =>  "ARRAY"
      case BIGINT        =>  "BIGINT"
      case BINARY        =>  "BINARY"
      case BIT           =>  "BIT"
      case BLOB          =>  "BLOB"
      case BOOLEAN       =>  "BOOLEAN"
      case CHAR          =>  "CHAR"
      case CLOB          =>  "CLOB"
      case DATALINK      =>  "DATALINK"
      case DATE          =>  "DATE"
      case DECIMAL       =>  "DECIMAL"
      case DISTINCT      =>  "DISTINCT"
      case DOUBLE        =>  "DOUBLE"
      case FLOAT         =>  "FLOAT"
      case INTEGER       =>  "INTEGER"
      case JAVA_OBJECT   =>  "JAVA_OBJECT"
      case LONGNVARCHAR  =>  "LONGNVARCHAR"
      case LONGVARBINARY =>  "LONGVARBINARY"
      case LONGVARCHAR   =>  "LONGVARCHAR"
      case NCHAR         =>  "NCHAR"
      case NCLOB         =>  "NCLOB"
      case NULL          =>  "NULL"
      case NUMERIC       =>  "NUMERIC"
      case NVARCHAR      =>  "NVARCHAR"
      case OTHER         =>  "OTHER"
      case REAL          =>  "REAL"
      case REF           =>  "REF"
      case ROWID         =>  "ROWID"
      case SMALLINT      =>  "SMALLINT"
      case SQLXML        =>  "SQLXML"
      case STRUCT        =>  "STRUCT"
      case TIME          =>  "TIME"
      case TIMESTAMP     =>  "TIMESTAMP"
      case TINYINT       =>  "TINYINT"
      case VARBINARY     =>  "VARBINARY"
      case VARCHAR       =>  "VARCHAR"
      case n             => s"UNKNOWN(n)"
    }

    // pad by adding spaces after commas where needed .. this is really brittle
    def realign(s: String, expected: Int): String = {
      val lines = s.lines.toList
      val cols  = lines.map { s =>
        val chunks = s.split(",")
        if (chunks.length == expected) Right(chunks.init.map(_ + ",").toVector :+ chunks.last)
        else Left(s)
      }
      val widths = cols.collect { case Right(ss) => ss.map(_.length) } .transpose.map(_.max)
      val format = widths.map(n => "%-" + n + "s").mkString
      cols.map {
        case Left(s)   => s
        case Right(ss) => String.format(format, ss: _*)
      } .mkString("\n")
    }


    // pad by adding spaces before ]
    def brack(s: String, expected: Int): String = {
      val lines = s.lines.toList
      val cols  = lines.map { s =>
        val chunks = s.split("]")
        if (chunks.length == expected) Right(chunks.init.toVector :+ "]" + chunks.last)
        else Left(s)
      }
      val widths = cols.collect { case Right(ss) => ss.map(_.length) } .transpose.map(_.max)
      val format = widths.map(n => "%-" + n + "s").mkString
      cols.map {
        case Left(s)   => s
        case Right(ss) => String.format(format, ss: _*)
      } .mkString("\n")
    }

}
