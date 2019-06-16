import sbt._

object Dependencies {
  lazy val scalaTest  = "org.scalatest" %% "scalatest"   % "3.0.5"
  lazy val cats       = "org.typelevel" %% "cats-core"   % "1.5.0"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "1.1.0"

  lazy val zio        = "org.scalaz"    %% "scalaz-zio"              % "1.0-RC5"
  lazy val zioCats    = "org.scalaz"    %% "scalaz-zio-interop-cats" % "1.0-RC5"

  lazy val fs2 = "co.fs2" %% "fs2-io" % "1.0.0"

  lazy val http4sServer  = "org.http4s" %% "http4s-blaze-server" % "0.20.1"
  lazy val http4sClient  = "org.http4s" %% "http4s-blaze-client" % "0.20.1"
  lazy val http4sDsl     = "org.http4s" %% "http4s-dsl"          % "0.20.1"
  lazy val http4sCirce   = "org.http4s" %% "http4s-circe"        % "0.20.1"

  lazy val circeCore     = "io.circe"  %% s"circe-core"           % "0.11.1"
  lazy val circeGeneric  = "io.circe"  %% s"circe-generic"        % "0.11.1"
  lazy val circeGenericX = "io.circe"  %% s"circe-generic-extras" % "0.11.1"

  lazy val log4CatsCore  = "io.chrisdavenport" %% s"log4cats-core"  % "0.3.0"
  lazy val log4CatsSlf4j = "io.chrisdavenport" %% s"log4cats-slf4j" % "0.3.0"
  
  lazy val scalafx       = "org.scalafx" %% "scalafx" % "8.0.144-R12"
}
