package tsql.h2

import shapeless.{ Witness => W, _ }
import shapeless.test._

import doobie.imports.ConnectionIO
import org.specs2.mutable.Specification
import tsql._
import tsql.JdbcType._

object H2InferenceSpec extends Specification {
  
  type H2VARCHAR = W.`"VARCHAR"`.T
  type H2CHAR  = W.`"CHAR"`.T
  type H2INTEGER    = W.`"INTEGER"`.T

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
        ParameterMeta[JdbcVarChar, H2VARCHAR, NullableUnknown, W.`1`.T] :: HNil
      ]](a)
    }

    "infer UpdateIO[«fancy», «fancy»] for parameterized update w/ placeholders and columns" in {
      val a = tsql"delete from country where name like ? -- returning code, name"
      checkType[UpdateIO[
        ParameterMeta[JdbcVarChar, H2VARCHAR, NullableUnknown, W.`1`.T] :: HNil,
        Any :: Any :: HNil
      ]](a)
    }

    "infer QueryO[«fancy»] for non-parameterized select" in {
      val a = tsql"select name, population from city"
      checkType[QueryO[
        ColumnMeta[JdbcVarChar, H2VARCHAR, NoNulls, W.`"CITY"`.T, W.`"NAME"`      .T]  ::
        ColumnMeta[JdbcInteger, H2INTEGER, NoNulls, W.`"CITY"`.T, W.`"POPULATION"`.T]  ::
        HNil
      ]](a)
    }

    "infer QueryO[«fancy»] for parameterized select w/ interpolated arguments" in {
      val s = "irrelevant"
      val a = tsql"select name, population from city where countrycode = $s"
      checkType[QueryO[
        ColumnMeta[JdbcVarChar, H2VARCHAR, NoNulls, W.`"CITY"`.T, W.`"NAME"`      .T]  ::
        ColumnMeta[JdbcInteger, H2INTEGER, NoNulls, W.`"CITY"`.T, W.`"POPULATION"`.T]  ::
        HNil
      ]](a)
    }

    "infer QueryIO[«fancy», «fancy»] for parameterized select w/ placeholders" in {
      val a = tsql"select name, population from city where countrycode = ?"
      checkType[QueryIO[
        ParameterMeta[JdbcChar, H2CHAR, NullableUnknown, W.`1`.T] ::
        HNil,
        ColumnMeta[JdbcVarChar, H2VARCHAR, NoNulls, W.`"CITY"`.T, W.`"NAME"`      .T]  ::
        ColumnMeta[JdbcInteger, H2INTEGER, NoNulls, W.`"CITY"`.T, W.`"POPULATION"`.T]  ::
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
      val a = tsql"select ?::INTEGER as foo"
      checkType[QueryIO[
        // Interestingly the param is a varchar ... hm
        ParameterMeta[JdbcVarChar, H2VARCHAR, NullableUnknown, W.`1`.T] ::
        HNil,
        ColumnMeta[JdbcInteger, H2INTEGER, NullableUnknown, W.`""`.T, W.`"FOO"`.T] ::
        HNil]](a)
    }

  }

}