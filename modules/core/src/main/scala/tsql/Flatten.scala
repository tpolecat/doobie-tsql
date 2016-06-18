package tsql

import shapeless._

/** 
 * Typeclass witnessing that P and S can be concatenated to form Out, *and* that Out can
 * be split into P and S. This is a generalization of shapeless.ops.hlist.Prepend
 */

trait Concat[P <: HList, S <: HList] {
  type Out <: HList 
  def apply(p: P, s: S): Out
  def unapply(o: Out): (P, S)
}


trait LowestPriorityPrepend {

  type Aux[P <: HList, S <: HList, Out0 <: HList] =
    Concat[P, S] { 
      type Out = Out0 
    }

  implicit def hlistPrepend[PH, PT <: HList, S <: HList](
    implicit pt : Concat[PT, S]
  ): Concat.Aux[PH :: PT, S, PH :: pt.Out] =
    new Concat[PH :: PT, S] {
      
      type Out = PH :: pt.Out

      def apply(prefix : PH :: PT, suffix : S): Out = 
        prefix.head :: pt(prefix.tail, suffix)

      def unapply(o: Out): (PH :: PT, S) = {
        val (p, s) = pt.unapply(o.tail)
        (o.head :: p, s)
      }

    }
}

trait LowPriorityPrepend extends LowestPriorityPrepend {

  implicit def hnilPrepend0[P <: HList]: Aux[P, HNil, P] =
    new Concat[P, HNil] {
      type Out = P
      def apply(prefix : P, suffix : HNil): P = prefix
      def unapply(o: Out): (P, HNil) = (o, HNil)
    }
}

object Concat extends LowPriorityPrepend {

  def apply[P <: HList, S <: HList](implicit prepend: Concat[P, S]): Aux[P, S, prepend.Out] = 
    prepend

  implicit def hnilPrepend1[S <: HList]: Aux[HNil, S, S] =
    new Concat[HNil, S] {
      type Out = S
      def apply(prefix : HNil, suffix : S): S = suffix
      def unapply(o: Out): (HNil, S) = (HNil, o)
    }

}