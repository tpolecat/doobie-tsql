

val amm = taskKey[Unit]("Run the Ammonite REPL")

val ammSettings = Seq(
  libraryDependencies += "com.lihaoyi" % "ammonite-repl" % "0.5.9-SNAPSHOT" % "test" cross CrossVersion.full,
  amm := toError((runner in run).value.run(
      "ammonite.repl.Main", 
      Attributed.data((fullClasspath in Test).value), {
        val opts = scalacOptions.value.mkString("List(\"", "\",\"", "\")")
        List("-p", s"compiler.settings.processArguments($opts, true)")
      },
      streams.value.log
  ))
)

lazy val buildSettings = Seq(
  organization := "org.tpolecat",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", scalaVersion.value, "2.12.0-M3")
)

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
      "-Ywarn-dead-code" //,
      // "-Ywarn-value-discard"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1")
) ++ ammSettings

lazy val root = project.in(file("."))
  .settings(buildSettings)
  .aggregate(core, example)

lazy val core = project.in(file("core"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.typelevel" %% "macro-compat"   % "1.1.1",
      "org.tpolecat"  %% "doobie-core"    % "0.2.3"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )

lazy val example = project.in(file("example"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    scalacOptions ++= Seq(
      "-Xmacro-settings:doobie.driver=org.postgresql.Driver",
      "-Xmacro-settings:doobie.connect=jdbc:postgresql:world",
      "-Xmacro-settings:doobie.user=postgres",
      "-Xmacro-settings:doobie.password="
    ),
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-contrib-postgresql" % "0.2.3"
    )
  )
  .dependsOn(core)

