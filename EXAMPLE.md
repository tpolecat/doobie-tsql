
### Quick Start

Same setup as with normal doobie, but we also import `tsql._`. We're using the same test database as the [**book of doobie**](http://tpolecat.github.io/doobie-0.2.3/00-index.html) so check that out if you want to set it up yourself.

```scala
import doobie.imports._, tsql._
```

Ok. first thing to notice is that SQL literals are checked at compile-time. If the SQL doesn't make sense to the database it won't compile.

```scala
scala> tsql"select id, name from country"
<console>:19: error: ERROR: column "id" does not exist
       tsql"select id, name from country"
                   ^
```

Ok so how does the compiler know how to do that? At compile-time it goes out and talks to the database using connect info specified via `scalacOptions`.

```scala
scalacOptions ++= Seq(
  "-Xmacro-settings:doobie.driver=org.postgresql.Driver",
  "-Xmacro-settings:doobie.connect=jdbc:postgresql:world",
  "-Xmacro-settings:doobie.user=postgres",
  "-Xmacro-settings:doobie.password="
)
```

### Inference 1. Updates

The inferred type of the `tsql` literal is potentially fancy, but will be no fancier than necessary. In the case of an unconditiona update there are no parameters and nothing returned other than a row count, so the inferred type is `ConnectionIO[Unit]`.

```scala
scala> tsql"delete from country"
res1: doobie.hi.ConnectionIO[Int] = Gosub()
```

Similarly an update with interpolated arguments is fully specified.

```scala
scala> val c = "USA"
c: String = USA

scala> tsql"delete from country where code = $c"
res2: doobie.imports.ConnectionIO[Int] = Gosub()
```

Note that a type mismatch in the SQL is a compiler error. The error message isn't very good at the moment, sorry.

```scala
scala> tsql"delete from country where population = $c"
<console>:20: error: parameter types don't match up, sorry
       tsql"delete from country where population = $c"
       ^
```

Updates can also contain *placeholders*, which results in a fancier return type.

```scala
scala> val up = tsql"delete from country where population = ?"
up: tsql.Update[shapeless.::[tsql.ParameterMeta[Int(4),String("int4"),tsql.NullableUnknown,Int(1)],shapeless.HNil]] = tsql.Update@44217db5
```

Ooooookay. So, if we unpack this type it looks like this:

```scala
Update[
  ParameterMeta[INTEGER, int4, NullableUnknown, 1] ::
  HNil
]
```

We have an `Update` value whose type argument is an HList of `ParameterMeta` types (in this case just one). What it's saying to us is:

- The JDBC type is `INTEGER`.
- The Schema type is `int4`, which is the Postgres name for a 4-byte integer.
- Nullity is unknown; we don't know if the parameter can be `NULL` or not. This is typical for parameter metadata. You need to understand your query and schema to know whether it makes sense to pass a `NULL` (which will be `None` in Scala) or an unlifted value.
- The parameter type is `IN` which happens to have code 1, which is always the case unless you're doing prepared statements, which we're not. This parameter will probably go away.

So we can now apply an argument of some type that satisfies the above constraint, and there may be many such types. But for now let's just use an `Int`, yielding a `ConnectionIO[Int]` as before.

```scala
scala> up(42)
res4: doobie.imports.ConnectionIO[Int] = Gosub()
```

Multi-parameter queries take multiple arguments. This is done via `shapeless.ProductArgs` and doesn't 


### Inference 2: Selects










