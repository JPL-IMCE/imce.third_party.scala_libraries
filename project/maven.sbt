// Do not enable this because it breaks sbt-dependency-graph's ability to detect dependencies from
// aggregate *.zip artifacts to their aggregated *.jar artifacts.
// With maven resolver, SBT stores POM metadata in ~/.ivy2/maven-cache.
// Somehow, the IVY files for *.zip artifacts in ~/.ivy2/cache do not have dependencies to their aggregated artifacts.
// Since sbt-dependency-graph uses sbt's IVY APIs to construct the graph, the graph is incomplete!
//addMavenResolverPlugin
