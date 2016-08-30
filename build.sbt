import sbt.Keys._
import sbt._

import gov.nasa.jpl.imce.sbt._

import org.fusesource.scalate.TemplateEngine
import scala.util.matching.Regex

useGpg := true

updateOptions := updateOptions.value.withCachedResolution(true)

developers := List(
  Developer(
    id="NicolasRouquette",
    name="Nicolas F. Rouquette",
    email="nicolas.f.rouquette@jpl.nasa.gov",
    url=url("https://github.com/NicolasRouquette")))

val resourceArtifact = settingKey[Artifact]("Specifies the project's resource artifact")

def IMCEThirdPartyProject(projectName: String, location: String): Project =
  Project(projectName, file("."))
    .enablePlugins(IMCEGitPlugin)
    .enablePlugins(IMCEReleasePlugin)
    .settings(IMCEReleasePlugin.packageReleaseProcessSettings)
    .settings(
      IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
      IMCEKeys.licenseYearOrRange := "2015-2016",
      IMCEKeys.organizationInfo := IMCEPlugin.Organizations.thirdParty,
      git.baseVersion := Versions.version,
      scalaVersion := Versions.scala_version,
      projectID := {
        val previous = projectID.value
        previous.extra(
          "build.date.utc" -> buildUTCDate.value,
          "artifact.kind" -> "third_party.aggregate.libraries")
      }

    )
    .settings(

      // disable publishing the main jar produced by `package`
      publishArtifact in(Compile, packageBin) := false,

      // disable publishing the main API jar
      publishArtifact in(Compile, packageDoc) := false,

      // disable publishing the main sources jar
      publishArtifact in(Compile, packageSrc) := false,

      // disable publishing the jar produced by `test:package`
      publishArtifact in(Test, packageBin) := false,

      // disable publishing the test API jar
      publishArtifact in(Test, packageDoc) := false,

      // disable publishing the test sources jar
      publishArtifact in(Test, packageSrc) := false,

      // name the '*-resource.zip' in the same way as other artifacts
      com.typesafe.sbt.packager.Keys.packageName in Universal :=
        normalizedName.value + "_" + scalaBinaryVersion.value + "-" + version.value,

      resourceArtifact := Artifact((name in Universal).value, "zip", "zip", Some("resource"), Seq(), None, Map()),

      artifacts += resourceArtifact.value,

      // contents of the '*-resource.zip' to be produced by 'universal:packageBin'
      mappings in Universal <++= (
        appConfiguration,
        classpathTypes,
        update,
        streams) map {
        (appC, cpT, up, s) =>

          def getFileIfExists(f: File, where: String)
          : Option[(File, String)] =
            if (f.exists()) Some((f, s"$where/${f.getName}")) else None

          val ivyHome: File =
            Classpaths
              .bootIvyHome(appC)
              .getOrElse(sys.error("Launcher did not provide the Ivy home directory."))

          val libDir = location + "/lib/"
          val srcDir = location + "/lib.sources/"
          val docDir = location + "/lib.javadoc/"

          s.log.info(s"====== $projectName =====")

          val providedOrganizationArtifacts: Set[String] = (for {
            cReport <- up.configurations
            if Configurations.Provided.name == cReport.configuration
            oReport <- cReport.details
            mReport <- oReport.modules
            (artifact, file) <- mReport.artifacts
            if "jar" == artifact.extension
          } yield {
            s.log.info(s"provided: ${oReport.organization}, ${file.name}")
            s"{oReport.organization},${oReport.name}"
          }).to[Set]

          val fileArtifacts = for {
            cReport <- up.configurations
            if Configurations.Compile.name == cReport.configuration
            oReport <- cReport.details
            organizationArtifactKey = s"{oReport.organization},${oReport.name}"
            if !providedOrganizationArtifacts.contains(organizationArtifactKey)
            mReport <- oReport.modules
            (artifact, file) <- mReport.artifacts
            if !mReport.evicted && "jar" == artifact.extension
          } yield (oReport.organization, oReport.name, file, artifact)

          val fileArtifactsByType = fileArtifacts.groupBy { case (_, _, _, a) =>
            a.`classifier`.getOrElse(a.`type`)
          }
          val jarArtifacts = fileArtifactsByType("jar").map { case (o, _, jar, _) => o -> jar }.to[Set].to[Seq].sortBy { case (o, jar) => s"$o/${jar.name}" }
          val srcArtifacts = fileArtifactsByType("sources").map { case (o, _, jar, _) => o -> jar }.to[Set].to[Seq].sortBy { case (o, jar) => s"$o/${jar.name}" }
          val docArtifacts = fileArtifactsByType("javadoc").map { case (o, _, jar, _) => o -> jar }.to[Set].to[Seq].sortBy { case (o, jar) => s"$o/${jar.name}" }

          val jars = jarArtifacts.map { case (o, jar) =>
            s.log.info(s"* jar: $o/${jar.name}")
            jar -> (libDir + jar.name)
          }
          val srcs = srcArtifacts.map { case (o, jar) =>
            s.log.info(s"* src: $o/${jar.name}")
            jar -> (srcDir + jar.name)
          }
          val docs = docArtifacts.map { case (o, jar) =>
            s.log.info(s"* doc: $o/${jar.name}")
            jar -> (docDir + jar.name)
          }

          jars ++ srcs ++ docs
      },

      extractArchives := {},

      artifacts <+= (name in Universal) { n => Artifact(n, "zip", "zip", Some("resource"), Seq(), None, Map()) },
      packagedArtifacts <+= (packageBin in Universal, name in Universal) map { (p, n) =>
        Artifact(n, "zip", "zip", Some("resource"), Seq(), None, Map()) -> p
      }
    )

lazy val scalaLibs = IMCEThirdPartyProject("scala-libraries", "scalaLibs")
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-library" % scalaVersion.value %
      "compile" withSources() withJavadoc(),

      "org.scala-lang" % "scala-compiler" % scalaVersion.value %
      "compile" withSources() withJavadoc(),

      "org.scala-lang" % "scala-reflect" % scalaVersion.value %
      "compile" withJavadoc() withJavadoc(),

      "org.scala-lang" % "scalap" % scalaVersion.value %
      "compile" withSources() withJavadoc(),

      "org.scala-lang.modules" %% "scala-xml" % Versions.scala_xml_version %
      "compile" withSources() withJavadoc(),

      "org.scala-lang.modules" %% "scala-parser-combinators" % Versions.scala_parser_combinators_version %
      "compile" withSources() withJavadoc(),

      "org.scala-lang.modules" %% "scala-swing" % Versions.scala_swing_version %
      "compile" withSources() withJavadoc(),

      "org.scala-lang.modules" %% "scala-java8-compat" % Versions.scala_java8_compat_version %
        "compile" withSources() withJavadoc(),

      "org.scala-lang.plugins" %% "scala-continuations-library" % Versions.scala_continuations_version %
      "compile" withSources() withJavadoc()
   ))
