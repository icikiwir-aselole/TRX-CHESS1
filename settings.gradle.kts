import org.gradle.api.initialization.resolve.RepositoriesMode
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "TRX-CHESS"
include(":app", ":core:common", ":chess", ":engine:api", ":engine:uci", ":engine:native", ":analysis:coordinator", ":position:api", ":data", ":security", ":overlay", ":automation", ":ui", ":network:api", ":diagnostics", ":benchmark")
