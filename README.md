# doobie-tsql

Compile-time checked SQL literals with fancy inferred types. For **doobie**.

This is a **prototype** the works with 0.2.3 but I would like it to be included as an add-on for 0.3, and subsume the existing high-level API in doobie 0.4.

The high points:

- @tpolecat wrote a macro! But all it does is infer types so it doesn't really count.
- The new `tsql` interpolator checks statements against the live schema at compile-time and infers fancy types that allow fine-grained type mappings that can be constrained based on schema-specific types or even table/column names.
- The new `Read` and `Write` typeclasses subsume the `Meta/Atom/Composite` stack. The design is more general and much simpler.

TODO:

- [x] Updates returning generated keys.
- [x] Bulk updates.
- [x] remove param type and nullity for ParameterMeta
- [ ] Process
- [ ] `in` clauses (hard, ok to punt)
- [ ] generalized Write deriving (generic, unitary, etc.)
- [ ] array~collection read/write via CBF
- [ ] date/time type mappings
- [ ] narrowed derivations (ARRAY int4 to Array[Int] for instance)
= [ ] clean up `TPrint` impl, get working in normal REPL
- [ ] tut doc
