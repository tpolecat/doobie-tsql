package tsql

trait ColumnMeta[
  -J <: Int,     // JDBC Type
  -S <: String,  // Schema Type
  -N <: Nullity, // Nullability
  -T <: String,  // Table Name (or alias)
  -C <: String   // Column Name (or alias)
]

trait ParameterMeta[
  -J <: Int,     // JDBC Type
  -S <: String,  // Schema Type
  -N <: Nullity, // Nullability
  -M <: Int      // Parameter Mode
]