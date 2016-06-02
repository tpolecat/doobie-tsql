package tsql.spec

import shapeless.Witness
import tsql.spec.JdbcType._
import java.sql.ResultSet
import scalaz.Coyoneda

/** 
 * Witness that a column of JcbcType `J` *and* schema type `S` can be read into Scala type `A`. 
 * From such a value we can derive otherwise unconstrained `Read` instances.
 */
final case class AdvancedGet[J <: JdbcType, S <: String, A](run: Coyoneda[(ResultSet, Int) => ?, A]) {
  def map[B](f: A => B): AdvancedGet[J, S, B] =
    new AdvancedGet(run.map(f))
}

object AdvancedGet {

  // doesn't work, rats
  def array[S <: String](s: S)(implicit w: Witness.Aux[S]): AdvancedGet[JdbcType.JdbcArray, w.T, Object] =
    BasicGet.Array.narrow[w.T].map(_.getArray)

}