import sbt._

object Dependencies {
  lazy val scalaTest  = "org.scalatest" %% "scalatest"   % "3.0.5"
  lazy val cats       = "org.typelevel" %% "cats-core"   % "1.5.0"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "1.1.0"
  lazy val zio        = "org.scalaz"    %% "scalaz-zio"  % "1.0-RC3"
  lazy val fs2        = "co.fs2"        %% "fs2-io"      % "1.0.0"
  lazy val scalafx    = "org.scalafx"   %% "scalafx"     % "8.0.144-R12"

}