addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.12.0")

assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "commons", xs @ _*) => MergeStrategy.first
  case PathList("META_INF", "elki", "de.lmu.ifi.dbs.elki.datasource.DatabaseConnection") => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

