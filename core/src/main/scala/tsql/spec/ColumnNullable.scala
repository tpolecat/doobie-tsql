package tsql.spec

import java.sql.ResultSetMetaData._
import shapeless.Witness

sealed trait ColumnNullable {
  type W <: Int with Singleton
}

object ColumnNullable {

  type Aux[W0] = ColumnNullable { type W = W0 }

  def valueOf[A <: ColumnNullable](implicit w: Witness.Aux[A#W]): A#W =
    w.value

  type NoNulls         = ColumnNullable.Aux[Witness.`columnNoNulls`        .T]
  type Nullable        = ColumnNullable.Aux[Witness.`columnNullable`       .T]
  type NullableUnknown = ColumnNullable.Aux[Witness.`columnNullableUnknown`.T]

}


