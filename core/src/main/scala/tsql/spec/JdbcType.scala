package tsql.spec

import java.sql.Types._
import shapeless.Witness

sealed trait JdbcType {
  type W <: Int with Singleton
}

object JdbcType {

  type Aux[W0] = JdbcType { type W = W0 }

  def valueOf[A <: JdbcType](implicit w: Witness.Aux[A#W]): A#W =
    w.value

  type JdbcArray         = JdbcType.Aux[Witness.`ARRAY`         .T]
  type JdbcBigInt        = JdbcType.Aux[Witness.`BIGINT`        .T]
  type JdbcBinary        = JdbcType.Aux[Witness.`BINARY`        .T]
  type JdbcBit           = JdbcType.Aux[Witness.`BIT`           .T]
  type JdbcBlob          = JdbcType.Aux[Witness.`BLOB`          .T]
  type JdbcBoolean       = JdbcType.Aux[Witness.`BOOLEAN`       .T]
  type JdbcChar          = JdbcType.Aux[Witness.`CHAR`          .T]
  type JdbcClob          = JdbcType.Aux[Witness.`CLOB`          .T]
  type JdbcDataLink      = JdbcType.Aux[Witness.`DATALINK`      .T]
  type JdbcDate          = JdbcType.Aux[Witness.`DATE`          .T]
  type JdbcDecimal       = JdbcType.Aux[Witness.`DECIMAL`       .T]
  type JdbcDistinct      = JdbcType.Aux[Witness.`DISTINCT`      .T]
  type JdbcDouble        = JdbcType.Aux[Witness.`DOUBLE`        .T]
  type JdbcFloat         = JdbcType.Aux[Witness.`FLOAT`         .T]
  type JdbcInteger       = JdbcType.Aux[Witness.`INTEGER`       .T]
  type JdbcJavaObject    = JdbcType.Aux[Witness.`JAVA_OBJECT`   .T]
  type JdbcLongnVarChar  = JdbcType.Aux[Witness.`LONGNVARCHAR`  .T]
  type JdbcLongVarBinary = JdbcType.Aux[Witness.`LONGVARBINARY` .T]
  type JdbcLongVarChar   = JdbcType.Aux[Witness.`LONGVARCHAR`   .T]
  type JdbcNChar         = JdbcType.Aux[Witness.`NCHAR`         .T]
  type JdbcNClob         = JdbcType.Aux[Witness.`NCLOB`         .T]
  type JdbcNull          = JdbcType.Aux[Witness.`NULL`          .T]
  type JdbcNumeric       = JdbcType.Aux[Witness.`NUMERIC`       .T]
  type JdbcNVarChar      = JdbcType.Aux[Witness.`NVARCHAR`      .T]
  type JdbcOther         = JdbcType.Aux[Witness.`OTHER`         .T]
  type JdbcReal          = JdbcType.Aux[Witness.`REAL`          .T]
  type JdbcRef           = JdbcType.Aux[Witness.`REF`           .T]
  type JdbcRowId         = JdbcType.Aux[Witness.`ROWID`         .T]
  type JdbcSmallInt      = JdbcType.Aux[Witness.`SMALLINT`      .T]
  type JdbcSqlXml        = JdbcType.Aux[Witness.`SQLXML`        .T]
  type JdbcStruct        = JdbcType.Aux[Witness.`STRUCT`        .T]
  type JdbcTime          = JdbcType.Aux[Witness.`TIME`          .T]
  type JdbcTimestamp     = JdbcType.Aux[Witness.`TIMESTAMP`     .T]
  type JdbcTinyInt       = JdbcType.Aux[Witness.`TINYINT`       .T]
  type JdbcVarBinary     = JdbcType.Aux[Witness.`VARBINARY`     .T]
  type JdbcVarChar       = JdbcType.Aux[Witness.`VARCHAR`       .T]

}
