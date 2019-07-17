name := "renervator-wol"

version := "0.1"

scalaVersion := "2.12.8"


// Only necessary for SNAPSHOT releases
resolvers += Resolver.sonatypeRepo("snapshots")

scalacOptions ++= Seq("-Ypartial-unification")

libraryDependencies ++= {
  val scalaTestVersion = "3.0.8"
  val http4sVersion = "0.20.0"
  Seq(
    "org.typelevel" %% "cats-core" % "2.0.0-M4",
    "org.typelevel" %% "cats-effect" % "2.0.0-M4",
    //"dev.zio" %% "zio" % "1.0.0-RC10-1",
    //http4s
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion,
    "io.circe" %% "circe-generic" % "0.12.0-M4",
    //test
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    "org.mockito" % "mockito-core" % "2.7.22" % Test
  )
}

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](
  name,
  version,
  scalaVersion,
  sbtVersion,
  BuildInfoKey.action("commitHash") {
    git.gitHeadCommit.value
  }
)
buildInfoOptions := Seq(BuildInfoOption.BuildTime)
buildInfoPackage := "com.wanari.renervator"

