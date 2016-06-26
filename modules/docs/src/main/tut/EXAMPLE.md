
### Quick Start

Same setup as with normal doobie, but we also import `tsql._`. We're using the same test database as the [**book of doobie**](http://tpolecat.github.io/doobie-0.2.3/00-index.html) so check that out if you want to set it up yourself.

```tut:silent
import doobie.imports._, tsql._
```

Ok. first thing to notice is that SQL literals are checked at compile-time. If the SQL doesn't make sense to the database it won't compile. If the error from the server contains an offset we can position the carat in the right place (only works for Postgres at the moment).

```tut:fail
tsql"select id, name from country"
```

Ok so how does that work? At compile-time the compiler goes out and talks to the database using connect info specified via `scalacOptions`.

```scala
scalacOptions ++= Seq(
  "-Xmacro-settings:doobie.driver=org.postgresql.Driver",
  "-Xmacro-settings:doobie.connect=jdbc:postgresql:world",
  "-Xmacro-settings:doobie.user=postgres",
  "-Xmacro-settings:doobie.password="
)
```

### Inference 1. Updates

The inferred type of the `tsql` literal is potentially fancy, but will be no fancier than necessary. In the case of an unconditiona update the computation is fully specified: there are no parameters and nothing returned other than a row count. So the inferred type is `ConnectionIO[Unit]`.

```tut
tsql"delete from country"
```

Similarly an update with interpolated arguments is fully specified.

```tut
val c = "USA"
tsql"delete from country where code = $c"
```

Note that a type mismatch in the SQL is a compiler error. The error message isn't very good at the moment, sorry.

```tut:fail
tsql"delete from country where population = $c"
```

Updates can also contain *placeholders*, which results in a fancier return type.

```tut
val up = tsql"delete from country where population = ?"
```

Ooooookay. So, if we unpack this type it looks like this:

```scala
up: Update[
  ParameterMeta[Int(4), String("int4"), NullableUnknown, Int(1)] ::
  HNil
] = Update@8ca4fb2
```

We have an `Update` whose type argument is an HList of `ParameterMeta` types (in this case just one). What it's saying to us is, we have a single parameter with the following properties:

- The JDBC type is `INTEGER` (4).
- The Schema type is `int4`, which is the Postgres name for a 4-byte integer.
- Nullity is unknown; we don't know if the parameter can be `NULL` or not. This is typical for parameter metadata. You need to understand your query and schema to know whether it makes sense to pass a `NULL` (which will be `None` in Scala) or an unlifted value.
- The parameter type is `IN` (1), which is always the case unless you're doing prepared statements, which we're not. This parameter will probably go away.

So we can now apply an argument of some type that satisfies the above constraint, and there may be many such types. But for now let's just use an `Int`, yielding a `ConnectionIO[Int]` as before.

```tut
up(42)
```

Multi-parameter placeholder queries become `Update`s that take multiple arguments. This is done via `shapeless.ProductArgs` and doesn't have an arity limit.

```tut
val up = tsql"update country set name = ? where code = ? or population > ?"
up("foo", "bar", 123456)
```

The type above cleans up to be:

```scala
up: tsql.Update[
  ParameterMeta[Int(12), String("varchar"), NullableUnknown, Int(1)] ::
  ParameterMeta[Int( 1), String("bpchar"),  NullableUnknown, Int(1)] ::
  ParameterMeta[Int( 4), String("int4"),    NullableUnknown, Int(1)] ::
  HNil
] = tsql.Update@6b2ada94
```

### Inference 2: Selects

`SELECT` statements have fancy types describing inputs (if any) and output columns.

```tut
val q = tsql"select code, name, population from country"
```

Let's look at this type more closely:

```scala
q: tsql.QueryO[
  ColumnMeta[Int( 1), String("bpchar"),  NoNulls, String("country"), String("code")      ] ::
  ColumnMeta[Int(12), String("varchar"), NoNulls, String("country"), String("name")      ] ::
  ColumnMeta[Int( 4), String("int4"),    NoNulls, String("country"), String("population")] ::
  HNil
] = tsql.QueryO@29da9a01
```

So the type of `q` tells us:

- It's a `QueryO` which means it's a `Query` with an unknown `O`utput mapping (we know the column types but don't know the Scala type we want to map to).
- There are three columns, of JDBC type `CHAR`, `VARCHAR`, and `INTEGER` with Schema types `bpchar`, `varchar`, and `int4`, respectively.
- The columns might not be nullable, which means it might be safe to map them to unlifted (i.e., non-`Option` types).
- The columns are all from the `country` table/alias/projection and are named `code`, `name`, and `population`, respectively.

In order to use this query we need to provide an output type, which specifies both the row type mapping and the aggregate structure. The element type can be any [nested] product type whose eventual elements have primitive mappings consistent with the JDBC types (more on this later). The aggregate type can be anything with a sensible `CanBuildFrom`, `Reducer`, or `MonadPlus`.

A basic mapping might be a list of triples. Specifying this output type results in a `ConnectionIO` and we're done.

```tut
q.as[List[(String, String, Int)]]
```

We can also map case classes.

```tut
case class Country(code: String, name: String, population: Int)
q.as[Vector[Country]]
```

And nested products (in this case a pair with a case class as an element).

```tut
case class CountryInfo(name: String, population: Int)
q.as[Array[(String, CountryInfo)]]
```

If we can map a row to a pair as above, we can accumulate directly to a `Map`, which can be useful.

```tut
q.as[Map[String, CountryInfo]]
```
