package tsql

import JdbcType._
import java.sql.ResultSet
import scalaz._, Scalaz._
import shapeless.{ Witness => W , _}
import shapeless.test._
import org.specs2.mutable.Specification

object ReadDerivationSpec extends Specification {

  type M[A <: Int] = ColumnMeta[A, String, NoNulls, String, String]

  "read instance derivation (flat)" should {

    "support HLists" in {
      implicitly[Read[M[JdbcInteger] :: M[JdbcVarChar] :: HNil, Int :: String :: HNil]]
      true
    }

    "support tuples" in {
      implicitly[Read[M[JdbcInteger] :: M[JdbcVarChar] :: HNil, (Int, String)]]
      true
    }
  
    "support generic products" in {
      case class Foo(n: Int, s: String)
      implicitly[Read[M[JdbcInteger] :: M[JdbcVarChar] :: HNil, Foo]]
      true
    }

    "support single-element products" in {
      case class Foo(n: Int)
      implicitly[Read[M[JdbcInteger] :: HNil, Foo]]
      true
    }

    "support standalone single elements" in {
      implicitly[Read[M[JdbcInteger] :: HNil, Int]]
      true
    }

    "support nested tuples (!)" in {
      implicitly[
        Read[
          M[JdbcInteger] :: M[JdbcInteger] :: M[JdbcVarChar] :: HNil, 
          (Int, (Int, String))
        ]
      ]
      true
    }

  }

}