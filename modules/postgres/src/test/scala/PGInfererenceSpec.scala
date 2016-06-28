package tsql.postgres

import shapeless.{ Witness => W, _ }
import shapeless.test._

import doobie.imports.ConnectionIO
import org.specs2.mutable.Specification
import tsql._
import tsql.JdbcType._

object PGInferenceSpec extends Specification {
  
  type PGvarchar = W.`"varchar"`.T
  type PGbpchar  = W.`"bpchar"`.T
  type PGtext    = W.`"text"`.T
  type PGint4    = W.`"int4"`.T

  def checkType[A](a: A): Boolean = true

  "Statement Inference" should {

    "infer ConnectionIO[Int] for an non-parameterized update" in {
      val a = tsql"delete from country"
      checkType[ConnectionIO[Int]](a)
    }

    "infer UpdateO[«fancy»] for an non-parameterized update w/ columns" in {
      val a = tsql"delete from country -- returning code, name"
      checkType[UpdateO[Any :: Any :: HNil]](a)
    }

    "infer ConnectionIO[Int] for parameterized update w/ interpolated arguments" in {
      val s = "irrelevant"
      val a = tsql"delete from country where name like $s"
      checkType[ConnectionIO[Int]](a)
    }


    "infer UpdateO[«fancy»] for parameterized update w/ interpolated arguments and columns" in {
      val s = "irrelevant"
      val a = tsql"delete from country where name like $s -- returning code, name"
      checkType[UpdateO[Any :: Any :: HNil]](a)
    }

    "infer UpdateI[«fancy»] for parameterized update w/ placeholders" in {
      val a = tsql"delete from country where name like ?"
      checkType[UpdateI[
        ParameterMeta[JdbcVarChar, PGtext] :: HNil
      ]](a)
    }

    "infer UpdateIO[«fancy», «fancy»] for parameterized update w/ placeholders and columns" in {
      val a = tsql"delete from country where name like ? -- returning code, name"
      checkType[UpdateIO[
        ParameterMeta[JdbcVarChar, PGtext] :: HNil,
        Any :: Any :: HNil
      ]](a)
    }

    "infer QueryO[«fancy»] for non-parameterized select" in {
      val a = tsql"select name, population from city"
      checkType[QueryO[
        ColumnMeta[JdbcVarChar, PGvarchar, NoNulls, W.`"city"`.T, W.`"name"`      .T]  ::
        ColumnMeta[JdbcInteger, PGint4,    NoNulls, W.`"city"`.T, W.`"population"`.T]  ::
        HNil
      ]](a)
    }

    "infer QueryO[«fancy»] for parameterized select w/ interpolated arguments" in {
      val s = "irrelevant"
      val a = tsql"select name, population from city where countrycode = $s"
      checkType[QueryO[
        ColumnMeta[JdbcVarChar, PGvarchar, NoNulls, W.`"city"`.T, W.`"name"`      .T]  ::
        ColumnMeta[JdbcInteger, PGint4,    NoNulls, W.`"city"`.T, W.`"population"`.T]  ::
        HNil
      ]](a)
    }

    "infer QueryIO[«fancy», «fancy»] for parameterized select w/ placeholders" in {
      val a = tsql"select name, population from city where countrycode = ?"
      checkType[QueryIO[
        ParameterMeta[JdbcChar, PGbpchar] ::
        HNil,
        ColumnMeta[JdbcVarChar, PGvarchar, NoNulls, W.`"city"`.T, W.`"name"`      .T]  ::
        ColumnMeta[JdbcInteger, PGint4,    NoNulls, W.`"city"`.T, W.`"population"`.T]  ::
        HNil
      ]](a)
    }

  }

  "Nullity Inference" should {

    "fail for untypeable placeholder" in {
      illTyped(""" tsql"select ?" """)
      true
    }

    "infer NullableUnknown for typed placeholder" in {
      val a = tsql"select ?::Int4 as foo"
      checkType[QueryIO[
        ParameterMeta[JdbcInteger, PGint4] ::
        HNil,
        ColumnMeta[JdbcInteger, PGint4, NullableUnknown, W.`""`.T, W.`"foo"`.T] ::
        HNil]](a)
    }

  }

}