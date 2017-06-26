// Library versions all in one place, for convenience and sanity.
lazy val ammoniteVersion      = "1.0.0-RC9"
lazy val scalaCheckVersion    = "1.13.4"
lazy val specs2Version        = "3.8.6"
lazy val kindProjectorVersion = "0.9.3"
lazy val paradiseVersion      = "2.1.0"
lazy val doobieVersion        = "0.4.2-SNAPSHOT"
lazy val macroCompatVersion   = "1.1.1"
lazy val mySqlVersion         = "6.0.2"
lazy val catsVersion          = "0.9.0"

resolvers in ThisBuild +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

lazy val buildSettings = Seq(
  organization := "org.tpolecat",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  scalaOrganization := "org.typelevel",
  scalaVersion := "2.12.2-bin-typelevel-4",
  libraryDependencies ++= Seq(
    "org.scalacheck" %% "scalacheck"  % scalaCheckVersion % "test",
    "org.specs2"     %% "specs2-core" % specs2Version     % "test"
  )
)

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
    "-language:higherKinds",             // Allow higher-kinded types
    "-language:implicitConversions",     // Allow definition of implicit functions called views
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    "-Xfuture",                          // Turn on future language features.
    "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
    "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
    "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",            // Option.apply used implicit view.
    "-Xlint:package-object-classes",     // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match",              // Pattern match may not be typesafe.
    "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification",             // Enable partial unification in type constructor inference
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    // "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",              // Warn if a local definition is unused.
    // "-Ywarn-unused:params",              // Warn if a value parameter is unused.
    // "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",            // Warn if a private member is unused.
    "-Ywarn-value-discard",              // Warn when non-Unit expression results are unused.
    "-Yinduction-heuristics",            // speeds up the compilation of inductive implicit resolution
    // "-Ykind-polymorphism",               // type and method definitions with type parameters of arbitrary kinds
    "-Yliteral-types",                   // literals can appear in type position
    "-Xstrict-patmat-analysis",          // more accurate reporting of failures of match exhaustivity
    "-Xlint:strict-unsealed-patmat"      // warn on inexhaustive matches against unsealed traits
  ),
  scalacOptions in (Compile, console) ~= (_.filterNot(Set(
    "-Xfatal-warnings",
    "-Ywarn-unused:imports"
  ))),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % kindProjectorVersion),
  publishArtifact in (Compile, packageDoc) := false
)

lazy val root = project.in(file("."))
  .settings(buildSettings)
  .aggregate(core, postgres, h2, mysql)

lazy val core = project.in(file("modules/core"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    name := "doobie-tsql-core",
    libraryDependencies ++= Seq(
      scalaOrganization.value % "scala-reflect"    % scalaVersion.value,
      scalaOrganization.value % "scala-compiler"   % scalaVersion.value,
      "org.typelevel"        %% "macro-compat"     % macroCompatVersion,
      "org.typelevel"        %% "cats"             % catsVersion,
      "org.tpolecat"         %% "doobie-core-cats" % doobieVersion
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.patch)
  )

lazy val postgres = project.in(file("modules/postgres"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    name := "doobie-tsql-postgres",
    scalacOptions ++= Seq(
      "-Xmacro-settings:doobie.driver=org.postgresql.Driver",
      "-Xmacro-settings:doobie.connect=jdbc:postgresql:world",
      "-Xmacro-settings:doobie.user=postgres",
      "-Xmacro-settings:doobie.password="
    ),
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-postgres" % doobieVersion
    ),
    initialCommands := """
      |import cats._, cats.implicits._
      |import doobie.imports._
      |import doobie.postgresql.pgtypes._
      |val xa: Transactor[Task] = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
      |import xa.yolo._
      |import org.postgis._
      |import org.postgresql.util._
      |import org.postgresql.geometric._
      |import doobie.tsql._
      |import shapeless._
      |import doobie.tsql.amm._, pprint.TPrint, pprint.Config.Colors._
      |def tp[A:TPrint](a:A) = println(pprint.tprint[A])
      """.stripMargin
  )
  .dependsOn(core)

lazy val h2 = project.in(file("modules/h2"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    name := "doobie-tsql-h2",
    scalacOptions ++= Seq(
      "-Xmacro-settings:doobie.driver=org.h2.Driver",
      "-Xmacro-settings:doobie.connect=jdbc:h2:zip:./modules/h2/world.zip!/world",
      "-Xmacro-settings:doobie.user=",
      "-Xmacro-settings:doobie.password="
    ),
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-h2-cats" % doobieVersion
    ),
    initialCommands := s"""
      |import doobie.tsql._, doobie.tsql.amm._, cats._, cats.implicits._, doobie.imports._, shapeless._
      |""".stripMargin
  )
  .dependsOn(core)

lazy val mysql = project.in(file("modules/mysql"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    name := "doobie-tsql-mysql",
    scalacOptions ++= Seq(
      "-Xmacro-settings:doobie.driver=com.mysql.cj.jdbc.Driver",
      "-Xmacro-settings:doobie.connect=jdbc:mysql://localhost/world?useSSL=false&serverTimezone=UTC",
      "-Xmacro-settings:doobie.user=root",
      "-Xmacro-settings:doobie.password=",
      "-Xmacro-settings:doobie.checkParameters=false"
    ),
    libraryDependencies ++= Seq(
      "mysql" % "mysql-connector-java" % mySqlVersion
    ),
    initialCommands := s"""
      |import doobie.tsql._, doobie.tsql.amm._, cats._, cats.implicits._, doobie.imports._, shapeless._
      |""".stripMargin
  )
  .dependsOn(core)


lazy val docs = project.in(file("modules/docs"))
  .settings(buildSettings)
  .settings(commonSettings)
  .enablePlugins(TutPlugin)
  .settings(
    name := "doobie-tsql-docs",
    scalacOptions in Tut ++= Seq(
      "-Xmacro-settings:doobie.driver=org.postgresql.Driver",
      "-Xmacro-settings:doobie.connect=jdbc:postgresql:world",
      "-Xmacro-settings:doobie.user=postgres",
      "-Xmacro-settings:doobie.password="
    ),
    scalacOptions in Tut ~= (_.filterNot(Set(
      "-Xfatal-warnings",
      "-Ywarn-unused:imports"
    ))),
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-postgres-cats" % doobieVersion
    ),
    tutTargetDirectory := (baseDirectory in root).value,
    tut := {
      val data = tut.value
      data.map(_._2).foreach(println)
      // TODO: remove lines that look like `scala> tp(...)`
      data
    }
  )
  .dependsOn(postgres, ammonite)

lazy val ammonite = project.in(file("modules/ammonite"))
  .dependsOn(core)
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    name := "doobie-tsql-ammonite",
    libraryDependencies += "com.lihaoyi" % "ammonite-repl" % ammoniteVersion cross CrossVersion.patch
  )
