package tsql

/** 
 * A symmetric Read/Write pair. Presence of an implicit pair implies presence of both elements, but
 * not vice-versa. This data type is just a convenience for deriving symmetric pairs and should
 * *not* be used as a constraint.
 */
case class ReadWrite[I, O, A](read: Read[O, A], write: Write[I, A]) {
  def xmap[B](f: A => B)(g: B => A): ReadWrite[I, O, B] =
    ReadWrite(read.map(f), write.contramap(g))
}
