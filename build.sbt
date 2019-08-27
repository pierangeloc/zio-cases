import Dependencies._

enablePlugins(JmhPlugin)

lazy val tpolecatSettings = Seq(
  scalacOptions ++=
    Seq(
      "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
      "-encoding", "utf-8",                // Specify character encoding used by source files.
      "-explaintypes",                     // Explain type errors in more detail.
      "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
      "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
      "-language:higherKinds",             // Allow higher-kinded types
      "-language:implicitConversions",     // Allow definition of implicit functions called views
      "-language:postfixOps",     // Allow definition of implicit functions called views
      "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
      "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
      //      "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
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
      // "-Yno-imports",                      // No predef or default imports
      "-Ypartial-unification",             // Enable partial unification in type constructor inference
      "-Ywarn-dead-code",                  // Warn when dead code is identified.
      "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
      "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
      "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
      "-Ywarn-numeric-widen",              // Warn when numerics are widened.
      //      "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
      //      "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
      //      "-Ywarn-unused:locals",              // Warn if a local definition is unused.
      //      "-Ywarn-unused:params",              // Warn if a value parameter is unused.
      // "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates",            // Warn if a private member is unused.
      "-Ywarn-value-discard",              // Warn when non-Unit expression results are unused.
      "-Ywarn-macros:before", // via som
      "-Yrangepos" // for longer squiggles
    )
  ,
  scalacOptions in (Compile, console) --= Seq("-Xfatal-warnings", "-Ywarn-unused:imports", "-Yno-imports")
)

lazy val commonSettings = inThisBuild(
  tpolecatSettings ++
    Seq(
      scalaVersion := "2.12.8",
      addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),
      addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0-M4")
    )
)

lazy val mandelbrot = project
  .in(file("mandelbrot"))
  .settings(commonSettings)
  .settings(
      name := "mandelbrot",
      libraryDependencies ++= (
        Seq(
          zio,
          zioCats,
          scalafx,
          scalaTags,
          scalaTest % Test
        ) ++ http4sAll ++ circeAll
      ),
    // Allows to read the generated JS on client
    resources in Compile += (fastOptJS in (frontend, Compile)).value.data,
    // Lets the backend to read the .map file for js
    resources in Compile += (fastOptJS in (frontend, Compile)).value
      .map((x: sbt.File) => new File(x.getAbsolutePath + ".map"))
      .data,
    // Lets the server read the jsdeps file
    (managedResources in Compile) += (artifactPath in (frontend, Compile, packageJSDependencies)).value,
    // do a fastOptJS on reStart
    reStart := (reStart dependsOn (fastOptJS in (frontend, Compile))).evaluated,
    // This settings makes reStart to rebuild if a scala.js file changes on the client
    watchSources ++= (watchSources in frontend).value,
    mainClass in reStart := Some("io.tuliplogic.fractals.HttpMain")
  )

lazy val frontend = project
  .in(file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "frontend"
  )
  .settings(
    scalaJSUseMainModuleInitializer := true,
    // Build a js dependencies file
    skip in packageJSDependencies := false,
    jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),

    // Put the jsdeps file on a place reachable for the server
    crossTarget in (Compile, packageJSDependencies) := (resourceManaged in Compile).value,
    libraryDependencies ++= Seq(
      "dev.zio"               %%% "zio"                                   % zioVersion,
      "dev.zio"               %%% "zio-streams"                           % zioVersion,
      "dev.zio"               %% "zio-interop-reactivestreams"           % zioVersion,
      "org.scala-js"          %%% "scalajs-dom"                           % "0.9.7",
      "com.lihaoyi"           %%% "scalatags"                             % "0.6.8",
      "com.softwaremill.sttp" %%% "core"                                  % "1.6.4",
      "com.softwaremill.sttp" %%% "monix"                                 % "1.6.4"
    )
  )


lazy val `simple-http4s` = project
  .in(file("simple-http4s"))
  .settings(commonSettings)
  .settings(
      name := "simple-http4s",
      libraryDependencies ++= Seq(
        zio,
        zioCats,
        http4sServer,
        http4sDsl,
        http4sCirce,
        circeCore,
        circeGeneric,
        circeGenericX,
        log4CatsSlf4j
      )
  )

//lazy val root = project
//  .in(file("."))
//  .settings(commonSettings)
//  .settings(
//    name := "zio-cases",
//    organization := "io.tuliplogic",
//    scalaVersion := "2.12.6",
//
//  )
