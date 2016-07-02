package tsql.postgres

import shapeless.{ Witness => W, _ }
import shapeless.test._

import doobie.imports.ConnectionIO
import org.specs2.mutable.Specification
import tsql._
import tsql.JdbcType._
import tsql.postgres._

object PGWriteInstanceSpec extends Specification {
  
  def ok[A](a: A): Boolean = true

  "Boolean Array Write" should {

    "provide a read mapping from bool[] to Array[Boolean]" in
      ok(tsql"select ? :: bool[]".apply(Array(true, false)))

    // "provide a read mapping from bool[] to Vector[Boolean]" in
    //   ok(tsql"select ? :: bool[]".apply(Vector(true, false)))

    "provide a read mapping from bit[] to Array[Boolean]" in
      ok(tsql"select ? :: bit[]".apply(Array(true, false)))

  }

  "Int Array Write Mappings" should {

    "provide a read mapping from integer[] to Array[Int]" in
      ok(tsql"select ? :: integer[]".apply(Array(1, 2)))

  }

  "Long Array Write" should {

    "provide a read mapping from bigint[] to Array[Long]" in
      ok(tsql"select ? :: bigint[]".apply(Array(1L, 2L)))

  }

  "Float Array Write" should {

    "provide a read mapping from real[] to Array[Float]" in
      ok(tsql"select ? :: real[]".apply(Array(1.23f, 4.56f)))

  }

  "Double Array Write" should {

    "provide a read mapping from float[] to Array[Double]" in
      ok(tsql"select ? :: float[]".apply(Array(1.23, 4.56)))

  }

  "String Array Write" should {

    "provide a read mapping from text[] to Array[String]" in
      ok(tsql"select ? :: text[]".apply(Array("foo", "bar")))

    "provide a read mapping from varchar[] to Array[String]" in
      ok(tsql"select ? :: varchar[]".apply(Array("foo", "bar")))

  }

}
