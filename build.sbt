name := "zio-demo"

version := "0.1"

scalaVersion := "2.13.7"

val zioVersion = "1.0.12"

val tapirVersion = "0.19.0"

val zioCats = "3.1.1.0"

libraryDependencies ++= Seq(

  // zio
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-interop-cats" % zioCats,

  // tapir
  "com.softwaremill.sttp.tapir" %% "tapir-zio"               % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-zio-http4s-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-client"     % tapirVersion

)