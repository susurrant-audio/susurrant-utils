name := "susurrant-utils"

organization := "org.chrisjr"

version := "0.0.1"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-common" % "2.6.0"  % "provided" excludeAll(
    ExclusionRule(organization = "javax.servlet")
  ),
  "cc.mallet" % "mallet" % "2.0.7",
  "com.lambdaworks" %% "jacks" % "2.3.3",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "org.scalaz" %% "scalaz-core" % "7.1.1",
  "org.apache.spark" %% "spark-core" % "1.2.1"  % "provided",
  "org.apache.spark" %% "spark-mllib" % "1.2.1" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test" withSources() withJavadoc()
)

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)) 

initialCommands := "import org.chrisjr.susurrantutils._"

assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "commons", xs @ _*) => MergeStrategy.first
  case PathList("META_INF", "elki", "de.lmu.ifi.dbs.elki.datasource.DatabaseConnection") => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

