# doobie-tsql

Compile-time checked SQL literals for **[doobie]()** with fancy inferred types.

This is a prototype that works with **doobie 0.4.2-SNAPSHOT** for **Cats** on **Scala 2.12**. There is a snapshot release on Sonatype that you can use thus:

```scala
resolvers in ThisBuild +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-tsql"          % "0.2-SNAPSHOT",
  "org.tpolecat" %% "doobie-tsql-postgres" % "0.2-SNAPSHOT" // optional, for array type mappings and some other things
)
```

The high points:

- The new `tsql` interpolator checks statements against the live schema at compile-time and infers fancy types that allow fine-grained type mappings that can be constrained based on schema-specific types or even table/column names.
- The new `Read` and `Write` typeclasses subsume the `Meta/Atom/Composite` stack. The design is more general and much simpler.
- Some rough edges and basically no doc yet. Sorry.

See the [**EXAMPLE**](EXAMPLE.md) for much more information.

### Building

If you want to build and run the tests you will need to set up MySQL and Postgres as specified in the `.travis.yml` file. The H2 tests don't require any setup.

### TODO

- [x] CI, etc.
- [ ] better handling of credentials
- [x] Updates returning generated keys.
- [x] Bulk updates.
- [x] remove param type and nullity for ParameterMeta
- [x] Stream
- [ ] `in` clauses (hard, ok to punt)
- [ ] generalized Write deriving (generic, unitary, etc.)
- [x] array~collection read via CBF
- [ ] array~collection write via CBF
- [ ] date/time type mappings
- [x] narrowed derivations (ARRAY int4 to Array[Int] for instance)
= [x] clean up `TPrint` impl, get working in normal REPL
- [ ] tut micro-site
- [x] reducer, monadplus
