import sbt._

object Dependencies {
  val http4sVersion = "0.21.0-M4"
  val zioVersion    = "1.0.0-RC12-1"
  val fs2Version    = "1.1.0-M1"

  lazy val scalaTest  = "org.scalatest" %% "scalatest"   % "3.0.5"
  lazy val cats       = "org.typelevel" %% "cats-core"   % "1.5.0"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "2.0.0-M5"

  lazy val zio        = "dev.zio"       %% "zio"              % zioVersion
  lazy val zioCats    = "dev.zio"       %% "zio-interop-cats" % "2.0.0.0-RC2"

  lazy val fs2 = "co.fs2" %% "fs2-io" % fs2Version

  lazy val http4sServer  = "org.http4s" %% "http4s-blaze-server" % http4sVersion
  lazy val http4sClient  = "org.http4s" %% "http4s-blaze-client" % http4sVersion
  lazy val http4sDsl     = "org.http4s" %% "http4s-dsl"          % http4sVersion
  lazy val http4sCirce   = "org.http4s" %% "http4s-circe"        % http4sVersion

  lazy val http4sAll = Seq(http4sServer, http4sClient, http4sDsl, http4sCirce)

  lazy val circeCore     = "io.circe"  %% s"circe-core"           % "0.11.1"
  lazy val circeGeneric  = "io.circe"  %% s"circe-generic"        % "0.11.1"
  lazy val circeGenericX = "io.circe"  %% s"circe-generic-extras" % "0.11.1"

  lazy val circeAll = Seq(circeCore, circeGeneric, circeGenericX)

  lazy val log4CatsCore  = "io.chrisdavenport" %% s"log4cats-core"  % "0.3.0"
  lazy val log4CatsSlf4j = "io.chrisdavenport" %% s"log4cats-slf4j" % "0.3.0"
  
  lazy val scalafx       = "org.scalafx" %% "scalafx" % "8.0.144-R12"

  lazy val scalaTags     = "com.lihaoyi" %% "scalatags" % "0.6.8"

  lazy val zioTest    = "dev.zio" %% "zio-test"     % "1.0.0-RC12-1" % "test"
  lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % "1.0.0-RC12-1" % "test->test;compile->compile"



}
