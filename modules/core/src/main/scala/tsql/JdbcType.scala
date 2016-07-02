package doobie.tsql

import java.sql.Types._
import shapeless.Witness

object JdbcType {

  def valueOf[A](implicit w: Witness.Aux[A]): A =
    w.value

  type JdbcArray         = Witness.`ARRAY`         .T
  type JdbcBigInt        = Witness.`BIGINT`        .T
  type JdbcBinary        = Witness.`BINARY`        .T
  type JdbcBit           = Witness.`BIT`           .T
  type JdbcBlob          = Witness.`BLOB`          .T
  type JdbcBoolean       = Witness.`BOOLEAN`       .T
  type JdbcChar          = Witness.`CHAR`          .T
  type JdbcClob          = Witness.`CLOB`          .T
  type JdbcDataLink      = Witness.`DATALINK`      .T
  type JdbcDate          = Witness.`DATE`          .T
  type JdbcDecimal       = Witness.`DECIMAL`       .T
  type JdbcDistinct      = Witness.`DISTINCT`      .T
  type JdbcDouble        = Witness.`DOUBLE`        .T
  type JdbcFloat         = Witness.`FLOAT`         .T
  type JdbcInteger       = Witness.`INTEGER`       .T
  type JdbcJavaObject    = Witness.`JAVA_OBJECT`   .T
  type JdbcLongnVarChar  = Witness.`LONGNVARCHAR`  .T
  type JdbcLongVarBinary = Witness.`LONGVARBINARY` .T
  type JdbcLongVarChar   = Witness.`LONGVARCHAR`   .T
  type JdbcNChar         = Witness.`NCHAR`         .T
  type JdbcNClob         = Witness.`NCLOB`         .T
  type JdbcNull          = Witness.`NULL`          .T
  type JdbcNumeric       = Witness.`NUMERIC`       .T
  type JdbcNVarChar      = Witness.`NVARCHAR`      .T
  type JdbcOther         = Witness.`OTHER`         .T
  type JdbcReal          = Witness.`REAL`          .T
  type JdbcRef           = Witness.`REF`           .T
  type JdbcRowId         = Witness.`ROWID`         .T
  type JdbcSmallInt      = Witness.`SMALLINT`      .T
  type JdbcSqlXml        = Witness.`SQLXML`        .T
  type JdbcStruct        = Witness.`STRUCT`        .T
  type JdbcTime          = Witness.`TIME`          .T
  type JdbcTimestamp     = Witness.`TIMESTAMP`     .T
  type JdbcTinyInt       = Witness.`TINYINT`       .T
  type JdbcVarBinary     = Witness.`VARBINARY`     .T
  type JdbcVarChar       = Witness.`VARCHAR`       .T

}
