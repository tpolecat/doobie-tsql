package tsql.mysql

import shapeless.{ Witness => W, _ }
import shapeless.test._

import doobie.imports.ConnectionIO
import org.specs2.mutable.Specification
import tsql._
import tsql.JdbcType._

object PGInferenceSpec extends Specification {
  
  type MsVARCHAR = W.`"VARCHAR"`.T
  type PGbpchar  = W.`"bpchar"`.T
  type PGtext    = W.`"text"`.T
  type MsINT    = W.`"INT"`.T

  def checkType[A](a: A): Boolean = true

  "Statement Inference" should {

    "infer ConnectionIO[Int] for an non-parameterized update" in {
      val a = tsql"delete from country"
      checkType[ConnectionIO[Int]](a)
    }

    "infer ConnectionIO[Int] for parameterized update with interpolated arguments" in {
      val s = "irrelevant"
      val a = tsql"delete from country where name like $s"
      checkType[ConnectionIO[Int]](a)
    }

    "infer Update[Any] for parameterized update with placeholders" in {
      val a = tsql"delete from country where name like ?"
      checkType[Update[Any]](a)
    }

    "infer QueryO[«fancy»] for non-parameterized select" in {
      val a = tsql"select xname, population from city"
      checkType[QueryO[
        ColumnMeta[JdbcVarChar, MsVARCHAR, NoNulls, W.`"city"`.T, W.`"xname"`     .T]  ::
        ColumnMeta[JdbcInteger, MsINT,     NoNulls, W.`"city"`.T, W.`"population"`.T]  ::
        HNil
      ]](a)
    }

    "infer QueryO[«fancy»] for parameterized select with interpolated arguments" in {
      val s = "irrelevant"
      val a = tsql"select xname, population from city where countrycode = $s"
      checkType[QueryO[
        ColumnMeta[JdbcVarChar, MsVARCHAR, NoNulls, W.`"city"`.T, W.`"xname"`      .T]  ::
        ColumnMeta[JdbcInteger, MsINT,     NoNulls, W.`"city"`.T, W.`"population"`.T]  ::
        HNil
      ]](a)
    }

    "infer QueryIO[Any, «fancy»] for parameterized select with placeholders" in {
      val a = tsql"select xname, population from city where countrycode = ?"
      checkType[QueryIO[
        Any,
        ColumnMeta[JdbcVarChar, MsVARCHAR, NoNulls, W.`"city"`.T, W.`"xname"`     .T]  ::
        ColumnMeta[JdbcInteger, MsINT,     NoNulls, W.`"city"`.T, W.`"population"`.T]  ::
        HNil
      ]](a)
    }

  }

}