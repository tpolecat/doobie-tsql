// package tsql

// import JdbcType._
// import java.sql.ResultSet
// import scalaz._, Scalaz._
// import shapeless.{ Witness => W , _}
// import shapeless.test._

// object ReadSpec {

//   // These are just compilation tests.

//   implicit class SingleColumnOps[J <: JdbcType, S <: String, N <: Nullity, A](read: Read[ColumnMeta[J, S, N], A]) {

//     // We can narrow the schema
//     def schema[S0 <: String](implicit ev: S =:= String): Read[ColumnMeta[J, S0, N], A] = 
//       read.asInstanceOf[Read[ColumnMeta[J, S0, N], A]]

//   }



//   type Blah = W.`"Blah"`.T

//   // FIRST TESTCASE

//   implicit val rInt = Read.basic[JdbcInteger, Int](_ getInt _)

//   // Check the variance of S
//             implicitly[Read[ColumnMeta[JdbcInteger, String, NoNulls], Int]]   // provided
//             implicitly[Read[ColumnMeta[JdbcInteger, Blah,   NoNulls], Int]]   // refined
//   illTyped("implicitly[Read[ColumnMeta[JdbcInteger, Any,    NoNulls], Int]]") // generalized

//   // Check the variance of N
//             implicitly[Read[ColumnMeta[JdbcInteger, String, NoNulls        ], Int]]   // provided
//             implicitly[Read[ColumnMeta[JdbcInteger, String, NullableUnknown], Int]]   // refined
//   illTyped("implicitly[Read[ColumnMeta[JdbcInteger, String, Nullable       ], Int]]") // generalized

//   // Option shold work with any nullity
//             implicitly[Read[ColumnMeta[JdbcInteger, String, Nullity        ], Option[Int]]]   // derived
//             implicitly[Read[ColumnMeta[JdbcInteger, String, NoNulls        ], Option[Int]]]   // refined
//             implicitly[Read[ColumnMeta[JdbcInteger, String, Nullable       ], Option[Int]]]   // refined
//             implicitly[Read[ColumnMeta[JdbcInteger, String, NullableUnknown], Option[Int]]]   // refined

//   // SECOND

//   implicit val rArr = Read.advanced[JdbcArray, Blah, java.sql.Array](_ getArray _).map { a =>
//     a.getArray.asInstanceOf[Array[Integer]].map(_.toInt)
//   }

//   // Check the variance of S
//             implicitly[Read[ColumnMeta[JdbcArray, Blah,    NoNulls], Array[Int]]]   // provided
//             implicitly[Read[ColumnMeta[JdbcArray, Nothing, NoNulls], Array[Int]]]   // narrowed
//   illTyped("implicitly[Read[ColumnMeta[JdbcArray, String,  NoNulls], Array[Int]]]") // generalized


//   implicitly[Read[String, Int] <:< Read[Any, Int]]


//   case class Woo(n: Int)

//   implicit val rWoo = rInt.map(Woo(_)).schema[W.`"woo"`.T]

//   // implicitly[Read[
//   //   ColumnMeta[JdbcInteger, W.`"woo"`  .T, NoNulls] ::
//   //   ColumnMeta[JdbcArray,   W.`"_int4"`.T, NoNulls] :: HNil,
//   //   Woo :: Array[Int] :: HNil
//   // ]]


//   // implicitly[Read[ColumnMeta[JdbcInteger, W.`"blah"`.T, NoNulls], Int]]
//   // implicitly[Read[ColumnMeta[JdbcArray,   W.`"_int4"`.T, NoNulls], Array[Int]]]
//   // implicitly[Read[ColumnMeta[JdbcArray,   W.`"_int4"`.T, NullableUnknown], Array[Int]]]


// }