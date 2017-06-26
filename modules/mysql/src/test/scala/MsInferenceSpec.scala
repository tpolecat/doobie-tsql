package doobie.tsql.mysql

import shapeless._

import doobie.imports.ConnectionIO
import org.specs2.mutable.Specification
import doobie.tsql._
import doobie.tsql.JdbcType._

object PGInferenceSpec extends Specification {

  type MsVARCHAR = "VARCHAR"
  type PGbpchar  = "bpchar"
  type PGtext    = "text"
  type MsINT     = "INT"

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

    "infer UpdateI[Any] for parameterized update w/ placeholders" in {
      val a = tsql"delete from country where name like ?"
      checkType[UpdateI[Any]](a)
    }


    "infer UpdateIO[any, «fancy»] for parameterized update w/ placeholders and columns" in {
      val a = tsql"delete from country where name like ? -- returning code, name"
      checkType[UpdateIO[
        Any,
        Any :: Any :: HNil
      ]](a)
    }

    "infer QueryO[«fancy»] for non-parameterized select" in {
      val a = tsql"select xname, population from city"
      checkType[QueryO[
        ColumnMeta[JdbcVarChar, MsVARCHAR, NoNulls, "city", "xname"     ]  ::
        ColumnMeta[JdbcInteger, MsINT,     NoNulls, "city", "population"]  ::
        HNil
      ]](a)
    }

    "infer QueryO[«fancy»] for parameterized select w/ interpolated arguments" in {
      val s = "irrelevant"
      val a = tsql"select xname, population from city where countrycode = $s"
      checkType[QueryO[
        ColumnMeta[JdbcVarChar, MsVARCHAR, NoNulls, "city", "xname"     ]  ::
        ColumnMeta[JdbcInteger, MsINT,     NoNulls, "city", "population"]  ::
        HNil
      ]](a)
    }

    "infer QueryIO[Any, «fancy»] for parameterized select w/ placeholders" in {
      val a = tsql"select xname, population from city where countrycode = ?"
      checkType[QueryIO[
        Any,
        ColumnMeta[JdbcVarChar, MsVARCHAR, NoNulls, "city", "xname"     ]  ::
        ColumnMeta[JdbcInteger, MsINT,     NoNulls, "city", "population"]  ::
        HNil
      ]](a)
    }

  }

}
