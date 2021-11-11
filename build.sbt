name := "zio-demo"

version := "0.1"

scalaVersion := "2.13.7"

val zioVersion = "1.0.12"

libraryDependencies ++= Seq(

  // zio
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion
)