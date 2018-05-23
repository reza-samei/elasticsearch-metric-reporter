
// ============================================================

lazy val common = Seq(
    scalaVersion := "2.11.12",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    libraryDependencies ++= Seq(
        "circe-core", "circe-generic", "circe-parser"
    ).map { m => "io.circe" %% m % "0.9.3" % Test },
    scalacOptions ++= Seq(
        "-deprecation",
        "-feature"
    )
)

def define(moduleName : String, artifact : String, dirName : String) = {
    Project(moduleName, file(s"module/${dirName}"))
        .settings(
            name := artifact
        ).settings(common : _*)
}

def flink(moduleName : String, flinkVersion : String) = {
    define(
        moduleName,
        s"elastic-reporter-flink-${flinkVersion}",
        moduleName
    ).settings(
        libraryDependencies += "org.apache.flink" % "flink-metrics-core" % flinkVersion % Provided,
        libraryDependencies += "org.apache.flink" % "flink-core" % flinkVersion % Provided,
        libraryDependencies += "org.apache.flink" %% "flink-runtime" % flinkVersion % Provided,
        // libraryDependencies += "xmlenc" % "xmlenc" % "0.52" % Provided,
        assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
    )
}

// ============================================================

lazy val cmn = define(
    "common",
    "elastic-reporter-common",
    "common"
).settings(
    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25" % Provided
)

lazy val flink14 = flink("flink14", "1.4.2").dependsOn(cmn)

lazy val examplejob = {

    val flinkDependencies = Seq(
        "flink-scala",
        "flink-streaming-scala"
    ) map { m =>
        "org.apache.flink" %% m % "1.4.2" % Provided
    }

    define("examplejob", "examplejob", "examplejob")
        .settings(
            assembly / mainClass := Some("org.example.Job"),
            libraryDependencies ++= flinkDependencies,
            // libraryDependencies += "org.apache.flink" % "flink-mterics-dropwizard" % "1.4.2",
            libraryDependencies += "org.apache.flink" % "flink-metrics-dropwizard" % "1.4.2" % Provided,
            Compile / run := Defaults.runTask(Compile / fullClasspath,
                Compile / run / mainClass,
                Compile / run / runner
            ).evaluated,
            Compile / run / fork := true,
            Global / cancelable := true,
            assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = false)
        )
}

lazy val root = (project in file(".")).aggregate(cmn, flink14, examplejob)

// ============================================================

/*
lazy val xjava = define("xjava", "xjava", "xjava")

lazy val base = define(
    "base",
    "elasticsearch-metric-reporter-base",
    "base"
).settings(
    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25" % Provided
).dependsOn(xjava)

lazy val flink14 = flink("flink14", "1.4.2")

lazy val flink12 = flink("flink12", "1.2.1")
    .settings(
        sourceDirectory := (sourceDirectory in flink14).value,
    )

lazy val newgen = define(
    "newgen",
    "esreporter-common",
    "newgen"
).settings(
    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25" % Provided
).dependsOn(xjava)

lazy val newgenflink14 = flink("ngflink", "1.4.2").dependsOn(newgen)
*/

// ============================================================