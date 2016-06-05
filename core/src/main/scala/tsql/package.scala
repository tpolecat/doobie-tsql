package object tsql {

  implicit def toTsqlInterpolator(sc: StringContext): TSql.Interpolator =
    new TSql.Interpolator(sc)

  // TODO: lift a string literal
  // def tsql(sql: String): Any = macro something

}



