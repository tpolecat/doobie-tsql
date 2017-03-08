// Library versions all in one place, for convenience and sanity.
lazy val scalaCheckVersion    = "1.13.4"
lazy val specs2Version        = "3.8.6"
lazy val kindProjectorVersion = "0.9.3"
lazy val paradiseVersion      = "2.1.0"
lazy val doobieVersion        = "0.4.2-SNAPSHOT"
lazy val macroCompatVersion   = "1.1.1"
lazy val mySqlVersion         = "6.0.2"

resolvers in ThisBuild +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

lazy val buildSettings = Seq(
  organization := "org.tpolecat",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  scalaOrganization := "org.typelevel",
  scalaVersion := "2.12.1",
  libraryDependencies ++= macroParadise(scalaVersion.value) ++ Seq(
    "org.scalacheck" %% "scalacheck"  % scalaCheckVersion % "test",
    "org.specs2"     %% "specs2-core" % specs2Version     % "test"
  )
)

def macroParadise(v: String): List[ModuleID] =
  if (v.startsWith("2.10")) List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
  else Nil

lazy val commonSettings = Seq(
    scalacOptions ++= Seq(
      "-encoding", "UTF-8", // 2 args
      "-feature",
      "-deprecation",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:experimental.macros",
      "-unchecked",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ypartial-unification",
      "-Yliteral-types"
    ),
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
      "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.typelevel" %% "macro-compat"   % macroCompatVersion,
      "org.tpolecat"  %% "doobie-core"    % doobieVersion
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full)
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
      |import scalaz._,Scalaz._
      |import scalaz.concurrent.Task
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
      "org.tpolecat"  %% "doobie-h2" % doobieVersion
    ),
    initialCommands := s"""
      |import doobie.tsql._, doobie.tsql.amm._, scalaz._, Scalaz._, doobie.imports._, shapeless._
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
      |import doobie.tsql._, doobie.tsql.amm._, scalaz._, Scalaz._, doobie.imports._, shapeless._
      |""".stripMargin
  )
  .dependsOn(core)


lazy val docs = project.in(file("modules/docs"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(tutSettings)
  .settings(
    name := "doobie-tsql-docs",
    scalacOptions ++= Seq(
      "-Xmacro-settings:doobie.driver=org.postgresql.Driver",
      "-Xmacro-settings:doobie.connect=jdbc:postgresql:world",
      "-Xmacro-settings:doobie.user=postgres",
      "-Xmacro-settings:doobie.password="
    ),
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-postgres" % doobieVersion
    ),
    tutTargetDirectory := (baseDirectory in root).value,
    tut := {
      val data = tut.value
      data.map(_._2).foreach(println)
      // TODO: remove lines that look like `scala> tp(...)`
      data
    }
  )
  .dependsOn(postgres)
