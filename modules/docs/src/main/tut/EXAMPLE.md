
### Quick Start

Same setup as with normal **doobie**, but we also import `tsql._`. We're using the same test database as the [**book of doobie**](http://tpolecat.github.io/doobie-0.2.3/00-index.html) so check that out if you want to set it up yourself.

```tut:silent
import doobie.imports._, tsql._
import shapeless._
```

```tut:invisible
// borrow some stuff from ammonite so we can format the types nicely
import tsql.amm._, pprint.TPrint, pprint.Config.Colors._
def tp[A:TPrint](a:A) = println(pprint.tprint[A])
```


Ok. first thing to notice is that SQL literals are checked at compile-time. If the SQL doesn't make sense to the database it's a type error. 

```tut:fail
tsql"select id, name from country"
```

Note that the carat is in the right place. We can only do this for Postgres because nobody else provides source offsets in their exceptions. For other vendors the carat appears at the beginning of the statement.

Ok so how does that work? At compile-time the compiler goes out and talks to the database using connect info specified via `scalacOptions`. We could also do this with an annotation or something but this is ok for now.

```scala
scalacOptions ++= Seq(
  "-Xmacro-settings:doobie.driver=org.postgresql.Driver",
  "-Xmacro-settings:doobie.connect=jdbc:postgresql:world",
  "-Xmacro-settings:doobie.user=postgres",
  "-Xmacro-settings:doobie.password="
)
```

Ok so the big difference betweeen the `sql` interpolator and the `tsql` interpolator is that the latter infers interesting types.

### Inference 1. Simple Updates

The inferred type of a `tsql` literal is potentially fancy, but in some cases can be trivial. In the case of an unconditional update the computation is fully specified: there are no parameters and nothing returned other than a row count. So the inferred type is `ConnectionIO[Int]`.

```tut
tsql"delete from country"
```

Similarly an update with interpolated arguments is fully specified. As with the old `sql` interpolator the interpolated argument becomes a JDBC `setXXX` operation and is not an injection risk.

```tut
val c = "USA"
tsql"delete from country where code = $c"
```

In addition, the type of the argument must conform with the schema; a mismatch is a type error. For example, here we attempt to use `c` (a `String`) in a position where a numeric type is expected.

```tut:fail
tsql"delete from country where population = $c"
```

The following update contain a `?` placeholder, yielding a fancier type.

```tut
val up = tsql"delete from country where population = ?"
```

If we unpack this type and rewrite the singletons to be more readable it looks like this:

```tut
tp(up)
```

_Note that I'm using Li Haoyi's `PPrint` library to do this; **doobie-tsql** comes with `TPrint` instances that you can import and use in the standard REPL or Ammonite._

Ok, so we have an `UpdateI` which means it's an update with an unknown input mapping (we know the parameter types but don't know the Scala type we want to map from). Its type argument is an HList of `ParameterMeta` (in this case just one). What it's saying to us is that we have a single parameter with the following properties:

- The JDBC type is `INTEGER` (singleton type `4`).
- The Schema type is `int4`, which is the Postgres name for a 4-byte integer.

We can now apply an argument of some type that satisfies the above constraint, and there may be many such types. But for now let's just use an `Int`, yielding a `ConnectionIO[Int]` as before.

```tut
up(42)
```

Multi-parameter placeholder queries become `UpdateI`s that take multiple arguments (there is no arity limit).

```tut
val up = tsql"update country set name = ? where code = ? or population > ?"
```

The type cleans up to be:

```tut
tp(up)
```

And we can complete the specification by providing arguments that conform with the above constraints.

```tut
up("foo", "bar", 123456)
```


### Inference 2: Selects

`SELECT` statements have fancy types describing the output columns in addition to any input parameters.

```tut
val q = tsql"select code, name, population from country"
```

Let's look at this type more closely:

```tut
tp(q)
```

So the type of `q` tells us:

- It's a `QueryO` which means it's a query with an unknown output mapping (we know the column types but don't know the Scala type we want to map to).
- There are three columns, of JDBC type `CHAR`, `VARCHAR`, and `INTEGER`; and of Schema types `bpchar`, `varchar`, and `int4`, respectively.
- The columns might not be nullable, which means it might be safe to map them to unlifted (i.e., non-`Option` types).
- The columns are all from the `country` table/alias/projection and are named `code`, `name`, and `population`, respectively.

In order to use this query we can provide an output type, which specifies both the row type mapping and the aggregate structure. The element type can be any [nested] product type whose eventual elements have primitive mappings consistent with the JDBC types (more on this later). The aggregate type can be anything with a sensible `CanBuildFrom`, `Reducer`, or `MonadPlus`.

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

We can also ask for a `.unique` or `.option` result as with the existing `sql` interpolator.

```tut
tsql"select name, population from city where id = 42".unique[(String, Int)]
```


