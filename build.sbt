name := "susurrant-utils"

organization := "org.chrisjr"

version := "0.0.1"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-common" % "2.6.0"  % "provided" excludeAll(
    ExclusionRule(organization = "javax.servlet")
  ),
  "cc.mallet" % "mallet" % "2.0.9-SNAPSHOT",
  "com.lambdaworks" %% "jacks" % "2.3.3",
  "com.typesafe.play" %% "play-json" % "2.3.1",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "org.scalaz" %% "scalaz-core" % "7.1.1",
  "org.apache.spark" %% "spark-core" % "1.2.1"  % "provided",
  "org.apache.spark" %% "spark-mllib" % "1.2.1" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test" withSources() withJavadoc()
)

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.mavenLocal

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)) 

initialCommands := "import org.chrisjr.susurrantutils._"

