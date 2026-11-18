pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "JFPFinance"

include(":app")
include(":core")
include(":finance-engine")
include(":ai")
include(":integrations")
include(":reports")
include(":backup")
include(":data")
include(":security")
include(":ui-comum")
include(":ui-dashboard")
include(":ui-movimentacoes")
include(":ui-planejamento")
include(":ui-patrimonio")
include(":ui-assistente")
