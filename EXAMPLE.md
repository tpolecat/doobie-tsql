
### Quick Start

This tutorial assumes you are familiar with **doobie** and know what to do once you have a `ConnectionIO`. The examples given here are simply constructions to demonstrate types and are never executed. We will use the same imports as with normal **doobie**, but we also import `tsql._`.

```scala
import doobie.imports._, doobie.tsql._
import shapeless._
```




Ok. first thing to notice is that SQL literals are checked at compile-time. If the SQL doesn't make sense to the database it's a type error.

```scala
scala> tsql"select id, name from country"
<console>:26: error: ERROR: column "id" does not exist
       tsql"select id, name from country"
                   ^
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

### Simple Updates

The inferred type of a `tsql` literal is potentially fancy, but in some cases can be trivial. In the case of an unconditional update the computation is fully specified: there are no parameters and nothing returned other than a row count. So the inferred type is `ConnectionIO[Int]`.

```scala
scala> tsql"delete from country"
res2: doobie.hi.ConnectionIO[Int] = Free(...)
```

Similarly an update with interpolated arguments is fully specified. As with the old `sql` interpolator the interpolated argument becomes a JDBC `setXXX` operation and is not an injection risk.

```scala
scala> val c = "USA"
c: String = USA

scala> tsql"delete from country where code = $c"
res3: doobie.imports.ConnectionIO[Int] = Free(...)
```

In addition, the type of the argument must conform with the schema; a mismatch is a type error. For example, here we attempt to use `c` (a `String`) in a position where a numeric type is expected.

```scala
scala> tsql"delete from country where population = $c"
<console>:27: error: parameter types don't match up, sorry
       tsql"delete from country where population = $c"
            ^
```

The following update contain a `?` placeholder, yielding a fancier type.

```scala
scala> val up = tsql"delete from country where population = ?"
up: doobie.tsql.UpdateI[doobie.tsql.ParameterMeta[Int(4),String("int4")] :: shapeless.HNil] = doobie.tsql.UpdateI@6850a5f1
```

If we unpack this type and rewrite the singletons to be more readable it looks like this:

```
UpdateI[
  ParameterMeta[INTEGER, int4] ::
  HNil
]
```

_Note that I'm using Li Haoyi's `PPrint` library to do this; **doobie-tsql** comes with `TPrint` instances that you can import and use in the standard REPL or Ammonite._

Ok, so we have an `UpdateI` which means it's an update with an unknown input mapping (we know the parameter types but don't know the Scala type we want to map from). Its type argument is an HList of `ParameterMeta` (in this case just one). What it's saying to us is that we have a single parameter with the following properties:

- The JDBC type is `INTEGER` (singleton type `4`).
- The Schema type is `int4`, which is the Postgres name for a 4-byte integer.

We can now apply an argument of some type that satisfies the above constraint, and there may be many such types. But for now let's just use an `Int`, yielding a `ConnectionIO[Int]` as before.

```scala
scala> up(42)
res6: doobie.imports.ConnectionIO[Int] = Free(...)
```

Note that nullity is always unknown for parameters; if you wish to set a parameter to `NULL` you can use an `Option` type, and `None` will result in a `.setNull(...)` call on the underlying statement.

> TODO
> ```tut
> def opUp(pop: Option[Int]) = up(pop) // compiles fine
> ```


Multi-parameter placeholder queries become `UpdateI`s that take multiple arguments (there is no arity limit).

```scala
scala> val up = tsql"update country set name = ? where code = ? or population > ?"
up: doobie.tsql.UpdateI[doobie.tsql.ParameterMeta[Int(12),String("varchar")] :: doobie.tsql.ParameterMeta[Int(1),String("bpchar")] :: doobie.tsql.ParameterMeta[Int(4),String("int4")] :: shapeless.HNil] = doobie.tsql.UpdateI@530387e1
```

The type cleans up to be:

```
UpdateI[
  ParameterMeta[VARCHAR, varchar] ::   
  ParameterMeta[CHAR,    bpchar ] ::   
  ParameterMeta[INTEGER, int4   ] ::   
  HNil
]
```

And we can complete the specification by providing arguments that conform with the above constraints.

```scala
scala> up("foo", "bar", 123456)
res8: doobie.imports.ConnectionIO[Int] = Free(...)
```


### Simple Selects

`SELECT` statements have fancy types describing the output columns in addition to any input parameters.

```scala
scala> val q = tsql"select code, name, population from country"
q: doobie.tsql.QueryO[doobie.tsql.ColumnMeta[Int(1),String("bpchar"),doobie.tsql.NoNulls,String("country"),String("code")] :: doobie.tsql.ColumnMeta[Int(12),String("varchar"),doobie.tsql.NoNulls,String("country"),String("name")] :: doobie.tsql.ColumnMeta[Int(4),String("int4"),doobie.tsql.NoNulls,String("country"),String("population")] :: shapeless.HNil] = doobie.tsql.QueryO@1c5f693b
```

Let's look at this type more closely:

```
QueryO[
  ColumnMeta[CHAR,    bpchar,  NoNulls, country, code      ] ::      
  ColumnMeta[VARCHAR, varchar, NoNulls, country, name      ] ::      
  ColumnMeta[INTEGER, int4,    NoNulls, country, population] ::      
  HNil
]
```

So the type of `q` tells us:

- It's a `QueryO` which means it's a query with an unknown output mapping (we know the column types but don't know the Scala types we want to map to).
- There are three columns, of JDBC type `CHAR`, `VARCHAR`, and `INTEGER`; and of Schema types `bpchar`, `varchar`, and `int4`, respectively.
- The columns might not be nullable, which means it might be safe to map them to unlifted (i.e., non-`Option` types).
- The columns are all from the `country` table/alias and are named `code`, `name`, and `population`, respectively.

In order to use this query we can provide an output type, which specifies both the row type mapping *and* the aggregate structure. The element type can be any [nested] product type whose eventual elements have primitive mappings consistent with the `ColumnMeta` (more on this later). The aggregate type can be anything with a sensible `CanBuildFrom` or `Alternative`.

So a basic mapping might be a list of triples. Specifying this output type results in a `ConnectionIO` and we're done.

```scala
scala> q.as[List[(String, String, Int)]]
res10: doobie.imports.ConnectionIO[List[(String, String, Int)]] = Free(...)
```

We can also map case classes.

```scala
scala> case class Country(code: String, name: String, population: Int)
defined class Country

