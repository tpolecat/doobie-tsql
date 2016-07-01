package tsql.postgres

import shapeless.{ Witness => W, _ }
import shapeless.test._

import doobie.imports.ConnectionIO
import org.specs2.mutable.Specification
import tsql._
import tsql.JdbcType._
import tsql.postgres._

object PGMappingSpec extends Specification {
  
  def checkType[A](a: A): Boolean = true

  "Text Array Mappings" should {

    "properly map text[] to Array[String]" in {
      val a = tsql"select ARRAY['foo'] :: text[]".unique[Array[String]]
      checkType[ConnectionIO[Array[String]]](a)
      true
    }

    "properly map text[] to Vector[String]" in {
      val a = tsql"select ARRAY['foo'] :: text[]".unique[Vector[String]]
      checkType[ConnectionIO[Vector[String]]](a)
    }

    "properly map varchar[] to Array[String]" in {
      val a = tsql"select ARRAY['foo'] :: varchar[]".unique[Array[String]]
      checkType[ConnectionIO[Array[String]]](a)
    }

    "properly map varchar[] to Vector[String]" in {
      val a = tsql"select ARRAY['foo'] :: varchar[]".unique[Vector[String]]
      checkType[ConnectionIO[Vector[String]]](a)
    }

  }

  "Int Array Mappings" should {

    "properly map integer[] to Array[Int]" in {
      val a = tsql"select ARRAY[1,2,3] :: integer[]".unique[Array[Int]]
      checkType[ConnectionIO[Array[Int]]](a)
      true
    }

    "properly map integer[] to Vector[Int]" in {
      val a = tsql"select ARRAY[1,2,3] :: integer[]".unique[Vector[Int]]
      checkType[ConnectionIO[Vector[Int]]](a)
    }

  }

}