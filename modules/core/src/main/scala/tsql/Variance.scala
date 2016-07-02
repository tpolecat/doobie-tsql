package doobie.tsql.stuff

import shapeless.{ Witness => W }

object Variance {

  type implies[A,B] = A <:< B

  // Ok this is just to demonstrate the variance we're using here, in a simpler way so we can be
  // sure it makes sense. The gist is that constraints are contravariant: if I satisfy a broad
  // constraint then I satisfy any more specific constraint as well.

  // A constraint does nothing but flip the variance
  trait Constraint[-S <: String]

  // If we can read a supertype it means we can read all subtypes
  implicitly[Constraint[String] implies Constraint[W.`"Foo"`.T]]

  // Witness that we can read an A under constraints C
  trait Reader[+C, A]

  // If we can read under a broad constraint we can read under a more specific one
  implicitly[Reader[Constraint[String], Int] implies Reader[Constraint[W.`"Foo"`.T], Int]]


  //---- Note that writing has the same relationshp.

  // If we can write a supertype it means we can write all subtypes
  implicitly[Constraint[String] implies Constraint[W.`"Foo"`.T]]

  // Witness that we can write an A under constraints C
  trait Writer[+C, A]

  // If we can write under a broad constraint we can write under a more specific one
  implicitly[Writer[Constraint[String], Int] implies Writer[Constraint[W.`"Foo"`.T], Int]]


}