package tsql

trait ColumnMeta[
  -J <: JdbcType, 
  -S <: String  ,
  -N <: Nullity ,
  -T <: String  ,
  -C <: String
]