scala> q.as[Vector[Country]]
res11: doobie.imports.ConnectionIO[Vector[Country]] = Free(...)
```

And nested products (in this case a pair with a case class as an element).

```scala
scala> case class CountryInfo(name: String, population: Int)
defined class CountryInfo

scala> q.as[Array[(String, CountryInfo)]]
res12: doobie.imports.ConnectionIO[Array[(String, CountryInfo)]] = Free(...)
```

If we can map a row to a pair as above, we can accumulate directly to a `Map`, which can be useful.

```scala
scala> q.as[Map[String, CountryInfo]]
res13: doobie.imports.ConnectionIO[Map[String,CountryInfo]] = Free(...)
```

We can also ask for a `.unique` or `.option` result as with the existing `sql` interpolator.

```scala
scala> tsql"select name, population from city where id = 42".unique[(String, Int)]
res14: doobie.imports.ConnectionIO[(String, Int)] = Free(...)

scala> tsql"select name, population from city where id = 42".option[(String, Int)]
res15: doobie.imports.ConnectionIO[Option[(String, Int)]] = Free(...)
```

And as with the current `sql` interpolator we can ask for a `Stream`.

```scala
scala> tsql"select name, population from city where id = 42".stream[(String, Int)]
res16: fs2.Stream[doobie.imports.ConnectionIO,(String, Int)] = Segment(Emit(Chunk(()))).flatMap(<function1>).flatMap(<function1>).flatMap(<function1>)
```

### Parameterized Selects


### Updates Returning Columns


### Understanding Column and Parameter Constraints

- explain the variance
- note the MySQL issue

### Custom Type Mappings

- simple co/contravariant mappints
- schema-constrained mappings

### Using TPrint
