package tsql.spec

import tsql.spec.JdbcType._
import java.sql.ResultSet

trait AdvancedGet[J <: JdbcType, S <: String with Singleton, A] { outer =>
  type I
  val basic: BasicGet[J, I]
  val ia: I => A

  def map[B](f: A => B): AdvancedGet[J, S, B] =
    new AdvancedGet[J, S, B] {
      type I = outer.I
      val basic = outer.basic
      val ia = outer.ia andThen f
    }

}
