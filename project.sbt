sbtPlugin := false

name := "imce.third_party.scala_libraries"

moduleName := "imce.third_party.scala_libraries"

organization := "gov.nasa.jpl.imce"

homepage := Some(url("https://github.com/JPL-IMCE/imce.third_party.scala_libraries"))

organizationName := "JPL-IMCE"

organizationHomepage := Some(url("http://www.jpl.nasa.gov"))

git.remoteRepo := "git@github.com:JPL-IMCE/imce.third_party.scala_libraries.git"

startYear := Some(2015)

scmInfo := Some(ScmInfo(
  browseUrl = url("https://github.com/JPL-IMCE/imce.third_party.scala_libraries"),
  connection = "scm:"+git.remoteRepo.value))

developers := List(
  Developer(
    id="NicolasRouquette",
    name="Nicolas F. Rouquette",
    email="nicolas.f.rouquette@jpl.nasa.gov",
    url=url("https://github.com/NicolasRouquette")))

