package doobie.tsql

import JdbcType._
import shapeless._
import shapeless.test._
import org.specs2.mutable.Specification

object ReadVarianceSpec extends Specification {

  type Blah = "blah"

  "variance for default broad Read instances" should {

    "allow reading of narrower schema types, but not wider schema types" in {
      type T[S <: String] = Read[ColumnMeta[JdbcInteger, S, NoNulls, String, String], Int]
                implicitly[T[String]]   // provided
                implicitly[T[Blah  ]]   // refined
      illTyped("implicitly[T[Any   ]]") // generalized
      true
    }

    "allow reading of narrower table types, but not wider table types" in {
      type T[S <: String] = Read[ColumnMeta[JdbcInteger, String, NoNulls, S, String], Int]
                implicitly[T[String]]   // provided
                implicitly[T[Blah  ]]   // refined
      illTyped("implicitly[T[Any   ]]") // generalized
      true
    }

    "allow reading of narrower column types, but not wider column types" in {
      type T[S <: String] = Read[ColumnMeta[JdbcInteger, String, NoNulls, String, S], Int]
                implicitly[T[String]]   // provided
                implicitly[T[Blah  ]]   // refined
      illTyped("implicitly[T[Any   ]]") // generalized
      true
    }

    "allow reading of unlifted type with refined nullity, but not generalized nullity" in {
      type T[N <: Nullity] = Read[ColumnMeta[JdbcInteger, String, N, String, String], Int]
                implicitly[T[NoNulls        ]]   // provided
                implicitly[T[NullableUnknown]]   // refined
      illTyped("implicitly[T[Nullable       ]]") // generalized
      true
    }

    "allow reading of lifted type with any nullity" in {
      type T[N <: Nullity] = Read[ColumnMeta[JdbcInteger, String, N, String, String], Option[Int]]
      implicitly[T[Nullity        ]] // derived
      implicitly[T[NoNulls        ]] // refined
      implicitly[T[Nullable       ]] // refined
      implicitly[T[NullableUnknown]] // refined
      true
    }

  }

}
