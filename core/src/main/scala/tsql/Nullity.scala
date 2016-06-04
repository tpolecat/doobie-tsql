package tsql

sealed trait Nullity 
sealed trait NoNulls         extends Nullity 
sealed trait Nullable        extends Nullity
sealed trait NullableUnknown extends NoNulls with Nullable
