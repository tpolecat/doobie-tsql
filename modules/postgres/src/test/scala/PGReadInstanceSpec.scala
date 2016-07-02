package tsql.postgres

import shapeless.{ Witness => W, _ }
import shapeless.test._

import doobie.imports.ConnectionIO
import org.specs2.mutable.Specification
import tsql._
import tsql.JdbcType._
import tsql.postgres._

object PGReadInstanceSpec extends Specification {
  
  def ok[A](a: A): Boolean = true

  "Boolean Array Read" should {

    "provide a read mapping from bool[] to Array[Boolean]" in
      ok(tsql"select ARRAY[true, false]".unique[Array[Boolean]])    

    "provide a read mapping from bool[] to Vector[Boolean]" in
      ok(tsql"select ARRAY[true, false]".unique[Vector[Boolean]])    

    "provide a read mapping from bit[] to Array[Boolean]" in
      ok(tsql"select ARRAY[1, 0, 1] :: bit[]".unique[Array[Boolean]])    

    "provide a read mapping from bit[] to Vector[Boolean]" in
      ok(tsql"select ARRAY[1, 0, 1] :: bit[]".unique[Vector[Boolean]])    

  }

  "Int Array Read Mappings" should {

    "provide a read mapping from integer[] to Array[Int]" in
      ok(tsql"select ARRAY[1,2,3] :: integer[]".unique[Array[Int]])    

    "provide a read mapping from integer[] to Vector[Int]" in
      ok(tsql"select ARRAY[1,2,3] :: integer[]".unique[Vector[Int]])    

  }

  "Long Array Read" should {

    "provide a read mapping from bigint[] to Array[Long]" in
      ok(tsql"select ARRAY[1,2,3] :: bigint[]".unique[Array[Long]])    

    "provide a read mapping from bigint[] to Vector[Long]" in
      ok(tsql"select ARRAY[1,2,3] :: bigint[]".unique[Vector[Long]])    

  }

  "Float Array Read" should {

    "provide a read mapping from real[] to Array[Float]" in
      ok(tsql"select ARRAY[1.23, 4.56] :: real[]".unique[Array[Float]])    

    "provide a read mapping from real[] to Vector[Float]" in
      ok(tsql"select ARRAY[1.23, 4.56] :: real[]".unique[Vector[Float]])    

  }

  "Double Array Read" should {

    "provide a read mapping from float[] to Array[Double]" in
      ok(tsql"select ARRAY[1.23, 4.56] :: float[]".unique[Array[Double]])    

    "provide a read mapping from float[] to Vector[Double]" in
      ok(tsql"select ARRAY[1.23, 4.56] :: float[]".unique[Vector[Double]])    

  }

  "String Array Read" should {

    "provide a read mapping from text[] to Array[String]" in
      ok(tsql"select ARRAY['foo'] :: text[]".unique[Array[String]])    

    "provide a read mapping from text[] to Vector[String] via CBF" in
      ok(tsql"select ARRAY['foo'] :: text[]".unique[Vector[String]])    

    "provide a read mapping from varchar[] to Array[String]" in
      ok(tsql"select ARRAY['foo'] :: varchar[]".unique[Array[String]])    

    "provide a read mapping from varchar[] to Vector[String] via CBF" in
      ok(tsql"select ARRAY['foo'] :: varchar[]".unique[Vector[String]])    

  }

}
