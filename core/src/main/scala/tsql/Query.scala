package tsql

import shapeless._

class Query1[I,O](sql: String) extends ProductArgs {

  def applyProduct[A <: HList](a: A)(implicit ev: Write[I, A]): Query0[O] =
    new Query0[O] {      
    }

}

trait Query0[O] {

  def as[A](implicit ev: Read[O, A]): Query[A] =
    new Query[A] {
    }

}

trait Query[A] {
}