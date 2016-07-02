package doobie.tsql

trait ColumnMeta[
  -J <: Int,     // JDBC Type
  -S <: String,  // Schema Type
  -N <: Nullity, // Nullability
  -T <: String,  // Table Name (or alias)
  -C <: String   // Column Name (or alias)
]

trait ParameterMeta[
  -J <: Int,   // JDBC Type
  -S <: String // Schema Type

  // N.B. Nullability is always Unknown and mode is always IN so there's no point including them. 
  // -N <: Nullity, // Nullability
  // -M <: Int      // Parameter Mode

]