
### Quick Start

Same setup as with normal doobie, but we also import `tsql._`. We're using the same test database as the [**book of doobie**](http://tpolecat.github.io/doobie-0.2.3/00-index.html) so check that out if you want to set it up yourself.

```scala
import doobie.imports._, tsql._
```

Ok. first thing to notice is that SQL literals are checked at compile-time. If the SQL doesn't make sense to the database it won't compile. If the error from the server contains an offset we can position the carat in the right place (only works for Postgres at the moment).

```scala
scala> tsql"select id, name from country"
RETURNING List()
<console>:19: error: ERROR: column "id" does not exist
       tsql"select id, name from country"
                   ^
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

```scala
scala> tsql"delete from country"
RETURNING List()
res1: doobie.hi.ConnectionIO[Int] = Gosub()
```

Similarly an update with interpolated arguments is fully specified.

```scala
scala> val c = "USA"
c: String = USA

scala> tsql"delete from country where code = $c"
RETURNING List()
res2: doobie.imports.ConnectionIO[Int] = Gosub()
```

Note that a type mismatch in the SQL is a compiler error. The error message isn't very good at the moment, sorry.

```scala
scala> tsql"delete from country where population = $c"
RETURNING List()
<console>:20: error: parameter types don't match up, sorry
       tsql"delete from country where population = $c"
            ^
```

Updates can also contain *placeholders*, which results in a fancier return type.

```scala
scala> val up = tsql"delete from country where population = ?"
RETURNING List()
up: tsql.Update[shapeless.::[tsql.ParameterMeta[Int(4),String("int4"),tsql.NullableUnknown,Int(1)],shapeless.HNil]] = tsql.Update@75d33053
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

```scala
scala> up(42)
res4: doobie.imports.ConnectionIO[Int] = Gosub()
```

Multi-parameter placeholder queries become `Update`s that take multiple arguments. This is done via `shapeless.ProductArgs` and doesn't have an arity limit.

```scala
scala> val up = tsql"update country set name = ? where code = ? or population > ?"
RETURNING List()
up: tsql.Update[shapeless.::[tsql.ParameterMeta[Int(12),String("varchar"),tsql.NullableUnknown,Int(1)],shapeless.::[tsql.ParameterMeta[Int(1),String("bpchar"),tsql.NullableUnknown,Int(1)],shapeless.::[tsql.ParameterMeta[Int(4),String("int4"),tsql.NullableUnknown,Int(1)],shapeless.HNil]]]] = tsql.Update@7fa1477d

scala> up("foo", "bar", 123456)
res5: doobie.imports.ConnectionIO[Int] = Gosub()
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

```scala
scala> val q = tsql"select code, name, population from country"
RETURNING List()
q: tsql.QueryO[shapeless.::[tsql.ColumnMeta[Int(1),String("bpchar"),tsql.NoNulls,String("country"),String("code")],shapeless.::[tsql.ColumnMeta[Int(12),String("varchar"),tsql.NoNulls,String("country"),String("name")],shapeless.::[tsql.ColumnMeta[Int(4),String("int4"),tsql.NoNulls,String("country"),String("population")],shapeless.HNil]]]] = tsql.QueryO@3d552fa
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

```scala
scala> q.as[List[(String, String, Int)]]
res6: doobie.imports.ConnectionIO[List[(String, String, Int)]] = Gosub()
```

We can also map case classes.

```scala
scala> case class Country(code: String, name: String, population: Int)
defined class Country

scala> q.as[Vector[Country]]
res7: doobie.imports.ConnectionIO[Vector[Country]] = Gosub()
```

And nested products (in this case a pair with a case class as an element).

```scala
scala> case class CountryInfo(name: String, population: Int)
defined class CountryInfo

scala> q.as[Array[(String, CountryInfo)]]
res8: doobie.imports.ConnectionIO[Array[(String, CountryInfo)]] = Gosub()
```

If we can map a row to a pair as above, we can accumulate directly to a `Map`, which can be useful.

```scala
scala> q.as[Map[String, CountryInfo]]
res9: doobie.imports.ConnectionIO[Map[String,CountryInfo]] = Gosub()
```
